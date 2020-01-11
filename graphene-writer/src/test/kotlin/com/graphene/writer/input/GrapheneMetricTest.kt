package com.graphene.writer.input

import java.util.Collections
import java.util.TreeMap
import kotlin.test.assertEquals
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.Test

internal class GrapheneMetricTest {

  @Test
  internal fun `should return graphite key joined with dot ordered by numeric`() {
    // given
    val grapheneMetric = GrapheneMetric(
      Source.GRAPHITE,
      "a.b.c",
      Collections.emptyMap(),
      TreeMap(mapOf(
        Pair("0", "a"),
        Pair("1", "b"),
        Pair("2", "c"))
      ),
      1.0,
      DateTimeUtils.currentTimeMillis()
    )

    // when
    val id = grapheneMetric.id

    // then
    assertEquals(id, "a.b.c")
  }
}
