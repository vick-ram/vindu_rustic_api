import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    kotlin("plugin.serialization").version("2.1.20")
}

group = "org.example"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    project.setProperty("mainClassName", mainClass.get())

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // This dependency is used by the application.
    implementation(kotlin("stdlib"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.sessions)
    implementation(libs.bcrypt.hashing)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.hsts)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.swagger.core)
    implementation(libs.ktor.swagger.ui)

    //Ktor client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.pesapal.kotlin.client)

    //Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)

    //Webjars
    implementation(libs.ktor.server.webjars)
    implementation(libs.ktor.server.thymeleaf)
    implementation(libs.thymeleaf.layout.dialect)
    implementation(libs.webjars.locator.core)
    implementation(libs.feather)
    implementation(libs.tabler)

    //Logging
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.call.logging)

    //Database
    implementation(libs.postgresql)
    implementation(libs.r2dbc)
    implementation(libs.postgresql.pool)

    //Migrations
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.r2)

    //Redis
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.lattuce.core)

    //Mail
    implementation(libs.jakarta.mail)
    implementation(libs.firebase.admin)
    implementation(libs.sns.client)

    //DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)
    implementation(libs.koin.logger)

    implementation(libs.skiko)

    implementation("com.github.vick-ram:kcelery:0.0.1")

    // Test
    runtimeOnly(libs.kotest.runner)
    testImplementation(libs.kotest.assertion)
    testImplementation(libs.ktor.kotest.assertion)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        // Enforces the use of the new feature
        freeCompilerArgs.add("-Xcollection-literals")
    }
}

application {
    // Define the main class for the application.
    mainClass = "org.example.AppKt"
}

fun loadEnvVars(): Map<String, String> {
    val configFile = file("../.idea/workspace.xml")
    if (!configFile.exists()) return emptyMap()

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configFile)
    val envs = doc.getElementsByTagName("env")
    val result = mutableMapOf<String, String>()

    for (i in 0 until envs.length) {
        val node = envs.item(i) as Element
        val name = node.getAttribute("name")
        val value = node.getAttribute("value")
        result[name] = value
    }
    return result
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.findByName("run")?.let { runTask ->
    if (runTask is JavaExec) {
        val envVars = loadEnvVars()
        envVars.forEach { (key, value) ->
            runTask.systemProperty(key, value)
        }
    }
}

