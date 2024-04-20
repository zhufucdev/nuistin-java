package com.javamasters.view;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.Subject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.TextListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

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

    public void bindEnabled(Component button, Observable<Boolean> observable) {
        disposables.add(observable.subscribe(button::setEnabled));
    }

    public void twoWayBindText(TextComponent textComponent, Subject<String> subject) {
        disposables.add(new EventListenerDisposable(textComponent, "Text", (TextListener) e -> {
            var text = textComponent.getText();
            if (text.equals(subject.blockingLast())) {
                return;
            }
            subject.onNext(text);
            subject.onComplete();
        }));
        disposables.add(subject.subscribe(s -> {
            if (s.equals(textComponent.getText())) {
                return;
            }
            textComponent.setText(s);
        }));
    }

    public void twoWayBindPassword(JPasswordField passwordField, Subject<String> subject) {
        disposables.add(new EventListenerDisposable(passwordField, "Action", (ActionListener) e -> {
            var password = new String(passwordField.getPassword());
            subject.onNext(password);
        }));
        disposables.add(subject.subscribe(p -> {
            var password = new String(passwordField.getPassword());
            if (p.equals(password)) {
                return;
            }
            passwordField.setText(p);
        }));
    }

    static class EventListenerDisposable implements Disposable {
        private final Component component;
        private final Object listener;
        private final Method removeListenerFn;
        private boolean disposed = false;

        EventListenerDisposable(Component component, String name, Object listener) {
            this.component = component;
            this.listener = listener;

            var addListenerFn =
                    Arrays.stream(component.getClass().getMethods())
                            .filter(s -> s.getName().equals("add" + name + "Listener")).findFirst();
            if (addListenerFn.isEmpty()) {
                throw new RuntimeException(new NoSuchMethodException("add" + name + "Listener"));
            }
            var removeListenerFn =
                    Arrays.stream(component.getClass().getMethods())
                            .filter(s -> s.getName().equals("remove" + name + "Listener")).findFirst();
            if (removeListenerFn.isEmpty()) {
                throw new RuntimeException(new NoSuchMethodException("remove" + name + "Listener"));
            }
            this.removeListenerFn = removeListenerFn.get();
            try {
                addListenerFn.get().invoke(component, listener);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void dispose() {
            if (disposed) {
                return;
            }
            try {
                removeListenerFn.invoke(component, listener);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
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
