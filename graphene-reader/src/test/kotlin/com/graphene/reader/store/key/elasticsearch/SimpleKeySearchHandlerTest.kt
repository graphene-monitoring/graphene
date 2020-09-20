package com.graphene.reader.store.key.elasticsearch

import com.graphene.common.beans.Path
import com.graphene.reader.store.key.elasticsearch.handler.ElasticsearchClient
import com.graphene.reader.store.key.elasticsearch.handler.SimpleKeySearchHandler
import com.graphene.reader.utils.ElasticsearchTestUtils
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong

internal class SimpleKeySearchHandlerTest {

  private lateinit var simpleKeySearchHandler: SimpleKeySearchHandler
  private val elasticsearchClient: ElasticsearchClient = mockk()

  @BeforeEach
  internal fun setUp() {
    simpleKeySearchHandler = SimpleKeySearchHandler(elasticsearchClient)
  }

  @Test
  internal fun `should return deduplicated hierarchy metric path`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("@tenant", "NONE"), Pair("depth", 4), Pair("leaf", true), Pair("path", "hosts.i-a.cpu.usage")),
          arrayOf(Pair("@tenant", "NONE"), Pair("depth", 4), Pair("leaf", true), Pair("path", "hosts.i-b.cpu.usage")),
          arrayOf(Pair("@tenant", "NONE"), Pair("depth", 4), Pair("leaf", true), Pair("path", "hosts.i-c.cpu.usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }

    // when
    val hierarchyMetricPath = simpleKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "hosts.*.cpu.*",
        anyLong(),
        anyLong()
      )

    // then
    assertEquals(1, hierarchyMetricPath.size)
  }

  @Test
  internal fun `should return a plain path if without complex query path`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("@tenant", "NONE"), Pair("depth", 4), Pair("leaf", true), Pair("path", "servers.server1.cpu.usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val paths = simpleKeySearchHandler
      .getPaths(
        "NONE",
        listOf("servers.server1.cpu.usage"),
        anyLong(),
        anyLong()
      )

    // then
    assertEquals(1, paths.size)
    assertTrue(paths.contains(Path("servers.server1.cpu.usage")))
  }

  @Test
  internal fun `should return result about query if with complex query path`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("@tenant", "NONE"), Pair("depth", 4), Pair("leaf", true), Pair("path", "servers.server1.cpu.usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val paths = simpleKeySearchHandler
      .getPaths(
        "NONE",
        listOf("servers.*.cpu.usage"),
        anyLong(),
        anyLong()
      )

    // then
    assertEquals(1, paths.size)
    assertTrue(paths[0].path.equals("servers.server1.cpu.usage"))
  }
}
