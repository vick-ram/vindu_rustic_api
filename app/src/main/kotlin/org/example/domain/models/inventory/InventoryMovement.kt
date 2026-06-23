package org.example.domain.models.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class InventoryMovement(
    val id: String = Ulid.generate(),

    @SerialName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @SerialName(value = "movement_type")
    val movementType: String,

    val quantity: Int,

    @SerialName(value = "reference_type")
    val referenceType: String? = null,

    @SerialName(value = "reference_id")
    val referenceId: Long? = null,

    @SerialName(value = "performed_by")
    val performedBy: String? = null,

    val notes: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
