package com.graphene.function.prometheus.grammar

import io.kotlintest.shouldBe
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table
import io.mockk.every
import io.mockk.mockk
import org.antlr.v4.runtime.*
import org.junit.jupiter.api.Test

class PrometheusLexerTest {

  @Test
  internal fun `should tokenize common lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        ",",
        listOf(expectedToken(PrometheusLexer.COMMA, 0, 0, ","))
      ),
      row(
        "()",
        listOf(
          expectedToken(PrometheusLexer.LEFT_PAREN, 0, 0, "("),
          expectedToken(PrometheusLexer.RIGHT_PAREN, 1, 1, ")")
        )
      ),
      row(
        "{}",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 1, 1, "}")
        )
      ),
      row(
        "[5m]",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
          expectedToken(PrometheusLexer.DURATION, 1, 2, "5m"),
          expectedToken(PrometheusLexer.RIGHT_BRACKET, 3, 3, "]")
        )
      ),
      row(
        "[ 5m]",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
          expectedToken(PrometheusLexer.DURATION, 2, 3, "5m"),
          expectedToken(PrometheusLexer.RIGHT_BRACKET, 4, 4, "]")
        )
      ),
      row(
        "[  5m]",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
          expectedToken(PrometheusLexer.DURATION, 3, 4, "5m"),
          expectedToken(PrometheusLexer.RIGHT_BRACKET, 5, 5, "]")
        )
      ),
      row(
        "[  5m ]",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
          expectedToken(PrometheusLexer.DURATION, 3, 4, "5m"),
          expectedToken(PrometheusLexer.RIGHT_BRACKET, 6, 6, "]")
        )
      ),
      row(
        "\r\n\r",
        listOf()
      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  @Test
  internal fun `should tokenize number lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        "1",
        listOf(expectedToken(PrometheusLexer.NUMBER, 0, 0, "1"))
      ),
      row(
        "4.23",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 3, "4.23")
        )
      ),
      row(
        ".3",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 1, ".3")
        )
      ),
      row(
        "5.",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 1, "5.")
        )
      ),
      row(
        "NaN",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "NaN")
        )
      ),
      row(
        "nAN",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "nAN")
        )
      ),
      row(
        "NaN 123",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "NaN"),
          expectedToken(PrometheusLexer.NUMBER, 4, 6, "123")
        )
      ),
      row(
        "NaN123",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 5, "NaN123")
        )
      ),
      row(
        "iNf",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "iNf")
        )
      ),
      row(
        "Inf",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "Inf")
        )
      ),
      row(
        "+Inf",
        listOf(
          expectedToken(PrometheusLexer.ADD, 0, 0, "+"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf")
        )
      ),
      row(
        "+Inf 123",
        listOf(
          expectedToken(PrometheusLexer.ADD, 0, 0, "+"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf"),
          expectedToken(PrometheusLexer.NUMBER, 5, 7, "123")
        )
      ),
      row(
        "-Inf",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf")
        )
      ),
      row(
        "Infoo",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 4, "Infoo")
        )
      ),
      row(
        "-Infoo",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 5, "Infoo")
        )
      ),
      row(
        "-Inf 123",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf"),
          expectedToken(PrometheusLexer.NUMBER, 5, 7, "123")
        )
      )
//      row(
//        "0x123",
//        listOf(
//          expectedToken(PrometheusLexer.NUMBER, 0, 5, "0x123")
//        )
//      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  @Test
  internal fun `should tokenize strings lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        "\"test\\tsequence\"",
        listOf(expectedToken(PrometheusLexer.STRING, 0, 15, """"test\tsequence""""))
      ),
      row(
        "\"test\\\\.expression\"",
        listOf(expectedToken(PrometheusLexer.STRING, 0, 18, """"test\\.expression""""))
      ),
      row(
        "\"test\\.expression\"",
        listOf(expectedToken(PrometheusLexer.STRING, 0, 17, """"test\.expression""""))
      ),
      row(
        "`test\\.expression`",
        listOf(expectedToken(PrometheusLexer.STRING, 0, 17, """`test\.expression`"""))
      )
//      ,
//      row(
//        ".٩",
//        listOf(expectedToken(PrometheusLexer.STRING, 0, 17, """`test\.expression`"""))
//      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  @Test
  internal fun `should tokenize durations lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        "5s",
        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "5s"))
      ),
      row(
        "123m",
        listOf(expectedToken(PrometheusLexer.DURATION, 0, 3, "123m"))
      ),
      row(
        "1h",
        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "1h"))
      ),
      row(
        "3w",
        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "3w"))
      ),
      row(
        "1y",
        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "1y"))
      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  @Test
  internal fun `should tokenize identifiers lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        "abc",
        listOf(expectedToken(PrometheusLexer.IDENTIFIER, 0, 2, "abc"))
      ),
      row(
        "a:bc",
        listOf(expectedToken(PrometheusLexer.METRIC_IDENTIFIER, 0, 3, "a:bc"))
      ),
      row(
        "abc d",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 2, "abc"),
          expectedToken(PrometheusLexer.IDENTIFIER, 4, 4, "d")
        )
      ),
      row(
        ":bc",
        listOf(expectedToken(PrometheusLexer.METRIC_IDENTIFIER, 0, 2, ":bc"))
      )
//      ,
//      row(
//        "0a:bc",
//        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "1y"))
//      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  @Test
  internal fun `should tokenize comments lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens"),
      row(
        "# some comment",
        listOf(expectedToken(PrometheusLexer.COMMENT, 0, 13, "# some comment"))
      ),
      row(
        "5 # 1+1\\n5",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 0, "5"),
          expectedToken(PrometheusLexer.COMMENT, 2, 6, "# 1+1"),
          // Please fix me below string type
          expectedToken(PrometheusLexer.STRING, 7, 8, "\\n"),
          expectedToken(PrometheusLexer.NUMBER, 9, 9, "5")
        )
      )
    )

    // then
    table.forAll { input, expectedTokens ->
      assertToken(input, expectedTokens)
    }
  }

  private fun assertToken(input: String, expectedTokens: List<Token>) {
    val prometheusLexer = PrometheusLexer(CharStreams.fromString(input))
    val actualTokens = makeActualTokens(prometheusLexer)

    for ((index, actualToken) in actualTokens.iterator().withIndex()) {
      actualToken.type shouldBe expectedTokens[index].type
      actualToken.startIndex shouldBe expectedTokens[index].startIndex
      actualToken.stopIndex shouldBe expectedTokens[index].stopIndex
      actualToken.text shouldBe expectedTokens[index].text
    }

    actualTokens.size shouldBe expectedTokens.size
  }

  private fun makeActualTokens(prometheusLexer: PrometheusLexer): MutableList<Token> {
    val actualTokens = mutableListOf<Token>()

    while (true) {
      val nextToken = prometheusLexer.nextToken()

      if (nextToken.type == Token.EOF) {
        break
      }

      actualTokens.add(nextToken)
    }
    return actualTokens
  }

  private fun expectedToken(type: Int, startIndex: Int, stopIndex: Int, text: String): Token {
    return mockk<Token>().also {
      every { it.type } answers { type }
      every { it.startIndex } answers { startIndex }
      every { it.stopIndex } answers { stopIndex }
      every { it.text } answers { text }
    }
  }

}
