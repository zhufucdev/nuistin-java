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
import io.reactivex.rxjava3.subjects.AsyncSubject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class SettingsJsonProvider implements Settings {
    private final DataIO dataIO;
    private final Gson gson;
    private final AsyncSubject<String> language = AsyncSubject.create();
    private JsonObject obj;

    public SettingsJsonProvider(DataIO dataIO) {
        this.dataIO = dataIO;
        gson = new Gson();
    }

    private void ensureObj() {
        if (obj != null) {
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
        obj = gson.fromJson(reader, new TypeToken<JsonObject>() {
        }.getType());

        language.onNext(obj.get("language").getAsString());
        language.onComplete();
    }

    private boolean saveObj() throws IOException {
        if (dataIO.available()) {
            var writer = new JsonWriter(new OutputStreamWriter(dataIO.openOutputStream()));
            gson.toJson(obj, writer);
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
        return Single.create(emitter -> {
            ensureObj();
            obj.addProperty("language", language);
            try {
                emitter.onSuccess(saveObj());
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }
}
