package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.thymeleaf.Thymeleaf
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.context.IExpressionContext
import org.thymeleaf.linkbuilder.ILinkBuilder
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver

fun Application.configureFrontend() {
    install(Thymeleaf) {
        setTemplateResolver(
            (if (developmentMode) {
            FileTemplateResolver().apply {
                cacheManager = null
                prefix = "app/src/main/resources/templates/"
            }
        } else {
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
            }
        }).apply {
            suffix = ".html"
            characterEncoding = "utf-8"
        })

        addDialect(LayoutDialect())

        setLinkBuilder(KtorLinkBuilder())

    }
}

class KtorLinkBuilder : ILinkBuilder {
    override fun getName(): String? = "KtorLinkBuilder"

    override fun getOrder(): Int? = 1

    override fun buildLink(context: IExpressionContext?, base: String?, parameters: Map<String?, Any?>?): String? {
        if (base == null) return null
        return if (base.startsWith("/")) {
            base
        } else {
            "/$base"
        }
    }
}

