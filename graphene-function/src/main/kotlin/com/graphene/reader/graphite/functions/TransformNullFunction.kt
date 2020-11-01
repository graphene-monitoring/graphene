package com.graphene.reader.graphite.functions

import com.google.common.math.DoubleMath
import com.graphene.reader.beans.TimeSeries
import com.graphene.reader.exceptions.InvalidArgumentException
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException
import com.graphene.reader.graphite.Target
import com.graphene.reader.graphite.evaluation.TargetEvaluator
import com.graphene.reader.utils.TimeSeriesUtils
import java.util.*

class TransformNullFunction(
  text: String
) : GrapheneFunction(text, "transformNull") {

  override fun evaluate(evaluator: TargetEvaluator?): MutableList<TimeSeries> {
    val processedArguments: MutableList<TimeSeries> = ArrayList()
    processedArguments.addAll(evaluator!!.eval(arguments[0] as Target))

    if (processedArguments.size == 0) return ArrayList()

    if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
      throw TimeSeriesNotAlignedException()
    }

    try {
      val numericValue = if (1 < arguments.size) arguments[1] as Double else 0.0
      transformNullUsingNumericValue(numericValue, processedArguments)
    } catch (e: ClassCastException) {
      transformNullUsingRecentValue(arguments[1] as String, processedArguments)
    }

    return processedArguments
  }

  private fun transformNullUsingNumericValue(transform: Double, processedArguments: MutableList<TimeSeries>) {
    val length = processedArguments[0].values.size

    for (ts in processedArguments) {
      for (i in 0 until length) {
        ts.values[i] = if (ts.values[i] != null) ts.values[i] else transform
      }
      ts.name = "transformNull(${ts.name}," + (if (DoubleMath.isMathematicalInteger(transform)) transform.toInt().toString() else transform) + ")"
    }
  }

  private fun transformNullUsingRecentValue(transform: String, processedArguments: MutableList<TimeSeries>) {
    val length = processedArguments[0].values.size

    for (ts in processedArguments) {
      var recentValue = 0.0
      for (i in 0 until length) {
        if (Objects.nonNull(ts.values[i])) {
          recentValue = ts.values[i]
        } else {
          ts.values[i] = recentValue
        }
      }
      ts.name = "transformNull(${ts.name},$transform)"
    }
  }

  @Throws(InvalidArgumentException::class)
  override fun checkArguments() {
    if (arguments.size < 1 || arguments.size > 2) throw InvalidArgumentException("transformNull: number of arguments is ${arguments.size}. Must be one or two.")
    if (arguments[0] !is Target) throw InvalidArgumentException("transformNull: argument is ${arguments[0].javaClass.name}. Must be series.")

    if (arguments.size > 1 && (arguments[1] !is Double && "recent" != arguments[1])) throw InvalidArgumentException("transformNull: argument is ${arguments[1].javaClass.name}. Must be a number.")
  }
}
