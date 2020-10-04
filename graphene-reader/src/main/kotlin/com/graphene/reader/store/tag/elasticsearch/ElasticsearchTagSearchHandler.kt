package com.graphene.reader.store.tag.elasticsearch

import com.graphene.reader.service.tag.TagSearchHandler
import com.graphene.reader.store.key.elasticsearch.handler.ElasticsearchClient
import com.graphene.reader.store.tag.elasticsearch.optimizer.ElasticsearchTagSearchQueryOptimizer
import com.graphene.reader.store.tag.elasticsearch.optimizer.TagSearchTarget
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component

/**
 *
 * @author jerome89
 * @since 1.6.0
 */
@Component
class ElasticsearchTagSearchHandler(
  private val elasticsearchClient: ElasticsearchClient,
  private val elasticsearchTagSearchQueryOptimizer: ElasticsearchTagSearchQueryOptimizer
) : TagSearchHandler {

  private val logger = LogManager.getLogger(javaClass)

  override fun getTags(tagInput: String, tagExpressions: MutableList<String>, from: Long, to: Long): List<String> {
    return if (tagExpressions.isNullOrEmpty()) {
      return listOf(NAME_FIELD)
    } else {
      val tagsToExclude = mutableListOf<String>()
      for (tagExpression in tagExpressions) {
        val head = tagExpression.split(EQUALS)[0]
        if (head.endsWith('!')) {
          tagsToExclude.add(head.substring(0, head.length - 1))
        } else {
          tagsToExclude.add(head)
        }
      }
      if (! tagsToExclude.contains(NAME_FIELD)) {
        return listOf(NAME_FIELD)
      } else {
        val tagSearchTarget = TagSearchTarget(tagKey = TAGS_FIELD, tagValue = tagInput, tagExpressions = tagExpressions)
        getTagKeys(tagSearchTarget, from, to, tagsToExclude)
      }
    }
  }

  override fun getTagValues(tagValueInput: String?, tagExpressions: MutableList<String>, tag: String, from: Long, to: Long): List<String> {
    val tagValues = mutableSetOf<String>()
    try {
      val response = elasticsearchClient.query(
        elasticsearchTagSearchQueryOptimizer.optimize(TagSearchTarget(tagKey = tag, tagValue = tagValueInput, tagExpressions = tagExpressions)),
        from,
        to,
        tag
      )
      tagValues.addAll(response.tagSearchResults)
    } catch (e: Exception) {
      logger.warn("Failed to find Tag Values: {tag: $tag}, {tagValueInput: $tagValueInput}, {tagExpressions: $tagExpressions}")
    }
    if (tagValues.size > 0) {
      tagValues.add("*")
    }
    return tagValues.sorted()
  }

  private fun getTagKeys(
    tagSearchTarget: TagSearchTarget,
    from: Long,
    to: Long,
    tagsToExclude: List<String>
  ): List<String> {
    val result = mutableSetOf<String>()
    try {
      val response = elasticsearchClient.query(
        elasticsearchTagSearchQueryOptimizer.optimize(tagSearchTarget),
        from,
        to,
        TAGS_FIELD
      )
      for (tag in response.tagSearchResults) {
        if (! tagsToExclude.contains(tag)) {
          result.add(tag)
        }
      }
    } catch (e: Exception) {
      logger.warn("Failed to find Tag Keys! e => ${e.message}")
    }
    return result.sorted()
  }

  companion object {
    internal val TAGS_FIELD = "@tags"
    internal val NAME_FIELD = "@name"
    internal val EQUALS = "="
  }
}
