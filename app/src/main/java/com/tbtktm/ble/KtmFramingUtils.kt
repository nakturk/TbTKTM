package com.tbtktm.ble

import java.security.SecureRandom

object KtmFramingUtils {
    private val secureRandom = SecureRandom()

    /**
     * KTM BLE protokolünün gerektirdiği 16 baytlık rastgele başlık + veri + padding + padding boyutu çerçevelemesi.
     */
    fun frame(data: ByteArray): ByteArray {
        val padLength = 16 - (data.size % 16)
        val totalLength = data.size + 16 + padLength
        val framed = ByteArray(totalLength)

        // 1. İlk 16 bayt rastgele veri
        val header = ByteArray(16)
        secureRandom.nextBytes(header)
        System.arraycopy(header, 0, framed, 0, 16)

        // 2. Asıl veri (Payload)
        System.arraycopy(data, 0, framed, 16, data.size)

        // 3. Kalan kısmı rastgele doldur (Padding)
        if (padLength > 1) {
            val padding = ByteArray(padLength - 1)
            secureRandom.nextBytes(padding)
            System.arraycopy(padding, 0, framed, 16 + data.size, padLength - 1)
        }

        // 4. En son bayt: Eklenen padding uzunluğu
        framed[totalLength - 1] = padLength.toByte()

        return framed
    }

    /**
     * Motosikletten gelen çerçevelenmiş verinin içindeki asıl payload'u çıkarır.
     */
    fun unframe(framedData: ByteArray): ByteArray {
        if (framedData.size <= 17) return ByteArray(0)
        val padLength = framedData.last().toInt() and 0xFF
        val payloadLength = framedData.size - 16 - padLength
        if (payloadLength <= 0) return ByteArray(0)

        val result = ByteArray(payloadLength)
        System.arraycopy(framedData, 16, result, 0, payloadLength)
        return result
    }

    // Görünürlük Baytları
    const val VISIBILITY_INVISIBLE: Byte = 0x00
    const val VISIBILITY_HALF: Byte = 0x01
    const val VISIBILITY_FULL: Byte = 0x02
}
