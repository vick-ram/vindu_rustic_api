package org.example.utils

import io.ktor.http.HttpStatusCode

data class ApiResponse<T>(
    val statusCode: HttpStatusCode,
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> success(status: HttpStatusCode = HttpStatusCode.OK, data: T, message: String): ApiResponse<T> {
            return ApiResponse(statusCode = status, data = data, message = message)
        }

        fun <T> error(status: HttpStatusCode = HttpStatusCode.InternalServerError, msg: String): ApiResponse<T> {
            return ApiResponse(statusCode = status, message = msg)
        }
    }
}