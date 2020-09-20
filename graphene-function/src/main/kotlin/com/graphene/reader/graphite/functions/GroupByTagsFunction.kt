package com.graphene.reader.graphite.functions

import com.google.common.collect.Lists
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.exceptions.EvaluationException
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.Target
import com.graphene.reader.graphite.evaluation.TargetEvaluator
import com.graphene.reader.utils.Grouper
import java.util.Optional

/**
 * @author jerome89
 */
class GroupByTagsFunction(text: String?) : GrapheneFunction(text, "groupByTags") {
  @Throws(EvaluationException::class)
  override fun evaluate(evaluator: TargetEvaluator): List<TimeSeries> {
    val processedArguments: List<TimeSeries> = ArrayList(evaluator.eval(arguments[0] as Target))
    if (processedArguments.isEmpty()) return ArrayList()
    val callbackName = arguments[1] as String
    val tagKeys: MutableList<String> = Lists.newArrayList()
    for (i in 2 until arguments.size) {
      tagKeys.add(arguments[i] as String)
    }
    return Grouper(processedArguments, callbackName, from, to).byTagKeys(tagKeys)
  }

  @Throws(InvalidArgumentException::class)
  override fun checkArguments() {
    check(
      arguments.size >= 3,
      "groupByTags: number of arguments is " + arguments.size + ". Must be a least two."
    )
    val argSeries: Optional<Any> = Optional.ofNullable(arguments[0])
    check(
      argSeries.orElse(null) is Target,
      "groupByTags: argument is " + getClassName(argSeries.orElse(null)) + ". Must be series"
    )
    for (i in 1 until arguments.size) {
      val argString: Optional<Any> = Optional.ofNullable(arguments[i])
      check(
        argString.orElse(null) is String,
        "groupByTags: argument is " + getClassName(argSeries.orElse(null)) + ". Must be String"
      )
    }
  }
}
