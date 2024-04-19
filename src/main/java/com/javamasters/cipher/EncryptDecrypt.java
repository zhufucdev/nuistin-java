package com.javamasters.cipher;

public interface EncryptDecrypt {
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] data);
}
