package com.graphene.writer.store.key.elasticsearch.handler

import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.Source
import com.graphene.writer.store.KeyStoreHandlerProperty
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClientFactory
import com.graphene.writer.store.key.elasticsearch.GrapheneIndexRequest
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory

/**
 *
 * @author dark
 * @authot jerome89
 * @since 1.0.0
 */
class IndexBasedKeyStoreHandler(
  private val elasticsearchClientFactory: ElasticsearchClientFactory,
  val property: KeyStoreHandlerProperty
) : AbstractElasticsearchKeyStoreHandler(elasticsearchClientFactory, property) {

  override fun isProcessable(metric: GrapheneMetric): Boolean {
    return metric.source == Source.GRAPHITE
  }

  override fun mapToGrapheneIndexRequests(metric: GrapheneMetric): List<GrapheneIndexRequest> {
    return mutableListOf(GrapheneIndexRequest(metric.id, source(metric), metric.timestampMillis()))
  }

  override fun templateSource(): String = SOURCE

  override fun templateName(): String = TEMPLATE_NAME

  private fun source(metric: GrapheneMetric): XContentBuilder {
    val nodes = metric.nodes

    val source = XContentFactory.jsonBuilder()
      .startObject()
      .field(DEPTH, nodes.size)
      .field(LEAF, true)

    for (node in nodes) {
      source.field(node.key, node.value)
    }

    return source.endObject()
  }

  companion object {
    const val DEPTH = "depth"
    const val LEAF = "leaf"

    const val TEMPLATE_NAME = "index-based-key-path-template"
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
            "dynamic_templates": [
              {
                "leaf": {
                  "match": "leaf",
                  "match_mapping_type": "boolean",
                  "mapping": {
                    "type": "boolean"
                  }
                }
              },
              {
                "depth": {
                  "match": "depth",
                  "match_mapping_type": "long",
                  "mapping": {
                    "type": "integer"
                  }
                }
              },
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
