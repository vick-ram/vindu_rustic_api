package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.AddressMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.identity.Address
import java.math.BigDecimal

@Component
class AddressRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    addressMapper: AddressMapper
) : CrudRepository<Address, String>(
    connectionFactory = connectionFactory,
    tableName = "addresses",
    mapper = addressMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get addresses by user
    suspend fun findByUserId(userId: String): List<Address> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = $1 
            ORDER BY is_default DESC, created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("$1" to userId))
    }

    // Get default address for user
    suspend fun findDefaultByUserId(userId: String): Address? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = $1 AND is_default = true 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("$1", userId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Set address as default (and unset others)
    suspend fun setDefault(userId: String, addressId: String): Address? {
        return connectionFactory.withTransaction { connection ->
            // First, unset all defaults for this user
            val unsetSql = """
                UPDATE $tableName 
                SET is_default = false 
                WHERE user_id = $1 AND is_default = true
            """.trimIndent()

            connection.createStatement(unsetSql)
                .bind("$1", userId)
                .execute()
                .awaitSingle()

            // Then set the new default
            val setSql = """
                UPDATE $tableName 
                SET is_default = true 
                WHERE id = $1 AND user_id = $2 
                RETURNING *
            """.trimIndent()

            connection.createStatement(setSql)
                .bind("$1", addressId)
                .bind("$2", userId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Search addresses by recipient name or phone
    suspend fun searchByUser(
        userId: String,
        query: String
    ): List<Address> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = $1 
              AND (recipient_name ILIKE :query 
                   OR phone_number ILIKE :query 
                   OR address_line1 ILIKE :query 
                   OR city ILIKE :query)
            ORDER BY is_default DESC, created_at DESC
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "$1" to userId,
                "query" to "%$query%"
            )
        )
    }

    // Find addresses near a location (requires PostGIS or similar)
    suspend fun findNearby(
        latitude: BigDecimal,
        longitude: BigDecimal,
        radiusKm: Double = 10.0,
        limit: Int = 20
    ): List<Address> {
        // This assumes you have PostGIS installed
        val sql = """
            SELECT *, 
                   (6371 * acos(cos(radians(:latitude)) 
                    * cos(radians(latitude)) 
                    * cos(radians(longitude) - radians(:longitude)) 
                    + sin(radians(:latitude)) 
                    * sin(radians(latitude)))) AS distance
            FROM $tableName 
            WHERE latitude IS NOT NULL 
              AND longitude IS NOT NULL
              AND (6371 * acos(cos(radians(:latitude)) 
                   * cos(radians(latitude)) 
                   * cos(radians(longitude) - radians(:longitude)) 
                   + sin(radians(:latitude)) 
                   * sin(radians(latitude)))) <= :radiusKm
            ORDER BY distance ASC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "latitude" to latitude.toDouble(),
                "longitude" to longitude.toDouble(),
                "radiusKm" to radiusKm,
                "limit" to limit
            )
        )
    }

    // Count addresses for a user
    suspend fun countByUser(userId: String): Long {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE user_id = $1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("$1", userId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) }
                .awaitFirstOrNull() ?: 0L
        }
    }

    // Validate address ownership
    suspend fun isOwnedBy(addressId: String, userId: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE id = $1 AND user_id = $2
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("$1", addressId)
                .bind("$2", userId)
                .execute()
                .awaitSingle()
                .map { row, _ -> (row.get("count", Long::class.java) ?: 0) > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Bulk create addresses
    suspend fun bulkCreate(addresses: List<Address>): List<Address> {
        if (addresses.isEmpty()) return emptyList()

        val columns = listOf(
            "user_id", "label", "recipient_name", "phone_number",
            "country_code", "country", "city", "state", "postal_code",
            "address_line1", "address_line2", "latitude", "longitude", "is_default"
        )

        val placeholders = List(addresses.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            addresses.forEachIndexed { index, address ->
                statement.bind("recipient_name_$index", address.recipientName)
                statement.bind("phone_number_$index", address.phoneNumber)
                statement.bind("country_code_$index", address.countryCode)
                statement.bind("country_$index", address.country)
                statement.bindNullable("user_id_$index", address.userId, String::class.java)
                statement.bindNullable("label_$index", address.label, String::class.java)
                statement.bindNullable("city_$index", address.city, String::class.java)
                statement.bindNullable("state_$index", address.state, String::class.java)
                statement.bindNullable("postal_code_$index", address.postalCode, String::class.java)
                statement.bindNullable("address_line2_$index", address.addressLine2, String::class.java)
                statement.bindNullable("latitude_$index", address.latitude, BigDecimal::class.java)
                statement.bindNullable("longitude_$index", address.longitude, BigDecimal::class.java)
                statement.bind("address_line1_$index", address.addressLine1)
                statement.bind("is_default_$index", address.isDefault)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }
}