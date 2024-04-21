package com.javamasters.view.component;

import com.javamasters.i18n.Resources;
import com.javamasters.model.ISP;
import com.javamasters.view.ReactiveUi;
import com.javamasters.view.model.KeychainViewModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import javax.swing.*;
import java.awt.*;

public class SignInForm extends Container {
    private final Choice ispDropdown = new Choice();
    private final KeychainViewModel keychain;

    public final BehaviorSubject<String>
            username = BehaviorSubject.create(),
            password = BehaviorSubject.create();

    private final ReactiveUi rui = new ReactiveUi();
    private final CompositeDisposable disposables = new CompositeDisposable(rui);

    public SignInForm(KeychainViewModel keychain, Resources resources) {
        this.keychain = keychain;
        var usernameLabel = new Labeled<>(new TextField()) {
            @Override
            public Insets getInsets() {
                return new Insets(0, 4, 10, 6);
            }
        };
        add(usernameLabel);
        var passwordLabel = new Labeled<>(new JPasswordField()) {
            @Override
            public Insets getInsets() {
                return new Insets(0, 4, 18, 6);
            }
        };
        add(passwordLabel);
        add(ispDropdown);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        rui.bindText(usernameLabel.label, resources.getString("username"));
        rui.bindText(passwordLabel.label, resources.getString("password"));
        rui.twoWayBindText(usernameLabel.field, username);
        rui.twoWayBindPassword(passwordLabel.field, password);

        disposables.add(
                resources.getTranslation().subscribe(t -> {
                    ispDropdown.removeAll();
                    for (int i = 0; i < ISP.values().length; i++) {
                        ispDropdown.add(t.getString(ISP.values()[i].name().toLowerCase()));
                    }
                })
        );
    }

    @Override
    public void removeNotify() {
        disposables.dispose();
        super.removeNotify();
    }
}
