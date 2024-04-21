package com.javamasters.data.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.javamasters.data.Settings;
import com.javamasters.data.io.DataIO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.function.Consumer;

public class SettingsJsonProvider implements Settings {
    private final DataIO dataIO;
    private final Gson gson;
    private final BehaviorSubject<String>
            language = BehaviorSubject.create(),
            authServer = BehaviorSubject.create(),
            pingAddress = BehaviorSubject.create(),
            nic = BehaviorSubject.create();
    private JsonObject delegated;

    public SettingsJsonProvider(DataIO dataIO) {
        this.dataIO = dataIO;
        gson = new Gson();
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
    public Observable<String> preferredLanguage() {
        ensureObj();
        return language;
    }

    @Override
    public Single<Boolean> setLanguage(String language) {
        this.language.onNext(language);
        return dispatchWrite(obj -> obj.addProperty("language", language));
    }

    @Override
    public Observable<String> authServer() {
        return authServer;
    }

    @Override
    public Single<Boolean> setAuthServer(String url) {
        authServer.onNext(url);
        return dispatchWrite(obj -> obj.addProperty("authserver", url));
    }

    @Override
    public Observable<String> pingAddress() {
        return pingAddress;
    }

    @Override
    public Single<Boolean> setPingAddress(String host) {
        pingAddress.onNext(host);
        return dispatchWrite(obj -> obj.addProperty("ping", host));
    }

    @Override
    public Observable<String> nic() {
        return nic;
    }

    @Override
    public Single<Boolean> setNic(String name) {
        nic.onNext(name);
        return dispatchWrite(obj -> obj.addProperty("nic", name));
    }

    private Single<Boolean> dispatchWrite(Consumer<JsonObject> applier) {
        return Single.create(emitter -> {
            ensureObj();
            applier.accept(delegated);
            try {
                emitter.onSuccess(saveObj());
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }
}
