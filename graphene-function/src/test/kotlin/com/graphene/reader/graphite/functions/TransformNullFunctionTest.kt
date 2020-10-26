package com.graphene.reader.graphite.functions

import com.graphene.common.utils.DateTimeUtils
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.PathTarget
import com.graphene.reader.graphite.evaluation.EvaluationContext
import com.graphene.reader.graphite.utils.ValueFormatter
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TransformNullFunctionTest : GrapheneFunctionTestHelper() {

  lateinit var transformNullFunction: TransformNullFunction

  @BeforeEach
  internal fun setUp() {
    transformNullFunction = TransformNullFunction(FUNCTION_NAME)
  }

  @Test
  override fun `should evaluate time series data by function`() {
    // given
    setUpTimeSeriesList(
      transformNullFunction,
      listOf(
        timeSeriesWithTags(
          name = TIME_SERIES_NAME_1,
          from = "2019-10-10 10:00:00",
          to = "2019-10-10 10:02:00",
          step = 60,
          values = arrayOf(10.0, null, 12.0, null, null, null),
          tags = mapOf(
            Pair("host", "i-00"),
            Pair("dimension", "cpu_user")
          )
        )
      )
    )
    transformNullFunction.addArg("recent")

    // when
    val timeSeriesList = transformNullFunction.evaluate(targetEvaluator())

    // then
    timeSeriesList[0].name shouldBe "transformNull(hosts.server1.cpu.usage,recent)"
    timeSeriesList[0].values shouldBe arrayOf(10.0, 10.0, 12.0, 12.0, 12.0, 12.0)
  }

  @Test
  override fun `should throw an exception if invalid arguments by function's rule`() {
    // given
    val table = table(
      headers("argument"),
      row("test"),
      row(null)
    )

    // when
    table.forAll { argument ->
      transformNullFunction.addArg(argument)

      shouldThrow<InvalidArgumentException> { transformNullFunction.checkArguments() }
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

    val table = table(
      headers("argument0", "argument1"),
      row(pathTarget, 0.0),
      row(pathTarget, "recent")
    )

    table.forAll { argument0, argument1 ->
      val transformNullFunction = TransformNullFunction(FUNCTION_NAME)
      transformNullFunction.addArg(argument0)
      transformNullFunction.addArg(argument1)

      shouldNotThrow<InvalidArgumentException> { transformNullFunction.checkArguments() }
    }
  }

  companion object {
    const val FUNCTION_NAME = "transformNull"
  }
}
