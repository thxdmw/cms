package com.thx.module.payment.infrastructure;

import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 渠道配置等敏感数据的 AES-GCM 加解密。主密钥来自环境变量 {@code PAYMENT_MASTER_KEY}
 * （Base64 编码的 16/24/32 字节 AES Key），禁止落库、禁止进 Git、禁止硬编码——
 * 校验方式与 {@code ShiroConfig} 里 remember-me 的 AES Key 校验保持一致的工程习惯：
 * 启动时立即校验，长度不对直接拒绝启动，而不是留到运行期才报错。
 * <p>
 * 密文格式：{@code Base64(IV(12 字节) + ciphertext+tag)}，IV 每次加密随机生成并随密文一起保存，
 * 不使用 Hutool 等第三方封装，直接用 JDK 内置 {@code javax.crypto}，便于对 IV 生成与存储完全显式控制。
 */
@Component
public class AesGcmCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCipher(@Value("${cms.payment.master-key:}") String masterKeyBase64) {
        this.masterKey = buildKey(masterKeyBase64);
    }

    private SecretKeySpec buildKey(String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.trim().isEmpty()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_MASTER_KEY_INVALID,
                    "环境变量 PAYMENT_MASTER_KEY（cms.payment.master-key）未配置");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(masterKeyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_MASTER_KEY_INVALID,
                    "PAYMENT_MASTER_KEY 不是合法的 Base64 编码");
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_MASTER_KEY_INVALID,
                    "PAYMENT_MASTER_KEY 解码后长度必须是 16/24/32 字节，实际为 " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR, "渠道配置加密失败", e);
        }
    }

    public String decrypt(String base64CipherText) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64CipherText);
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR, "渠道配置密文格式非法");
            }
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR, "渠道配置解密失败", e);
        }
    }
}
