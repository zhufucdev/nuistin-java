package com.javamasters.view;

import com.javamasters.i18n.Resources;
import com.javamasters.view.model.KeychainViewModel;

import javax.swing.*;
import java.awt.*;

public class SignInForm extends Container {
    private final LabeledTextField username = new LabeledTextField(), password = new LabeledTextField();
    private final KeychainViewModel keychain;

    private final ReactiveUi rui = new ReactiveUi();

    public SignInForm(KeychainViewModel keychain, Resources resources) {
        this.keychain = keychain;

        add(username);
        add(password);
        var layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        rui.bindText(username.label, resources.getString("username"));
        rui.bindText(password.label, resources.getString("password"));
    }

    @Override
    public void removeNotify() {
        rui.dispose();
        super.removeNotify();
    }
}
