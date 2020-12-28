package com.graphene.reader.graphite.functions

import com.graphene.reader.exceptions.InvalidArgumentException
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeriesByTagFunctionTest {
  lateinit var seriesByTagFunction: SeriesByTagFunction

  @BeforeEach
  internal fun setUp() {
    seriesByTagFunction = SeriesByTagFunction(FUNCTION_NAME)
  }

  @Test
  fun `should throw an exception if invalid arguments by function's rule`() {
    // given
    val table = table(
      headers("argument"),
      row(NUMERIC_ARGUMENT)
//      row(null),
//      row("tagKey="),
//      row("tagKey=~"),
//      row("tagKey!="),
//      row("tagKey=~"),
//      row("tagKey!=~"),
//      row("tagKey tagValue"),
//      row("tagKey="),
//      row("=tagValue")
    )

    // when
    table.forAll { argument ->
      val seriesByTagFunction = SeriesByTagFunction(FUNCTION_NAME)
        .apply { addArg(argument) }

      assertThrows<InvalidArgumentException> { seriesByTagFunction.checkArguments() }
    }
  }

  @Test
  fun `shouldn't throw an exception if an argument is valid arguments by function's rule`() {
    // given
    val tagExpressions = listOf("host=i-00", "dimension=cpu_user")

    for (tagExpression in tagExpressions) {
      seriesByTagFunction.addArg(tagExpression)
    }
    // when
    assertDoesNotThrow(seriesByTagFunction::checkArguments)
  }

  companion object {
    const val FUNCTION_NAME = "seriesByTag"
    const val NUMERIC_ARGUMENT = 99.9
  }
}
