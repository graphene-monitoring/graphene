package com.graphene.reader.store.key.elasticsearch.handler

import com.graphene.common.utils.DateTimeUtils
import com.graphene.reader.store.tag.elasticsearch.optimizer.ElasticsearchTagSearchQueryOptimizer
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TagBasedKeySearchHandlerSpec : Spek({
  val day = 86_400_000

  describe("getPaths") {
    it("should be always empty because it doesn't support") {
      val tagBasedKeySearchHandler = TagBasedKeySearchHandler(mockk(), mockk())

      val paths = tagBasedKeySearchHandler.getPaths("NONE", mutableListOf(), DateTimeUtils.currentTimeMillis() - day, DateTimeUtils.currentTimeMillis())

      paths shouldBe emptyList()
    }
  }

  describe("getPathsByTags") {
    it("should return path with matched tag expression") {
      // given
      val searchHit = mockk<SearchHit> {
        every { sourceAsMap } returns mapOf<String, Any>(
          "@key" to "cpu_idle;host=a",
          "@name" to "cpu_idle",
          "@tags" to arrayOf("host"),
          "host" to "a"
        )
      }

      val elasticsearchClientFixture = mockElasticsearchClient(searchHit)
      val elasticsearchTagSearchQueryOptimizerFixture = mockk<ElasticsearchTagSearchQueryOptimizer> {
        every { optimize(any()) } returns mockk(relaxed = true)
      }
      val tagBasedKeySearchHandler = TagBasedKeySearchHandler(elasticsearchClientFixture, elasticsearchTagSearchQueryOptimizerFixture)

      // when
      val paths = tagBasedKeySearchHandler.getPathsByTags("NONE", listOf("host=a"), DateTimeUtils.currentTimeMillis() - day, DateTimeUtils.currentTimeMillis())

      // then
      paths.size shouldBeExactly 1
      paths[0].path shouldBe "cpu_idle;host=a"
      paths[0].getTags() shouldBe mapOf("host" to "a")
    }
  }
})

private fun mockElasticsearchClient(searchHit: SearchHit): ElasticsearchClient {
  return mockk {
    val searchHits = SearchHits(arrayOf(searchHit), 1L, 0f)
    every { query(any(), any(), any()) } returns ElasticsearchClient.Response("scrollId-1", searchHits, 1, setOf())
    every { searchScroll(any()) } returns mockk {
      every { hits } returns SearchHits(arrayOf(), 0L, 0f)
      every { scrollId } returns "scrollId-2"
    }
    every { clearScroll(any()) } just Runs
  }
}
