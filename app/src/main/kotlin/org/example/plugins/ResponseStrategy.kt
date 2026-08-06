package org.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import org.example.config.FormPages
import org.example.domain.models.ErrorResponse
import org.example.utils.respondHtml

interface ResponseStrategy {
    suspend fun respond(call: ApplicationCall, status: HttpStatusCode, message: String, errors: List<String>?)
}

class HtmlResponseStrategy : ResponseStrategy {
    override suspend fun respond(call: ApplicationCall, status: HttpStatusCode, message: String, errors: List<String>?) {
        val currentTemplate = call.getCurrentTemplate()
        val requestPath = call.request.path()

        val useCurrentTemplate = when {
            status == HttpStatusCode.BadRequest && FormPages.matches(currentTemplate, requestPath) -> true
            status == HttpStatusCode.Conflict && FormPages.matches(currentTemplate, requestPath) -> true
            currentTemplate != null && shouldUseCurrentTemplate(status, currentTemplate, requestPath) -> true
            else -> false
        }

        if (useCurrentTemplate && currentTemplate != null) {
            call.respondHtml<Unit>(
                template = currentTemplate,
                status = status,
                message = message,
                errors = errors
            )
        } else {
            val model: MutableMap<String, Any> = mutableMapOf(
                "status" to status.value,
                "message" to message,
                "errorCode" to when (status) {
                    HttpStatusCode.NotFound -> "404"
                    HttpStatusCode.Unauthorized -> "401"
                    else -> "500"
                },
            )

            if (errors != null) {
                model["errors"] = errors
            }

            val template = when (status) {
                HttpStatusCode.NotFound -> "error/index.html"
                HttpStatusCode.Unauthorized -> "auth/login.html"
                else -> "error/index.html"
            }

            call.respond(status, ThymeleafContent(template, model))
        }
    }

    private fun shouldUseCurrentTemplate(status: HttpStatusCode, template: String, path: String): Boolean {
        return when (status) {
            HttpStatusCode.BadRequest, HttpStatusCode.Conflict, HttpStatusCode.Unauthorized ->
                FormPages.matches(template, path)
            else -> false
        }
    }
}

class ApiResponseStrategy : ResponseStrategy {
    override suspend fun respond(call: ApplicationCall, status: HttpStatusCode, message: String, errors: List<String>?) {
        call.respond(
            status,
            ErrorResponse(
                status = status.value,
                message = message,
                errors = errors,
                path = call.request.path()
            )
        )
    }
}