package com.graphene.writer.store.key.elasticsearch.handler

import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.Source
import com.graphene.writer.store.KeyStoreHandlerProperty
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClientFactory
import com.graphene.writer.store.key.elasticsearch.GrapheneIndexRequest
import java.util.Collections
import java.util.TreeMap
import org.apache.logging.log4j.LogManager
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory

/**
 * @author jerome89
 * @since 1.6.0
 */
class TagBasedKeyStoreHandler(
  private val elasticsearchClientFactory: ElasticsearchClientFactory,
  val property: KeyStoreHandlerProperty
) : AbstractElasticsearchKeyStoreHandler(elasticsearchClientFactory, property) {

  private val log = LogManager.getLogger(javaClass)

  override fun isProcessable(metric: GrapheneMetric): Boolean {
    if (Source.GRAPHITE == metric.source) {
      log.warn("Please change store handler to simple or index-based key store handler because TagBasedKeyStoreHandler does not support the old graphite format.")
      return false
    }
    return true
  }

  override fun mapToGrapheneIndexRequests(metric: GrapheneMetric?): List<GrapheneIndexRequest> {
    if (Source.GRAPHITE == metric!!.source) {
      log.warn("Please change store handler to simple or index-based key store handler because TagBasedKeyStoreHandler does not support the old graphite format.")
      return Collections.emptyList<GrapheneIndexRequest>()
    }
    val grapheneIndexRequests = mutableListOf<GrapheneIndexRequest>()
    grapheneIndexRequests.add(GrapheneIndexRequest("${metric!!.id}", source(metric.tags, metric), metric.timestampMillis()))
    return grapheneIndexRequests
  }

  override fun templateSource(): String = SOURCE

  override fun templateName(): String = TEMPLATE_NAME

  private fun source(tags: TreeMap<String, String>, grapheneMetric: GrapheneMetric): XContentBuilder {
    val source = XContentFactory.jsonBuilder()
      .startObject()

    for (tag in tags) {
      source.field(tag.key, tag.value)
    }

    source.field(TAGS_FIELD, grapheneMetric.tags.keys)
    source.field(NAME_FIELD, grapheneMetric.metricKey())

    return source.endObject()
  }

  companion object {
    const val TEMPLATE_NAME = "tag-based-key-path-template"
    const val TAGS_FIELD = "@tags"
    const val NAME_FIELD = "@name"
    const val SOURCE = """
      {
        "settings": {
          "number_of_replicas": 0,
          "number_of_shards": 5,
          "auto_expand_replicas": "0-1"
        },
        "mappings": {
          "path": {
            "dynamic_templates": [
              {
                "else": {
                  "match": "*",
                  "unmatch": "depth",
                  "match_mapping_type": "string",
                  "mapping": {
                    "type": "keyword"
                  }
                }
              }
            ]
          }
        }
      }
    """
  }
}
