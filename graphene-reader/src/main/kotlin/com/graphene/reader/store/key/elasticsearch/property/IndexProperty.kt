package com.graphene.reader.store.key.elasticsearch.property

import com.graphene.reader.store.key.elasticsearch.selector.KeySelectorProperty

/**
 * @author Andrei Ivanov
 * @author Dark
 *
 * @since 1.0.0
 */
interface IndexProperty {
  fun keySelectorProperty(): KeySelectorProperty
  fun clusterName(): String?
  fun index(): String?
  fun tenant(): String
  fun type(): String?
  fun cluster(): List<String>
  fun port(): Int
  fun userName(): String?
  fun userPassword(): String?
  fun protocol(): String
  fun scroll(): Int
  fun timeout(): Int
  fun maxPaths(): Int
  fun maxTagResults(): Int
}
