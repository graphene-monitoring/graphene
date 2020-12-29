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
}
