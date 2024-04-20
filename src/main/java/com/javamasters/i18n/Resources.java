package com.javamasters.i18n;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import java.util.Locale;

public class Resources implements Disposable {
    private final BehaviorSubject<Translation> translation = BehaviorSubject.create();
    private final Disposable delegatedDisposable;

    public Resources(Observable<String> language) {
        delegatedDisposable = language.subscribe(l -> {
            Translation next;
            if (l.equals("system")) {
                var iso = Locale.getDefault().getLanguage();
                next = Translation.ofIso631_1(iso);
            } else {
                next = Translation.ofName(l);
            }
            if (next == null) {
                throw new NullPointerException("No such translation: " + l);
            }
            translation.onNext(next);
        });
    }

    public Observable<String> getString(String key, Object ...args) {
        return translation.map(t -> t.getString(key, args));
    }

    public Observable<Translation> getTranslation() {
        return translation;
    }

    @Override
    public boolean isDisposed() {
        return delegatedDisposable.isDisposed();
    }

    @Override
    public void dispose() {
        delegatedDisposable.dispose();
    }
}
