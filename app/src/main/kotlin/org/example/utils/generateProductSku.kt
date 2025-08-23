package org.example.utils

import kotlinx.datetime.LocalDateTime

fun generateProductSku(categoryName: String): String {
    val timestamp = LocalDateTime.now().year.toString()

    val prefix = categoryName.take(3).uppercase()

    val chars = ('A'..'Z') + ('0'..'9')
    val randomSuffix = (1..6)
        .map { chars.random() }
        .joinToString("")

    return "$prefix-$timestamp-$randomSuffix"
}