package com.javamasters.view;

import com.javamasters.data.Library;
import com.javamasters.model.Account;
import com.javamasters.net.Authenticator;
import com.javamasters.net.DefaultNetworkInterface;
import com.javamasters.net.NoNetworkInterfaceException;
import com.javamasters.net.NoSuchAccountException;
import com.javamasters.view.component.AuthStateIndicator;
import com.javamasters.view.component.NetworkForm;
import com.javamasters.view.component.SignInForm;
import com.javamasters.view.component.SimpleMessageDialog;
import com.javamasters.view.model.KeychainViewModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.NetworkInterface;

public class MainWindow extends Frame {
    private final Library library;
    private final Button signInOutButton = new Button();
    private final SignInForm signInForm;
    private final AuthStateIndicator stateIndicator;
    private final KeychainViewModel keychainViewModel;
    private final Observable<Authenticator> authenticator;
    private final Observable<Authenticator.State> authState;
    private boolean signInMode = true;

    private final ReactiveUi rui = new ReactiveUi();
    private final CompositeDisposable subscriptions = new CompositeDisposable();

    public MainWindow(Library library) {
        this.library = library;
        keychainViewModel = new KeychainViewModel(library.getAccountProvider());
        signInForm = new SignInForm(keychainViewModel, library.getResources());
        var nicObservable =
                library.getSettings()
                        .nic()
                        .observeOn(Schedulers.io())
                        .map(name -> name.isEmpty() ? DefaultNetworkInterface.get() : NetworkInterface.getByName(name));
        authenticator = Observable.combineLatest(
                library.getSettings().authServer(),
                library.getSettings().pingAddress(),
                nicObservable,
                (authserver, ping, nic) -> {
                    if (nic == null) {
                        throw new NoNetworkInterfaceException();
                    }
                    return new Authenticator(authserver, ping, nic);
                });
        authState = authenticator.flatMap(Authenticator::getState);
        stateIndicator = new AuthStateIndicator(authState, library.getResources()) {
            @Override
            public Insets getInsets() {
                return new Insets(0, 10, 4, 10);
            }
        };
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        var fillWidth = new GridBagConstraints();
        fillWidth.fill = GridBagConstraints.HORIZONTAL;
        fillWidth.weightx = 1;
        fillWidth.gridx = 0;

        var header = new Container() {
            @Override
            public Insets getInsets() {
                return new Insets(12, 12, 12, 12);
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(signInForm);
        add(header, fillWidth);
        add(signInOutButton, fillWidth);

        var fillHeight = new GridBagConstraints();
        fillHeight.fill = GridBagConstraints.HORIZONTAL;
        fillHeight.weighty = 1;
        fillHeight.gridx = 0;

        add(new Component() {
        }, fillHeight);
        add(new JToolBar.Separator(), fillWidth);
        add(stateIndicator, fillWidth);

        setSize(400, 300);
        setMinimumSize(new Dimension(400, 250));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                super.windowClosing(e);
            }
        });

        rui.bindLabel(
                signInOutButton,
                authState.flatMap(state -> switch (state) {
                    case Unspecified, Unauthenticated, Offline -> {
                        signInMode = true;
                        yield library.getResources().getString("signup");
                    }
                    case Online -> {
                        signInMode = false;
                        yield library.getResources().getString("signout");
                    }
                })
        );
        rui.bindTitle(this, library.getResources().getString("app_name"));
        rui.bindEnabled(
                signInOutButton,
                Observable.combineLatest(signInForm.username, signInForm.password, (username, password) -> !username.isEmpty() && !password.isEmpty())
        );

        signInOutButton.addActionListener(e -> {
            var username = signInForm.username.getValue();
            var password = signInForm.password.getValue();
            var isp = signInForm.isp.getValue();
            var account = new Account(username, password, isp);
            if (signInMode) {
                var signinDispose = authenticator.blockingFirst()
                        .login(account)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                succeeded -> {
                                    if (!succeeded) {
                                        showErrorDialog(null, "sign_up_failed");
                                    }
                                },
                                error -> showErrorDialog(error, "sign_up_failed")
                        );
                subscriptions.add(signinDispose);
            } else {
                var signoutDispose = authenticator.blockingFirst()
                        .logout(account)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                succeeded -> {
                                    if (!succeeded) {
                                        showErrorDialog(null, "sign_out_failed");
                                    }
                                },
                                error -> showErrorDialog(error, "sign_out_failed")
                        );
                subscriptions.add(signoutDispose);
            }
        });
        stateIndicator.optionsButton.addActionListener(e -> {
            var nw = new NetworkForm(library.getSettings(), library.getResources());
            var dialog = new Dialog(MainWindow.this);
            dialog.add(nw);
            dialog.setSize(300, 140);
            dialog.setResizable(false);
            dialog.setVisible(true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dialog.dispose();
                }
            });
        });
    }

    private void showErrorDialog(Throwable error, String titleResId) {
        var resources = library.getResources();
        Observable<String> messgae = null;
        if (error instanceof NoSuchAccountException) {
            messgae = Observable.just(((NoSuchAccountException) error).response.data().get("text").toString());
        }
        var dialog = new SimpleMessageDialog(
                this,
                resources.getString(titleResId),
                messgae != null ? messgae : resources.getString("check_password"),
                resources
        );
        dialog.setVisible(true);
    }

    @Override
    public void dispose() {
        rui.dispose();
        keychainViewModel.dispose();
        subscriptions.dispose();
        super.dispose();
    }
}
