package org.example.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import org.example.plugins.setCurrentTemplate

private data class ApiResponse<T>(
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

suspend fun <T: Any> ApplicationCall.respondHtml(
    template: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    data: T? = null,
    message: String? = null,
    errors: List<String>? = null,
    mapData: MutableMap<String, Any> = mutableMapOf()
) {
    mapData["status"] = status.value
    data?.let { mapData["data"] = it }
    message?.let { mapData["message"] = it }
    errors?.let { mapData["errors"] = it }

    setCurrentTemplate(template)

    respond(ThymeleafContent(template, mapData))
}

