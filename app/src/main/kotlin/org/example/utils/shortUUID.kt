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