package com.graphene.writer.input.influxdb

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

internal class InfluxDbMetricConverterTest {

  @Test
  internal fun `should convert to graphene metric format#1`() {
    // given
    val influxDbMetricConverter = InfluxDbMetricConverter()

    // when
    val grapheneMetric = influxDbMetricConverter.convert("cpu,cpu=cpu1,host=server1 usage_user=8.4 1576318510000000000")[0]

    // then influx db format to graphene metric

    // measurement
    grapheneMetric.meta["@measurement"] shouldBe "cpu"
    grapheneMetric.metricKey() shouldBe "cpu_usage_user"

    // tag
    grapheneMetric.tags shouldBe mapOf(Pair("cpu", "cpu1"), Pair("host", "server1"))

    // field
    grapheneMetric.value shouldBe 8.4

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576318510
  }

  @Test
  internal fun `should convert to graphene metric format#2`() {
    // given
    val influxDbMetricConverter = InfluxDbMetricConverter()

    // when
    val grapheneMetrics = influxDbMetricConverter.convert("disk,device=disk1s4,fstype=apfs,host=server1,mode=rw,path=/private/var/vm used=3221245952i,used_percent=2.102430211035405,free=149994110976i 1576383340000000000")

    // then influx db format to graphene metric

    // then #1
    var grapheneMetric = grapheneMetrics[0]

    grapheneMetric.meta["@measurement"] shouldBe "disk"

    // id
    grapheneMetric.id shouldBe "disk_used;device=disk1s4&fstype=apfs&host=server1&mode=rw&path=/private/var/vm"
    grapheneMetric.metricKey() shouldBe "disk_used"

    // tag
    grapheneMetric.tags shouldBe mapOf(
      Pair("device", "disk1s4"),
      Pair("fstype", "apfs"),
      Pair("host", "server1"),
      Pair("mode", "rw"),
      Pair("path", "/private/var/vm")
    )

    grapheneMetric.value shouldBe 3221245952.0

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576383340

    // then #2
    grapheneMetric = grapheneMetrics[1]

    grapheneMetric.meta["@measurement"] shouldBe "disk"

    // id
    grapheneMetric.id shouldBe "disk_used_percent;device=disk1s4&fstype=apfs&host=server1&mode=rw&path=/private/var/vm"
    grapheneMetric.metricKey() shouldBe "disk_used_percent"

    // tag
    grapheneMetric.tags shouldBe mapOf(
      Pair("device", "disk1s4"),
      Pair("fstype", "apfs"),
      Pair("host", "server1"),
      Pair("mode", "rw"),
      Pair("path", "/private/var/vm")
    )

    grapheneMetric.value shouldBe 2.102430211035405

    // then #3
    grapheneMetric = grapheneMetrics[2]

    // id
    grapheneMetric.id shouldBe "disk_free;device=disk1s4&fstype=apfs&host=server1&mode=rw&path=/private/var/vm"
    grapheneMetric.metricKey() shouldBe "disk_free"
    grapheneMetric.value shouldBe 149994110976.0

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576383340
  }

  @Test
  internal fun `should convert to graphene metric format#3`() {
    // given
    val influxDbMetricConverter = InfluxDbMetricConverter()

    // when
    val grapheneMetric = influxDbMetricConverter.convert("cpu,cpu=cpu1,host=server1 usage_user=8.4 1576318510000000000\n")[0]

    // then influx db format to graphene metric

    // measurement
    grapheneMetric.metricKey() shouldBe "cpu_usage_user"
    grapheneMetric.meta["@measurement"] shouldBe "cpu"

    // tag
    grapheneMetric.tags shouldBe mapOf(Pair("cpu", "cpu1"), Pair("host", "server1"))

    // field
    grapheneMetric.value shouldBe 8.4

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576318510
  }

  @Test
  internal fun `should convert to graphene metric format#4`() {
    // given
    val influxDbMetricConverter = InfluxDbMetricConverter()

    // when
    val grapheneMetric = influxDbMetricConverter.convert("BrokerTopicMetrics,host=server1 ProduceMessageConversionsPerSec.EventType=\"requests\" 1576318510000000000")[0]

    // then influx db format to graphene metric

    // measurement
    grapheneMetric.meta["@measurement"] shouldBe "BrokerTopicMetrics"
    grapheneMetric.metricKey() shouldBe "BrokerTopicMetrics_ProduceMessageConversionsPerSec_EventType"

    // tag
    grapheneMetric.tags shouldBe mapOf(Pair("host", "server1"))

    // field
    grapheneMetric.value shouldBe 0.0

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576318510
  }

  @Test
  internal fun `should convert to graphene metric format#5`() {
    // given
    val influxDbMetricConverter = InfluxDbMetricConverter()

    // when
    val grapheneMetric = influxDbMetricConverter.convert("jvm.memoryPool,host=server1,name=G1\\ Old\\ Gen CollectionUsage.committed=2 1576318510000000000")[0]

    // then influx db format to graphene metric

    // measurement
    grapheneMetric.meta["@measurement"] shouldBe "jvm_memoryPool"
    grapheneMetric.metricKey() shouldBe "jvm_memoryPool_CollectionUsage_committed"

    // tag
    grapheneMetric.tags shouldBe mapOf(
      "host" to "server1",
      "name" to "G1_Old_Gen"
    )

    // field
    grapheneMetric.value shouldBe 2.0

    // timestamp
    grapheneMetric.timestampSeconds shouldBe 1576318510
  }
}
