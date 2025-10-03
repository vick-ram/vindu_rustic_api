package org.example.utils

data class AppConfig(
    val environment: Boolean,
    val logLevel: String,
    val port: Int
) {
    companion object {
        fun fromEnvironment(env: EnvironmentConfig): AppConfig {
            return AppConfig(
                environment = env.getBoolean("ktor.development", true),
                logLevel = env.get("ktor.logLevel", "WARNING"),
                port = env.getInt("ktor.serverPort"),
            )
        }
    }
}

data class SecurityConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val clientID: String,
    val clientSecret: String,
) {
    companion object {
        fun fromEnvironment(env: EnvironmentConfig): SecurityConfig {
            return SecurityConfig(
                secret = env.getRequired("jwt.secret"),
                issuer = env.getRequired("jwt.issuer"),
                audience = env.getRequired("jwt.audience"),
                realm = env.getRequired("jwt.realm"),
                clientID = env.getRequired("google.clientID"),
                clientSecret = env.getRequired("google.clientSecret"),
            )
        }
    }
}

data class EmailConfig(
    val smtpHost: String,
    val smtpPort: Int,
    val username: String,
    val password: String,
    val fromEmail: String,
    val debugMode: Boolean = false,
    val timeouts: TimeoutConfig = TimeoutConfig()
) {
    data class TimeoutConfig(
        val connection: Int = 5000,
        val read: Int = 5000,
        val write: Int = 5000
    )

    companion object {
        fun fromEnvironment(env: EnvironmentConfig): EmailConfig {
            return EmailConfig(
                smtpHost = env.getRequired("email.host"),
                smtpPort = env.getInt("email.port", 587),
                username = env.getRequired("email.username"),
                password = env.getRequired("email.password"),
                fromEmail = env.getRequired("email.fromEmail"),
                debugMode = env.getBoolean("email.debug", false),
                timeouts = TimeoutConfig(
                    connection = env.getInt("email.timeouts.connection"),
                    read = env.getInt("email.timeouts.read"),
                    write = env.getInt("email.timeouts.write")
                ),
            )
        }
    }
}

data class FcmConfig(
    val credentialPath: String? = null,
    val projectId: String? = null
) {
    companion object {
        fun fromEnvironment(env: EnvironmentConfig): FcmConfig {
            return FcmConfig(
                credentialPath = env.getRequired("fcm.credentialPath"),
                projectId = env.getRequired("fcm.projectId"),
            )
        }
    }
}

data class DatabaseConfig(
    val dbPort: Int,
    val driver: String,
    val dbName: String,
    val user: String,
    val password: String,
    val poolSize: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long,
    val minimumIdle: Int,
    val leakDetectionThreshold: Long,
    val cachePrepStmts: Boolean,
    val prepStmtCacheSize: Int,
    val prepStmtCacheSqlLimit: Int,
    val useServerPrepStmts: Boolean
) {
    companion object {
        fun fromEnvironment(env: EnvironmentConfig): DatabaseConfig {
            return DatabaseConfig(
                dbPort = env.getInt("database.port"),
                driver = env.getRequired("database.driver"),
                dbName = env.getRequired("database.db"),
                user = env.getRequired("database.user"),
                password = env.getRequired("database.password"),
                poolSize = env.getInt("database.poolSize", 10),
                connectionTimeout = env.getLong("database.connectionTimeout", 30000),
                idleTimeout = env.getLong("database.idleTimeout", 600000),
                maxLifetime = env.getLong("database.maxLifetime", 1800000),
                minimumIdle = env.getInt("database.minimumIdle", 5),
                leakDetectionThreshold = env.getLong("database.leakDetectionThreshold", 60000),
                cachePrepStmts = env.getBoolean("database.cachePrepStmts", true),
                prepStmtCacheSize = env.getInt("database.prepStmtCacheSize", 250),
                prepStmtCacheSqlLimit = env.getInt("database.prepStmtCacheSqlLimit", 2048),
                useServerPrepStmts = env.getBoolean("database.useServerPrepStmts", true)
            )
        }
    }
}