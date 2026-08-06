package org.example.domain.models

import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String>?,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
)
