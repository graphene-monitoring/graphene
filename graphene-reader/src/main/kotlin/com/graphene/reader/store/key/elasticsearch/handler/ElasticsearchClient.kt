package com.graphene.reader.store.key.elasticsearch.handler

import com.graphene.reader.exceptions.TooMuchDataExpectedException
import com.graphene.reader.store.key.elasticsearch.property.IndexProperty
import com.graphene.reader.store.key.elasticsearch.selector.KeySelector
import com.graphene.reader.store.key.elasticsearch.selector.RollingKeySelector
import java.util.Objects
import javax.annotation.PreDestroy
import org.apache.commons.lang3.StringUtils
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.support.IndicesOptions
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetMappingsRequest
import org.elasticsearch.client.indices.GetMappingsResponse
import org.elasticsearch.cluster.metadata.MappingMetaData
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ElasticsearchClient(
  private val client: RestHighLevelClient,
  private val indexProperty: IndexProperty
) {
  val keySelector: KeySelector = RollingKeySelector(indexProperty.keySelectorProperty())
  val maxTagResults: Int = indexProperty.maxTagResults()

  fun query(query: QueryBuilder, from: Long, to: Long, tagAggregationField: String = ""): Response {
    val searchSourceBuilder = SearchSourceBuilder()
    val selectedIndex = keySelector.select(indexProperty.index()!!, indexProperty.tenant(), from, to)
    val searchRequest = SearchRequest(*selectedIndex.toTypedArray())

    searchRequest.indicesOptions(IndicesOptions.fromOptions(true, true, true, false))
    searchSourceBuilder.query(query)

    if (StringUtils.isNotBlank(tagAggregationField)) {
      searchSourceBuilder.fetchSource(arrayOf(tagAggregationField), null)
      searchSourceBuilder.aggregation(AggregationBuilders.terms(AGGREGATION_KEY).field(tagAggregationField).size(maxTagResults))
      searchSourceBuilder.size(AGGREGATION_RESULT_SIZE)
    } else {
      searchRequest.scroll(TimeValue(indexProperty.timeout().toLong()))
      searchSourceBuilder.size(indexProperty.scroll())
    }

    searchRequest.source(searchSourceBuilder)
    val searchResponse = client.search(searchRequest, RequestOptions.DEFAULT)

    return Response.of(searchResponse)
  }

  fun getFieldMapping(from: Long, to: Long): MappingsResponse {
    val getMappingsRequest = GetMappingsRequest()
    val selectedIndex = keySelector.select(indexProperty.index()!!, indexProperty.tenant(), from, to)
    getMappingsRequest.indices(*selectedIndex.toTypedArray())
    getMappingsRequest.indicesOptions(
      IndicesOptions.fromOptions(
        true, true, true, false
      )
    )
    val response = client.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT)

    return MappingsResponse.of(response)
  }

  fun searchScroll(response: Response): Response {
    val scrollRequest = SearchScrollRequest(response.scrollId)
    scrollRequest.scroll(TimeValue.timeValueMillis(indexProperty.timeout().toLong()))

    val searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT)
    return Response.of(searchResponse)
  }

  fun clearScroll(scrollIds: List<String>) {
    val clearScrollRequest = ClearScrollRequest()
    clearScrollRequest.scrollIds(scrollIds)

    val clearScroll = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT)
    if (!clearScroll.isSucceeded) {
      logger.warn("[$scrollIds] scroll clears is failed.")
    }
  }

  private fun throwIfExceededMaxPaths(response: SearchResponse) {
    // if total hits exceeds maximum - abort right away returning empty array
    if (response.hits.totalHits > indexProperty.maxPaths()) {
      logger.debug("Total number map paths exceeds the limit: " + response.hits.totalHits)
      throw TooMuchDataExpectedException(
        "Total number map paths exceeds the limit: " +
          response.hits.totalHits +
          " (the limit is " +
          indexProperty.maxPaths() +
          ")")
    }
  }

  @PreDestroy
  fun shutdown() {
    logger.info("Shutting down elasticsearch client")
    client.close()
  }

  data class MappingsResponse(
    val mappings: MutableCollection<MappingMetaData>
  ) {
    companion object {
      fun of(response: GetMappingsResponse): MappingsResponse {
        return MappingsResponse(
          response.mappings().values
        )
      }
    }
  }

  data class Response(
    val scrollId: String,
    val hits: SearchHits,
    val size: Int,
    val tagSearchResults: Set<String>
  ) {
    companion object {
      fun of(response: SearchResponse): Response {
        val tagSearchResults = mutableSetOf<String>()
        if (Objects.nonNull(response.aggregations) &&
          Objects.nonNull(response.aggregations.asMap[AGGREGATION_KEY]) &&
          ! (response.aggregations.asMap[AGGREGATION_KEY] as ParsedStringTerms).buckets.isNullOrEmpty()) {
          for (bucket in (response.aggregations.asMap[AGGREGATION_KEY] as ParsedStringTerms).buckets) {
            tagSearchResults.add(bucket.keyAsString)
          }
        }
        return Response(
          response.scrollId ?: EMPTY_SCROLL_ID,
          response.hits,
          response.hits.hits.size,
          tagSearchResults
        )
      }
    }
  }

  companion object {
    internal const val EMPTY_SCROLL_ID = ""
    internal const val AGGREGATION_KEY = "AGGREGATION"
    internal const val AGGREGATION_RESULT_SIZE = 0
    internal val logger = LoggerFactory.getLogger(ElasticsearchClient::class.java)
  }
}
