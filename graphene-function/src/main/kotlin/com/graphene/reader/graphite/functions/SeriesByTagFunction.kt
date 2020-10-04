package com.graphene.reader.graphite.functions

import com.google.common.collect.Lists
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.exceptions.EvaluationException
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.evaluation.TargetEvaluator
import java.util.Optional

/**
 * @author jerome89
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
    for (argument in arguments) {
      val argString: Optional<Any> = Optional.ofNullable(argument)
      check(
        argString.orElse(null) is String,
        "seriesByTag: argument is " +
          getClassName(argString.orElse(null)) + ". Must be a string."
      )
    }
  }
}
