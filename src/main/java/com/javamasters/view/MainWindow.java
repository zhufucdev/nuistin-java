package com.javamasters.view;

import com.javamasters.data.Library;
import com.javamasters.net.Authenticator;
import com.javamasters.net.DefaultNetworkInterface;
import com.javamasters.net.NoNetworkInterfaceException;
import com.javamasters.view.component.AuthStateIndicator;
import com.javamasters.view.component.NetworkForm;
import com.javamasters.view.component.SignInForm;
import com.javamasters.view.model.KeychainViewModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.NetworkInterface;

public class MainWindow extends Frame {
    private final Library library;
    private final Button signInButton = new Button();
    private final SignInForm signInForm;
    private final AuthStateIndicator stateIndicator;
    private final ReactiveUi rui = new ReactiveUi();
    private final KeychainViewModel keychainViewModel;
    private final Observable<Authenticator> authenticator;

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
        stateIndicator = new AuthStateIndicator(authenticator.flatMap(Authenticator::getState), library.getResources()) {
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
        add(signInButton, fillWidth);

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

        rui.bindLabel(signInButton, library.getResources().getString("signin"));
        rui.bindTitle(this, library.getResources().getString("app_name"));
        rui.bindEnabled(
                signInButton,
                Observable.combineLatest(signInForm.username, signInForm.password, (username, password) -> !username.isEmpty() && !password.isEmpty())
        );

        signInButton.addActionListener(e -> {
            
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

    @Override
    public void dispose() {
        rui.dispose();
        keychainViewModel.dispose();
        super.dispose();
    }
}
