package com.graphene.reader.graphite.functions

import com.google.common.collect.Lists
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.exceptions.EvaluationException
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.evaluation.TargetEvaluator

typealias SeriesByTagArgument = String

/**
 * @author jerome89
 * @author dark
 */
class SeriesByTagFunction(text: String?) : GrapheneFunction(text, "seriesByTag") {
  @Throws(EvaluationException::class)
  override fun evaluate(evaluator: TargetEvaluator): List<TimeSeries> {
    val tagExpressions: MutableList<String> = Lists.newArrayList()
    for (arg in arguments) {
      tagExpressions.add(arg as String)
    }
    return evaluator.evalByTags(tenant, tagExpressions, from, to)
  }

  @Throws(InvalidArgumentException::class)
  override fun checkArguments() {
    check(
      arguments.size != 0,
      "seriesByTag: number of arguments is " +
        arguments.size + ". Must be at least one."
    )

    for (seriesByTagArgument in arguments) {
      check(seriesByTagArgument is SeriesByTagArgument,
        "seriesByTag: argument is ${getClassName(seriesByTagArgument)}. Must be a string argument. Please check $arguments."
      )

      val (tagKey, tagValue) = (seriesByTagArgument as SeriesByTagArgument).toTagExpressionPair()
      check(tagKey.isNotNullOrBlank() && tagValue.isNotNullOrBlank(),
        "seriesByTag: incomplete arguments. Please check $arguments."
      )
    }
  }

  private fun SeriesByTagArgument.toTagExpressionPair(): List<String> {
    return split(extractTagExpressionMatcherElseThrow())
  }

  private fun SeriesByTagArgument.extractTagExpressionMatcherElseThrow(): String {
    return when {
      this.contains("!=~") -> "!=~"
      this.contains("!=") -> "!="
      this.contains("=~") -> "=~"
      this.contains("=") -> "="
      else -> throw InvalidArgumentException("seriesByTag: incomplete arguments. Please check $this.")
    }
  }

  private fun String?.isNotNullOrBlank(): Boolean {
    return !this.isNullOrBlank()
  }
}
