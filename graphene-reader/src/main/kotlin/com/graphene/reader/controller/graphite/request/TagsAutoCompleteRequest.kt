package com.graphene.reader.controller.graphite.request

import com.graphene.common.utils.DateTimeUtils

data class TagsAutoCompleteRequest(
  val pretty: Int = 0,
  val expr: List<String> = emptyList(),
  val tag: String = "",
  val tagPrefix: String = "",
  val valuePrefix: String = "",
  var from: Long = DateTimeUtils.currentTimeSeconds() - 86_400L,
  var until: Long = DateTimeUtils.currentTimeSeconds()
)
