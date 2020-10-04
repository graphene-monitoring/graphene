package com.graphene.reader.graphite.functions

import com.google.common.base.Joiner
import com.google.common.collect.Lists
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.exceptions.EvaluationException
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.graphite.Target
import com.graphene.reader.graphite.evaluation.TargetEvaluator
import java.util.Optional
import kotlin.collections.ArrayList

/**
 * @author jerome89
 */
class AliasByTagsFunction(text: String?) : GrapheneFunction(text, "aliasByTags") {
  @Throws(EvaluationException::class)
  override fun evaluate(evaluator: TargetEvaluator): List<TimeSeries> {
    val processedArguments: List<TimeSeries> = ArrayList(evaluator.eval(arguments[0] as Target))
    if (processedArguments.isEmpty()) return ArrayList()
    val tagKeys: MutableList<String> = Lists.newArrayList()
    for (i in 1 until arguments.size) {
      tagKeys.add(arguments[i] as String)
    }
    return setNamesByTags(processedArguments, tagKeys)
  }

  private fun setNamesByTags(timeSeriesList: List<TimeSeries>, tagKeys: List<String>): List<TimeSeries> {
    for (ts in timeSeriesList) {
      val parts: MutableList<String?> = Lists.newArrayList()
      for (tagKey in tagKeys) {
        if (ts.tags.containsKey(tagKey)) {
          parts.add(ts.tags[tagKey])
        }
      }
      if (!parts.isEmpty()) {
        ts.name = Joiner.on(".").join(parts)
      }
    }
    return timeSeriesList
  }

  @Throws(InvalidArgumentException::class)
  override fun checkArguments() {
    check(
      arguments.size >= 2,
      "aliasByTags: number of arguments is " +
        arguments.size + ". Must be at least two."
    )
    val argSeries: Optional<Any> = Optional.ofNullable(arguments[0])
    check(
      argSeries.orElse(null) is Target,
      "aliasByTags: First argument is " +
        getClassName(argSeries.orElse(null)) + ". Must be series."
    )
    for (i in 1 until arguments.size) {
      val argNumber: Optional<Any> = Optional.ofNullable(arguments[i])
      check(
        argNumber.orElse(null) is String,
        "aliasByTags: argument is " +
          getClassName(argNumber.orElse(null)) + ". Must be a string."
      )
    }
  }
}
