package com.graphene.reader.store.key.elasticsearch.handler

import com.graphene.reader.store.key.elasticsearch.property.TagBasedKeySearchHandlerProperty
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ElasticsearchClientSpec : Spek({
  describe("clearScroll") {
    val clientFixture = mockk<RestHighLevelClient>(relaxed = true)
    val indexPropertyFixture = TagBasedKeySearchHandlerProperty()

    context("scroll id is empty") {
      it("should not be called") {
        val elasticsearchClient = ElasticsearchClient(clientFixture, indexPropertyFixture)

        elasticsearchClient.clearScroll(listOf())

        verify {
          clientFixture.clearScroll(any(), any<RequestOptions>()) wasNot Called
        }
      }
    }

    context("scroll id is not empty") {
      it("should be called") {
        val elasticsearchClient = ElasticsearchClient(clientFixture, indexPropertyFixture)

        elasticsearchClient.clearScroll(listOf("scrollId-1"))

        verify(exactly = 1) {
          clientFixture.clearScroll(any(), any<RequestOptions>())
        }
      }
    }
  }
})
