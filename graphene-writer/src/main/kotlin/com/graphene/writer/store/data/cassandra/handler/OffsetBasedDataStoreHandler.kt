package com.graphene.writer.store.data.cassandra.handler

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.graphene.common.store.data.cassandra.CassandraFactory
import com.graphene.writer.error.exception.UnsupportedRollupException
import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.store.DataStoreHandler
import com.graphene.writer.store.DataStoreHandlerProperty
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.apache.logging.log4j.LogManager

/**
 * @author Andrei Ivanov
 * @author dark
 * @author jerome89
 *
 * @since 1.0.0
 */
class OffsetBasedDataStoreHandler(
  cassandraFactory: CassandraFactory,
  dataStoreHandlerProperty: DataStoreHandlerProperty
) : DataStoreHandler {

  private val logger = LogManager.getLogger(OffsetBasedDataStoreHandler::class.java)
  private val ttl = dataStoreHandlerProperty.ttl
  private val bucketSize = dataStoreHandlerProperty.bucketSize.toLong()
  // bucketSize: range of short data type 0 ~ 32767
  // rollup should divide 60 or divided by 60
  var rollup: Int = 60
  var query: String
  private var cluster: Cluster = cassandraFactory.createCluster(dataStoreHandlerProperty.property)
  private var session: Session
  private var statement: PreparedStatement
  private var executor: Executor

  init {
    this.rollup = dataStoreHandlerProperty.rollup
    validateRollup(rollup)
    this.query = """
    UPDATE ${dataStoreHandlerProperty.keyspace}_${dataStoreHandlerProperty.bucketSize}.${dataStoreHandlerProperty.columnFamily}_${rollup}s
    USING TTL ?
    SET data = ?
    WHERE tenant = ?
          AND path = ?
          AND startTime = ?
          AND offset = ?;"""
    this.session = cluster.connect()
    this.statement = session.prepare(query)
    this.executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
  }

  override fun handle(grapheneMetric: GrapheneMetric) {
    val future = session.executeAsync(boundStatement(grapheneMetric))

    Futures.addCallback(
      future,
      object : FutureCallback<ResultSet> {
        override fun onSuccess(result: ResultSet?) {
          // nothing
        }

        override fun onFailure(t: Throwable) {
          logger.error(t)
        }
      },
      executor
    )
  }

  private fun boundStatement(grapheneMetric: GrapheneMetric): BoundStatement {
    return statement.bind(
      ttl,
      grapheneMetric.value,
      grapheneMetric.getTenant(),
      grapheneMetric.key,
      getStartTime(grapheneMetric.timestampSeconds),
      getOffset(grapheneMetric.timestampSeconds)
    )
  }

  fun getStartTime(timestamp: Long): Long {
    val remainder = timestamp % (bucketSize * rollup)
    return timestamp - remainder
  }

  fun getOffset(timestamp: Long): Short {
    val remainder = timestamp % (bucketSize * rollup)
    return (remainder / rollup).toShort()
  }

  override fun close() {
    doShutdown()
  }

  private fun doShutdown() {
    logger.info("Closing Offset based Cassandra session")
    logger.info("Waiting for Offset based Cassandra queries to be completed")
    waitToBeCompleted()
    session.close()
    logger.info("Closing Offset based Cassandra cluster")
    cluster.close()
    logger.info("Closed Offset based Cassandra cluster")
  }

  private fun waitToBeCompleted() {
    while (getInFlightQueries(session.state) > 0) {
      try {
        Thread.sleep(100)
      } catch (ignored: InterruptedException) {
      }
    }
  }

  private fun validateRollup(rollup: Int) {
    if (rollup <= 0) {
      throw UnsupportedRollupException("Rollup is $rollup <= 0!. It should be greater than 0.")
    }
    logger.info("Rollup: $rollup")
  }

  private fun getInFlightQueries(state: Session.State): Int {
    var result = 0
    val hosts = state.connectedHosts
    for (host in hosts) {
      result += state.getInFlightQueries(host)
    }

    return result
  }
}
