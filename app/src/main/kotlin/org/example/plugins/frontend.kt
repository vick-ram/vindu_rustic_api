package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.thymeleaf.Thymeleaf
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.thymeleaf.context.IExpressionContext
import org.thymeleaf.linkbuilder.ILinkBuilder
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver

private class KtorLinkBuilder : ILinkBuilder {
    override fun getName(): String = "KtorLinkBuilder"

    override fun getOrder(): Int = 1

    override fun buildLink(context: IExpressionContext?, base: String?, parameters: Map<String?, Any?>?): String? {
        if (base == null) return null
        return if (base.startsWith("/")) {
            base
        } else {
            "/$base"
        }
    }
}

object FrontendModule : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)

        application.installTemplateContext()

        application.install(Thymeleaf) {
            setTemplateResolver(
                (if (config.server.development) {
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
}

