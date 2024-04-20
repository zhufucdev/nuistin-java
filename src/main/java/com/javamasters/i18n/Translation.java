package com.javamasters.i18n;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Translation {
    public record Metadata(String name, String iso) {
    }

    public final Metadata metadata;
    private final JsonObject translations;

    public Translation(String resName) throws IOException {
        this(Objects.requireNonNull(Translation.class.getClassLoader().getResource(resName)));
    }

    public Translation(URL url) throws IOException {
        var gson = new Gson();
        var ris = url.openStream();
        var reader = new JsonReader(new InputStreamReader(ris));
        JsonObject obj = gson.fromJson(reader, JsonObject.class);
        metadata = gson.fromJson(obj.get("meta"), Metadata.class);
        obj.remove("meta");
        translations = obj;
    }

    public String getString(String key, Object... args) {
        var format = translations.get(key).getAsString();
        return String.format(format, args);
    }

    private static final HashMap<String, URL> namedTranslations = new HashMap<>();
    private static final List<Metadata> metadataList = new ArrayList<>();
    static {
        var rootUrl = Translation.class.getClassLoader().getResource("translations");
        Objects.requireNonNull(rootUrl);
        try {
            var uri = rootUrl.toURI();
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            FileSystem zipfs = FileSystems.newFileSystem(uri, env);
            Files.walk(Path.of(uri)).skip(1).forEach(path -> {
                try {
                    var url = path.toUri().toURL();
                    var translation = new Translation(url);
                    namedTranslations.put(translation.metadata.name, url);
                    metadataList.add(translation.metadata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            zipfs.close();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Translation ofName(String name) throws IOException {
        if (namedTranslations.containsKey(name)) {
            var url = namedTranslations.get(name);
            return new Translation(url);
        } else {
            return null;
        }
    }

    public static Translation ofIso631_1(String iso) throws IOException {
        for (var metadata : metadataList) {
            if (metadata.iso.equals(iso)) {
                return ofName(metadata.name);
            }
        }
        return null;
    }

    public static List<String> getNames() {
        return namedTranslations.keySet().stream().sorted().toList();
    }
}
