package org.example.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

data class ApiResponse<T>(
    val statusCode: HttpStatusCode,
    val data: T? = null,
    val message: String? = null,
    val errors: List<String>? = null
)

suspend fun <T> ApplicationCall.respondApi(
    status: HttpStatusCode = HttpStatusCode.OK,
    data: T? = null,
    message: String? = null,
    errors: List<String>? = null
) {
    val response = ApiResponse(status, data, message, errors)
    respond(status, response)
}

