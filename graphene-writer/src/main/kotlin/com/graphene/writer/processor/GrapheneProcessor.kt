package com.graphene.writer.processor

import com.graphene.writer.blacklist.BlacklistService
import com.graphene.writer.event.GrapheneDataStoreEvent
import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.store.StoreHandler
import com.graphene.writer.store.StoreHandlerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class GrapheneProcessor(
  val blacklistService: BlacklistService,
  val applicationEventPublisher: ApplicationEventPublisher,
  val storeHandlerFactory: StoreHandlerFactory
) {

  val log = LogManager.getLogger(javaClass)
  var storeHandlers: MutableList<StoreHandler> = mutableListOf()
  var dropCount: AtomicLong = AtomicLong(0)
  lateinit var scheduler: ScheduledExecutorService

  @PostConstruct
  fun init() {
    storeHandlers.addAll(storeHandlerFactory.keyStoreHandlers())
    storeHandlers.addAll(storeHandlerFactory.dataStoreHandlers())
    scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleWithFixedDelay(
      {
        log.info("Drop count : $dropCount")
      },
      10, 10, TimeUnit.SECONDS
    )
  }

  @Async("grapheneProcessorExecutor")
  fun process(grapheneMetric: GrapheneMetric) {
    if (blacklistService.isBlackListed(grapheneMetric)) {
      return
    }

    val metricIdBytes = grapheneMetric.id.toByteArray()
    if (METRIC_ID_LIMIT < metricIdBytes.size) {
      dropCount.incrementAndGet()
      log.debug("Graphene has a metric key limit of 512 bytes. But it is ${metricIdBytes.size} bytes. Drop the graphene metric : $grapheneMetric.")
      return
    }

    for (storeHandler in storeHandlers) {
      storeHandler.handle(grapheneMetric)
    }

    applicationEventPublisher.publishEvent(GrapheneDataStoreEvent(grapheneMetric))
  }

  @PreDestroy
  fun destroy() {
    for (storeHandler in storeHandlers) {
      storeHandler.close()
    }
    scheduler.shutdown()
  }

  companion object {
    const val METRIC_ID_LIMIT = 512
  }
}
