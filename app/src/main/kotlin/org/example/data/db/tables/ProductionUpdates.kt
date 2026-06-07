package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object ProductionUpdates : CustomTable("production_updates") {
    val jobId = reference("job_id", ProductionJobs)
    val status = varchar("status", 50)
    val stageName = varchar("stage_name", 255).nullable()
    val description = text("description").nullable()
    val imageUrl = text("image_url").nullable()
    val postedBy = reference("posted_by", Users)
}