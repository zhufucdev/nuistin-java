package com.javamasters.view;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.awt.*;

public class ReactiveUi implements Disposable {
    private final CompositeDisposable disposables = new CompositeDisposable();

    public void bindLabel(Button button, Observable<String> observable) {
        disposables.add(observable.subscribe(button::setLabel));
    }

    public void bindText(Label label, Observable<String> observable) {
        disposables.add(observable.subscribe(label::setText));
    }

    public void bindTitle(Frame frame, Observable<String> observable) {
        disposables.add(observable.subscribe(frame::setTitle));
    }

    @Override
    public boolean isDisposed() {
        return disposables.isDisposed();
    }

    @Override
    public void dispose() {
        disposables.dispose();
    }
}
