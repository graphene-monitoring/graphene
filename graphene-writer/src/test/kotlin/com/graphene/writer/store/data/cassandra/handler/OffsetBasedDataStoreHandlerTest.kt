package com.graphene.writer.store.data.cassandra.handler

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.graphene.common.store.data.cassandra.CassandraFactory
import com.graphene.common.store.data.cassandra.property.CassandraDataHandlerProperty
import com.graphene.writer.store.DataStoreHandlerProperty
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class OffsetBasedDataStoreHandlerTest {
  private lateinit var offsetBasedDataStoreHandler: OffsetBasedDataStoreHandler
  private val cassandraFactory: CassandraFactory = mockk()
  private val cluster: Cluster = mockk()
  private val session: Session = mockk()
  private val preparedStatement: PreparedStatement = mockk()

  @Test
  internal fun `should query to proper keyspace, table with given bucketSize = 150 and rollup = 15`() {
    // given
    val dataStoreHandlerProperty = DataStoreHandlerProperty(
      type = "OffsetBasedDataStoreHandler",
      ttl = 60,
      rollup = 15,
      tenant = "NONE",
      bucketSize = 150,
      columnFamily = "metric",
      keyspace = "metric_offset",
      property = CassandraDataHandlerProperty()
    )
    val query =
      """
          UPDATE metric_offset_150.metric_15s
          USING TTL ?
          SET data = ?
          WHERE tenant = ?
                AND path = ?
                AND startTime = ?
                AND offset = ?;
      """.trimIndent()

    every { cassandraFactory.createCluster(any()) } answers { cluster }
    every { cluster.connect() } answers { session }
    every { session.prepare(any() as String) } answers { preparedStatement }

    // when
    offsetBasedDataStoreHandler = OffsetBasedDataStoreHandler(
      cassandraFactory,
      dataStoreHandlerProperty
    )

    // then
    assertEquals(query, offsetBasedDataStoreHandler.query.trimIndent())
  }

  @Test
  internal fun `should not throw exception when rollup is greater than 0`() {
    // given
    val dataStoreHandlerProperty = DataStoreHandlerProperty(
      type = "OffsetBasedDataStoreHandler",
      ttl = 60,
      rollup = 120,
      tenant = "NONE",
      bucketSize = 150,
      columnFamily = "metric",
      keyspace = "metric_offset",
      property = CassandraDataHandlerProperty()
    )

    every { cassandraFactory.createCluster(any()) } answers { cluster }
    every { cluster.connect() } answers { session }
    every { session.prepare(any() as String) } answers { preparedStatement }

    // when & then
    assertDoesNotThrow { OffsetBasedDataStoreHandler(cassandraFactory, dataStoreHandlerProperty) }
  }

  @Test
  internal fun `should throw exception when rollup is less than or equal to 0`() {
    // given
    val dataStoreHandlerProperty = DataStoreHandlerProperty(
      type = "OffsetBasedDataStoreHandler",
      ttl = 60,
      rollup = 0,
      tenant = "NONE",
      bucketSize = 150,
      columnFamily = "metric",
      keyspace = "metric_offset",
      property = CassandraDataHandlerProperty()
    )

    // when & then
    assertThrows<Exception> { OffsetBasedDataStoreHandler(cassandraFactory, dataStoreHandlerProperty) }
  }

  @Test
  internal fun `should return proper startTime and offset with given rollup and bucketSize`() {
    // given
    val dataStoreHandlerProperty = DataStoreHandlerProperty(
      type = "OffsetBasedDataStoreHandler",
      ttl = 60,
      rollup = 15,
      tenant = "NONE",
      bucketSize = 10,
      columnFamily = "metric",
      keyspace = "metric_offset",
      property = CassandraDataHandlerProperty()
    )

    every { cassandraFactory.createCluster(any()) } answers { cluster }
    every { cluster.connect() } answers { session }
    every { session.prepare(any() as String) } answers { preparedStatement }

    // when
    offsetBasedDataStoreHandler = OffsetBasedDataStoreHandler(
      cassandraFactory,
      dataStoreHandlerProperty
    )
    val timestamp1 = 35L // -> should return offset = 2 and startTime = 0 because 35 will be considered as 30 and quotient is 2
    val timestamp2 = 45L // -> should return offset = 3 and startTime = 0 because 45 = 15 * 3
    val timestamp3 = 180L // -> should return offset = 2 and startTime = 150 because bucketSize is 10

    // then
    assertEquals(0, offsetBasedDataStoreHandler.getStartTime(timestamp1))
    assertEquals(2, offsetBasedDataStoreHandler.getOffset(timestamp1))

    assertEquals(0, offsetBasedDataStoreHandler.getStartTime(timestamp2))
    assertEquals(3, offsetBasedDataStoreHandler.getOffset(timestamp2))

    assertEquals(150, offsetBasedDataStoreHandler.getStartTime(timestamp3))
    assertEquals(2, offsetBasedDataStoreHandler.getOffset(timestamp3))
  }
}
