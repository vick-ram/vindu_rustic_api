package org.example.config.security

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private val verifier = BCrypt.verifyer()
    private const val COST = 12

    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray())
    }

    fun verify(password: String, hash: String): Boolean {
        return verifier.verify(password.toCharArray(), hash).verified
    }
}