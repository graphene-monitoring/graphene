package com.graphene.reader.store.tag.elasticsearch

import com.graphene.reader.store.key.elasticsearch.handler.ElasticsearchClient
import com.graphene.reader.store.tag.elasticsearch.optimizer.ElasticsearchIntegratedTagSearchQueryOptimizer
import com.graphene.reader.utils.ElasticsearchTestUtils
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.Test

internal class ElasticsearchTagSearchHandlerTest {

  private val elasticsearchClient = mockk<ElasticsearchClient>()
  private val elasticsearchTagSearchHandler = ElasticsearchTagSearchHandler(elasticsearchClient, ElasticsearchIntegratedTagSearchQueryOptimizer())

  @Test
  internal fun `should return all option (*) whenever result is not empty`() {
    // given
    val response = ElasticsearchClient.Response("", SearchHits.empty(), 0, mutableSetOf("a"))

    every { elasticsearchClient.query(any(), any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when

    val result = elasticsearchTagSearchHandler
      .getTagValues(
        "", mutableListOf(), "a", 0, 10
      )

    // then
    assertEquals(2, result.size)
    assertEquals("*", result[0])
  }

  @Test
  internal fun `should not return all option (*) whenever result is empty`() {
    // given
    val response = ElasticsearchClient.Response("", SearchHits.empty(), 0, mutableSetOf())

    every { elasticsearchClient.query(any(), any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when

    val result = elasticsearchTagSearchHandler
      .getTagValues(
        "", mutableListOf(), "a", 0, 10
      )

    // then
    assertEquals(0, result.size)
  }
}
