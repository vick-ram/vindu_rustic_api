package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.webjars.Webjars

fun Application.configureWebjar() {
    install(Webjars) {
    path = "/webjars"
}}
