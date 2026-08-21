package com.tbtktm.ble

import java.security.SecureRandom

object EncryptionUtils {
    private val random = SecureRandom()

    fun frame(data: ByteArray): ByteArray {
        val padLen = 16 - (data.size % 16)
        val totalLen = data.size + 16 + padLen
        val framed = ByteArray(totalLen)

        // İlk 16 bayt rastgele salt
        val salt = ByteArray(16)
        random.nextBytes(salt)
        System.arraycopy(salt, 0, framed, 0, 16)

        // Payload
        System.arraycopy(data, 0, framed, 16, data.size)

        // Rastgele padding
        if (padLen > 1) {
            val padBytes = ByteArray(padLen - 1)
            random.nextBytes(padBytes)
            System.arraycopy(padBytes, 0, framed, 16 + data.size, padLen - 1)
        }

        // Son bayt padding boyutu
        framed[totalLen - 1] = padLen.toByte()
        return framed
    }

    fun unframe(framed: ByteArray): ByteArray {
        if (framed.size <= 17) return ByteArray(0)
        val padLen = framed.last().toInt() and 0xFF
        val dataLen = framed.size - 16 - padLen
        if (dataLen <= 0) return ByteArray(0)
        val result = ByteArray(dataLen)
        System.arraycopy(framed, 16, result, 0, dataLen)
        return result
    }
}
