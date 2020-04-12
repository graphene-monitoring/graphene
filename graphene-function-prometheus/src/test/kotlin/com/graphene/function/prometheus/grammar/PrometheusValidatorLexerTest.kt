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
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import java.util.Objects
import kotlin.reflect.KClass

class PrometheusValidatorLexerTest {

  @Test
  internal fun `should tokenize common lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
//      row(
//        ",",
//        listOf(expectedToken(PrometheusLexer.COMMA, 0, 0, ",")),
//        nonException()
//      ),
//      row(
//        "()",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_PAREN, 0, 0, "("),
//          expectedToken(PrometheusLexer.RIGHT_PAREN, 1, 1, ")")
//        ),
//        nonException()
//      ),
//      row(
//        "{}",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
//          expectedToken(PrometheusLexer.RIGHT_BRACE, 1, 1, "}")
//        ),
//        nonException()
//      ),
//      row(
//        "[5m]",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
//          expectedToken(PrometheusLexer.DURATION, 1, 2, "5m"),
//          expectedToken(PrometheusLexer.RIGHT_BRACKET, 3, 3, "]")
//        ),
//        nonException()
//      ),
//      row(
//        "[ 5m]",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
//          expectedToken(PrometheusLexer.DURATION, 2, 3, "5m"),
//          expectedToken(PrometheusLexer.RIGHT_BRACKET, 4, 4, "]")
//        ),
//        nonException()
//      ),
//      row(
//        "[  5m]",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
//          expectedToken(PrometheusLexer.DURATION, 3, 4, "5m"),
//          expectedToken(PrometheusLexer.RIGHT_BRACKET, 5, 5, "]")
//        ),
//        nonException()
//      ),
//      row(
//        "[  5m ]",
//        listOf(
//          expectedToken(PrometheusLexer.LEFT_BRACKET, 0, 0, "["),
//          expectedToken(PrometheusLexer.DURATION, 3, 4, "5m"),
//          expectedToken(PrometheusLexer.RIGHT_BRACKET, 6, 6, "]")
//        ),
//        nonException()
//      ),
      row(
        "\r\n\r",
        emptyToken(),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize number lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "1",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 0, "1")
        ),
        nonException()
      ),
      row(
        "4.23",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 3, "4.23")
        ),
        nonException()
      ),
      row(
        ".3",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 1, ".3")
        ),
        nonException()
      ),
      row(
        "5.",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 1, "5.")
        ),
        nonException()
      ),
      row(
        "NaN",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "NaN")
        ),
        nonException()
      ),
      row(
        "nAN",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "nAN")
        ),
        nonException()
      ),
      row(
        "NaN 123",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "NaN"),
          expectedToken(PrometheusLexer.NUMBER, 4, 6, "123")
        ),
        nonException()
      ),
      row(
        "NaN123",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 5, "NaN123")
        ),
        nonException()
      ),
      row(
        "iNf",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "iNf")
        ),
        nonException()
      ),
      row(
        "Inf",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 2, "Inf")
        ),
        nonException()
      ),
      row(
        "+Inf",
        listOf(
          expectedToken(PrometheusLexer.ADD, 0, 0, "+"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf")
        ),
        nonException()
      ),
      row(
        "+Inf 123",
        listOf(
          expectedToken(PrometheusLexer.ADD, 0, 0, "+"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf"),
          expectedToken(PrometheusLexer.NUMBER, 5, 7, "123")
        ),
        nonException()
      ),
      row(
        "-Inf",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf")
        ),
        nonException()
      ),
      row(
        "Infoo",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 4, "Infoo")
        ),
        nonException()
      ),
      row(
        "-Infoo",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 5, "Infoo")
        ),
        nonException()
      ),
      row(
        "-Inf 123",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-"),
          expectedToken(PrometheusLexer.NUMBER, 1, 3, "Inf"),
          expectedToken(PrometheusLexer.NUMBER, 5, 7, "123")
        ),
        nonException()
      ),
      row(
        "0x123",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 4, "0x123")
        ),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize strings lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "\"test\\tsequence\"",
        listOf(
          expectedToken(PrometheusLexer.STRING, 0, 15, """"test\tsequence"""")
        ),
        nonException()
      ),
      row(
        "\"test\\\\.expression\"",
        listOf(
          expectedToken(PrometheusLexer.STRING, 0, 18, """"test\\.expression"""")
        ),
        nonException()
      ),
      row(
        "\"test\\.expression\"",
        listOf(
          expectedToken(PrometheusLexer.STRING, 0, 17, """"test\.expression"""")
        ),
        nonException()
      ),
      row(
        "`test\\.expression`",
        listOf(
          expectedToken(PrometheusLexer.STRING, 0, 17, """`test\.expression`""")
        ),
        nonException()
      ),
      row(
        ".٩",
        listOf(expectedToken(PrometheusLexer.STRING, 0, 0, ".")),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize durations lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "5s",
        listOf(
          expectedToken(PrometheusLexer.DURATION, 0, 1, "5s")
        ),
        nonException()
      ),
      row(
        "123m",
        listOf(
          expectedToken(PrometheusLexer.DURATION, 0, 3, "123m")
        ),
        nonException()
      ),
      row(
        "1h",
        listOf(
          expectedToken(PrometheusLexer.DURATION, 0, 1, "1h")
        ),
        nonException()
      ),
      row(
        "3w",
        listOf(
          expectedToken(PrometheusLexer.DURATION, 0, 1, "3w")
        ),
        nonException()
      ),
      row(
        "1y",
        listOf(
          expectedToken(PrometheusLexer.DURATION, 0, 1, "1y")
        ),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize identifiers lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "abc",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 2, "abc")
        ),
        nonException()
      ),
      row(
        "a:bc",
        listOf(
          expectedToken(PrometheusLexer.METRIC_IDENTIFIER, 0, 3, "a:bc")
        ),
        nonException()
      ),
      row(
        "abc d",
        listOf(
          expectedToken(PrometheusLexer.IDENTIFIER, 0, 2, "abc"),
          expectedToken(PrometheusLexer.IDENTIFIER, 4, 4, "d")
        ),
        nonException()
      ),
      row(
        ":bc",
        listOf(
          expectedToken(PrometheusLexer.METRIC_IDENTIFIER, 0, 2, ":bc")
        ),
        nonException()
      )
//      ,
//      row(
//        "0a:bc",
//        listOf(expectedToken(PrometheusLexer.DURATION, 0, 1, "1y"))
//      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize comments lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "# some comment",
        listOf(
          expectedToken(PrometheusLexer.COMMENT, 0, 13, "# some comment")
        ),
        nonException()
      ),
      row(
        "5 # 1+1\\n5",
        listOf(
          expectedToken(PrometheusLexer.NUMBER, 0, 0, "5"),
          expectedToken(PrometheusLexer.COMMENT, 2, 6, "# 1+1"),
          // Please fix me below string type
          expectedToken(PrometheusLexer.STRING, 7, 8, "\\n"),
          expectedToken(PrometheusLexer.NUMBER, 9, 9, "5")
        ),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize operators lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "=",
        listOf(
          expectedToken(PrometheusLexer.ASSIGN, 0, 0, "=")
        ),
        nonException()
      ),
      // Inside braces equality is a single '=' character.
      row(
        "{=}",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.EQL, 1, 1, "="),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 2, 2, "}")
        ),
        nonException()
      ),
      row(
        "==",
        listOf(
          expectedToken(PrometheusLexer.EQL, 0, 1, "==")
        ),
        nonException()
      ),
      row(
        "!=",
        listOf(
          expectedToken(PrometheusLexer.NEQ, 0, 1, "!=")
        ),
        nonException()
      ),
      row(
        "<",
        listOf(
          expectedToken(PrometheusLexer.LSS, 0, 0, "<")
        ),
        nonException()
      ),
      row(
        ">",
        listOf(
          expectedToken(PrometheusLexer.GTR, 0, 0, ">")
        ),
        nonException()
      ),
      row(
        ">=",
        listOf(
          expectedToken(PrometheusLexer.GTE, 0, 1, ">=")
        ),
        nonException()
      ),
      row(
        "<=",
        listOf(
          expectedToken(PrometheusLexer.LTE, 0, 1, "<=")
        ),
        nonException()
      ),
      row(
        "+",
        listOf(
          expectedToken(PrometheusLexer.ADD, 0, 0, "+")
        ),
        nonException()
      ),
      row(
        "-",
        listOf(
          expectedToken(PrometheusLexer.SUB, 0, 0, "-")
        ),
        nonException()
      ),
      row(
        "*",
        listOf(
          expectedToken(PrometheusLexer.MUL, 0, 0, "*")
        ),
        nonException()
      ),
      row(
        "/",
        listOf(expectedToken(PrometheusLexer.DIV, 0, 0, "/")),
        nonException()
      ),
      row(
        "^",
        listOf(expectedToken(PrometheusLexer.POW, 0, 0, "^")),
        nonException()
      ),
      row(
        "%",
        listOf(expectedToken(PrometheusLexer.MOD, 0, 0, "%")),
        nonException()
      ),
      row(
        "AND",
        listOf(expectedToken(PrometheusLexer.LAND, 0, 2, "AND")),
        nonException()
      ),
      row(
        "or",
        listOf(expectedToken(PrometheusLexer.LOR, 0, 1, "or")),
        nonException()
      ),
      row(
        "unless",
        listOf(expectedToken(PrometheusLexer.LUNLESS, 0, 5, "unless")),
        nonException()
      )

    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize aggregators lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "sum",
        listOf(expectedToken(PrometheusLexer.SUM, 0, 2, "sum")),
        nonException()
      ),
      row(
        "AVG",
        listOf(expectedToken(PrometheusLexer.AVG, 0, 2, "AVG")),
        nonException()
      ),
      row(
        "MAX",
        listOf(expectedToken(PrometheusLexer.MAX, 0, 2, "MAX")),
        nonException()
      ),
      row(
        "min",
        listOf(expectedToken(PrometheusLexer.MIN, 0, 2, "min")),
        nonException()
      ),
      row(
        "count",
        listOf(expectedToken(PrometheusLexer.COUNT, 0, 4, "count")),
        nonException()
      ),
      row(
        "stdvar",
        listOf(expectedToken(PrometheusLexer.STDVAR, 0, 5, "stdvar")),
        nonException()
      ),
      row(
        "stddev",
        listOf(expectedToken(PrometheusLexer.STDDEV, 0, 5, "stddev")),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize keywords lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "offset",
        listOf(expectedToken(PrometheusLexer.OFFSET, 0, 5, "offset")),
        nonException()
      ),
      row(
        "by",
        listOf(expectedToken(PrometheusLexer.BY, 0, 1, "by")),
        nonException()
      ),
      row(
        "without",
        listOf(expectedToken(PrometheusLexer.WITHOUT, 0, 6, "without")),
        nonException()
      ),
      row(
        "on",
        listOf(expectedToken(PrometheusLexer.ON, 0, 1, "on")),
        nonException()
      ),
      row(
        "ignoring",
        listOf(expectedToken(PrometheusLexer.IGNORING, 0, 7, "ignoring")),
        nonException()
      ),
      row(
        "group_left",
        listOf(expectedToken(PrometheusLexer.GROUP_LEFT, 0, 9, "group_left")),
        nonException()
      ),
      row(
        "group_right",
        listOf(expectedToken(PrometheusLexer.GROUP_RIGHT, 0, 10, "group_right")),
        nonException()
      ),
      row(
        "bool",
        listOf(expectedToken(PrometheusLexer.BOOL, 0, 3, "bool")),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should tokenize selectors lex by prometheus rule for given input text`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
//      row(
//        "台北",
//        emptyToken(),
//        nonException()
//      ),
//      row(
//        "{台北='a'}",
//        emptyToken(),
//        IllegalVectorPairException::class
//      ),
//      row(
//        "{0a='a'}",
//        emptyToken(),
//        nonException()
//      ),
      row(
        "{foo='bar'}",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 3, "foo"),
          expectedToken(PrometheusLexer.EQL, 4, 4, "="),
          expectedToken(PrometheusLexer.STRING, 5, 9, "'bar'"),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 10, 10, "}")
        ),
        nonException()
      ),
      row(
        """{foo="bar\"bar"}""",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 3, "foo"),
          expectedToken(PrometheusLexer.EQL, 4, 4, "="),
          expectedToken(PrometheusLexer.STRING, 5, 14, """"bar\"bar""""),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 15, 15, "}")
        ),
        nonException()
      ),
      row(
        """{NaN	!= "bar" }""",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 3, "NaN"),
          expectedToken(PrometheusLexer.NEQ, 5, 6, "!="),
          expectedToken(PrometheusLexer.STRING, 8, 12, """"bar""""),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 14, 14, "}")
        ),
        nonException()
      ),
      row(
        """{NaN	!= "bar" }""",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 3, "NaN"),
          expectedToken(PrometheusLexer.NEQ, 5, 6, "!="),
          expectedToken(PrometheusLexer.STRING, 8, 12, """"bar""""),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 14, 14, "}")
        ),
        nonException()
      ),
      row(
        """{alert=~"bar" }""",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 5, "alert"),
          expectedToken(PrometheusLexer.EQL_REGEX, 6, 7, "=~"),
          expectedToken(PrometheusLexer.STRING, 8, 12, """"bar""""),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 14, 14, "}")
        ),
        nonException()
      ),
      row(
        """{on!~"bar"}""",
        listOf(
          expectedToken(PrometheusLexer.LEFT_BRACE, 0, 0, "{"),
          expectedToken(PrometheusLexer.IDENTIFIER, 1, 2, "on"),
          expectedToken(PrometheusLexer.NEQ_REGEX, 3, 4, "!~"),
          expectedToken(PrometheusLexer.STRING, 5, 9, """"bar""""),
          expectedToken(PrometheusLexer.RIGHT_BRACE, 10, 10, "}")
        ),
        nonException()
      ),
      row(
        """{foo:a="bar"}""",
        emptyToken(),
        NotAllowedMetricIdentifierInsideBraceException::class
      ),
      row(
        """{alert!#"bar"}""",
        emptyToken(),
        NotIncludeQueryOperatorInVectorMatchingException::class
      ),
      row(
        """{foo=}""",
        emptyToken(),
        IllegalVectorPairException::class
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should fail if mismatched syntax in the input`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "=~",
        emptyToken(),
        NotAllowedSingleNonMetricIdentifierException::class
      ),
      row(
        "!~",
        emptyToken(),
        NotAllowedSingleNonMetricIdentifierException::class
      ),
      row(
        "!(",
        emptyToken(),
        UnknownTokenException::class
      ),
      row(
        "1a",
        emptyToken(),
        NotAllowedSingleNonMetricIdentifierException::class
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should fail if mismatched parentheses in the input`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        "(",
        emptyToken(),
        IllegalParenException::class
      ),
      row(
        "())",
        emptyToken(),
        IllegalParenException::class
      ),
      row(
        "(()",
        emptyToken(),
        IllegalParenException::class
      ),
      row(
        "{",
        emptyToken(),
        IllegalBraceException::class
      ),
      row(
        "}",
        emptyToken(),
        IllegalBraceException::class
      ),
      row(
        "{{",
        emptyToken(),
        IllegalBraceException::class
      ),
      row(
        "{{}}",
        emptyToken(),
        IllegalBraceException::class
      ),
      row(
        "[",
        emptyToken(),
        IllegalBracketException::class
      ),
      row(
        "[[",
        emptyToken(),
        IllegalBracketException::class
      ),
      row(
        "[]]",
        emptyToken(),
        IllegalBracketException::class
      ),
      row(
        "[[]]",
        emptyToken(),
        IllegalBracketException::class
      ),
      row(
        "]",
        emptyToken(),
        IllegalBracketException::class
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  @Test
  internal fun `should fail if encoding issue in the input`() {
    // given
    val table = table(
      headers("input", "expectedTokens", "expectedException"),
      row(
        """\"\xff\"""",
        emptyToken(),
        nonException()
      ),
      row(
        """\xff""",
        emptyToken(),
        nonException()
      )
    )

    // then
    table.forAll { input, expectedTokens, expectedException ->
      assertToken(input, expectedTokens, expectedException)
    }
  }

  private fun emptyToken() = emptyList<Token>()

  private fun assertToken(input: String, expectedTokens: List<Token>?, expectedException: KClass<out Exception>?) {
    val prometheusLexer = PrometheusValidatorLexer(CharStreams.fromString(input))

    return if (Objects.nonNull(expectedException)) {
      val assertThrows = assertThrows<Throwable> {
        getActualTokens(prometheusLexer)
      }

      expectedException!!.isInstance(assertThrows) shouldBe true
    } else {
      val actualTokens = getActualTokens(prometheusLexer).also {
        it.size shouldBe expectedTokens!!.size
      }

      for ((index, actualToken) in actualTokens.iterator().withIndex()) {
        actualToken.run {
          type shouldBe expectedTokens!![index].type
          startIndex shouldBe expectedTokens[index].startIndex
          stopIndex shouldBe expectedTokens[index].stopIndex
          text shouldBe expectedTokens[index].text
        }
      }
    }
  }

  private fun nonException(): KClass<out Exception>? = null

  private fun getActualTokens(prometheusLexer: PrometheusLexer): MutableList<Token> {
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
