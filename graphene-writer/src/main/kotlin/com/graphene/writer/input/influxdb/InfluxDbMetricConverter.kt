package com.graphene.writer.input.influxdb

import com.graphene.writer.input.GrapheneMetric
import com.graphene.writer.input.MetricConverter
import com.graphene.writer.input.NotFinishedConvertException
import com.graphene.writer.input.Source
import com.graphene.writer.input.UnexpectedConverterException
import java.util.TreeMap
import java.util.concurrent.TimeUnit

/**
 *
 * This class converts InfluxDB's plain metric format to graphene metric format.
 * below links details about InfluxDB's plain metric format:
 * https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/
 *
 * @author dark
 * @since 1.5.0
 */
class InfluxDbMetricConverter : MetricConverter<String> {

  override fun convert(metricStr: String): List<GrapheneMetric> {
    val metric = metricStr.replace("\\ ", "_")
    try {
      var stage = ConvertStage.MEASUREMENT

      var tmpRegistry = StringBuilder()
      var tmpKey: String? = null
      val grapheneMetrics = mutableListOf<GrapheneMetric>()

      var meta: MutableMap<String, String> = mutableMapOf()
      var tags: TreeMap<String, String> = TreeMap()

      for (index in metric.withIndex()) {
        var char = index.value

        when (stage) {
          ConvertStage.MEASUREMENT -> {
            if (char == ',') {
              meta["@measurement"] = tmpRegistry.toString().replace(".", "_")
              tmpRegistry.clear()
              stage = ConvertStage.TAG_KEY
            }
          }
          ConvertStage.TAG_KEY -> {
            if (char == '=') {
              tmpKey = tmpRegistry.toString().replace(".", "_")
              tmpRegistry.clear()
              stage = ConvertStage.TAG_VALUE
            }
          }
          ConvertStage.TAG_VALUE -> {
            if (char == ',') {
              tags[tmpKey!!] = tmpRegistry.toString()
              tmpKey = null
              tmpRegistry.clear()
              stage = ConvertStage.TAG_KEY
            }

            if (char == ' ') {
              tags[tmpKey!!] = tmpRegistry.toString()
              tmpKey = null
              tmpRegistry.clear()
              stage = ConvertStage.FIELD_KEY
            }
          }
          ConvertStage.FIELD_KEY -> {
            if (char == '=') {
              tmpKey = tmpRegistry.toString().replace(".", "_")
              tmpRegistry.clear()
              stage = ConvertStage.FIELD_VALUE
            }
          }
          ConvertStage.FIELD_VALUE -> {
            if (char == ',') {
              var id = key(meta, tmpKey)

              for (tag in tags) {
                id += withAndOperator(tag, tags)
              }
              grapheneMetrics.add(GrapheneMetric(
                source = Source.INFLUXDB,
                id = id,
                meta = meta,
                tags = tags,
                nodes = TreeMap(),
                value = toDouble(tmpRegistry) ?: 0.0,
                timestampSeconds = 1L))
              tmpKey = null
              tmpRegistry.clear()
              stage = ConvertStage.FIELD_KEY
            }

            if (char == ' ') {
              var id = key(meta, tmpKey)

              for (tag in tags) {
                id += withAndOperator(tag, tags)
              }
              grapheneMetrics.add(GrapheneMetric(
                source = Source.INFLUXDB,
                id = id,
                meta = meta,
                tags = tags,
                nodes = TreeMap(),
                value = toDouble(tmpRegistry) ?: 0.0,
                timestampSeconds = 1L))
              tmpKey = null
              tmpRegistry.clear()
              stage = ConvertStage.TIMESTAMP
            }
          }
          ConvertStage.TIMESTAMP -> {
            val timestampSecond = if (metric.endsWith("\n")) {
              TimeUnit.NANOSECONDS.toSeconds(metric.replace("\n", "").substring(index.index).toLong())
            } else {
              TimeUnit.NANOSECONDS.toSeconds(metric.substring(index.index).toLong())
            }

            for (grapheneMetric in grapheneMetrics) {
              grapheneMetric.timestampSeconds = timestampSecond
            }
            return grapheneMetrics
          }
        }

        if (char != ' ' && char != '=' && char != ',') {
          tmpRegistry.append(char)
        }
      }

      throw NotFinishedConvertException("Fail to convert InfluxDB metric : $metric")
    } catch (e: Exception) {
      throw UnexpectedConverterException("Fail to convert InfluxDB metric : $metric", e)
    }
  }

  private fun key(meta: MutableMap<String, String>, tmpKey: String?) =
    "${meta["@measurement"]}_$tmpKey;"

  private fun withAndOperator(tag: MutableMap.MutableEntry<String, String>, tags: TreeMap<String, String>): String {
    return if (tag.key == tags.lastKey()) {
      "${tag.key}=${tag.value}"
    } else {
      "${tag.key}=${tag.value}&"
    }
  }

  private fun toDouble(tmpRegistry: StringBuilder): Double? {
    var value = if (tmpRegistry.last() == 'i') {
      tmpRegistry.substring(0, tmpRegistry.lastIndex)
    } else {
      tmpRegistry.toString()
    }

    return try {
      value.toDouble()
    } catch (e: NumberFormatException) {
      null
    }
  }
}
