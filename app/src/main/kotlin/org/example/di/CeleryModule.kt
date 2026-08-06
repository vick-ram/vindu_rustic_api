package org.example.di

import io.celery.CeleryApp
import org.koin.dsl.module

val celeryModule = module {
    single<CeleryApp> {
        CeleryApp(
            name = "vindu_rustic",
        )
    }
}