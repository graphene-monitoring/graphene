package com.graphene.common.utils

import java.security.MessageDigest

/**
 * Hashing Utils
 * @author Sam Clarke <www.samclarke.com>
 * @license MIT
 */
object HashUtils {
  fun String.sha512() = this.hashWithAlgorithm("SHA-512")

  fun String.sha256() = this.hashWithAlgorithm("SHA-256")

  fun String.sha1() = this.hashWithAlgorithm("SHA-1")

  /**
   * Supported algorithms on Android:
   *
   * Algorithm	Supported API Levels
   * MD5          1+
   * SHA-1	    1+
   * SHA-224	    1-8,22+
   * SHA-256	    1+
   * SHA-384	    1+
   * SHA-512	    1+
   */
  private fun String.hashWithAlgorithm(type: String): String {
    val HEX_CHARS = "0123456789abcdef"
    val bytes = MessageDigest
      .getInstance(type)
      .digest(this.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
      val i = it.toInt()
      result.append(HEX_CHARS[i shr 4 and 0x0f])
      result.append(HEX_CHARS[i and 0x0f])
    }

    return result.toString()
  }
}
