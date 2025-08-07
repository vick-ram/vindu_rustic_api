package org.example.utils

import kotlin.random.Random

fun generateOtp(): String {
    val otp = Random.nextLong(10000, 100000).toString()
    return otp
}