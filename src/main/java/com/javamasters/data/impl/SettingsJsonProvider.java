package com.javamasters.data.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.javamasters.data.Settings;
import com.javamasters.data.io.DataIO;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.function.Consumer;

public class SettingsJsonProvider implements Settings, Disposable {
    private final DataIO dataIO;
    private final Gson gson;
    private final BehaviorSubject<String>
            language = BehaviorSubject.create(),
            authServer = BehaviorSubject.create(),
            pingAddress = BehaviorSubject.create(),
            nic = BehaviorSubject.create();
    private final CompositeDisposable subscriptions = new CompositeDisposable();
    private JsonObject delegated;

    public SettingsJsonProvider(DataIO dataIO) {
        this.dataIO = dataIO;
        gson = new Gson();

        subscriptions.addAll(
                language.subscribeOn(Schedulers.io())
                        .subscribe(l -> writeJson(obj -> obj.addProperty("language", l))),
                authServer.subscribeOn(Schedulers.io())
                        .subscribe(a -> writeJson(obj -> obj.addProperty("authserver", a))),
                pingAddress.subscribeOn(Schedulers.io())
                        .subscribe(p -> writeJson(obj -> obj.addProperty("ping", p))),
                nic.subscribeOn(Schedulers.io())
                        .subscribe(n -> writeJson(obj -> obj.addProperty("nic", n)))
        );
    }

    private void ensureObj() {
        if (delegated != null) {
            return;
        }
        if (dataIO.created()) {
            try (var dis = dataIO.openInputStream()) {
                updateFromStream(dis);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            var resource = SettingsJsonProvider.class.getClassLoader().getResource("default_settings.json");
            Objects.requireNonNull(resource);
            try (var ris = resource.openStream()) {
                updateFromStream(ris);
                saveObj();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    private void updateFromStream(InputStream is) {
        var reader = new JsonReader(new InputStreamReader(is));
        delegated = gson.fromJson(reader, new TypeToken<JsonObject>() {
        }.getType());

        language.onNext(delegated.get("language").getAsString());
        authServer.onNext(delegated.get("authserver").getAsString());
        pingAddress.onNext(delegated.get("ping").getAsString());
        nic.onNext(delegated.get("nic").getAsString());
    }

    private boolean saveObj() throws IOException {
        if (dataIO.available()) {
            var writer = new JsonWriter(new OutputStreamWriter(dataIO.openOutputStream()));
            gson.toJson(delegated, writer);
            writer.close();
            return true;
        }
        return false;
    }

    @Override
    public Subject<String> preferredLanguage() {
        ensureObj();
        return language;
    }

    @Override
    public Subject<String> authServer() {
        ensureObj();
        return authServer;
    }

    @Override
    public Subject<String> pingAddress() {
        ensureObj();
        return pingAddress;
    }

    @Override
    public Subject<String> nic() {
        ensureObj();
        return nic;
    }

    private void writeJson(Consumer<JsonObject> applier) {
        ensureObj();
        applier.accept(delegated);
        try {
            saveObj();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDisposed() {
        return subscriptions.isDisposed();
    }

    @Override
    public void dispose() {
        subscriptions.dispose();
    }
}
