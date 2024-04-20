package com.javamasters.view;

import com.javamasters.data.Library;
import com.javamasters.view.model.KeychainViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class MainWindow extends Frame implements WindowListener {
    private final Library library;
    private final Button signInButton = new Button();
    private final SignInForm signInForm;
    private final ReactiveUi rui = new ReactiveUi();
    private final KeychainViewModel keychainViewModel;

    public MainWindow(Library library) {
        this.library = library;
        keychainViewModel = new KeychainViewModel(library.getAccountProvider());
        signInForm = new SignInForm(keychainViewModel, library.getResources());
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

        var fillHeight = new GridBagConstraints();
        fillHeight.fill = GridBagConstraints.HORIZONTAL;
        fillHeight.weighty = 1;
        fillHeight.gridx = 0;

        add(new Component(){}, fillHeight);
        add(signInButton, fillWidth);
        setSize(400, 300);
        setMinimumSize(new Dimension(200, 170));
        addWindowListener(this);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        rui.bindLabel(signInButton, library.getResources().getString("signin"));
        rui.bindTitle(this, library.getResources().getString("app_name"));
    }

    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public void dispose() {
        rui.dispose();
        keychainViewModel.dispose();
        super.dispose();
    }
}
