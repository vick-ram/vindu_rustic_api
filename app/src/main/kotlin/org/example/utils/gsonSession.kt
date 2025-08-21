package org.example.utils

import com.google.gson.Gson
import io.ktor.server.sessions.SessionSerializer

class GsonSessionSerializer<T>(private val gson: Gson, private val clazz: Class<T>): SessionSerializer<T> {
    override fun serialize(session: T): String = gson.toJson(session)
    override fun deserialize(text: String): T = gson.fromJson(text, clazz)
}