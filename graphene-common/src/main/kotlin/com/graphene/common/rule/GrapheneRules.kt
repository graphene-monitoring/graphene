package com.graphene.common.rule

object GrapheneRules {

  fun index(index: String, tenant: String, timestamp: String): String {
    return "${index}_${tenant}_$timestamp"
  }

  const val DEFAULT_ROTATION_STRATEGY = "timeBasedRotation"
  const val DEFAULT_TENANT = "NONE"
  const val METRIC_NAME_TAG = "@name"

  object SpecialChar {
    const val HASH = '#'
    const val DOUBLE_QUOTE = '"'
    const val AND = '&'
    const val SEMICOLON = ';'
    const val COMMA = ','
    const val BRACE_OPEN = '{'
    const val BRACE_CLOSE = '}'
    const val WHITESPACE = ' '
    const val EQUAL = '='
  }

  object Date {
    const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
  }

  object Key {
    const val DEFAULT_TYPE = "path"
    const val ROTATION_NONE = "0"
    const val ROTATION_1D = "1d"
    const val ROTATION_1W = "1w"

    fun extractWeekFrom(weeklyDate: String): Int {
      return weeklyDate.substring(6, weeklyDate.length).toInt()
    }
  }
}
