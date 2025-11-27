package com.stewartpoulnott.weighttracker

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {

    private const val SALT_LENGTH = 32

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    fun hashPassword(password: String, salt: String): String {
        val saltedPassword = password + salt
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(saltedPassword.toByteArray())
        return bytesToHex(hashBytes)
    }

    fun verifyPassword(inputPassword: String, storedHash: String, storedSalt: String): Boolean {
        val inputHash = hashPassword(inputPassword, storedSalt)
        return inputHash == storedHash
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach { byte ->
            val value = byte.toInt()
            result.append(hexChars[value shr 4 and 0x0F])
            result.append(hexChars[value and 0x0F])
        }
        return result.toString()
    }

    fun validatePassword(password: String): Pair<Boolean, String> {
        if (password.length < 8) {
            return Pair(false, "Password must be at least 8 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            return Pair(false, "Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must contain at least one number")
        }
        return Pair(true, "Password is valid")
    }
}