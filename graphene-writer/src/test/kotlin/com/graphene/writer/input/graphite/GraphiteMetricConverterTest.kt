package com.graphene.writer.input.graphite

import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.Source
import java.util.Collections
import java.util.TreeMap
import kotlin.test.assertEquals
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.Test

internal class GraphiteMetricConverterTest {

  private val graphiteMetricConverter = GraphiteMetricConverter()

  @Test
  internal fun `should convert Graphite old version protocol to graphene metric`() {
    // given
    val key = "a.b.c"
    val value = 1.0
    val timestamp = DateTimeUtils.currentTimeMillis()

    // when
    val grapheneMetric = graphiteMetricConverter.convert(
      GraphiteMetric(
        key,
        value,
        timestamp
      )
    )[0]

    // then
    assertEquals(
      GrapheneMetric(
        Source.GRAPHITE,
        "a.b.c",
        Collections.emptyMap(),
        TreeMap(),
        TreeMap(mutableMapOf(Pair("0", "a"), Pair("1", "b"), Pair("2", "c"))),
        1.0,
        timestamp
      ),
      grapheneMetric
    )
  }

  @Test
  internal fun `should convert Graphite old version protocol to graphene metric with unknown key`() {
    // given
    val key = "a.b..c"
    val value = 1.0
    val timestamp = DateTimeUtils.currentTimeMillis()

    // when
    val grapheneMetric = graphiteMetricConverter.convert(
      GraphiteMetric(
        key,
        value,
        timestamp
      )
    )[0]

    // then
    assertEquals(
      GrapheneMetric(
        Source.GRAPHITE,
        "a.b.unknown.c",
        Collections.emptyMap(),
        TreeMap(),
        TreeMap(mutableMapOf(Pair("0", "a"), Pair("1", "b"), Pair("2", "unknown"), Pair("3", "c"))),
        1.0,
        timestamp
      ),
      grapheneMetric
    )
  }

  @Test
  internal fun `should convert Graphite tag version to graphene metric`() {
    // given
    val key = "cpu.usage;hostname=local"
    val value = 1.0
    val timestamp = DateTimeUtils.currentTimeMillis()

    // when
    val grapheneMetric = graphiteMetricConverter.convert(
      GraphiteMetric(
        key,
        value,
        timestamp
      )
    )[0]

    // then
    assertEquals(
      GrapheneMetric(
        Source.GRAPHITE_TAG,
        "cpu.usage;hostname=local",
        Collections.emptyMap(),
        TreeMap(mutableMapOf(Pair("hostname", "local"))),
        TreeMap(mutableMapOf(Pair("0", "cpu"), Pair("1", "usage"))),
        1.0,
        timestamp
      ),
      grapheneMetric
    )
  }

  @Test
  internal fun `should convert Graphite tag version to graphene metric with unknown key`() {
    // given
    val key = "cpu..usage;hostname=local"
    val value = 1.0
    val timestamp = DateTimeUtils.currentTimeMillis()

    // when
    val grapheneMetric = graphiteMetricConverter.convert(
      GraphiteMetric(
        key,
        value,
        timestamp
      )
    )[0]

    // then
    assertEquals(
      GrapheneMetric(
        Source.GRAPHITE_TAG,
        "cpu.unknown.usage;hostname=local",
        Collections.emptyMap(),
        TreeMap(mutableMapOf(Pair("hostname", "local"))),
        TreeMap(mutableMapOf(Pair("0", "cpu"), Pair("1", "unknown"), Pair("2", "usage"))),
        1.0,
        timestamp
      ),
      grapheneMetric
    )
  }
}
