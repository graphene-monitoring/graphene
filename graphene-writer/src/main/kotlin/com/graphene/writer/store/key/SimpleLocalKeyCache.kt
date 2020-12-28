package com.graphene.writer.store.key

import com.google.common.cache.CacheBuilder
import java.util.Objects
import java.util.concurrent.TimeUnit

/**
 *
 * This class is not thread safe.
 * If you need to handle parallel operation, please consider double-check locking at getOrCreate method.
 *
 * @author jerome89
 */
class SimpleLocalKeyCache<K>(
  expireInterval: Long
) : KeyCache<K> {
  private val internalSimpleCache = CacheBuilder.newBuilder()
    .expireAfterAccess(expireInterval, TimeUnit.SECONDS)
    .build<K, Byte>()

  override fun get(key: K): K? {
    return if (Objects.nonNull(internalSimpleCache.getIfPresent(key))) key
    else null
  }

  override fun putIfAbsent(key: K): Boolean {
    val cacheEntry = internalSimpleCache.getIfPresent(key)
    if (Objects.isNull(cacheEntry)) {
      internalSimpleCache.put(key, 0.toByte())
      return true
    }
    return false
  }

  override fun put(key: K): Boolean {
    internalSimpleCache.put(key, 0.toByte())
    return true
  }
}
