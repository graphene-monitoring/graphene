package com.graphene.writer.domain

import com.graphene.reader.utils.MetricRule
import com.graphene.writer.config.Rollup

/**
 * @author Andrei Ivanov
 */
class Metric {

  private var key: MetricKey? = null
  var value: Double = 0.toDouble()

  val id: String
    get() = tenant + "_" + path

  val tenant: String
    get() = key!!.tenant

  val path: String
    get() = key!!.path

  val timestamp: Long
    get() = key!!.timestamp

  constructor(input: String, rollup: Rollup) {
    val splitInput = input.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    // We were interning tenant and path here - we are going to store them all (or almost so) constantly anyhow in multiple places
    // In fact this also work for a moderate metrics stream. Once we start receiving 10s of millions different metrics, it tends to degrade quite a bit
    // So, leaving this only for tenant
    this.key = MetricKey(
      if (splitInput.size >= 4) splitInput[3].intern() else MetricRule.defaultTenant(),
      splitInput[0],
      normalizeTimestamp(java.lang.Long.parseLong(splitInput[2]), rollup))
    this.value = java.lang.Double.parseDouble(splitInput[1])
  }

  private fun normalizeTimestamp(timestamp: Long, rollup: Rollup): Long {
    return timestamp / rollup.rollup * rollup.rollup
  }

  override fun toString(): String {
    return "Metric{" +
      "key=" + key +
      ", value=" + value +
      '}'.toString()
  }
}
