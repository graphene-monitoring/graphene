package com.graphene.reader.store.key.elasticsearch.property

import com.graphene.common.rule.GrapheneRules
import com.graphene.reader.store.key.elasticsearch.selector.KeySelectorProperty
import java.util.ArrayList

class SimpleKeySearchHandlerProperty : IndexProperty {

  var keySelectorProperty: KeySelectorProperty = KeySelectorProperty()
  var clusterName: String? = null
  var index: String? = null
  var tenant: String = GrapheneRules.DEFAULT_TENANT
  var type: String? = null
  var cluster: List<String> = ArrayList()
  var port: Int = 0
  var userName: String? = ""
  var userPassword: String? = ""
  var protocol: String = "http"
  var scroll: Int = 0
  var timeout: Int = 0
  var maxPaths: Int = 0
  var maxTagResults: Int = 0

  override fun keySelectorProperty(): KeySelectorProperty = keySelectorProperty

  override fun clusterName(): String? = clusterName

  override fun index(): String? = index

  override fun tenant(): String = tenant

  override fun type(): String? = type

  override fun cluster(): List<String> = cluster

  override fun port(): Int = port

  override fun userName(): String? = userName

  override fun userPassword(): String? = userPassword

  override fun protocol(): String = protocol

  override fun scroll(): Int = scroll

  override fun timeout(): Int = timeout

  override fun maxPaths(): Int = maxPaths

  override fun maxTagResults(): Int = maxTagResults

  override fun toString(): String {
    return "SimpleKeySearchHandler{" +
      "keySelectorProperty=$keySelectorProperty" +
      ", clusterName=$clusterName" +
      ", index=$index" +
      ", type=$type" +
      ", tenant=$tenant" +
      ", cluster=$cluster" +
      ", port=$port" +
      ", userName=$userName" +
      ", protocol=$protocol" +
      ", scroll=$scroll" +
      ", timeout=$timeout" +
      ", maxPaths=$maxPaths" +
      "}"
  }
}
