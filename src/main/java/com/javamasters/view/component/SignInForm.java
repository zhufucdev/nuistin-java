package com.javamasters.view.component;

import com.javamasters.i18n.Resources;
import com.javamasters.model.ISP;
import com.javamasters.view.ReactiveUi;
import com.javamasters.view.model.KeychainViewModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SignInForm extends Container {
    private final KeychainViewModel keychain;
    private final Choice ispDropdown = new Choice();
    private final Labeled<TextField> usernameLabel;
    private final Labeled<JPasswordField> passwordLabel;

    public final BehaviorSubject<String> username = BehaviorSubject.create(), password = BehaviorSubject.create();
    public final BehaviorSubject<ISP> isp = BehaviorSubject.create();

    private final ReactiveUi rui = new ReactiveUi();
    private final CompositeDisposable disposables = new CompositeDisposable(rui);
    private final Map<String, ISP> namedIsps = new HashMap<>();

    public SignInForm(KeychainViewModel keychain, Resources resources) {
        this.keychain = keychain;

        usernameLabel = new Labeled<>(new TextField()) {
            @Override
            public Insets getInsets() {
                return new Insets(0, 4, 10, 6);
            }
        };
        passwordLabel = new Labeled<>(new JPasswordField()) {
            @Override
            public Insets getInsets() {
                return new Insets(0, 4, 18, 6);
            }
        };
        add(usernameLabel);
        add(passwordLabel);
        add(ispDropdown);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        rui.bindText(usernameLabel.label, resources.getString("username"));
        rui.bindText(passwordLabel.label, resources.getString("password"));
        rui.twoWayBindText(usernameLabel.field, username);
        rui.twoWayBindPassword(passwordLabel.field, password);

        isp.onNext(ISP.Campus);
        ispDropdown.addItemListener(e -> {
            var next = namedIsps.get((String) e.getItem());
            isp.onNext(next);
        });
        disposables.add(
                isp.subscribe(isp -> namedIsps.entrySet().stream()
                        .filter(pair -> pair.getValue().equals(isp))
                        .findFirst()
                        .ifPresent(next -> ispDropdown.select(next.getKey())))
        );

        disposables.add(
                resources.getTranslation().subscribe(t -> {
                    ispDropdown.removeAll();
                    for (int i = 0; i < ISP.values().length; i++) {
                        var isp = ISP.values()[i];
                        var name = t.getString(isp.name().toLowerCase());
                        ispDropdown.add(name);
                        namedIsps.put(name, isp);
                    }
                })
        );

        setupAutofill();
    }

    private void setupAutofill() {
        disposables.add(keychain.getAccountIds()
                .flatMap(saved -> username.map(input ->
                        input.isEmpty() ? Optional.<String>empty() :
                                saved.stream().filter(id -> id.startsWith(input) && !id.equals(input))
                                        .sorted()
                                        .findFirst())
                )
                .subscribe(potential -> {
                    if (potential.isPresent()) {
                        var inputTail = usernameLabel.field.getText().length();
                        usernameLabel.field.setText(potential.get());
                        usernameLabel.field.setSelectionStart(inputTail);
                        usernameLabel.field.setSelectionEnd(potential.get().length());
                    }
                })
        );
        usernameLabel.field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                tryAutofill(usernameLabel.field.getText());
            }
        });
        usernameLabel.field.addActionListener(e -> {
            tryAutofill(usernameLabel.field.getText());
            passwordLabel.field.grabFocus();
        });
        usernameLabel.field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    passwordLabel.field.grabFocus();
                }
            }
        });
    }

    private void tryAutofill(String username) {
        disposables.add(
                keychain.getAccount(username)
                        .onErrorComplete()
                        .subscribe(acc -> {
                            password.onNext(acc.password());
                            isp.onNext(acc.isp());
                        })
        );
    }

    @Override
    public void removeNotify() {
        disposables.dispose();
        super.removeNotify();
    }
}
