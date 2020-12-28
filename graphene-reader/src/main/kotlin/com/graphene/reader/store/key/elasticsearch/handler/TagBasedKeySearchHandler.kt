package com.graphene.reader.store.key.elasticsearch.handler

import com.graphene.common.HierarchyMetricPaths
import com.graphene.common.beans.Path
import com.graphene.reader.service.index.KeySearchHandler
import com.graphene.reader.store.tag.elasticsearch.optimizer.ElasticsearchTagSearchQueryOptimizer
import com.graphene.reader.store.tag.elasticsearch.optimizer.TagSearchTarget
import java.util.Objects
import org.apache.logging.log4j.LogManager
import org.elasticsearch.index.query.QueryBuilder

/**
 *
 * @author jerome89
 * @since 1.6.0
 */
class TagBasedKeySearchHandler(
  private val elasticsearchClient: ElasticsearchClient,
  private val elasticsearchTagSearchQueryOptimizer: ElasticsearchTagSearchQueryOptimizer
) : KeySearchHandler {

  private val log = LogManager.getLogger(javaClass)

  override fun getPaths(tenant: String, pathExpressions: MutableList<String>, from: Long, to: Long): List<Path> {
    log.info("Search without tags is not supported on TagBasedKeySearchHandler.")
    return emptyList()
  }

  override fun getPathsByTags(tenant: String, tagExpressions: List<String>, from: Long, to: Long): List<Path> {
    val result = mutableSetOf<Path>()
    val queryBuilder = elasticsearchTagSearchQueryOptimizer.optimize(TagSearchTarget(tagExpressions = tagExpressions))
    queryThenAppend(result, queryBuilder, from, to)
    return result.sortedWith(compareBy { it.path })
  }

  private fun queryThenAppend(result: MutableSet<Path>, queryBuilder: QueryBuilder, from: Long, to: Long) {
    val scrollIds = mutableListOf<String>()
    try {
      var response = elasticsearchClient.query(queryBuilder, from * 1000, to * 1000)

      while (response.hits.hits.isNotEmpty()) {
        for (hit in response.hits) {
          val sources = hit.sourceAsMap as Map<String, *>
          if (! sources.containsKey(KEY)) {
            continue
          }
          val path = Path(sources[KEY].toString())
          if (Objects.nonNull(hit.sourceAsMap)) {
            for (source in sources) {
              if (! INTERNAL_KEYS.contains(source.key)) {
                path.addTag(source.key, source.value.toString())
              }
            }
          }
          result.add(path)
        }

        response = elasticsearchClient.searchScroll(response)
        scrollIds.add(response.scrollId)
      }

      if (scrollIds.isNotEmpty()) {
        elasticsearchClient.clearScroll(scrollIds)
      }
    } catch (e: Exception) {
      log.warn("Search request is failed: " + e.message)
    }
  }

  override fun getHierarchyMetricPaths(tenant: String, pathExpression: String, from: Long, to: Long): MutableCollection<HierarchyMetricPaths.HierarchyMetricPath> {
    log.info("Hierarchical search is not supported on TagBasedKeySearchHandler.")
    return mutableListOf()
  }

  companion object {
    const val KEY = "@key"
    const val NAME = "@name"
    const val TAGS = "@tags"
    val INTERNAL_KEYS = setOf(KEY, NAME, TAGS)
  }
}
