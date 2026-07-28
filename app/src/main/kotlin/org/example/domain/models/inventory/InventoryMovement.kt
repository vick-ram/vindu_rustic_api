package org.example.domain.models.inventory

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class InventoryMovement(
    val id: String = Ulid.generate(),

    @SerialName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @OneOf("inbound", "outbound", "adjustment", "reservation_hold", "reservation_release")
    @SerialName(value = "movement_type")
    val movementType: String,

    val quantity: Int,

    @SerialName(value = "reference_type")
    val referenceType: String? = null,

    @SerialName(value = "reference_id")
    val referenceId: String? = null,

    @SerialName(value = "performed_by")
    val performedBy: String? = null,

    val notes: String? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
