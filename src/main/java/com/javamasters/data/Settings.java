package com.javamasters.data;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface Settings {
    Observable<String> preferredLanguage();
    Single<Boolean> setLanguage(String language);
    Observable<String> authServer();
    Single<Boolean> setAuthServer(String url);
    Observable<String> pingAddress();
    Single<Boolean> setPingAddress(String host);
    Observable<String> nic();
    Single<Boolean> setNic(String name);
}
