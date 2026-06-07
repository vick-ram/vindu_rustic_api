package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductionJobs
import org.example.data.db.tables.ProductionUpdates
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductionJobEntity(id: EntityID<String>) : CustomEntity(id, ProductionJobs) {
    companion object : CustomEntityClass<ProductionJobEntity>(ProductionJobs)

    var orderItemId by ProductionJobs.orderItemId
    var productId by ProductionJobs.productId
    var assignedTo by ProductionJobs.assignedTo
    var status by ProductionJobs.status
    var priority by ProductionJobs.priority
    var quantity by ProductionJobs.quantity
    var notes by ProductionJobs.notes
    var startedAt by ProductionJobs.startedAt
    var completedAt by ProductionJobs.completedAt
    var estimatedCompletionAt by ProductionJobs.estimatedCompletionAt

    val updates by ProductionUpdateEntity referrersOn ProductionUpdates.jobId
}