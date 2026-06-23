package org.example.config

import io.ktor.server.application.Application

data class AppConfig(
    val database: DatabaseConfig,
    val security: SecurityConfig,
    val cors: CorsConfig,
    val logging: CallLoggingConfig,
    val email: EmailConfig,
    val fcm: FcmConfig,
    val sms: SMSConfig,
    val payment: PaymentConfig,
    val server: ServerConfig
) {
    companion object {
        fun load(application: Application) : AppConfig {
            val env = EnvironmentConfig(application)

            return AppConfig(
                database = DatabaseConfig(
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
                    useServerPrepStmts = env.getBoolean("database.useServerPrepStmts", true),
                    runMigrations = env.getBoolean("database.runMigrations", true)
                ),
                security = SecurityConfig(
                    secret = env.getRequired("jwt.secret"),
                    issuer = env.getRequired("jwt.issuer"),
                    audience = env.getRequired("jwt.audience"),
                    realm = env.getRequired("jwt.realm"),
                    clientID = env.getRequired("google.clientID"),
                    clientSecret = env.getRequired("google.clientSecret"),
                    secretEncryptionKey = env.getRequired("session.encryptionKey"),
                    secretSignKey = env.getRequired("session.signKey")
                ),
                cors = CorsConfig(
                    allowedHosts = env.getList("cors.allowedHosts", listOf("*")),
                    allowedMethods = env.getList("cors.allowedMethods", listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")),
                    allowedHeaders = env.getList("cors.allowedHeaders", listOf("*")),
                    allowCredentials = env.getBoolean("cors.allowCredentials", true),
                    maxAgeSeconds = env.getLong("cors.maxAge", 3600)
                ),
                logging = CallLoggingConfig(
                    enabled = env.getBoolean("logging.enabled", true),
                    logLevel = env.get("logging.level", "INFO"),
                    excludePaths = env.getList("logging.excludePaths")
                ),
                email = EmailConfig(
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
                ),
                fcm = FcmConfig(
                    credentialPath = env.getRequired("fcm.credentialPath"),
                    projectId = env.getRequired("fcm.projectId"),
                ),
                sms = SMSConfig(
                    accessKey = env.get("sms.accessKey", ""),
                    secretKey = env.get("sms.secretKey", ""),
                    region = env.get("sms.region", "us-east-1")
                ),
                payment = PaymentConfig(
                    consumerKey = env.getRequired("pesapal.consumerKey"),
                    consumerSecret = env.getRequired("pesapal.consumerSecret"),
                    baseUrl = env.getRequired("pesapal.baseUrl"),
                    ipnUrl = env.getRequired("pesapal.ipnUrl"),
                    callbackUrl = env.getRequired("pesapal.callbackUrl"),
                    cancellationUrl = env.getRequired("pesapal.cancellationUrl"),
                ),
                server = ServerConfig(
                    host = env.get("server.host", "0.0.0.0"),
                    port = env.getInt("server.port", 8080),
                    development = env.getBoolean("server.development", false),
                    ssl = SslConfig(
                        enabled = env.getBoolean("server.ssl.enabled", false),
                        keyStorePath = env.getOptional("server.ssl.keyStorePath"),
                        keyStorePassword = env.getOptional("server.ssl.keyStorePassword"),
                        privateKeyPassword = env.getOptional("server.ssl.privateKeyPassword")
                    )
                )
            )
        }
    }
}

data class OAuthProvider(
    val clientId: String,
    val clientSecret: String,
    val authorizeUrl: String,
    val tokenUrl: String,
    val userInfoUrl: String,
    val scopes: List<String>
)

data class SecurityConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessExpiry: Long = 15 * 60 * 1000L,
    val refreshExpiry: Long = 7 * 24 * 60 * 60 * 1000L,
    val oauthConfig: OAuthProvider,
    val secretEncryptionKey: String,
    val secretSignKey: String
)

data class EmailConfig(
    val smtpHost: String,
    val smtpPort: Int,
    val username: String,
    val password: String,
    val fromEmail: String,
    val debugMode: Boolean = false,
    val timeouts: TimeoutConfig = TimeoutConfig()
)

data class TimeoutConfig(
    val connection: Int = 5000,
    val read: Int = 5000,
    val write: Int = 5000
)

data class FcmConfig(
    val credentialPath: String? = null,
    val projectId: String? = null
)

data class DatabaseConfig(
    val dbHost: String = "localhost",
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
    val useServerPrepStmts: Boolean,
    val runMigrations: Boolean,
    val isDevMode: Boolean = false
) {
    val jdbcUrl: String
        get() = when {
            driver == "h2" -> "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            else -> "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        }

    val r2dbcUrl: String
        get() = when {
            driver == "h2" -> "r2dbc:h2:mem:///$dbName"
            else -> "r2dbc:postgresql://$user:$password@$dbHost:$dbPort/$dbName"
        }
}

data class PaymentConfig(
    val consumerKey: String,
    val consumerSecret: String,
    val baseUrl: String,
    val ipnUrl: String,
    val callbackUrl: String,
    val cancellationUrl: String,
)

data class CallLoggingConfig(
    val enabled: Boolean,
    val logLevel: String,
    val excludePaths: List<String>
)

data class CorsConfig(
    val allowedHosts: List<String>,
    val allowedMethods: List<String>,
    val allowedHeaders: List<String>,
    val allowCredentials: Boolean,
    val maxAgeSeconds: Long
)

data class SslConfig(
    val enabled: Boolean,
    val keyStorePath: String?,
    val keyStorePassword: String?,
    val privateKeyPassword: String?
)

data class ServerConfig(
    val host: String,
    val port: Int,
    val development: Boolean,
    val ssl: SslConfig
)

data class SMSConfig(
    val accessKey: String,
    val secretKey: String,
    val region: String
)
