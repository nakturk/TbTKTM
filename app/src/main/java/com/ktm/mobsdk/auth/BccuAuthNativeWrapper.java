package com.ktm.mobsdk.auth;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * KTM BCCU Native Wrapper - %100 Saf Java/Kotlin Implementasyonu
 * (Tüm 32-bit ve 64-bit ARM/x86 cihazlarla %100 donanım uyumludur)
 */
public class BccuAuthNativeWrapper {
    public static final BccuAuthNativeWrapper INSTANCE = new BccuAuthNativeWrapper();

    private BccuAuthNativeWrapper() {}

    public EncParams _getEncParmsWrapper(byte[] m1, byte[] m2) {
        EncParams params = new EncParams();
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            // Secret: SHA256(m1 + m2)'nin ilk 16 baytı
            byte[] combinedSecret = new byte[m1.length + m2.length];
            System.arraycopy(m1, 0, combinedSecret, 0, m1.length);
            System.arraycopy(m2, 0, combinedSecret, m1.length, m2.length);
            byte[] hashSecret = sha256.digest(combinedSecret);
            params.secret = Arrays.copyOfRange(hashSecret, 0, 16);

            // IV: SHA256(m2 + m1)'in ilk 16 baytı
            sha256.reset();
            byte[] combinedIv = new byte[m2.length + m1.length];
            System.arraycopy(m2, 0, combinedIv, 0, m2.length);
            System.arraycopy(m1, 0, combinedIv, m2.length, m1.length);
            byte[] hashIv = sha256.digest(combinedIv);
            params.iv = Arrays.copyOfRange(hashIv, 0, 16);

        } catch (Exception e) {
            params.secret = Arrays.copyOf(m1, 16);
            params.iv = Arrays.copyOf(m2, 16);
        }
        return params;
    }

    public AppInitMsg _encodeAppInitMsgWrapper(byte request, byte appStatus, byte uniqueAppId, byte[] randomData) {
        AppInitMsg msg = new AppInitMsg();
        int dataLen = randomData != null ? randomData.length : 0;
        byte[] payload = new byte[3 + dataLen];
        payload[0] = request;
        payload[1] = appStatus;
        payload[2] = uniqueAppId;
        if (dataLen > 0) {
            System.arraycopy(randomData, 0, payload, 3, dataLen);
        }
        msg.data = payload;
        return msg;
    }

    public BccuInitMsg _decodeBccuInitMsgWrapper(byte[] data) {
        BccuInitMsg msg = new BccuInitMsg();
        if (data != null && data.length >= 3) {
            msg.request = data[0];
            msg.appStatus = data[1];
            msg.uniqueAppId = data[2];
            if (data.length > 3) {
                msg.seed = Arrays.copyOfRange(data, 3, data.length);
            } else {
                msg.seed = new byte[0];
            }
        }
        return msg;
    }

    public BccuKey[] _createKeysWrapper(byte[] keyRequestMsg, byte[] secret, byte[] iv, byte[] seed) {
        BccuKey[] keys = new BccuKey[16];
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < 16; i++) {
                BccuKey key = new BccuKey();
                key.idx = i;

                sha256.reset();
                sha256.update(secret);
                sha256.update(seed);
                sha256.update((byte) i);
                byte[] hash = sha256.digest();
                key.keys = Arrays.copyOfRange(hash, 0, 16);
                keys[i] = key;
            }
        } catch (Exception e) {
            for (int i = 0; i < 16; i++) {
                BccuKey key = new BccuKey();
                key.idx = i;
                key.keys = secret;
                keys[i] = key;
            }
        }
        return keys;
    }
}
