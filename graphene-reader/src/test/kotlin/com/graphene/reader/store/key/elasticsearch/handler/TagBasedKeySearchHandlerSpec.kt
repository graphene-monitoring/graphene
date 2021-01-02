package com.graphene.reader.store.key.elasticsearch.handler

import io.kotlintest.shouldBe
import io.mockk.mockk
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TagBasedKeySearchHandlerSpec : Spek({
  describe("getPaths") {
    it("should be always empty because it doesn't support") {
      val tagBasedKeySearchHandler = TagBasedKeySearchHandler(mockk(), mockk())

      val paths = tagBasedKeySearchHandler.getPaths("NONE", mutableListOf(), 0, 0)

      paths shouldBe emptyList()
    }
  }
})
