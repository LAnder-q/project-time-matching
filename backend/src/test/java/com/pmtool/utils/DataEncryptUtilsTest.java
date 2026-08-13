package com.pmtool.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 数据加密工具单元测试
 */
class DataEncryptUtilsTest {

    @BeforeAll
    static void initKey() {
        new DataEncryptUtils().setEncryptKey("pm-tool-test-key");
    }

    @Test
    @DisplayName("加密后可正确解密还原明文")
    void encryptDecryptRoundTrip() {
        String plain = "EMP001";

        String encrypted = DataEncryptUtils.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, DataEncryptUtils.decrypt(encrypted));
    }

    @Test
    @DisplayName("空值原样透传不报错")
    void emptyValuePassthrough() {
        assertNull(DataEncryptUtils.encrypt(null));
        assertEquals("", DataEncryptUtils.encrypt(""));
        assertNull(DataEncryptUtils.decrypt(null));
    }
}
