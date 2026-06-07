package org.example.config

import io.ktor.server.application.Application

interface ApplicationPlugin {
    fun install(application: Application)
}

object PluginRegistry {
    private val plugins = mutableListOf<ApplicationPlugin>()

    fun register(vararg plugin: ApplicationPlugin) {
        plugins.addAll(plugin)
    }

    fun installAll(application: Application) {
        plugins.forEach {it.install(application)}
    }
}

annotation class AutoInstall