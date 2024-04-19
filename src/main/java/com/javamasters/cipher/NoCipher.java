package com.javamasters.cipher;

public class NoCipher implements EncryptDecrypt{
    @Override
    public byte[] encrypt(byte[] data) {
        return data;
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return data;
    }
}
