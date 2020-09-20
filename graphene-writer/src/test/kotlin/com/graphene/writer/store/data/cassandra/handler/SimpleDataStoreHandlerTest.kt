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
import org.junit.jupiter.api.assertThrows

internal class SimpleDataStoreHandlerTest {
  private lateinit var simpleDataStoreHandler: SimpleDataStoreHandler
  private val cassandraFactory: CassandraFactory = mockk()
  private val cluster: Cluster = mockk()
  private val session: Session = mockk()
  private val preparedStatement: PreparedStatement = mockk()

  @Test
  internal fun `should query to proper keyspace, table with given rollup = 10`() {
    // given
    val dataStoreHandlerProperty = DataStoreHandlerProperty(
      type = "SimpleDataStoreHandler",
      ttl = 60,
      rollup = 10,
      tenant = "NONE",
      bucketSize = 120,
      columnFamily = "metric",
      keyspace = "metric",
      property = CassandraDataHandlerProperty()
    )

    val query =
      """
          UPDATE metric.metric
          USING TTL ?
          SET data = ?
          WHERE tenant = ?
                AND path = ?
                AND time = ?;
      """.trimIndent()

    every { cassandraFactory.createCluster(any()) } answers { cluster }
    every { cluster.connect() } answers { session }
    every { session.prepare(any() as String) } answers { preparedStatement }

    // when
    simpleDataStoreHandler = SimpleDataStoreHandler(
      cassandraFactory,
      dataStoreHandlerProperty
    )

    // then
    assertEquals(query, simpleDataStoreHandler.query.trimIndent())
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
    assertThrows<Exception> { SimpleDataStoreHandler(cassandraFactory, dataStoreHandlerProperty) }
  }
}
