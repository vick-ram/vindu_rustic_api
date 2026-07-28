package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CouponMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import org.example.plugins.NotFoundException
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class CouponRepository @Inject constructor(connectionFactory: ConnectionFactory, couponMapper: CouponMapper) :
    CrudRepository<Coupon, String>(
        connectionFactory = connectionFactory,
        tableName = "coupons",
        mapper = couponMapper
    ) {

    suspend fun findByCode(code: String): Coupon? {
        val sql = "SELECT * FROM coupons WHERE code = :code"
        return executeQuery(sql, mapOf("code" to code), mapper = rowMapper).firstOrNull()
    }

    suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon? {
        val now = OffsetDateTime.now()

        val sql = """
            SELECT * FROM coupons
                WHERE code = :code
                AND is_active = true
                AND (starts_at IS NULL OR starts_at <= :now)
                AND (ends_at IS NULL OR ends_at <= :now)
            LIMIT 1
        """.trimIndent()

        val coupon = executeQuery(
            sql = sql,
            params = mapOf("code" to code, "now" to now),
            mapper = rowMapper
        ).firstOrNull() ?: return null

        coupon.minOrderAmount?.let { minAmount ->
            if (orderAmount < minAmount) return null
        }
        // Check usage limit
        coupon.usageLimit?.let { limit ->
            if (coupon.usageCount >= limit) return null
        }

        return coupon
    }

    suspend fun applyCoupon(
        couponId: String,
        orderId: String,
        userId: String,
        discountAmount: BigDecimal
    ): CouponUsage = connectionFactory.withTransaction { connection ->
        val usageId = Ulid.generate()
        val sql = """
            INSERT INTO coupon_usages (id, coupon_id, order_id, user_id, discount_amount, created_at)
            VALUES (:id, :couponId, :orderId, :userId, :discountAmount, :createdAt)
            RETURNING *
        """.trimIndent()

        // Insert coupon usage and map the resulting record directly
        connection.createNamedStatement(
            sql, mapOf(
                "id" to usageId,
                "couponId" to couponId,
                "orderId" to orderId,
                "userId" to userId,
                "discountAmount" to discountAmount,
                "createdAt" to OffsetDateTime.now()
            )
        )
            .execute()
            .awaitSingle()
            .map { row, _ ->
                CouponUsage(
                    id = row.get("id", String::class.java)!!,
                    couponId = row.get("coupon_id", String::class.java)!!,
                    orderId = row.get("order_id", String::class.java)!!,
                    userId = row.get("user_id", String::class.java)!!,
                    discountAmount = row.get("discount_amount", BigDecimal::class.java)!!
                )
            }
            .awaitSingle()
    }

    suspend fun incrementUsage(couponId: String): Boolean = connectionFactory.withTransaction { connection ->
        val sql = """
            UPDATE coupons 
            SET usage_count = usage_count + 1, updated_at = :now
            WHERE id = :id
        """.trimIndent()

        val updatedRows = connection.createNamedStatement(sql, mapOf("id" to couponId, "'now" to OffsetDateTime.now()))
            .execute()
            .awaitSingle()
            .rowsUpdated
            .awaitSingle()

        if (updatedRows == 0L) {
            throw NotFoundException("Coupon with ID $couponId not found")
        }

        true
    }
}