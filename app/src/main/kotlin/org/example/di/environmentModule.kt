package org.example.di

import io.ktor.server.application.*
import org.example.domain.repo.NotificationRepository
import org.example.utils.*
import org.example.utils.notifications.DatabaseNotificationService
import org.example.utils.notifications.EmailNotificationService
import org.example.utils.notifications.FcmNotificationService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val applicationClassModule = module {
    single { EnvironmentConfig(get<Application>()) }
}

val configModule = module {
    single { AppConfig.fromEnvironment(get<EnvironmentConfig>()) }
    single { SecurityConfig.fromEnvironment(get<EnvironmentConfig>()) }
    single { EmailConfig.fromEnvironment(get<EnvironmentConfig>()) }
    single { DatabaseConfig.fromEnvironment(get<EnvironmentConfig>()) }
    single { FcmConfig.fromEnvironment(get<EnvironmentConfig>()) }
}

val serviceModule = module {
//    single<CrudRepository<Notification, String>>(named("notifReal")) { NotificationService() }
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
}

