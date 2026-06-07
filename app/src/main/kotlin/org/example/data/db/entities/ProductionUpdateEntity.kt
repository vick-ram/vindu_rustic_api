package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductionUpdates
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductionUpdateEntity(id: EntityID<String>) : CustomEntity(id, ProductionUpdates) {
    companion object : CustomEntityClass<ProductionUpdateEntity>(ProductionUpdates)

    var jobId by ProductionUpdates.jobId
    var status by ProductionUpdates.status
    var stageName by ProductionUpdates.stageName
    var description by ProductionUpdates.description
    var imageUrl by ProductionUpdates.imageUrl
    var postedBy by ProductionUpdates.postedBy
}