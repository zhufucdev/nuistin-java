package com.javamasters.cipher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesCipherTest {

    @Test
    void encrypt_decrypt() {
        var cipher = new AesCipher("qwertyui");
        var ssource = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertArrayEquals(ssource, cipher.decrypt(cipher.encrypt(ssource)));
    }
}