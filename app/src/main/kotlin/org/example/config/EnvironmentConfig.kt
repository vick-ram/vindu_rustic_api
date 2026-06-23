package org.example.config

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
        val property = application.environment.config.propertyOrNull(key) ?: return null
        return try {
            property.getString()
        } catch (e: Exception) {
            // If it's a list, getString() might fail in some implementations (like YamlConfig)
            property.getList().firstOrNull()
        }
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
     * Get environment variable as a list
     */
    fun getList(key: String, default: List<String> = emptyList()): List<String> {
        val property = application.environment.config.propertyOrNull(key) ?: return default
        return try {
            property.getList()
        } catch (e: Exception) {
            // Fallback: if it's a single string, try to split it by comma
            try {
                property.getString().split(',').map { it.trim() }.filter { it.isNotBlank() }
            } catch (e2: Exception) {
                default
            }
        }
    }

    /**
     * Check if environment variable exists
     */
    fun exists(key: String): Boolean {
        val property = application.environment.config.propertyOrNull(key)
        return property != null
    }

}