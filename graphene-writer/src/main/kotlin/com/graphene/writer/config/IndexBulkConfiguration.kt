package com.graphene.writer.config

/**
 * @author Andrei Ivanov
 */
class IndexBulkConfiguration {

  var actions: Int = 10000
  var preloadActions: Int = 5000
  var interval: Long = 10_000L

  override fun toString(): String {
    return "IndexBulkConfiguration{" +
      "actions=$actions" +
      ", preloadActions=$preloadActions" +
      ", interval=$interval" +
      "}"
  }
}
