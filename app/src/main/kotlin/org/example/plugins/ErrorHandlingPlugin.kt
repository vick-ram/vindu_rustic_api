package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import org.example.config.ApplicationPlugin

class ErrorHandlingPlugin(
    private val exceptionMapper: ExceptionMapper = ExceptionMapper(),
    private val responseStrategies: List<ResponseStrategy> = listOf(
        HtmlResponseStrategy(),
        ApiResponseStrategy()
    )
) : ApplicationPlugin {

    override fun install(application: Application) {
        application.install(StatusPages) {
            exception<Throwable> { call, cause ->
                handleException(application, call, cause)
            }
        }
    }

    private suspend fun handleException(app: Application, call: ApplicationCall, exception: Throwable) {
        // Log the exception
        app.environment.log.error("Error handling request: ${call.request.path()}", exception)

        val (status, message, errors) = exceptionMapper.map(exception)
        val strategy = selectStrategy(call)

        strategy.respond(call, status, message, errors)
    }

    private fun selectStrategy(call: ApplicationCall): ResponseStrategy {
        val accept = call.request.headers["Accept"] ?: ""
        return if (accept.contains("text/html")) {
            responseStrategies.filterIsInstance<HtmlResponseStrategy>().first()
        } else {
            responseStrategies.filterIsInstance<ApiResponseStrategy>().first()
        }
    }
}