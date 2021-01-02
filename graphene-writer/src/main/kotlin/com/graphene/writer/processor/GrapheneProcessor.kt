package com.graphene.writer.processor

import com.graphene.writer.blacklist.BlacklistService
import com.graphene.writer.event.GrapheneDataStoreEvent
import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.store.StoreHandler
import com.graphene.writer.store.StoreHandlerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class GrapheneProcessor(
  val blacklistService: BlacklistService,
  val applicationEventPublisher: ApplicationEventPublisher,
  val storeHandlerFactory: StoreHandlerFactory
) {

  val log: Logger = LogManager.getLogger(javaClass)
  var storeHandlers: MutableList<StoreHandler> = mutableListOf()

  @PostConstruct
  fun init() {
    storeHandlers.addAll(storeHandlerFactory.keyStoreHandlers())
    storeHandlers.addAll(storeHandlerFactory.dataStoreHandlers())
  }

  @Async("grapheneProcessorExecutor")
  fun process(grapheneMetric: GrapheneMetric) {
    if (blacklistService.isBlackListed(grapheneMetric)) {
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
  }
}
