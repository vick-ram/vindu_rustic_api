package org.example.utils

import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.domain.models.ChatUser
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

class WebsocketConnectionManager {
    private val connectedUsers = ConcurrentHashMap<String, ChatUser>()
    private val roomSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()
    private val connectionMutex = Mutex()
    @OptIn(ExperimentalAtomicApi::class)
    private val connectionCounter = AtomicLong(0)

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun addUser(userId: String, username: String, socket: WebSocketSession): String {
        return connectionMutex.withLock {
            val sessionId = "session_${connectionCounter.incrementAndFetch()}_${System.currentTimeMillis()}"
            val user = ChatUser(userId, sessionId, socket, username)

            connectedUsers[sessionId] = user
            sessionId
        }
    }

    suspend fun removeUser(sessionId: String) {
        connectionMutex.withLock {
            connectedUsers.remove(sessionId)
            roomSubscriptions.forEach { (roomId, userSession) ->
                userSession.remove(sessionId)
                if (userSession.isEmpty()) {
                    roomSubscriptions.remove(roomId)
                }
            }
        }
    }

    fun subscribeToRoom(sessionId: String, roomId: String) {
        roomSubscriptions.computeIfAbsent(roomId) {
            ConcurrentHashMap.newKeySet()
        }.add(sessionId)
    }

    fun unsubscribeFromRoom(sessionId: String, roomId: String) {
        roomSubscriptions[roomId]?.remove(sessionId)
    }

    fun getUsersInRoom(roomId: String): List<ChatUser> {
        return roomSubscriptions[roomId]?.mapNotNull { connectedUsers[it] } ?: emptyList()
    }

    fun getAllConnectedUsers(): List<ChatUser> {
        return connectedUsers.values.toList()
    }

    fun getUserBySession(sessionId: String): ChatUser? {
        return connectedUsers[sessionId]
    }

    fun getUsersByUserId(userId: String): List<ChatUser> {
        return connectedUsers.values.filter { it.userId == userId }
    }
}