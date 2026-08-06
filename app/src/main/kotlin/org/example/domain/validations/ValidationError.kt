package org.example.domain.validations

data class ValidationError(
    val field: String,
    val errors: List<String>
)