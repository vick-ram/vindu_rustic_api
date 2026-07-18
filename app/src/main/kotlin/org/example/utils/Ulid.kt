package org.example.utils

import java.math.BigInteger
import java.security.SecureRandom

object Ulid {
    private val random = SecureRandom()

    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private var lastTimestamp = -1L
    private var lastRandom = ByteArray(10)

    @Synchronized
    fun generate(): String {
        val timestamp = System.currentTimeMillis()

        if (timestamp == lastTimestamp) {
            increment(lastRandom)
        } else {
            random.nextBytes(lastRandom)
            lastTimestamp = timestamp
        }

        val bytes = ByteArray(16)

        bytes[0] = (timestamp ushr 40).toByte()
        bytes[1] = (timestamp ushr 32).toByte()
        bytes[2] = (timestamp ushr 24).toByte()
        bytes[3] = (timestamp ushr 16).toByte()
        bytes[4] = (timestamp ushr 8).toByte()
        bytes[5] = timestamp.toByte()

        System.arraycopy(lastRandom, 0, bytes, 6, 10)

        return encodeBase32(bytes)
    }

    private fun increment(bytes: ByteArray) {
        for (i in bytes.indices.reversed()) {
            bytes[i]++
            if (bytes[i] != 0.toByte()) break
        }
    }

    private fun encodeBase32(bytes: ByteArray): String {
        var value = BigInteger(1, bytes)
        val result = CharArray(26)

        for (i in 25 downTo 0) {
            result[i] = ENCODING[(value.and(BigInteger.valueOf(31)).toInt())]
            value = value.shiftRight(5)
        }
        return String(result)
    }
}