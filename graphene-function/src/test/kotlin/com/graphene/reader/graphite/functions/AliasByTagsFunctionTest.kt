package com.graphene.reader.graphite.functions

import com.graphene.common.utils.DateTimeUtils
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.PathTarget
import com.graphene.reader.graphite.evaluation.EvaluationContext
import com.graphene.reader.graphite.utils.ValueFormatter
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * @author jerome89
 */
class AliasByTagsFunctionTest : GrapheneFunctionTestHelper() {
  lateinit var aliasByTagsFunction: AliasByTagsFunction

  @BeforeEach
  internal fun setUp() {
    aliasByTagsFunction = AliasByTagsFunction(FUNCTION_NAME)
  }

  @Test
  override fun `should evaluate time series data by function`() {
    // given
    setUpTimeSeriesList(
      aliasByTagsFunction,
      listOf(
        timeSeriesWithTags(
          TIME_SERIES_NAME_1,
          "2019-10-10 10:00:00",
          "2019-10-10 10:02:00",
          60,
          arrayOf(-0.0, -0.1),
          mapOf(
            Pair("host", "i-00"),
            Pair("dimension", "cpu_user")
          )
        ),
        timeSeriesWithTags(
          TIME_SERIES_NAME_2,
          "2019-10-10 10:00:00",
          "2019-10-10 10:02:00",
          60,
          arrayOf(-0.0, -0.1),
          mapOf(
            Pair("host", "i-01"),
            Pair("dimension", "cpu_idle")
          )
        )
      )
    )
    aliasByTagsFunction.addArg("host")
    aliasByTagsFunction.addArg("dimension")

    // when
    val timeSeriesList = aliasByTagsFunction.evaluate(targetEvaluator())

    // then
    assertThat("i-00.cpu_user", `is`(timeSeriesList[0].name))
    assertThat("i-01.cpu_idle", `is`(timeSeriesList[1].name))
  }

  @Test
  override fun `should throw an exception if invalid arguments by function's rule`() {
    // given
    val table = table(
      headers("argument"),
      row(NUMERIC_ARGUMENT),
      row(null)
    )

    // when
    table.forAll { argument ->
      aliasByTagsFunction.addArg(argument)

      assertThrows<InvalidArgumentException> { aliasByTagsFunction.checkArguments() }
    }
  }

  @Test
  override fun `shouldn't throw an exception if an argument is valid arguments by function's rule`() {
    // given
    val pathTarget = PathTarget(
      TIME_SERIES_NAME_1,
      EvaluationContext(ValueFormatter.getInstance(ValueFormatter.ValueFormatterType.MACHINE)),
      TIME_SERIES_NAME_1,
      "NONE",
      DateTimeUtils.from("2019-10-10 10:00:00"),
      DateTimeUtils.from("2019-10-10 10:02:00")
    )
    val tags = listOf("host", "dimension")
    aliasByTagsFunction.addArg(pathTarget)

    for (tag in tags) {
      aliasByTagsFunction.addArg(tag)
    }
    // when
    assertDoesNotThrow(aliasByTagsFunction::checkArguments)
  }

  companion object {
    const val FUNCTION_NAME = "aliasByTags"
    const val NUMERIC_ARGUMENT = 99.9
  }
}
