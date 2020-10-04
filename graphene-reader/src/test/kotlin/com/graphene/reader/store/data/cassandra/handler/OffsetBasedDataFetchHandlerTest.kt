package com.graphene.reader.store.data.cassandra.handler

import com.graphene.common.beans.OffsetRange
import com.graphene.common.beans.Path
import com.graphene.common.beans.SeriesRange
import com.graphene.common.store.data.cassandra.CassandraFactory
import com.graphene.common.store.data.cassandra.property.CassandraDataHandlerProperty
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.store.data.DataFetchHandlerProperty
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OffsetBasedDataFetchHandlerTest {

  private lateinit var offsetBasedDataFetchHandler: OffsetBasedDataFetchHandler
  private lateinit var dataFetchHandlerProperty: DataFetchHandlerProperty
  private val cassandraFactory: CassandraFactory = mockk(relaxed = true)

  @BeforeEach
  internal fun setUp() {
    val cassandraDataHandlerProperty = CassandraDataHandlerProperty()
    dataFetchHandlerProperty = DataFetchHandlerProperty(
      tenant = "",
      keyspace = "metric_offset",
      columnFamily = "metric",
      rollup = 60,
      bucketSize = 5,
      property = cassandraDataHandlerProperty
    )
    offsetBasedDataFetchHandler = OffsetBasedDataFetchHandler(cassandraFactory, dataFetchHandlerProperty)
  }

  @Test
  internal fun `should return valid queryOffsetRanges when from = 120, to = 780`() {
    // given
    val targetQueryOffsetRange = mapOf<Long, OffsetRange>(
      Pair(0, OffsetRange(2, 4)),
      Pair(300, OffsetRange(0, 4)),
      Pair(600, OffsetRange(0, 3))
    )

    // when
    val seriesRange = SeriesRange(120, 780, offsetBasedDataFetchHandler.rollup)
    val queryOffsetRanges = offsetBasedDataFetchHandler.createQueryOffsetRanges(seriesRange)

    // then
    for (i in 0L..600L step 300L) {
      assertEquals(targetQueryOffsetRange[i]!!.startOffset, queryOffsetRanges[i]!!.startOffset)
      assertEquals(targetQueryOffsetRange[i]!!.endOffset, queryOffsetRanges[i]!!.endOffset)
    }
  }

  @Test
  internal fun `should addValues to TimeSeries properly with PartialPathResults`() {
    // given
    val targetValues = arrayOf(5.toDouble(), null, 0.toDouble(), null, null, 0.toDouble(), null, 0.toDouble(), null, null, 0.toDouble(), 11.toDouble())
    val path = Path("")
    val offsetRange1 = OffsetRange(2, 4)
    val partialPathResult1 = OffsetBasedDataFetchHandler.PartialPathResult(path = path, startTime = 0, offsetRange = offsetRange1)
    partialPathResult1.values = arrayOf(5.toDouble(), null, 0.toDouble())
    val offsetRange2 = OffsetRange(0, 4)
    val partialPathResult2 = OffsetBasedDataFetchHandler.PartialPathResult(path = path, startTime = 300, offsetRange = offsetRange2)
    partialPathResult2.values = arrayOf(null, null, 0.toDouble(), null, 0.toDouble())
    val offsetRange3 = OffsetRange(0, 3)
    val partialPathResult3 = OffsetBasedDataFetchHandler.PartialPathResult(path = path, startTime = 600, offsetRange = offsetRange3)
    partialPathResult3.values = arrayOf(null, null, 0.toDouble(), 11.toDouble())

    // when
    val seriesRange = SeriesRange(120, 780, 60)
    val timeSeries = TimeSeries("", "", mutableMapOf<String, String>(), seriesRange)
    timeSeries.addValues(partialPathResult1.values, 0)
    timeSeries.addValues(partialPathResult2.values, 300)
    timeSeries.addValues(partialPathResult3.values, 600)

    // then
    assertEquals(targetValues.size, timeSeries.values.size)
    for (i in 0 until targetValues.size - 1) {
      assertEquals(timeSeries.values[i], targetValues[i])
    }
  }

  @Test
  internal fun `should query to proper keyspace, table with rollup = 60 and bucketSize = 300`() {
    // given
    val query =
      """
    SELECT offset, data
        FROM metric_offset_5.metric_60s
        WHERE path = ?
              AND tenant = ?
              AND startTime = ?
              AND offset >= ?
              AND offset <= ?
        ORDER BY offset;
      """.trimIndent()
    // when & then
    assertEquals(query, offsetBasedDataFetchHandler.query.trimIndent())
  }
}
