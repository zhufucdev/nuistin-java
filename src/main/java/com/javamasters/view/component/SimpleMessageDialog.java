package com.javamasters.view.component;

import com.javamasters.i18n.Resources;
import com.javamasters.view.ReactiveUi;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SimpleMessageDialog implements Disposable {
    private final Dialog dialog;
    private final ReactiveUi rui = new ReactiveUi();

    public SimpleMessageDialog(Frame owner, Observable<String> title, Observable<String> message, Resources resources) {
        dialog = new Dialog(owner);
        var label = new Label();
        var okButton = new Button();
        rui.bindTitle(dialog, title);
        rui.bindText(label, message);
        rui.bindLabel(okButton, resources.getString("ok"));
        dialog.setResizable(false);
        dialog.setSize(400, 100);

        dialog.setLayout(new GridBagLayout());
        var fillHeight = new GridBagConstraints();
        fillHeight.fill = GridBagConstraints.VERTICAL;
        fillHeight.weighty = 1;
        fillHeight.gridx = 0;
        fillHeight.gridy = 0;
        fillHeight.gridwidth = 2;
        dialog.add(label, fillHeight);

        var fillRemaining = new GridBagConstraints();
        fillRemaining.gridx = 1;
        fillRemaining.gridy = 1;
        dialog.add(okButton, fillRemaining);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        okButton.addActionListener(e -> dispose());
    }

    public void setVisible(boolean visible) {
        dialog.setVisible(visible);
    }

    @Override
    public void dispose() {
        dialog.dispose();
        rui.dispose();
    }

    @Override
    public boolean isDisposed() {
        return rui.isDisposed();
    }
}
