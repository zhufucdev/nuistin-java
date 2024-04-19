package com.javamasters.data;

import com.javamasters.model.Account;
import io.reactivex.rxjava3.core.Single;

import java.util.List;

public interface AccountProvider {
    Single<List<String>> getAccountIds();
    Single<Boolean> addAccount(Account account);
    Single<Boolean> removeAccount(Account account);
    Single<Account> getAccount(String accountId);
}
