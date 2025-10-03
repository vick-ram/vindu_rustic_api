package org.example.utils

import io.ktor.server.application.Application

class EnvironmentConfig(private val application: Application) {
    /**
     * Get required environment variable
     * @throws IllegalStateException if variable is not found
     */
    fun getRequired(key: String): String {
        return getOptional(key) ?: throw IllegalStateException(
            "Required environment variable '$key' is not set"
        )
    }

    /**
     * Get optional environment variable
     */
    fun getOptional(key: String): String? {
        return application.environment.config.propertyOrNull(key)?.getString()
    }

    /**
     * Get environment variable with default value
     */
    fun get(key: String, default: String): String {
        return getOptional(key) ?: default
    }

    /**
     * Get integer environment variable
     * @throws IllegalStateException
     */
    fun getInt(key: String, default: Int? = null): Int {
        return getOptional(key)?.toIntOrNull() ?: default ?: throw IllegalStateException(
            "Environment variable '$key' is not a valid integer or not set"
        )
    }

    /**
     * Get Long integer environment variable
     * @throws IllegalStateException
     */
    fun getLong(key: String, default: Long? = null): Long {
        return getOptional(key)?.toLongOrNull() ?: default ?: throw IllegalStateException(
            "Environment variable '$key' is not a valid Long integer or not set"
        )
    }

    /**
     * Get boolean environment variable
     * @throws IllegalStateException
     */
    fun getBoolean(key: String, default: Boolean? = null): Boolean {
        return when(getOptional(key)?.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            null -> default ?: throw IllegalStateException(
                "Environment variable '$key' is not set"
            )
            else -> throw IllegalStateException(
                "Environment variable '$key' is not a valid boolean"
            )
        }
    }

    /**
     * Get environment variable as a list (comma-separated)
     */
    fun getList(key: String, default: List<String> = emptyList()): List<String> {
        return getOptional(key)?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() } ?: default
    }

    /**
     * Check if environment variable exists
     */
    fun exists(key: String): Boolean {
        return getOptional(key) != null
    }

}
