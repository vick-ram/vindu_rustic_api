package org.example.domain.repo

import org.example.data.db.entities.UserEntity
import org.example.domain.models.identity.Session

interface SessionRepository: CrudRepository<Session, String> {
    suspend fun findByRefreshToken(refreshToken: String): Session?
    suspend fun deleteAllForUser(userId: String)
}