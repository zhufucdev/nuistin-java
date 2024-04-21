package com.javamasters.view.component;

import com.javamasters.i18n.Resources;
import com.javamasters.net.Authenticator;
import com.javamasters.view.ReactiveUi;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.awt.*;

public class AuthStateIndicator extends Container {
    private Authenticator.State currentState = Authenticator.State.Unspecified;
    private final CompositeDisposable subscriptions = new CompositeDisposable();

    public AuthStateIndicator(Observable<Authenticator.State> state, Resources resources) {
        var dot = new Canvas() {
            @Override
            public void paint(Graphics g) {
                switch (currentState) {
                    case Unspecified:
                        g.setColor(Color.GRAY);
                        break;
                    case Online:
                        g.setColor(Color.GREEN);
                        break;
                    case Offline:
                        g.setColor(Color.RED);
                        break;
                    case Unauthenticated:
                        g.setColor(Color.YELLOW);
                        break;
                }
                if (currentState == Authenticator.State.Unspecified) {
                    g.drawOval(0, 0, getWidth(), getHeight());
                } else {
                    g.fillOval(0, 0, getWidth(), getHeight());
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, 10);
            }
        };

        subscriptions.add(state.subscribe(next -> {
            currentState = next;
            dot.invalidate();
        }));
        var rui = new ReactiveUi();
        subscriptions.add(rui);

        var label = new Label() {
            @Override
            public Font getFont() {
                return new Font("Arial", Font.PLAIN, 12);
            }
        };
        rui.bindText(label, state.concatMap(s -> resources.getString("state." + s.name().toLowerCase())));

        setLayout(new GridBagLayout());
        add(dot);
        add(new Component() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(4, 0);
            }
        });
        var fillWidth = new GridBagConstraints();
        fillWidth.fill = GridBagConstraints.HORIZONTAL;
        fillWidth.weightx = 1;
        fillWidth.gridx = 2;
        add(label, fillWidth);
    }

    @Override
    public void removeNotify() {
        subscriptions.dispose();
        super.removeNotify();
    }
}
