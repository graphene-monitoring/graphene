package com.graphene.reader.store.data.cassandra.handler

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import com.google.common.base.Function
import com.google.common.base.Stopwatch
import com.google.common.collect.Lists
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.graphene.common.beans.Path
import com.graphene.common.beans.SeriesRange
import com.graphene.common.store.data.cassandra.CassandraFactory
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.error.exception.UnsupportedRollupException
import com.graphene.reader.exceptions.TooMuchDataExpectedException
import com.graphene.reader.service.metric.DataFetchHandler
import com.graphene.reader.store.data.DataFetchHandlerProperty
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * @author Andrei Ivanov
 * @author dark
 * @author jerome89
 */
class SimpleDataFetchHandler(
  cassandraFactory: CassandraFactory,
  dataFetchHandlerProperty: DataFetchHandlerProperty
) : DataFetchHandler {

  private val query: String
  private var rollup: Int = 60
  private var cluster: Cluster = cassandraFactory.createCluster(dataFetchHandlerProperty.property)
  private var session: Session
  private var statement: PreparedStatement
  private var maxPoints: Int = Int.MAX_VALUE

  init {
    this.rollup = dataFetchHandlerProperty.rollup
    validateRollup(rollup)
    this.query =
      """
    SELECT time, data
    FROM ${dataFetchHandlerProperty.keyspace}.${dataFetchHandlerProperty.columnFamily}_${rollup}s
    WHERE path = ?
          AND tenant = ?
          AND time >= ?
          AND time <= ?
    ORDER BY time;"""
    this.session = cluster.connect()
    this.statement = session.prepare(query)
    this.maxPoints = dataFetchHandlerProperty.maxPoints
  }

  private fun executeAsync(path: String, tenant: String, from: Long, to: Long): ResultSetFuture {
    return session.executeAsync(statement.bind(path, tenant, from, to))
  }

  private val executorService: ExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

  @Throws(ExecutionException::class, InterruptedException::class, TooMuchDataExpectedException::class)
  override fun getMetrics(tenant: String, paths: List<Path>, from: Long, to: Long): List<TimeSeries> {
    val seriesRange = SeriesRange(from, to, rollup)
    logger.debug(seriesRange.toString())
    logger.debug("Expected number of series is: ${paths.size}")
    // now build the weird data structures ("in the meanwhile")
    val timestampIndices = createTimestampIndices(seriesRange)
    // Fail (return empty list) right away if we exceed maximum number of points
    if (paths.size * seriesRange.expectedCount > maxPoints) {
      logger.debug("Expected total number of data points exceeds the limit: ${paths.size * seriesRange.expectedCount}")
      throw TooMuchDataExpectedException("Expected total number of data points exceeds the limit: ${paths.size * seriesRange.expectedCount} (the limit is $maxPoints)")
    }
    // Now let's query C*
    var futures: MutableList<ListenableFuture<SinglePathResult>> = Lists.newArrayListWithExpectedSize(paths.size)
    for (path in paths) {
      val serializeFunction = Function<ResultSet, SinglePathResult> { resultSet: ResultSet? ->
        val result = SinglePathResult(path)
        result.makeArray(resultSet, timestampIndices)
        result
      }
      futures.add(
        Futures.transform(
          executeAsync(path.path, tenant, seriesRange.from, seriesRange.to),
          serializeFunction,
          executorService
        )
      )
    }
    val timer = Stopwatch.createStarted()
    futures = Futures.inCompletionOrder(futures)
    val timeSeries: MutableList<TimeSeries> = ArrayList()
    for (future in futures) {
      val singlePathResult = future.get()
      if (singlePathResult.values != null) {
        val ts = TimeSeries(singlePathResult.path, singlePathResult.path, singlePathResult.tags, seriesRange, singlePathResult.values)
        timeSeries.add(ts)
      }
    }
    logger.debug("Number of series fetched: ${timeSeries.size}")
    timer.stop()
    logger.debug("Fetching from Cassandra took ${timer.elapsed(TimeUnit.MILLISECONDS)} milliseconds ($paths)")
    // sort it by path
    timeSeries.sortWith(Comparator.comparing { obj: TimeSeries -> obj.name })
    var totalPoints = 0
    for (ts in timeSeries) {
      totalPoints += ts.values.size
    }
    return timeSeries
  }

  private fun createTimestampIndices(seriesRange: SeriesRange): MutableMap<Long, Int> {
    val timestampIndices: MutableMap<Long, Int> = HashMap()
    var timestamp = seriesRange.from
    var index = 0
    while (timestamp <= seriesRange.to) {
      timestampIndices[timestamp] = index++
      timestamp += rollup.toLong()
    }
    return timestampIndices
  }

  override fun getRollup(): Int {
    return rollup
  }

  private fun validateRollup(rollup: Int) {
    if (rollup <= 0) {
      throw UnsupportedRollupException("Rollup is $rollup <= 0!. It should be greater than 0.")
    }
    OffsetBasedDataFetchHandler.logger.info("Rollup: $rollup")
  }

  @PreDestroy
  fun shutdown() {
    logger.info("Shutting down SimpleDataFetchHandler.")
    logger.info("Closing SimpleDataFetchHandler C* session.")
    logger.info("Waiting for C* queries to be completed.")
    while (getInFlightQueries(session.state) > 0) {
      try {
        Thread.sleep(100)
      } catch (ignored: InterruptedException) {
      }
    }
    session.close()
    logger.info("Closing SimpleDataFetchHandler.")
    cluster.close()
  }

  private fun getInFlightQueries(state: Session.State): Int {
    var result = 0
    val hosts = state.connectedHosts
    for (host in hosts) {
      result += state.getInFlightQueries(host)
    }
    return result
  }

  private class SinglePathResult constructor(path: Path) {
    var path: String = path.path
    var tags: Map<String, String> = path.getTags()
    var values: Array<Double?>? = null
    var isAllNulls = true

    fun makeArray(resultSet: ResultSet?, timestampIndices: Map<Long, Int>) {
      values = arrayOfNulls(timestampIndices.size)
      if (resultSet!!.availableWithoutFetching > 0) {
        isAllNulls = false
        for (row in resultSet) {
          values!![timestampIndices[row.getLong("time")]!!] = row.getDouble("data")
        }
      } else {
        for ((_, value) in timestampIndices) {
          values!![value] = null
        }
      }
    }
  }

  companion object {
    val logger: Logger = LogManager.getLogger(SimpleDataFetchHandler::class.java)
  }
}
