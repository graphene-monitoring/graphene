package com.graphene.writer.store.key.elasticsearch.handler

import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.Source
import com.graphene.writer.store.KeyStoreHandlerProperty
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClientFactory
import com.graphene.writer.store.key.elasticsearch.GrapheneIndexRequest
import java.util.TreeMap
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory

/**
 * @author Andrei Ivanov
 * @author dark
 * @author jerome89
 */
class SimpleKeyStoreHandler(
  elasticsearchClientFactory: ElasticsearchClientFactory,
  val property: KeyStoreHandlerProperty
) : AbstractElasticsearchKeyStoreHandler(elasticsearchClientFactory, property) {

  override fun isProcessable(metric: GrapheneMetric): Boolean {
    return metric.source == Source.GRAPHITE
  }

  override fun mapToGrapheneIndexRequests(metric: GrapheneMetric): List<GrapheneIndexRequest> {
    val grapheneIndexRequests = mutableListOf<GrapheneIndexRequest>()
    val nodes = metric.nodes
    val graphiteKeySb = StringBuilder()

    for (indexedValue in nodes.entries.withIndex()) {
      val depth = indexedValue.index
      if (graphiteKeySb.toString().isNotEmpty()) {
        graphiteKeySb.append(".")
      }
      graphiteKeySb.append(indexedValue.value.value)
      try {
        val graphiteKeyPart = graphiteKeySb.toString()
        val id = "${metric.getTenant()}_$graphiteKeyPart"
        grapheneIndexRequests.add(GrapheneIndexRequest(id, source(metric.getTenant(), graphiteKeyPart, depth, isLeaf(depth, nodes)), metric.timestampMillis()))
      } catch (e: Exception) {
        throw IllegalStateException("Invokes illegal state in map to index requests", e)
      }
    }

    return grapheneIndexRequests
  }

  override fun templateName(): String = TEMPLATE_NAME

  override fun templateSource(): String = SOURCE

  private fun source(tenant: String, graphiteKeyPart: String, depth: Int, leaf: Boolean): XContentBuilder {
    return XContentFactory.jsonBuilder()
      .startObject()
      .field(TENANT, tenant)
      .field(PATH, graphiteKeyPart)
      .field(DEPTH, depth + 1)
      .field(LEAF, leaf)
      .endObject()
  }

  private fun isLeaf(depth: Int, parts: TreeMap<String, String>) = depth == parts.size - 1

  companion object {
    const val TENANT = "tenant"
    const val DEPTH = "depth"
    const val LEAF = "leaf"
    const val PATH = "path"

    const val TEMPLATE_NAME = "simple-key-path-template"
    const val SOURCE =
      """
      {
        "settings": {
          "number_of_replicas": 0,
          "number_of_shards": 5,
          "auto_expand_replicas": "0-1"
        },
        "mappings": {
          "path": {
            "properties": {
              "path": {
                "type": "keyword"
              },
              "depth": {
                "type": "long"
              },
              "leaf": {
                "type": "boolean"
              },
              "tenant": {
                "type": "keyword"
              }
            }
          }
        }
      }
    """
  }
}
