package com.graphene.writer.store

import com.graphene.common.key.RotationProperty
import com.graphene.common.rule.GrapheneRules
import com.graphene.common.store.data.cassandra.CassandraFactory
import com.graphene.common.store.data.cassandra.property.CassandraDataHandlerProperty
import com.graphene.writer.error.Errors
import com.graphene.writer.store.data.cassandra.handler.OffsetBasedDataStoreHandler
import com.graphene.writer.store.data.cassandra.handler.SimpleDataStoreHandler
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClientFactory
import com.graphene.writer.store.key.elasticsearch.handler.IndexBasedKeyStoreHandler
import com.graphene.writer.store.key.elasticsearch.handler.LoggingKeyStoreHandler
import com.graphene.writer.store.key.elasticsearch.handler.SimpleKeyStoreHandler
import com.graphene.writer.store.key.elasticsearch.handler.TagBasedKeyStoreHandler
import com.graphene.writer.store.key.elasticsearch.property.ElasticsearchKeyStoreHandlerProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component

/**
 * @author dark
 * @author jerome89
 *
 * @since 1.1.0
 */
@Component
class StoreHandlerFactory(
  var dataStoreHandlersProperty: DataStoreHandlersProperty,
  var keyStoreHandlersProperty: KeyStoreHandlersProperty,
  var applicationContext: GenericApplicationContext
) {

  private val log = LoggerFactory.getLogger(StoreHandlerFactory::class.java)

  fun keyStoreHandlers(): List<KeyStoreHandler> {
    val keyStoreHandlers = mutableListOf<KeyStoreHandler>()
    for (handler in keyStoreHandlersProperty.handlers) {
      log.info("KeyStoreHandlerFactory Key : ${handler.key} / Value : ${handler.value}")
      val keyStoreHandler = keyStoreHandlersProperty.handlers[handler.key]!!
      if (keyStoreHandler.enabled) {
        when (val storeHandlerType = keyStoreHandler.type) {
          "LoggingKeyStoreHandler" -> keyStoreHandlers.add(createLoggingKeyStoreHandler(handler.key))
          "SimpleKeyStoreHandler" -> keyStoreHandlers.add(createSimpleKeyStoreHandler(handler.key, handler.value))
          "IndexBasedKeyStoreHandler" -> keyStoreHandlers.add(createIndexBasedKeyStoreHandler(handler.key, handler.value))
          "TagBasedKeyStoreHandler" -> keyStoreHandlers.add(createTagBasedKeyStoreHandler(handler.key, handler.value))
          else -> throw Errors.UNSUPPORTED_KEY_STORE_HANDLER_EXCEPTION.exception("$storeHandlerType is not supported!")
        }
      }
    }
    return keyStoreHandlers
  }

  private fun createIndexBasedKeyStoreHandler(handlerId: String, keyStoreHandlerProperty: KeyStoreHandlerProperty): KeyStoreHandler {
    log.info("StoreHandlerFactory registers IndexBasedKeyStoreHandler : $handlerId")

    return IndexBasedKeyStoreHandler(
      applicationContext.getBean("elasticsearchClientFactory") as ElasticsearchClientFactory,
      keyStoreHandlerProperty
    )
  }

  private fun createTagBasedKeyStoreHandler(handlerId: String, keyStoreHandlerProperty: KeyStoreHandlerProperty): KeyStoreHandler {
    log.info("StoreHandlerFactory registers TagBasedKeyStoreHandler : $handlerId")

    return TagBasedKeyStoreHandler(
      applicationContext.getBean("elasticsearchClientFactory") as ElasticsearchClientFactory,
      keyStoreHandlerProperty
    )
  }

  private fun createSimpleKeyStoreHandler(handlerId: String, keyStoreHandlerProperty: KeyStoreHandlerProperty): KeyStoreHandler {
    log.info("StoreHandlerFactory registers SimpleKeyStoreHandler : $handlerId")

    return SimpleKeyStoreHandler(
      applicationContext.getBean("elasticsearchClientFactory") as ElasticsearchClientFactory,
      keyStoreHandlerProperty
    )
  }

  fun dataStoreHandlers(): List<DataStoreHandler> {
    val dataStoreHandlers = mutableListOf<DataStoreHandler>()
    for (handler in dataStoreHandlersProperty.handlers) {
      log.info("DataStoreHandlerFactory Key : ${handler.key} / Value : ${handler.value}")
      val dataStoreHandler = dataStoreHandlersProperty.handlers[handler.key]!!
      if (dataStoreHandler.enabled) {
        when (val storeHandlerType = dataStoreHandler.type) {
          "SimpleDataStoreHandler" -> dataStoreHandlers.add(createSimpleDataStoreHandler(handler.key, handler.value))
          "OffsetBasedDataStoreHandler" -> dataStoreHandlers.add(createOffsetBasedDataStoreHandler(handler.key, handler.value))
          else -> throw Errors.UNSUPPORTED_DATA_STORE_HANDLER_EXCEPTION.exception("$storeHandlerType is not supported!")
        }
      }
    }
    return dataStoreHandlers
  }

  private fun createSimpleDataStoreHandler(handlerId: String, dataStoreHandlerProperty: DataStoreHandlerProperty): DataStoreHandler {
    log.info("StoreHandlerFactory registers SimpleDataStoreHandler : $handlerId")

    return SimpleDataStoreHandler(
      applicationContext.getBean("cassandraFactory") as CassandraFactory,
      dataStoreHandlerProperty
    )
  }

  private fun createOffsetBasedDataStoreHandler(handlerId: String, dataStoreHandlerProperty: DataStoreHandlerProperty): DataStoreHandler {
    log.info("StoreHandlerFactory registers OffsetBasedDataStoreHandler : $handlerId")

    return OffsetBasedDataStoreHandler(
      applicationContext.getBean("cassandraFactory") as CassandraFactory,
      dataStoreHandlerProperty
    )
  }

  private fun createLoggingKeyStoreHandler(handlerId: String): KeyStoreHandler {
    log.info("StoreHandlerFactory Register loggingKeyStoreHandler : $handlerId")

    return LoggingKeyStoreHandler()
  }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "graphene.writer.store.data")
data class DataStoreHandlersProperty(
  var handlers: Map<HandlerId, DataStoreHandlerProperty>
)

data class DataStoreHandlerProperty(
  val enabled: Boolean = false,
  var type: String,
  var tenant: String = GrapheneRules.DEFAULT_TENANT,
  var ttl: Int = 0,
  var rollup: Int = 60,
  var keyspace: String,
  var columnFamily: String,
  var bucketSize: Short,
  var property: CassandraDataHandlerProperty
)

@ConstructorBinding
@ConfigurationProperties(prefix = "graphene.writer.store.key")
data class KeyStoreHandlersProperty(
  var handlers: Map<HandlerId, KeyStoreHandlerProperty>
)

data class KeyStoreHandlerProperty(
  val enabled: Boolean,
  val type: String,
  var tenant: String = GrapheneRules.DEFAULT_TENANT,
  var rotation: RotationProperty = RotationProperty(),
  var property: ElasticsearchKeyStoreHandlerProperty = ElasticsearchKeyStoreHandlerProperty()
)

typealias HandlerId = String
