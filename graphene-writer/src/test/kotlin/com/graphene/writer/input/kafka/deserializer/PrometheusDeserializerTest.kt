package com.graphene.writer.input.kafka.deserializer

import com.graphene.writer.input.Source
import io.kotlintest.matchers.maps.shouldContainAll
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.Test

internal class PrometheusDeserializerTest {

  private val prometheusDeserializer = PrometheusDeserializer()

  @Test
  internal fun `should use current time if prometheus metric is without timestamp`() {
    // given current time
    DateTimeUtils.setCurrentMillisFixed(1580531520000)

    // when
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", SINGLE_PROMETHEUS_METRIC_WITHOUT_TIMESTAMP.toByteArray())

    // then
    grapheneMetric shouldNotBe null

    grapheneMetric[0].id shouldBe "http_requests_total;method=post;code=200"
    grapheneMetric[0].tags shouldContainAll mapOf(Pair("method", "post"), Pair("code", "200"))
    grapheneMetric[0].value shouldBe 1027.0
    grapheneMetric[0].source shouldBe Source.PROMETHEUS
    grapheneMetric[0].timestampMillis() shouldBe 1580531520000
  }

  @Test
  internal fun `should ignore if plain prometheus metric is invalid`() {
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", INVALID_PROMETHEUS_METRIC.toByteArray())

    grapheneMetric.size shouldBe 0
  }

  @Test
  internal fun `should deserialize from single prometheus metric`() {
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", SINGLE_PROMETHEUS_METRIC.toByteArray())

    grapheneMetric shouldNotBe null

    grapheneMetric[0].id shouldBe "http_requests_total;method=post;code=200"
    grapheneMetric[0].tags shouldContainAll mapOf(Pair("method", "post"), Pair("code", "200"))
    grapheneMetric[0].value shouldBe 1027.0
    grapheneMetric[0].source shouldBe Source.PROMETHEUS
    grapheneMetric[0].timestampMillis() shouldBe 1580531520000
  }

  @Test
  internal fun `should deserialize from multiple prometheus metric`() {
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", MULTIPLE_PROMETHEUS_METRIC_1.toByteArray())

    grapheneMetric shouldNotBe null

    grapheneMetric[0].id shouldBe "swap_in;host=local"
    grapheneMetric[0].tags shouldContainAll mapOf(Pair("host", "local"))
    grapheneMetric[0].value shouldBe 0.0
    grapheneMetric[0].source shouldBe Source.PROMETHEUS
    grapheneMetric[0].timestampMillis() shouldBe 1580532000000

    grapheneMetric[1].id shouldBe "swap_out;host=local"
    grapheneMetric[1].tags shouldContainAll mapOf(Pair("host", "local"))
    grapheneMetric[1].value shouldBe 0.0
    grapheneMetric[1].source shouldBe Source.PROMETHEUS
    grapheneMetric[1].timestampMillis() shouldBe 1580532000000
  }

  @Test
  internal fun `should deserialize from sequential multiple prometheus metric without hint or type`() {
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", MULTIPLE_PROMETHEUS_METRIC_2.toByteArray())

    grapheneMetric shouldNotBe null

    grapheneMetric[0].id shouldBe "swap_in;host=local"
    grapheneMetric[0].tags shouldContainAll mapOf(Pair("host", "local"))
    grapheneMetric[0].value shouldBe 0.0
    grapheneMetric[0].source shouldBe Source.PROMETHEUS
    grapheneMetric[0].timestampMillis() shouldBe 1580532000000

    grapheneMetric[1].id shouldBe "swap_out;host=local"
    grapheneMetric[1].tags shouldContainAll mapOf(Pair("host", "local"))
    grapheneMetric[1].value shouldBe 0.0
    grapheneMetric[1].source shouldBe Source.PROMETHEUS
    grapheneMetric[1].timestampMillis() shouldBe 1580532000000
  }

  @Test
  internal fun `should deserialize converted metric if label value has whitespace`() {
    val grapheneMetric = prometheusDeserializer.deserialize("prometheus.topic", SINGLE_PROMETHEUS_UNTYPED_METRIC.toByteArray())

    grapheneMetric shouldNotBe null

    grapheneMetric[0].id shouldBe "jvm_gc_CollectionCount;host=localhost;name=G1_Old_Generation"
    grapheneMetric[0].tags shouldContainAll mapOf(Pair("host", "localhost"), Pair("name", "G1_Old_Generation"))
    grapheneMetric[0].value shouldBe 0.0
    grapheneMetric[0].source shouldBe Source.PROMETHEUS
    grapheneMetric[0].timestampMillis() shouldBe 1580532000000

    grapheneMetric[1].id shouldBe "jvm_gc_CollectionCount;host=localhost;name=G1_Young_Generation"
    grapheneMetric[1].tags shouldContainAll mapOf(Pair("host", "localhost"), Pair("name", "G1_Young_Generation"))
    grapheneMetric[1].value shouldBe 14937.0
    grapheneMetric[1].source shouldBe Source.PROMETHEUS
    grapheneMetric[1].timestampMillis() shouldBe 1580532000000
  }

  companion object {
    var SINGLE_PROMETHEUS_METRIC_WITHOUT_TIMESTAMP =
      """# HELP swap_in Telegraf collected metric
        |# TYPE swap_in counter
        |http_requests_total{method="post",code="200"} 1027
      """.trimMargin("|")

    var SINGLE_PROMETHEUS_METRIC =
      """# HELP swap_in Telegraf collected metric
        |# TYPE swap_in counter
        |http_requests_total{method="post",code="200"} 1027 1580531540000
      """.trimMargin("|")

    var INVALID_PROMETHEUS_METRIC =
      """# HELP swap_in Telegraf collected metric
        |# TYPE swap_in counter
        |http_requests_total{method="post",code="200"} stringValue 1580531540000
      """.trimMargin("|")

    var MULTIPLE_PROMETHEUS_METRIC_1 =
      """# HELP swap_in Telegraf collected metric
        |# TYPE swap_in counter
        |swap_in{host="local"} 0 1580532000000
        |# HELP swap_out Telegraf collected metric
        |# TYPE swap_out counter
        |swap_out{host="local"} 0 1580532000000
      """.trimMargin("|")

    var MULTIPLE_PROMETHEUS_METRIC_2 =
      """# HELP swap_in Telegraf collected metric
        |# TYPE swap_in counter
        |swap_in{host="local"} 0 1580532000000
        |swap_out{host="local"} 0 1580532000000
        |
      """.trimMargin("|")

    var SINGLE_PROMETHEUS_UNTYPED_METRIC =
      """# HELP jvm_gc_CollectionCount Telegraf collected metric
      |# TYPE jvm_gc_CollectionCount untyped
      |jvm_gc_CollectionCount{host="localhost",name="G1 Old Generation"} 0 1580532000000
      |jvm_gc_CollectionCount{host="localhost",name="G1 Young Generation"} 14937 1580532000000
      |
    """.trimMargin("|")
  }
}
