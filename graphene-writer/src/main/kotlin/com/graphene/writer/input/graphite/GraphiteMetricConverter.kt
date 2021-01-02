package com.graphene.writer.input.graphite

import com.google.common.collect.Maps
import com.graphene.common.utils.HashUtils.sha512
import com.graphene.reader.utils.MetricRule
import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.MetricConverter
import com.graphene.writer.input.Source
import java.util.Collections
import java.util.TreeMap
import org.apache.commons.lang3.StringUtils

class GraphiteMetricConverter : MetricConverter<GraphiteMetric>, GraphiteAware {

  override fun convert(metric: GraphiteMetric): List<GrapheneMetric> {
    val nodes = convertNodes(metric)
    val tags = convertTags(metric)
    val source = judgeSource(tags)
    var key = getGraphiteKey(nodes)
    if (Source.GRAPHITE_TAG == source) {
      key = appendTagsToId(key, tags)
    }

    val grapheneMetric = GrapheneMetric(
      source = source,
      id = key.sha512(),
      key = key,
      meta = Collections.emptyMap(),
      tags = tags,
      nodes = nodes,
      value = metric.value,
      timestampSeconds = metric.timestamp
    )

    return listOf(grapheneMetric)
  }

  private fun judgeSource(tags: TreeMap<String, String>): Source {
    return if (tags.isEmpty()) Source.GRAPHITE
    else Source.GRAPHITE_TAG
  }

  private fun convertTags(metric: GraphiteMetric): TreeMap<String, String> {
    val keyParts = metric.key.split(SEMICOLON)
    if (keyParts.size <= 1) {
      return Maps.newTreeMap()
    }
    val tags = TreeMap<String, String>()
    for (i in 1 until keyParts.size) {
      val tagExpressions = keyParts[i].split(EQUALS)
      if (tagExpressions.size < 2 || StringUtils.isBlank(tagExpressions[0]) || StringUtils.isBlank(tagExpressions[1])) {
        continue
      }
      tags[tagExpressions[0]] = tagExpressions[1]
    }
    return tags
  }

  private fun appendTagsToId(id: String, tags: TreeMap<String, String>): String {
    var tagAppendedId = id
    for (tag in tags) {
      tagAppendedId += ";${tag.key}=${tag.value}"
    }
    return tagAppendedId
  }

  private fun convertNodes(metric: GraphiteMetric): TreeMap<String, String> {
    val separatedMetricKey = metric.key.split(SEMICOLON)[0].split(DELIMITER)

    val nodes = TreeMap<String, String>()
    for ((index, key) in separatedMetricKey.withIndex()) {
      nodes["$index"] = MetricRule.generate(key)
    }
    return nodes
  }

  companion object {
    private const val EQUALS = "="
    private const val SEMICOLON = ";"
    private const val DELIMITER = "."
  }
}
