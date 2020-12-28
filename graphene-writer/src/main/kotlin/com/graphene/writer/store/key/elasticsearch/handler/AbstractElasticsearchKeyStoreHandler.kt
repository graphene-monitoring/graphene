package com.graphene.writer.store.key.elasticsearch.handler

import com.graphene.common.utils.DateTimeUtils
import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.store.KeyStoreHandler
import com.graphene.writer.store.KeyStoreHandlerProperty
import com.graphene.writer.store.key.KeyCache
import com.graphene.writer.store.key.SimpleLocalKeyCache
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClient
import com.graphene.writer.store.key.elasticsearch.ElasticsearchClientFactory
import com.graphene.writer.store.key.elasticsearch.GrapheneIndexRequest
import com.graphene.writer.store.key.elasticsearch.MultiGetRequestContainer
import com.graphene.writer.store.key.elasticsearch.property.ElasticsearchKeyStoreHandlerProperty
import com.graphene.writer.util.NamedThreadFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory
import org.elasticsearch.client.RequestOptions

abstract class AbstractElasticsearchKeyStoreHandler(
  elasticsearchClientFactory: ElasticsearchClientFactory,
  keyStoreHandlerProperty: KeyStoreHandlerProperty
) : KeyStoreHandler, Runnable {

  private val logger: Logger

  private var elasticsearchClient: ElasticsearchClient
  private var keyStoreScheduler: ScheduledExecutorService
  private var index: String
  private var type: String
  private var templateIndexPattern: String
  private var tenant: String

  private var lastFlushTimeMillis = DateTimeUtils.currentTimeMillis()
  private var batchSize: Int = 0
  private var flushInterval: Long = 0
  private var targetProcessTime: Long
  private var requestOptions: RequestOptions
  private var cacheExpireIntervalInSeconds: Long

  private val metrics = LinkedBlockingDeque<GrapheneMetric>()
  private val preloadMetrics = LinkedBlockingDeque<GrapheneMetric>()
  private val cachedThreadPool = Executors.newCachedThreadPool(NamedThreadFactory("C-${this::class.simpleName!!}"))
  private var keyCache: KeyCache<String> // Default 5 Min

  init {
    val property = keyStoreHandlerProperty.property
    this.logger = LogManager.getLogger(this::class.java)

    this.index = property.index
    this.type = property.type
    this.tenant = keyStoreHandlerProperty.tenant
    this.templateIndexPattern = property.templateIndexPattern
    this.batchSize = property.bulk.actions
    this.flushInterval = property.bulk.interval
    this.keyCache = SimpleLocalKeyCache(property.cacheExpireIntervalInSeconds)
    this.cacheExpireIntervalInSeconds = property.cacheExpireIntervalInSeconds
    this.targetProcessTime = property.targetProcessTime

    this.elasticsearchClient = elasticsearchClient(keyStoreHandlerProperty, elasticsearchClientFactory, property)
    this.elasticsearchClient.createTemplateIfNotExists(templateIndexPattern, templateName(), templateSource())
    this.elasticsearchClient.createIndexIfNotExists(setOf(elasticsearchClient.getIndexWithCurrentDate(index, tenant)))

    this.keyStoreScheduler = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(this::class.simpleName!!))
    this.keyStoreScheduler.scheduleWithFixedDelay(this, property.initialSchedulerDelay, property.schedulerDelay, TimeUnit.MILLISECONDS)

    val requestOptionsBuilder = RequestOptions.DEFAULT.toBuilder()
    requestOptionsBuilder.setHttpAsyncResponseConsumerFactory(HttpAsyncResponseConsumerFactory
      .HeapBufferedResponseConsumerFactory(1000 * 1024 * 1024))
    this.requestOptions = requestOptionsBuilder.build()
  }

  private fun elasticsearchClient(
    keyStoreHandlerProperty: KeyStoreHandlerProperty,
    elasticsearchClientFactory: ElasticsearchClientFactory,
    property: ElasticsearchKeyStoreHandlerProperty
  ): ElasticsearchClient {
    return elasticsearchClientFactory.createElasticsearchClient(
      keyStoreHandlerProperty.rotation,
      property.cluster,
      property.port,
      property.userName,
      property.userPassword,
      property.protocol
    )
  }

  override fun handle(grapheneMetric: GrapheneMetric) {
    val index = elasticsearchClient.getIndexWithDate(this.index, this.tenant, grapheneMetric.timestampMillis())
    if (isProcessable(grapheneMetric) && keyCache.putIfAbsent("${index}_${grapheneMetric.id}")) {
      metrics.offer(grapheneMetric)
    }
    handlePreload(index, grapheneMetric)
  }

  private fun handlePreload(originalIndex: String, grapheneMetric: GrapheneMetric) {
    val preloadIndex = elasticsearchClient.getIndexWithDate(
      index,
      tenant,
      grapheneMetric.timestampMillis() + (this.cacheExpireIntervalInSeconds * 1_000L)
    )
    if (originalIndex != preloadIndex && isProcessable(grapheneMetric) && keyCache.putIfAbsent("${preloadIndex}_${grapheneMetric.key}")) {
      val preloadGrapheneMetric = grapheneMetric.copy(
        timestampSeconds = grapheneMetric.timestampSeconds + this.cacheExpireIntervalInSeconds
      )
      preloadMetrics.offer(preloadGrapheneMetric)
    }
  }

  override fun run() {
    val metricsList = mutableListOf<GrapheneMetric>()
    val preloadMetricsList = mutableListOf<GrapheneMetric>()
    metrics.drainTo(metricsList)
    preloadMetrics.drainTo(preloadMetricsList)
    if (lastFlushTimeMillis < DateTimeUtils.currentTimeMillis() - flushInterval) {
      flush(metricsList)
      flush(preloadMetricsList, this.cacheExpireIntervalInSeconds)
    }
  }

  private fun flush(metricsList: List<GrapheneMetric>, targetProcessTime: Long = this.targetProcessTime) {
    if (metricsList.isEmpty()) {
      return
    }

    var multiGetRequestContainer = MultiGetRequestContainer()
    var delay = 0L
    val additionalDelay = calculateAdditionalDelay(metricsList.size, targetProcessTime)
    for (metric in metricsList) {
      val index = elasticsearchClient.getIndexWithDate(index, tenant, metric.timestampMillis())
      multiGetRequestContainer.add(index, type, metric)
      if (multiGetRequestContainer.size() >= batchSize) {
        this.cachedThreadPool.execute {
          doFlush(multiGetRequestContainer, delay)
        }
        delay += additionalDelay
        multiGetRequestContainer = MultiGetRequestContainer()
      }
    }
    doFlush(multiGetRequestContainer, delay)
  }

  private fun calculateAdditionalDelay(size: Int, targetProcessTime: Long): Long {
    val batches: Long = (size / batchSize).toLong()
    return if (batches == 0L) {
      0L
    } else {
      targetProcessTime / batches
    }
  }

  private fun doFlush(multiGetRequestContainer: MultiGetRequestContainer, delay: Long) {
    if (0 >= multiGetRequestContainer.size()) {
      return
    }

    TimeUnit.MILLISECONDS.sleep(delay)

    try {
      elasticsearchClient.createIndexIfNotExists(multiGetRequestContainer.indices)

      if (multiGetRequestContainer.isMultiGetRequestsExist()) {
        val multiGetResponse = elasticsearchClient.mget(multiGetRequestContainer.multiGetRequest, requestOptions)
        val bulkRequest = mutableListOf<GrapheneIndexRequest>()

        for (response in multiGetResponse.responses) {
          if (response.isFailed) {
            logger.error("Fail to check duplicated index because ${response.failure.message}")
            continue
          }

          if (response.response.isExists) {
            continue
          }

          val metric = multiGetRequestContainer.metrics["${response.index}_${response.id}"]
          bulkRequest.addAll(mapToGrapheneIndexRequests(metric!!))
        }

        logger.info("Flushed multiGetRequests: ${multiGetRequestContainer.multiGetRequestSize()}.")

        if (bulkRequest.isNotEmpty()) {
          elasticsearchClient.bulkAsync(index, type, tenant, bulkRequest, requestOptions)
          logger.info("Requested to write ${bulkRequest.size} keys to ES.")
        }
      }

      lastFlushTimeMillis = DateTimeUtils.currentTimeMillis()
    } catch (e: Exception) {
      logger.error("Encountered error in busy loop: ", e)
    }
  }

  override fun close() {
    keyStoreScheduler.shutdown()
    logger.info("Sleeping for 10 seconds to allow leftovers to be written")
    try {
      Thread.sleep(10000)
    } catch (ignored: InterruptedException) {
    }
  }

  abstract fun isProcessable(metric: GrapheneMetric): Boolean

  abstract fun mapToGrapheneIndexRequests(metric: GrapheneMetric): List<GrapheneIndexRequest>

  abstract fun templateSource(): String

  abstract fun templateName(): String
}
