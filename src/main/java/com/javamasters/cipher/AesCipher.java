package com.javamasters.cipher;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class AesCipher implements EncryptDecrypt {
    private final Cipher cipher;
    private final Key key;
    private final byte[] salt;

    public AesCipher(String passphrase) {
        try {
            var keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            this.key = keyFactory.generateSecret(new PBEKeySpec(passphrase.toCharArray()));
            cipher = Cipher.getInstance("PBEWithMD5AndDES");

            var md = MessageDigest.getInstance("SHA-1");
            salt = md.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 20));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 20));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
