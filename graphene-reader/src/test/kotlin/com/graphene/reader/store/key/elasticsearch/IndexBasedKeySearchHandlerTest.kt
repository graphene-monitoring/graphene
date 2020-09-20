package com.graphene.reader.store.key.elasticsearch

import com.graphene.common.HierarchyMetricPaths
import com.graphene.common.beans.Path
import com.graphene.common.utils.DateTimeUtils
import com.graphene.reader.store.key.elasticsearch.handler.ElasticsearchClient
import com.graphene.reader.store.key.elasticsearch.handler.IndexBasedKeySearchHandler
import com.graphene.reader.store.key.elasticsearch.optimizer.IndexBasedElasticsearchQueryOptimizer
import com.graphene.reader.utils.ElasticsearchTestUtils
import io.mockk.every
import io.mockk.mockk
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

internal class IndexBasedKeySearchHandlerTest {

  private val elasticsearchClient = mockk<ElasticsearchClient>()
  private val indexBasedKeySearchHandler = IndexBasedKeySearchHandler(elasticsearchClient, IndexBasedElasticsearchQueryOptimizer())

  @Test
  internal fun `should return hierarchy metric paths group by text#1`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server1"), Pair("2", "cpu"), Pair("3", "usage")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server2"), Pair("2", "cpu"), Pair("3", "usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val hierarchyMetricPaths = indexBasedKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "servers.*.cpu.usage",
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      )

    // then
    assertEquals(1, hierarchyMetricPaths.size)

    assertTrue {
      val actualHierarchyMetricPath = extractGraphitePaths(hierarchyMetricPaths)[0]
      actualHierarchyMetricPath == "servers.server1.cpu.usage" ||
        actualHierarchyMetricPath == "servers.server2.cpu.usage"
    }
  }

  @Test
  internal fun `should return hierarchy metric paths group by text#2`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server1"), Pair("2", "cpu"), Pair("3", "usage")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server2"), Pair("2", "cpu"), Pair("3", "usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val hierarchyMetricPaths = indexBasedKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "servers.*",
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      )

    // then
    assertEquals(2, hierarchyMetricPaths.size)

    assertEquals(
      listOf(
        "servers.server1",
        "servers.server2"
      ),
      extractGraphitePaths(hierarchyMetricPaths)
    )
  }

  @Test
  internal fun `should get metric paths group by id`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server1"), Pair("2", "cpu"), Pair("3", "usage")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server2"), Pair("2", "cpu"), Pair("3", "usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val metricPaths = indexBasedKeySearchHandler
      .getPaths(
        "NONE",
        mutableListOf("servers.*.cpu.usage"),
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      )

    // then
    assertEquals(2, metricPaths.size)

    assertEquals(
      listOf(
        Path("servers.server1.cpu.usage"),
        Path("servers.server2.cpu.usage")
      ),
      metricPaths.toList()
    )
  }

  @Test
  internal fun `should remove duplication text if is the same index`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"), Pair("3", "d")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "d"), Pair("3", "d")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"), Pair("3", "e")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"), Pair("3", "t")),
          arrayOf(Pair("depth", 3), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val hierarchyMetricPaths = indexBasedKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "a.b.*",
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      )

    // then
    assertEquals(2, hierarchyMetricPaths.size)

    assertEquals(
      listOf(
        "a.b.c",
        "a.b.d"
      ),
      extractGraphitePaths(hierarchyMetricPaths)
    )
  }

  @Test
  internal fun `should get hierarchy metric path without low depth key`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"), Pair("3", "1")),
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "c"), Pair("3", "2")),
          // Low depth key
          arrayOf(Pair("depth", 3), Pair("leaf", true), Pair("0", "a"), Pair("1", "b"), Pair("2", "d"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val hierarchyMetricPaths = indexBasedKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "a.*.*.*",
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      )

    // then
    assertEquals(2, hierarchyMetricPaths.size)

    assertEquals(
      listOf(
        "a.b.c.1",
        "a.b.c.2"
      ),
      extractGraphitePaths(hierarchyMetricPaths)
    )
  }

  @Test
  internal fun `should return hierarchy metric paths composited by branch`() {
    // given
    val response = ElasticsearchClient.Response.of(
      ElasticsearchTestUtils.searchResponse(
        arrayOf(
          arrayOf(Pair("depth", 4), Pair("leaf", true), Pair("0", "servers"), Pair("1", "server1"), Pair("2", "cpu"), Pair("3", "usage"))
        )
      )
    )

    every { elasticsearchClient.query(any(), any(), any()) } answers { response }
    every { elasticsearchClient.searchScroll(any()) } answers { ElasticsearchTestUtils.emptyResponse() }
    every { elasticsearchClient.clearScroll(any()) } answers { Unit }

    // when
    val hierarchyMetricPaths = indexBasedKeySearchHandler
      .getHierarchyMetricPaths(
        "NONE",
        "servers.server1.*",
        DateTimeUtils.currentTimeSeconds(),
        DateTimeUtils.currentTimeSeconds()
      ).toList()

    // then
    assertEquals(1, hierarchyMetricPaths.size)

    assertEquals("servers.server1.cpu", hierarchyMetricPaths[0].id)
    assertEquals("cpu", hierarchyMetricPaths[0].text)
    assertEquals(HierarchyMetricPaths.BRANCH, hierarchyMetricPaths[0].leaf)
  }

  private fun extractGraphitePaths(hierarchyMetricPaths: MutableCollection<HierarchyMetricPaths.HierarchyMetricPath>): List<String> {
    return hierarchyMetricPaths
      .stream()
      .map { it.id }
      .collect(Collectors.toList())
  }
}
