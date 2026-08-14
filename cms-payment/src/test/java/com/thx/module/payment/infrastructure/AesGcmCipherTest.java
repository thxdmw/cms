package com.thx.module.payment.infrastructure;

import com.thx.module.payment.exception.PaymentException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 渠道配置加解密测试：往返一致、IV 随机不重复、主密钥长度非法时启动即失败。
 */
class AesGcmCipherTest {

    private String randomBase64Key(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void encryptThenDecryptRoundTripsToOriginalPlaintext() {
        AesGcmCipher cipher = new AesGcmCipher(randomBase64Key(32));
        String plaintext = "{\"appId\":\"2021000000000000\",\"privateKey\":\"MIIEvQIBADANBg...\"}";

        String cipherText = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(cipherText);

        assertEquals(plaintext, decrypted);
        assertNotEquals(plaintext, cipherText);
    }

    @Test
    void sameInputEncryptsToDifferentCipherTextBecauseIvIsRandom() {
        AesGcmCipher cipher = new AesGcmCipher(randomBase64Key(32));
        String plaintext = "same-plaintext";

        String first = cipher.encrypt(plaintext);
        String second = cipher.encrypt(plaintext);

        assertNotEquals(first, second);
        assertEquals(plaintext, cipher.decrypt(first));
        assertEquals(plaintext, cipher.decrypt(second));
    }

    @Test
    void rejectsMissingMasterKeyAtConstructionTime() {
        assertThrows(PaymentException.class, () -> new AesGcmCipher(""));
    }

    @Test
    void rejectsMasterKeyWithInvalidByteLength() {
        // 20 字节不是合法的 AES 密钥长度（只允许 16/24/32）
        assertThrows(PaymentException.class, () -> new AesGcmCipher(randomBase64Key(20)));
    }

    @Test
    void acceptsAll128192256BitKeyLengths() {
        new AesGcmCipher(randomBase64Key(16));
        new AesGcmCipher(randomBase64Key(24));
        new AesGcmCipher(randomBase64Key(32));
    }
}
