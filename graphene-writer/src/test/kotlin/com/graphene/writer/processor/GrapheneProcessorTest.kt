package com.graphene.writer.processor

import com.graphene.writer.blacklist.BlacklistService
import com.graphene.writer.store.DataStoreHandler
import com.graphene.writer.store.KeyStoreHandler
import com.graphene.writer.store.StoreHandlerFactory
import io.mockk.every
import io.mockk.mockk
import java.util.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.context.ApplicationEventPublisher

internal class GrapheneProcessorTest {

  private lateinit var grapheneProcessor: GrapheneProcessor

  private val keyStoreHandler: KeyStoreHandler = mockk()
  private val dataStoreHandler: DataStoreHandler = mockk()

  @BeforeEach
  internal fun setUp() {
    val blacklistService = mockk<BlacklistService> {
      every { isBlackListed(any()) } answers { false }
    }

    val storeHandlerFactory = mockk<StoreHandlerFactory> {
      every { keyStoreHandlers() } answers { listOf(keyStoreHandler) }
      every { dataStoreHandlers() } answers { listOf(dataStoreHandler) }
    }

    val applicationEventPublisher = mockk<ApplicationEventPublisher>()

    grapheneProcessor = GrapheneProcessor(
      blacklistService = blacklistService,
      applicationEventPublisher = applicationEventPublisher,
      storeHandlerFactory = storeHandlerFactory
    )
  }

//  @Test
//  internal fun `should drop metric if size is greater than 512MB`() {
//    // given
//    val grapheneMetric = GrapheneMetric(
//      source = Source.INFLUXDB,
//      id = "cpu_usage_guest;cpu=cpu-total&host=localhost&zone=ap-northeast-2&app=graphene-writer&app.kubernetes.io/managed-by=skaffold-v1.5.0&phase=develop&pod-template-hash=5876775597&skaffold.dev/builder=cluster&skaffold.dev/cleanup=true&skaffold.dev/deployer=kustomize&skaffold.dev/run-id=1d14919d-3b6e-4d8c-a0e7-cd681f177211&skaffold.dev/tag-policy=git-commit&skaffold.dev/tail1=true&skaffold.dev/tail2=true&skaffold.dev/tail3=true&skaffold.dev/tail4=true&skaffold.dev/tail5=true&skaffold.dev/tail6=true&skaffold.dev/tail7=true&skaffold.dev/tail8=true&skaffold.dev/tail9=true&skaffold.dev/tail10=true".sha512(),
//      key = "cpu_usage_guest;cpu=cpu-total&host=localhost&zone=ap-northeast-2&app=graphene-writer&app.kubernetes.io/managed-by=skaffold-v1.5.0&phase=develop&pod-template-hash=5876775597&skaffold.dev/builder=cluster&skaffold.dev/cleanup=true&skaffold.dev/deployer=kustomize&skaffold.dev/run-id=1d14919d-3b6e-4d8c-a0e7-cd681f177211&skaffold.dev/tag-policy=git-commit&skaffold.dev/tail1=true&skaffold.dev/tail2=true&skaffold.dev/tail3=true&skaffold.dev/tail4=true&skaffold.dev/tail5=true&skaffold.dev/tail6=true&skaffold.dev/tail7=true&skaffold.dev/tail8=true&skaffold.dev/tail9=true&skaffold.dev/tail10=true",
//      meta = mutableMapOf("@measurement" to "cpu"),
//      tags = TreeMap<String, String>(
//        mutableMapOf(
//          "cpu" to "cpu-total",
//          "host" to "localhost",
//          "zone" to "ap-northeast-2",
//          "app" to "graphene-writer",
//          "app.kubernetes.io/managed-by" to "skaffold-v1.5.0",
//          "phase" to "develop",
//          "pod-template-hash" to "5876775597",
//          "skaffold.dev/builder" to "cluster",
//          "skaffold.dev/cleanup" to "true",
//          "skaffold.dev/deployer" to "kustomize",
//          "skaffold.dev/run-id" to "1d14919d-3b6e-4d8c-a0e7-cd681f177211",
//          "skaffold.dev/tag-policy" to "git-commit",
//          "skaffold.dev/tail1" to "true",
//          "skaffold.dev/tail1" to "true",
//          "skaffold.dev/tail2" to "true",
//          "skaffold.dev/tail3" to "true",
//          "skaffold.dev/tail4" to "true",
//          "skaffold.dev/tail5" to "true",
//          "skaffold.dev/tail6" to "true",
//          "skaffold.dev/tail7" to "true",
//          "skaffold.dev/tail8" to "true",
//          "skaffold.dev/tail9" to "true",
//          "skaffold.dev/tail10" to "true"
//        )
//      ),
//      nodes = TreeMap<String, String>(),
//      value = 0.0,
//      timestampSeconds = DateTimeUtils.currentTimeSeconds()
//    )
//
//    // when
//    grapheneProcessor.process(grapheneMetric)
//
//    // then
//    verify(exactly = 0) {
//      keyStoreHandler.handle(any())
//      dataStoreHandler.handle(any())
//    }
//  }
}
