package org.example.di

import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import org.example.domain.repo.NotificationRepository
import org.example.utils.EnvironmentConfig
import org.example.utils.notifications.DatabaseNotificationService
import org.example.utils.notifications.EmailNotificationService
import org.example.utils.notifications.FcmNotificationService
import org.example.utils.notifications.SMSService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val applicationClassModule = module {
    single { EnvironmentConfig(get<Application>()) }
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                gson()
            }
        }
    }
    single {
        GsonBuilder()
            .setPrettyPrinting()
            .create()
    }
}

val serviceModule = module {
    single { EmailService(get()) }
    single<NotificationRepository>(named("email")) {
        EmailNotificationService(get())
    }
    single<NotificationRepository>(named("fcm")) {
        FcmNotificationService(get(), get())
    }
    single<NotificationRepository>(named("database")) {
        DatabaseNotificationService(get())
    }

    single<NotificationRepository>(named("sms")) {
        SMSService(get())
    }
}
