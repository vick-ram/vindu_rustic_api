package org.example.utils

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

fun shortUUID(): String {
    return UUID.randomUUID()
        .toString()
        .substring(0, 10)
}

private val counter = AtomicInteger(1)

fun numberedShortUUID(): String {
    val number = counter.getAndIncrement()
    val shortId = UUID.randomUUID().toString().substring(0, 10)
    return "$number-$shortId"
}

object CompactId {
    private const val BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val EPOCH = 1700000000000L

    private var lastTimestamp = -1L
    private var sequence = 0

    @Synchronized
    fun generate(): String {
        var timestamp = System.currentTimeMillis() - EPOCH
        if (timestamp == lastTimestamp) {
            sequence++
        } else {
            sequence = 0
            lastTimestamp = timestamp
        }

        return encode(timestamp, 8) + encode(sequence.toLong(), 2)
    }

    private fun encode(value: Long, minLength: Int): String {
        var num = value
        val sb = StringBuilder()

        repeat(minLength) {
            sb.insert(0, BASE62[(num % 62).toInt()])
            num /= 62
        }
        return sb.toString()
    }
}