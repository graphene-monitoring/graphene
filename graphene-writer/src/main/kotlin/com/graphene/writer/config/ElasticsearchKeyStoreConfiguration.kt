package com.graphene.writer.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties

import java.util.ArrayList
import javax.annotation.PostConstruct

/**
 * @author Andrei Ivanov
 */
@ConfigurationProperties(prefix = "graphene.writer.store.key")
class ElasticsearchKeyStoreConfiguration {

  var clusterName: String? = null
  var index: String? = null
  var type: String? = null
  var isCache: Boolean = false
  var expire: Long = 0
  var cluster: List<String> = ArrayList()
  var port: Int = 0
  var bulk: IndexBulkConfiguration? = null

  @PostConstruct
  fun init() {
    logger.info("Load Graphene elasticsearchKeyStore configuration : {}", toString())
  }

  override fun toString(): String {
    return "ElasticsearchKeyStoreConfiguration{" +
      "clusterName=$clusterName" +
      ", index=$index" +
      ", type=$type" +
      ", cache=$isCache" +
      ", expire=$expire" +
      ", cluster=$cluster" +
      ", port=$port" +
      ", bulk=$bulk}"
  }

  companion object {

    private val logger = LoggerFactory.getLogger(ElasticsearchKeyStoreConfiguration::class.java)
  }
}
