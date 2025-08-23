package org.example.utils

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateOrderNumber(): String {
    val timestamp = LocalDateTime.now().year
    val uuidString = Uuid.random().toString().substring(0, 8).uppercase()
    return "ORD-$timestamp-$uuidString"
}
