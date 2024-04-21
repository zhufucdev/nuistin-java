package com.javamasters.view.component;

import com.javamasters.data.Settings;
import com.javamasters.i18n.Resources;
import com.javamasters.view.ReactiveUi;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import javax.swing.*;
import java.awt.*;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class NetworkForm extends Container {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Map<String, NetworkInterface> namedInterfaces = new HashMap<>();

    public NetworkForm(Settings settings, Resources resources) {
        var niLabel = new Labeled<>(new Choice());
        var rui = new ReactiveUi();
        rui.bindText(niLabel.label, resources.getString("network_interface"));

        try {
            NetworkInterface.networkInterfaces().forEach(nis -> {
                var display = getNiName(nis);
                niLabel.field.add(display);
                namedInterfaces.put(display, nis);
            });
        } catch (SocketException e) {
            // ignored
        }
        disposables.add(settings.nic().subscribe(name -> {
            for (var pair : namedInterfaces.entrySet()) {
                if (pair.getValue().getName().equals(name)) {
                    if (!niLabel.field.getSelectedItem().equals(pair.getKey())) {
                        niLabel.field.select(pair.getKey());
                    }
                    break;
                }
            }
        }));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(niLabel);

        niLabel.field.addItemListener(e -> {
            var ni = namedInterfaces.get((String) e.getItem());
            settings.setNic(ni.getName())
                    .subscribeOn(Schedulers.io())
                    .blockingGet();
        });

        var pingLabel = new Labeled<>(new TextField());
        rui.bindText(pingLabel.label, resources.getString("ping_address"));

        BehaviorSubject<String> pingAddress = BehaviorSubject.create();
        rui.twoWayBindText(pingLabel.field, pingAddress);
        add(pingLabel);

        disposables.addAll(
                settings.pingAddress().first("").subscribe(pingAddress::onNext),
                pingAddress.buffer(1, TimeUnit.SECONDS)
                        .map(buf -> buf.isEmpty() ? Optional.empty() : Optional.of(buf.get(buf.size() - 1)))
                        .subscribe(nextPing -> nextPing.ifPresent(
                                        o -> settings.setPingAddress((String) o).blockingGet()
                                )
                        )
        );
    }

    @Override
    public void removeNotify() {
        disposables.dispose();
        super.removeNotify();
    }

    private static String getNiName(NetworkInterface ni) {
        String add = null;
        var addrIter = ni.getInetAddresses();
        while (addrIter.hasMoreElements()) {
            add = addrIter.nextElement().getHostAddress();
        }
        if (add != null) {
            return String.format("%s[%s]", ni.getDisplayName(), add);
        } else {
            return ni.getDisplayName();
        }
    }

    @Override
    public Insets getInsets() {
        return new Insets(12, 12, 12, 12);
    }
}
