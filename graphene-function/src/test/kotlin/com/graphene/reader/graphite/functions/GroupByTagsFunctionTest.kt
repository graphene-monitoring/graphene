package com.graphene.reader.graphite.functions

import com.graphene.common.utils.DateTimeUtils
import com.graphene.reader.exceptions.EvaluationException
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
class GroupByTagsFunctionTest : GrapheneFunctionTestHelper() {
  lateinit var groupByTagsFunction: GroupByTagsFunction

  @BeforeEach
  internal fun setUp() {
    groupByTagsFunction = GroupByTagsFunction(FUNCTION_NAME)
  }

  @Test
  override fun `should evaluate time series data by function`() {
    // given
    setUpTimeSeriesList(
      groupByTagsFunction,
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
            Pair("host", "i-00"),
            Pair("dimension", "cpu_idle")
          )
        )
      )
    )
    groupByTagsFunction.addArg("sum")
    groupByTagsFunction.addArg("host")

    // when
    val timeSeriesList = groupByTagsFunction.evaluate(targetEvaluator())

    // then
    assertThat("i-00", `is`(timeSeriesList[0].name))
    assertThat(arrayOf(0.0, -0.2), `is`(timeSeriesList[0].values))
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
      groupByTagsFunction.addArg(argument)

      assertThrows<InvalidArgumentException> { groupByTagsFunction.checkArguments() }
    }
  }

  @Test
  fun `should throw an exception if unsupported aggregator function is provided`() {
    // given
    setUpTimeSeriesList(
      groupByTagsFunction,
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
            Pair("host", "i-00"),
            Pair("dimension", "cpu_idle")
          )
        )
      )
    )
    groupByTagsFunction.addArg("abc")
    groupByTagsFunction.addArg("host")

    // when and then
    assertThrows<EvaluationException> { groupByTagsFunction.evaluate(targetEvaluator()) }
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
    groupByTagsFunction.addArg(pathTarget)
    groupByTagsFunction.addArg("sum")

    for (tag in tags) {
      groupByTagsFunction.addArg(tag)
    }
    // when
    assertDoesNotThrow(groupByTagsFunction::checkArguments)
  }

  companion object {
    const val FUNCTION_NAME = "aliasByTags"
    const val NUMERIC_ARGUMENT = 99.9
  }
}
