package com.javamasters.data.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.javamasters.cipher.EncryptDecrypt;
import com.javamasters.data.AccountProvider;
import com.javamasters.data.io.DataIO;
import com.javamasters.model.Account;
import io.reactivex.rxjava3.core.Single;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class AccountJsonProvider implements AccountProvider {
    private ArrayList<Account> accounts;
    private final Gson gson = new Gson();
    private final DataIO dataIO;
    private final EncryptDecrypt cipher;

    public AccountJsonProvider(DataIO dataIO, EncryptDecrypt cipher) {
        this.dataIO = dataIO;
        this.cipher = cipher;
    }

    private void ensureAccounts() {
        if (accounts == null) {
            if (dataIO.created()) {
                try (var ips = dataIO.openInputStream()) {
                    var buf = cipher.decrypt(ips.readAllBytes());
                    ips.close();
                    var json = new String(buf, StandardCharsets.UTF_8);
                    accounts = gson.fromJson(json, new TypeToken<ArrayList<Account>>() {
                    }.getType());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    accounts = new ArrayList<>();
                    saveAccounts();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void saveAccounts() throws IOException {
        var json = gson.toJson(accounts, new TypeToken<ArrayList<Account>>() {
        }.getType());
        var buf = cipher.encrypt(json.getBytes(StandardCharsets.UTF_8));
        if (dataIO.available()) {
            try (var ops = dataIO.openOutputStream()) {
                ops.write(buf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IOException("Data IO unavailable");
        }
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
