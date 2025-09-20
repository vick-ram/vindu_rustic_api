package org.example.utils

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlin.random.Random

fun generateOtp(): String {
    val otp = Random.nextLong(10000, 100000).toString()
    return otp
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun storeOtp(otp: String, email: String) {
    RedisService.commands.set("otp:$email", otp)
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun retrieveOtp(key: String): String? {
    return RedisService.commands.get(key)
}