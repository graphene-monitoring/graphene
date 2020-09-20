package com.graphene.writer.event

import com.graphene.writer.config.StatsProperty
import com.graphene.writer.util.NamedThreadFactory
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.springframework.context.event.EventListener
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component

/**
 * @author Andrei Ivanov
 */
@Component
@ManagedResource("com.graphene.writer.stats:name=GrapheneStats,type=GrapheneWriterEventListener")
class GrapheneWriterEventListener(
  private val statsProperty: StatsProperty
) {

  private val logger = LogManager.getLogger(GrapheneWriterEventListener::class.java)

  private val storeSuccess = AtomicLong(0)
  private val storeError = AtomicLong(0)
  private val stats = ConcurrentHashMap<String, StatsRecord>()

  @get:ManagedAttribute
  var lastStoreSuccess: Long = 0
  @get:ManagedAttribute
  var lastStoreError: Long = 0
  @get:ManagedAttribute
  var metricsReceived: Long = 0
  @get:ManagedAttribute
  var writeCount: Long = 0
  @get:ManagedAttribute
  val lastMetricsReceivedPerTenant = HashMap<String, Long>()
  @get:ManagedAttribute
  val writeCountPerTenant: Map<String, Long> = HashMap()

  private lateinit var scheduler: ScheduledExecutorService

  @PostConstruct
  fun init() {
    this.scheduler = Executors.newScheduledThreadPool(1, NamedThreadFactory(SCHEDULER_NAME))
    this.scheduler.scheduleAtFixedRate({ flush() }, 60 - System.currentTimeMillis() / 1000L % 60, statsProperty.interval.toLong(), TimeUnit.SECONDS)
  }

  private fun getStatsRecord(tenant: String): StatsRecord {
    var statsRecord: StatsRecord? = stats[tenant]
    if (statsRecord == null) {
      val newStatsRecord = StatsRecord()
      statsRecord = (stats as MutableMap<String, StatsRecord>).putIfAbsent(tenant, newStatsRecord)
      if (statsRecord == null) {
        statsRecord = newStatsRecord
      }
    }

    return statsRecord
  }

  fun flush() {
    val statsToFlush = HashMap<String, StatsRecord>()
    val storeSuccess = this.storeSuccess.getAndSet(0)
    val storeError = this.storeError.getAndSet(0)

    for ((key, value) in stats) {
      statsToFlush[key] = value.reset()
    }

    doFlush(statsToFlush, storeSuccess, storeError, DateTime.now(DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0).millis / 1000)
  }

  private fun doFlush(stats: Map<String, StatsRecord>, storeSuccess: Long, storeError: Long, timestamp: Long) {
    logger.debug("Flushing statsProperty for $timestamp")

    var totalReceived: Long = 0
    var totalWritten: Long = 0

    if (statsProperty.isLog) {
      logger.info("Graphene statsProperty:")
      logger.info("=========================================================================")
      logger.info("Tenant\t\tmetrics_received\t\twrite_count")
      logger.info("=========================================================================")
    }

    for ((tenant, statsRecord) in stats) {

      totalReceived += statsRecord.getMetricsReceived()
      totalWritten += statsRecord.getMetricsWritten()

      lastMetricsReceivedPerTenant[tenant] = statsRecord.getMetricsReceived()

      if (statsProperty.isLog) {
        logger.info(tenant + "\t\t" + statsRecord.metricsReceived + "\t\t" + statsRecord.getMetricsWritten())
      }
    }

    metricsReceived = totalReceived
    writeCount = totalWritten
    lastStoreSuccess = storeSuccess
    lastStoreError = storeError

    if (statsProperty.isLog) {
      logger.info("total\t\t$totalReceived\t\t$totalWritten")
      logger.info("=========================================================================")
      logger.info("store.success:\t\t$storeSuccess")
      logger.info("store.error:\t\t$storeError")
      logger.info("=========================================================================")
    }
  }

  @PreDestroy
  @Synchronized
  fun shutdown() {
    scheduler.shutdown()
  }

  // MBean
  @ManagedAttribute
  fun getStoreSuccess(): Long {
    return lastStoreSuccess
  }

  @ManagedAttribute
  fun getStoreError(): Long {
    return lastStoreError
  }

  @EventListener
  fun onGrapheneDataStoreEvent(event: GrapheneDataStoreEvent) {
    getStatsRecord(event.getTenant()).incMetricsWritten()
  }

  private inner class StatsRecord {
    var metricsReceived = AtomicLong(0)
    var metricsWritten = AtomicLong(0)

    constructor()

    constructor(metricsReceived: Long, metricsWritten: Long) {
      this.metricsReceived = AtomicLong(metricsReceived)
      this.metricsWritten = AtomicLong(metricsWritten)
    }

    /**
     * Resets the stats to zeroes and returns a snapshot of the record
     * @return snapshot of the record
     */
    fun reset(): StatsRecord {
      return StatsRecord(metricsReceived.getAndSet(0), metricsWritten.getAndSet(0))
    }

    fun incMetricsReceived() {
      metricsReceived.addAndGet(1)
    }

    fun incMetricsWritten() {
      metricsWritten.addAndGet(1)
    }

    fun getMetricsReceived(): Long {
      return metricsReceived.get()
    }

    fun getMetricsWritten(): Long {
      return metricsWritten.get()
    }
  }

  companion object {
    private const val SCHEDULER_NAME = "GrapheneWriterEventListener"
  }
}
