package com.graphene.writer.blacklist

import com.google.common.base.Joiner
import com.graphene.writer.input.GrapheneMetric
import java.util.HashMap
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * @author Andrei Ivanov
 */
@Component
class BlacklistService(
  val blackListConfiguration: BlacklistConfiguration
) {

  private val rules = HashMap<String, Pattern>()

  @PostConstruct
  fun init() {
    for ((key, value) in blackListConfiguration.blacklist) {
      rules[key] = Pattern.compile(Joiner.on("|").skipNulls().join(value))
    }
  }

  fun isBlackListed(grapheneMetric: GrapheneMetric): Boolean {
    val pattern = rules[grapheneMetric.getTenant()]
    return pattern?.matcher(grapheneMetric.key)?.matches() ?: false
  }
}
