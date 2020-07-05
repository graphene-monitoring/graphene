package com.graphene.reader.store.tag.elasticsearch.optimizer

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class ElasticsearchIntegratedTagSearchQueryOptimizerTest {

  @Test
  internal fun `should optimize tag value search query when tagExpressions, tagKey and tagValue are given`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagKey = "server", tagValue = "a", tagExpressions = arrayListOf("az=a", "dc=x") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "exists" : {
              "field" : "server",
              "boost" : 1.0
            }
          },
          {
            "regexp" : {
              "server" : {
                "value" : ".*a.*",
                "flags_value" : 65535,
                "max_determinized_states" : 10000,
                "boost" : 1.0
              }
            }
          },
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          },
          {
            "terms" : {
              "dc" : [
                "x"
              ],
              "boost" : 1.0
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }

  @Test
  internal fun `should optimize tag value search query when tagExpressions, tagKey are given`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagKey = "server", tagExpressions = arrayListOf("az=a", "dc=x") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "exists" : {
              "field" : "server",
              "boost" : 1.0
            }
          },
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          },
          {
            "terms" : {
              "dc" : [
                "x"
              ],
              "boost" : 1.0
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }

  @Test
  internal fun `should optimize tag value search query when tagExpressions is given which has negate expression`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagExpressions = arrayListOf("az=a", "dc!=x") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          }
        ],
        "must_not" : [
          {
            "terms" : {
              "dc" : [
                "x"
              ],
              "boost" : 1.0
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }

  @Test
  internal fun `should optimize tag value search query when tagExpressions is given which has terms expression`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagExpressions = arrayListOf("az=a", "dc={x,y}") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          },
          {
            "terms" : {
              "dc" : [
                "x",
                "y"
              ],
              "boost" : 1.0
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }

  @Test
  internal fun `should optimize tag value search query when tagExpressions is given which has regex expression`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagExpressions = arrayListOf("az=a", "dc=~{a,b}") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          },
          {
            "regexp" : {
              "dc" : {
                "value" : "(a|b)",
                "flags_value" : 65535,
                "max_determinized_states" : 10000,
                "boost" : 1.0
              }
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }

  @Test
  internal fun `should optimize tag value search query when tagExpressions including negative regex expression without brackets`() {
    // given
    val optimizer = ElasticsearchIntegratedTagSearchQueryOptimizer()

    // when
    val query = optimizer.optimize(TagSearchTarget(tagExpressions = arrayListOf("az=a", "dc!=~a*") as List<String>))

    // then
    val expectedQuery = """
    {
      "bool" : {
        "filter" : [
          {
            "terms" : {
              "az" : [
                "a"
              ],
              "boost" : 1.0
            }
          }
        ],
        "must_not" : [
          {
            "regexp" : {
              "dc" : {
                "value" : "a.*",
                "flags_value" : 65535,
                "max_determinized_states" : 10000,
                "boost" : 1.0
              }
            }
          }
        ],
        "adjust_pure_negative" : true,
        "boost" : 1.0
      }
    }"""
    assertEquals(expectedQuery.trimIndent(), query.toString().trimIndent())
  }
}
