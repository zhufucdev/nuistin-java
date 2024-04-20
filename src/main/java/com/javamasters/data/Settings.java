package com.javamasters.data;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface Settings {
    Observable<String> preferredLanguage();
    Single<Boolean> setLanguage(String language);
}
