package com.pmtool.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 数据加密工具类
 * 用于对人员、项目等敏感业务数据进行可逆加密存储
 * 采用 AES 对称加密，密钥从配置文件读取
 */
@Component
public class DataEncryptUtils {

    private static String encryptKey;

    @Value("${data.encrypt.key:pm-tool-data-encrypt-key-2026}")
    public void setEncryptKey(String key) {
        // AES 密钥需要 16/24/32 字节，这里补齐到 32 字节
        byte[] keyBytes = new byte[32];
        byte[] src = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
        encryptKey = new String(keyBytes, StandardCharsets.ISO_8859_1);
    }

    private static AES getAES() {
        return SecureUtil.aes(encryptKey.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * 加密明文
     * @param plainText 明文
     * @return 密文（Base64），明文为空则返回空
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return getAES().encryptBase64(plainText);
    }

    /**
     * 解密密文
     * @param cipherText 密文（Base64）
     * @return 明文，密文为空则返回空
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return getAES().decryptStr(cipherText);
        } catch (Exception e) {
            // 如果解密失败，说明可能是未加密的历史数据，直接返回原文
            return cipherText;
        }
    }
}
