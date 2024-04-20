package com.javamasters.data.impl;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.javamasters.cipher.AesCipher;
import com.javamasters.cipher.EncryptDecrypt;
import com.javamasters.cipher.NoCipher;
import com.javamasters.data.AccountProvider;
import com.javamasters.data.Library;
import com.javamasters.data.Settings;
import com.javamasters.data.io.FileIO;
import com.javamasters.i18n.Resources;

import java.io.File;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class LocalMachineLibrary implements Library {
    private final AccountProvider provider;
    private final Settings settings;
    private final Resources resources;

    public LocalMachineLibrary() {
        var dataDir = new File(getBaseDir(), "nuistin");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        var passwordsFile = new File(dataDir, "passwords");
        var settingsFile = new File(dataDir, "settings.json");
        provider = new AccountJsonProvider(new FileIO(passwordsFile), getCipher());
        settings = new SettingsJsonProvider(new FileIO(settingsFile));
        resources = new Resources(settings.preferredLanguage());
    }

    private static String getBaseDir() {
        var baseDir = System.getenv("XDG_DATA_HOME");
        if (baseDir == null) {
            var osName = System.getProperty("os.name", "generic").toLowerCase();
            if (osName.startsWith("windows")) {
                return System.getenv("APPDATA");
            } else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
                return Paths.get(System.getProperty("user.home", ""), "Library", "Application Support").toString();
            } else if (osName.startsWith("linux")) {
                return Paths.get(System.getProperty("user.home", ""), ".config").toString();
            }
        }
        return baseDir;
    }

    private static EncryptDecrypt getCipher() {
        try (var keyring = Keyring.create()) {
            try {
                var password = keyring.getPassword("nuistin", "default");
                return new AesCipher(password);
            } catch (PasswordAccessException e) {
                SecureRandom secureRandom;
                try {
                    secureRandom = SecureRandom.getInstanceStrong();
                } catch (NoSuchAlgorithmException ex) {
                    secureRandom = new SecureRandom();
                }
                var encoder = Base64.getEncoder();
                var seed = secureRandom.generateSeed(32);
                var newPassword = encoder.encodeToString(seed);
                keyring.setPassword("nuistin", "default", newPassword);
                return new AesCipher(newPassword);
            }
        } catch (BackendNotSupportedException e) {
            return new NoCipher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AccountProvider getAccountProvider() {
        return provider;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public Resources getResources() {
        return resources;
    }
}
