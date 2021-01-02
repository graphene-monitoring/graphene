package com.graphene.writer.store.key.elasticsearch

import com.graphene.writer.input.GrapheneMetric
import org.elasticsearch.action.get.MultiGetRequest

class MultiGetRequestContainer(
  val multiGetRequest: MultiGetRequest = MultiGetRequest(),
  val metrics: MutableMap<Index, GrapheneMetric> = mutableMapOf(),
  val indices: MutableSet<String> = mutableSetOf()
) {

  fun add(index: String, type: String, metric: GrapheneMetric) {
    multiGetRequest.add(MultiGetRequest.Item(index, type, metric.id))
    metrics["${index}_${metric.id}"] = metric
    indices.add(index)
  }

  fun size(): Int {
    return multiGetRequest.items.size
  }

  fun isMultiGetRequestsExist(): Boolean {
    return multiGetRequest.items.isNotEmpty()
  }

  fun multiGetRequestSize(): Int {
    return multiGetRequest.items.size
  }
}

typealias Index = String
