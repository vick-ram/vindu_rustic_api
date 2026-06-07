package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.slf4j.event.Level

object LoggingModule : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)

        val logLevel = when (config.logging.logLevel) {
            "INFO" -> Level.INFO
            "ERROR" -> Level.ERROR
            "WARN" -> Level.WARN
            "DEBUG" -> Level.DEBUG
            else -> Level.INFO
        }

        val excludePaths = config.logging.excludePaths

        application.install(CallLogging) {
            level = logLevel
            filter { call ->
                excludePaths.none { call.request.path().startsWith(it) }
            }
        }
    }
}