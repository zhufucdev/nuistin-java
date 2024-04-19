package com.javamasters.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.javamasters.cipher.EncryptDecrypt;
import com.javamasters.model.Account;
import io.reactivex.rxjava3.core.Single;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class LocalAccountProvider implements AccountProvider {
    private ArrayList<Account> accounts;
    private final Gson gson = new Gson();
    private final File dataFile;
    private final EncryptDecrypt cipher;

    public LocalAccountProvider(File dataFile, EncryptDecrypt cipher) {
        this.dataFile = dataFile;
        this.cipher = cipher;
    }

    private void ensureAccounts() {
        if (accounts == null) {
            if (dataFile.exists()) {
                try {
                    var buf = Files.readAllBytes(dataFile.toPath());
                    var json = new String(buf, StandardCharsets.UTF_8);
                    accounts = gson.fromJson(json, new TypeToken<>() {
                    }.getType());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                accounts = new ArrayList<>();
                try {
                    saveAccounts();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private void saveAccounts() throws IOException {
        var json = gson.toJson(accounts, new TypeToken<ArrayList<Account>>() {
        }.getType());
        var buf = cipher.encrypt(StandardCharsets.UTF_8.encode(json).array());
        Files.write(dataFile.toPath(), buf);
    }

    @Override
    public Single<List<String>> getAccountIds() {
        ensureAccounts();
        return Single.just(accounts.stream().map(Account::id).toList());
    }

    @Override
    public Single<Boolean> addAccount(Account account) {
        ensureAccounts();
        if (accounts.parallelStream().anyMatch(a -> a.id().equals(account.id()))) {
            return Single.just(false);
        }
        accounts.add(account);
        try {
            saveAccounts();
            return Single.just(true);
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    @Override
    public Single<Boolean> removeAccount(Account account) {
        ensureAccounts();
        var removed = accounts.remove(account);
        if (!removed) {
            return Single.just(false);
        }
        try {
            saveAccounts();
            return Single.just(true);
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    @Override
    public Single<Account> getAccount(String accountId) {
        ensureAccounts();
        var account = accounts.stream().filter(a -> a.id().equals(accountId)).findFirst().orElse(null);
        if (account != null) {
            return Single.just(account);
        } else {
            return Single.error(new NoSuchElementException());
        }
    }
}
