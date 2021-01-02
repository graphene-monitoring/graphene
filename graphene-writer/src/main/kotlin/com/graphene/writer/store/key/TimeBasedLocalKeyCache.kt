package com.graphene.writer.store.key

import com.google.common.cache.CacheBuilder
import com.graphene.common.utils.DateTimeUtils
import java.util.Objects
import java.util.concurrent.TimeUnit

/**
 *
 * This class is not thread safe.
 * If you need to handle parallel operation, please consider double-check locking at getOrCreate method.
 *
 * @author dark
 */
class TimeBasedLocalKeyCache<K>(
  private val expireInterval: Long
) : KeyCache<K> {

  private val internalTimeBasedCache = CacheBuilder.newBuilder()
    .expireAfterAccess(expireInterval, TimeUnit.SECONDS)
    .build<Long, MutableSet<K>>()

  override fun get(key: K): K? {
    return if (getOrCreate().contains(key)) key
    else null
  }

  override fun putIfAbsent(key: K): Boolean {
    return if (getOrCreate().contains(key)) false
    else put(key)
  }

  override fun put(key: K): Boolean {
    getOrCreate().add(key)
    return true
  }

  private fun getOrCreate(): MutableSet<K> {
    val normalizedTimeBucket = DateTimeUtils.currentTimeMillis() / TimeUnit.MINUTES.toMillis(expireInterval) * TimeUnit.MINUTES.toMillis(expireInterval)
    var cache = internalTimeBasedCache.getIfPresent(normalizedTimeBucket)

    if (Objects.isNull(cache)) {
      cache = mutableSetOf()
      internalTimeBasedCache.put(normalizedTimeBucket, cache)
    }

    return internalTimeBasedCache.getIfPresent(normalizedTimeBucket)!!
  }
}
