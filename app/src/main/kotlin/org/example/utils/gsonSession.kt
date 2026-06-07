package org.example.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.server.sessions.SessionSerializer
import java.lang.reflect.Type

class GsonSessionSerializer<T>(private val gson: Gson, private val clazz: Class<T>): SessionSerializer<T> {
    override fun serialize(session: T): String = gson.toJson(session)
    override fun deserialize(text: String): T = gson.fromJson(text, clazz)
}

object Json {

    fun <T: Any> decodeFromString(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return GsonFactory.gson.fromJson(json, type)
    }

    fun <T: Any> decodeFromString(json: String, clazz: Class<T>): T {
        return GsonFactory.gson.fromJson(json, clazz)
    }

    fun <T: Any> decodeFromString(json: String, type: Type?): T {
        return GsonFactory.gson.fromJson(json, type)
    }

    fun <T> encodeToString(src: T): String {
        return GsonFactory.gson.toJson(src)
    }
}
