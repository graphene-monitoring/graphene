package com.graphene.writer.store.key.elasticsearch.property

import com.graphene.writer.config.IndexBulkConfiguration
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory

class ElasticsearchKeyStoreHandlerProperty(
  var enabled: Boolean = false,
  var clusterName: String = "graphene",
  var templateIndexPattern: String = "metric*",
  var index: String = "metric",
  var type: String = "path",
  var cluster: List<String> = arrayListOf(),
  var port: Int = 9200,
  var userName: String? = "",
  var userPassword: String? = "",
  var protocol: String = "http",
  var bulk: IndexBulkConfiguration = IndexBulkConfiguration(),
  var cacheExpireIntervalInSeconds: Long = 300L,
  var targetProcessTime: Long = 30_000L
) {

  @PostConstruct
  fun init() {
    logger.info("Load Graphene ElasticsearchKeyStoreHandlerProperty : {}", toString())
  }

  override fun toString(): String {
    return "ElasticsearchKeyStoreHandlerProperty(enabled=$enabled, clusterName='$clusterName', index='$index', type='$type', cluster=$cluster, port=$port, protocol=$protocol, bulk=$bulk)"
  }

  companion object {
    private val logger = LoggerFactory.getLogger(ElasticsearchKeyStoreHandlerProperty::class.java)
  }
}
