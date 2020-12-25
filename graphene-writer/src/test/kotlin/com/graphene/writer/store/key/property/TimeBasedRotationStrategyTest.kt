package com.graphene.writer.store.key.property

import com.graphene.common.key.RotationProperty
import com.graphene.common.key.TimeBasedRotationStrategy
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.test.assertEquals
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.Test

internal class TimeBasedRotationStrategyTest {

  @Test
  internal fun `should return current time with day unit`() {
    table(
      headers("currentTime", "expectedIndexDate"),
      row("2019-10-01 00:00:00", "index_tenant_20191001"),
      row("2019-10-02 00:00:00", "index_tenant_20191002"),
      row("2019-12-31 00:00:00", "index_tenant_20191231")
    ).forAll { currentTime, expectedIndexDate ->
      // given
      val rotationProperty = RotationProperty(period = "1d")
      setCurrentMillisFixed(currentTime)

      // when
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(rotationProperty)

      // then
      assertEquals(expectedIndexDate, timeBasedRotationStrategy.getIndexWithCurrentDate("index", "tenant"))
    }
  }

  @Test
  internal fun `should return current time with week unit`() {
    table(
      headers("currentTime", "expectedIndexDate"),
      row("2019-10-01 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-02 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-03 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-04 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-05 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-06 00:00:00", "index_tenant_2019-w40"),
      row("2019-10-07 00:00:00", "index_tenant_2019-w41"),
      row("2019-12-31 00:00:00", "index_tenant_2020-w01"),
      row("2020-01-01 00:00:00", "index_tenant_2020-w01"),
      row("2020-01-08 00:00:00", "index_tenant_2020-w02")
    ).forAll { currentTime, expectedIndexDate ->
      // given
      val rotationProperty = RotationProperty(period = "1w")
      setCurrentMillisFixed(currentTime)

      // when
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(rotationProperty)

      // then
      assertEquals(expectedIndexDate, timeBasedRotationStrategy.getIndexWithCurrentDate("index", "tenant"))
    }
  }

  @Test
  internal fun `should calculate weeks between from and until`() {
    table(
      headers("from", "to", "expectedIndexes"),
      row("2019-10-07 10:00:00", "2019-10-07 11:00:00", setOf("index_tenant_2019-w41")),
      row("2019-10-07 10:00:00", "2019-10-14 11:00:00", setOf("index_tenant_2019-w41", "index_tenant_2019-w42")),
      row("2019-10-07 10:00:00", "2019-10-21 11:00:00", setOf("index_tenant_2019-w41", "index_tenant_2019-w42", "index_tenant_2019-w43")),
      row("2019-12-30 10:00:00", "2019-12-31 11:00:00", setOf("index_tenant_2020-w01"))
    ).forAll { from, to, expectedIndexes ->
      // given
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(RotationProperty(period = "1w"))

      // when
      val indexes = timeBasedRotationStrategy.getRangeIndex("index", "tenant", com.graphene.common.utils.DateTimeUtils.from(from), com.graphene.common.utils.DateTimeUtils.from(to))

      // then
      assertEquals(expectedIndexes, indexes)
    }
  }

  @Test
  internal fun `should calculate days between from and until when year is different`() {
    table(
      headers("from", "to", "expectedIndexes"),
      row("2019-12-30 10:00:00", "2020-01-02 11:00:00", setOf("index_tenant_20191230", "index_tenant_20191231", "index_tenant_20200101", "index_tenant_20200102"))
    ).forAll { from, to, expectedIndexes ->
      // given
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(RotationProperty(period = "1d"))

      // when
      val indexes = timeBasedRotationStrategy.getRangeIndex("index", "tenant", com.graphene.common.utils.DateTimeUtils.from(from), com.graphene.common.utils.DateTimeUtils.from(to))

      // then
      assertEquals(expectedIndexes, indexes)
    }
  }

  @Test
  internal fun `should calculate weeks between from and until when year is different`() {
    table(
      headers("from", "to", "expectedIndexes"),
      row("2019-12-23 10:00:00", "2020-01-06 11:00:00", setOf("index_tenant_2019-w52", "index_tenant_2020-w01", "index_tenant_2020-w02")),
      row("2019-12-30 10:00:00", "2020-01-06 11:00:00", setOf("index_tenant_2020-w01", "index_tenant_2020-w02"))
    ).forAll { from, to, expectedIndexes ->
      // given
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(RotationProperty(period = "1w"))

      // when
      val indexes = timeBasedRotationStrategy.getRangeIndex("index", "tenant", com.graphene.common.utils.DateTimeUtils.from(from), com.graphene.common.utils.DateTimeUtils.from(to))

      // then
      assertEquals(expectedIndexes, indexes)
    }
  }

  @Test
  internal fun `should calculate weeks between from and until when year is different2`() {
    // given
    val timeBasedRotationStrategy = TimeBasedRotationStrategy(RotationProperty(period = "1w"))

    // when
    val indexes = timeBasedRotationStrategy.getRangeIndex("index", "tenant", com.graphene.common.utils.DateTimeUtils.from("2019-12-23 10:00:00"), com.graphene.common.utils.DateTimeUtils.from("2021-01-08 10:00:00"))

    // then
    val givenWeekOf2019 = 1
    val givenWeekOf2021 = 1
    assertEquals(givenWeekOf2019 + MAXIMUM_WEEK_OF_YEAR + givenWeekOf2021, indexes.size)
  }

  @Test
  internal fun `should calculate weeks based on time milliseconds`() {
    table(
      headers("currentTime", "expectedIndexDate"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-01 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-02 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-03 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-04 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-05 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-06 00:00:00"), "index_tenant_2019-w40"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-10-07 00:00:00"), "index_tenant_2019-w41"),
      row(com.graphene.common.utils.DateTimeUtils.from("2019-12-31 00:00:00"), "index_tenant_2020-w01"),
      row(com.graphene.common.utils.DateTimeUtils.from("2020-01-01 00:00:00"), "index_tenant_2020-w01"),
      row(com.graphene.common.utils.DateTimeUtils.from("2020-01-08 00:00:00"), "index_tenant_2020-w02")
    ).forAll { currentTime, expectedIndexDate ->
      // given
      val rotationProperty = RotationProperty(period = "1w")

      // when
      val timeBasedRotationStrategy = TimeBasedRotationStrategy(rotationProperty)

      // then
      assertEquals(expectedIndexDate, timeBasedRotationStrategy.getIndexWithDate("index", "tenant", currentTime))
    }
  }

  companion object {
    const val MAXIMUM_WEEK_OF_YEAR = 52
  }

  private fun setCurrentMillisFixed(date: String) {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val currentTime = simpleDateFormat.parse(date).time
    DateTimeUtils.setCurrentMillisFixed(currentTime)
  }
}
