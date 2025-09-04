package org.example.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.server.sessions.SessionSerializer

class GsonSessionSerializer<T>(private val gson: Gson, private val clazz: Class<T>): SessionSerializer<T> {
    override fun serialize(session: T): String = gson.toJson(session)
    override fun deserialize(text: String): T = gson.fromJson(text, clazz)
}

object Json {
    private val gson = Gson()

    internal inline fun <reified T> decodeFromString(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }

    fun encodeToString(src: Any): String {
        return gson.toJson(src)
    }
}
