package org.example.domain.models.inventory

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import java.io.Serializable
import java.time.OffsetDateTime

data class InventoryMovement(
    val id: String,

    @SerializedName(value = "variant_id")
    val variantId: String,

    val warehouseId: String,

    @SerializedName(value = "movement_type")
    val movementType: String,

    val quantity: Int,

    @SerializedName(value = "reference_type")
    val referenceType: String? = null,

    @SerializedName(value = "reference_id")
    val referenceId: Long? = null,

    @SerializedName(value = "performed_by")
    val performedBy: String? = null,

    val notes: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
