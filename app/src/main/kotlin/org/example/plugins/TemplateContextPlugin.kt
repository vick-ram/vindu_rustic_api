package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.util.AttributeKey
import org.example.config.ApplicationPlugin

class TemplateContextPlugin : ApplicationPlugin {
    override fun install(application: Application) {
        application.installTemplateContext()
    }
}

class TemplateContext(var currentTemplate: String? = null)

val TemplateContextKey = AttributeKey<TemplateContext>("TemplateContext")

fun Application.installTemplateContext() {
    intercept(ApplicationCallPipeline.Setup) {
        call.attributes.put(TemplateContextKey, TemplateContext())
        proceed()
    }
}

fun ApplicationCall.setCurrentTemplate(template: String) {
    attributes[TemplateContextKey].currentTemplate = template
}

fun ApplicationCall.getCurrentTemplate(): String? {
    return attributes.getOrNull(TemplateContextKey)?.currentTemplate
}