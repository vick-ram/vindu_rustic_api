package org.example.plugins

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

val appHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        Gson()
    }
}