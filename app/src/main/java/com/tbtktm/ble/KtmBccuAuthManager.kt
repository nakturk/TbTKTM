package com.tbtktm.ble

import com.ktm.mobsdk.auth.BccuAuthNativeWrapper
import com.ktm.mobsdk.auth.BccuInitMsg
import com.ktm.mobsdk.auth.BccuKey
import com.ktm.mobsdk.auth.EncParams
import com.tbtktm.util.FileLogger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * KTM 1290 Super Adventure BCCU Native Authentication Motoru
 * Motosikletin Challenge-Response (M1 -> M2 -> Key Confirmation) el sıkışmasını
 * %100 orijinal protokol ile gerçekleştirir.
 */
object KtmBccuAuthManager {

    private val random = SecureRandom()
    private var stateM1: ByteArray? = null
    private var stateM2: ByteArray? = null
    private var stateSecret: ByteArray? = null
    private var stateIv: ByteArray? = null
    private var activeSessionKey: ByteArray? = null
    private var derivedKeys: List<BccuKey>? = null

    var isAuthenticated: Boolean = false
        private set

    fun reset() {
        stateM1 = null
        stateM2 = null
        stateSecret = null
        stateIv = null
        activeSessionKey = null
        derivedKeys = null
        isAuthenticated = false
        FileLogger.log(">> [BCCU AUTH] Oturum sıfırlandı.")
    }

    /**
     * Motosikletin TBT_AUTH_REQUESTS (0x0701) karakteristiğinden gelen baytları işler.
     * Dönen ByteArray doluysa, bu yanıt TBT_AUTH_REPLIES (0x0702) karakteristiğine yazılmalıdır.
     */
    fun handleIncomingAuthMessage(incomingBytes: ByteArray): ByteArray? {
        FileLogger.log(">> [BCCU AUTH] Gelen Auth Mesajı (${incomingBytes.size} B): ${incomingBytes.joinToString(" ") { "%02X".format(it) }}")

        try {
            // 1. Durum: İlk Bağlantı (M1 Challenge)
            if (stateM1 == null || (incomingBytes.size >= 16 && stateSecret == null)) {
                FileLogger.log(">> [BCCU AUTH] M1 Challenge Alındı. M2 Üretiliyor...")
                stateM1 = incomingBytes

                // 16 Bayt Rastgele M2 Üret
                val m2 = ByteArray(16)
                random.nextBytes(m2)
                stateM2 = m2

                // Secret ve IV türet
                val encParams: EncParams = BccuAuthNativeWrapper.INSTANCE._getEncParmsWrapper(incomingBytes, m2)
                stateSecret = encParams.secret
                stateIv = encParams.iv

                FileLogger.log(">> [BCCU AUTH] Secret & IV Başarıyla Türetildi.")

                // M2 Response Mesajını Encode Et (request=-1, appStatus=0, uniqueAppId=1)
                val randomBase = ByteArray(16)
                random.nextBytes(randomBase)
                val appInitMsg = BccuAuthNativeWrapper.INSTANCE._encodeAppInitMsgWrapper(
                    (-1).toByte(),
                    0.toByte(),
                    1.toByte(),
                    randomBase
                )

                val responsePayload = appInitMsg.data
                FileLogger.log(">> [BCCU AUTH] M2 Cevabı Gönderiliyor (${responsePayload.size} B)")
                return responsePayload
            }

            // 2. Durum: Motosikletten BccuInitMsg veya Key Request Gelmesi
            val secret = stateSecret
            val iv = stateIv
            if (secret != null && iv != null) {
                // Eğer gelen veri 16'nın tam katıysa deşifre et, değilse düz (raw) oku
                val payloadToProcess = if (incomingBytes.size % 16 == 0) {
                    try {
                        tryDecryptWithSecret(incomingBytes, secret, iv)
                    } catch (_: Exception) {
                        incomingBytes
                    }
                } else {
                    incomingBytes
                }

                FileLogger.log(">> [BCCU AUTH] İşlenen Mesaj (${payloadToProcess.size} B): ${payloadToProcess.joinToString(" ") { "%02X".format(it) }}")

                val bccuInitMsg: BccuInitMsg = BccuAuthNativeWrapper.INSTANCE._decodeBccuInitMsgWrapper(payloadToProcess)
                FileLogger.log(">> [BCCU AUTH] BccuInitMsg: request=${bccuInitMsg.request}, status=${bccuInitMsg.appStatus}, appId=${bccuInitMsg.uniqueAppId}")

                if (bccuInitMsg.request.toInt() == 1) {
                    // ComputeKeyArr İsteği
                    FileLogger.log(">> [BCCU AUTH] Motosiklet Anahtar Tablosu İstedi. Anahtarlar Hesaplanıyor...")
                    val seed = if (bccuInitMsg.seed != null && bccuInitMsg.seed.isNotEmpty()) bccuInitMsg.seed else ByteArray(16)
                    val keys = BccuAuthNativeWrapper.INSTANCE._createKeysWrapper(payloadToProcess, secret, iv, seed)
                    derivedKeys = keys.toList()

                    // Key Array Generated Başarı Cevabı (status=2)
                    val randomBase = ByteArray(16)
                    random.nextBytes(randomBase)
                    val appInitMsg = BccuAuthNativeWrapper.INSTANCE._encodeAppInitMsgWrapper(
                        (-1).toByte(),
                        2.toByte(), // keyArrayGeneratedSuccessfully
                        1.toByte(),
                        randomBase
                    )
                    return appInitMsg.data
                } else if (bccuInitMsg.request.toInt() in 16..31) {
                    // AssignKeyIndex (Motosiklet Belirli Bir Anahtar Seçti)
                    val keyIndex = bccuInitMsg.request.toInt() and 0x0F
                    FileLogger.log(">> [BCCU AUTH] Motosiklet KeyIndex=$keyIndex Seçti! Oturum Anahtarı Kilitlendi.")

                    val selectedKey = derivedKeys?.find { it.idx == keyIndex }?.keys ?: secret
                    activeSessionKey = selectedKey
                    isAuthenticated = true

                    FileLogger.log(">> [BCCU AUTH] 🎉 MOTOSİKLET BCCU YETKİLENDİRMESİ TAMAMLANDI (AUTHENTICATED)!")

                    // Confirm Key Number Cevabı
                    val confirmStatus = (keyIndex or 16).toByte()
                    val randomBase = ByteArray(16)
                    random.nextBytes(randomBase)
                    val appInitMsg = BccuAuthNativeWrapper.INSTANCE._encodeAppInitMsgWrapper(
                        (-1).toByte(),
                        confirmStatus,
                        1.toByte(),
                        randomBase
                    )
                    return appInitMsg.data
                }
            }
        } catch (e: Throwable) {
            FileLogger.log(">> [BCCU AUTH HATA] ${e.message}")
            e.printStackTrace()
        }

        return null
    }

    private fun tryDecryptWithSecret(message: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(message)
    }

    fun encryptWithSessionKey(data: ByteArray): ByteArray {
        val key = activeSessionKey ?: stateSecret ?: return data
        val iv = stateIv ?: return data
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
}
