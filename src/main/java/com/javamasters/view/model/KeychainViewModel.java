package com.javamasters.view.model;

import com.javamasters.model.Account;
import com.javamasters.view.AccountProvider;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.AsyncSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class KeychainViewModel implements Disposable {
    private final AccountProvider provider;
    private final AsyncSubject<List<String>> accountIds = AsyncSubject.create();
    private final CompositeDisposable disposables = new CompositeDisposable();

    public KeychainViewModel(AccountProvider provider) {
        this.provider = provider;
        var accId = provider.getAccountIds()
                .subscribeOn(Schedulers.io())
                .subscribe(accountIds::onNext);
        disposables.add(accId);
    }

    public Observable<List<String>> getAccountIds() {
        return accountIds.defaultIfEmpty(new ArrayList<>());
    }

    public Single<Boolean> addAccount(Account account) {
        return provider.addAccount(account)
                .subscribeOn(Schedulers.io())
                .concatMap(added -> added
                        ? accountIds.first(new ArrayList<>())
                        .map(v -> {
                            var next = Stream.concat(v.stream(), Stream.of(account.id())).toList();
                            accountIds.onNext(next);
                            accountIds.onComplete();
                            return true;
                        })
                        : Single.just(false)
                );
    }

    @Override
    public void dispose() {
        disposables.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposables.isDisposed();
    }
}
