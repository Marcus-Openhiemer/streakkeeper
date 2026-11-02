// FILE: src/main/java/com/streakkeeper/model/JsonPersistenceService.java
// PACKAGE: com.streakkeeper.model
package com.streakkeeper.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * JSON-backed persistence using Gson. Data lives at {@code ~/.streakkeeper/data.json}.
 *
 * <p>Why Gson: the brief explicitly requested it, and its field-based reflection
 * keeps the domain classes free of framework annotations.</p>
 */
public class JsonPersistenceService implements PersistenceService {

    private final Path dataFile;
    private final Gson gson;

    public JsonPersistenceService() {
        this.dataFile = Paths.get(System.getProperty("user.home"),
                ".streakkeeper", "data.json");
        this.gson = buildGson();
    }

    /** Test-friendly ctor: allows pointing at an arbitrary data file. */
    public JsonPersistenceService(Path dataFile) {
        this.dataFile = dataFile;
        this.gson = buildGson();
    }

    @Override
    public StreakModel load() {
        return loadFrom(dataFile);
    }

    @Override
    public void save(StreakModel model) {
        saveTo(model, dataFile);
    }

    @Override
    public StreakModel loadFrom(Path path) {
        try {
            if (path == null || !Files.exists(path)) return new StreakModel();
            String json = Files.readString(path);
            StreakModel model = gson.fromJson(json, StreakModel.class);
            return model != null ? model : new StreakModel();
        } catch (Exception e) {
            // Corrupt file should not crash the app: return a fresh model.
            e.printStackTrace();
            return new StreakModel();
        }
    }

    @Override
    public void saveTo(StreakModel model, Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, gson.toJson(model));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Gson has no built-in LocalDate support, so we register an adapter that
     * uses ISO-8601 strings — human-readable and future-proof.
     */
    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();
    }

    private static final class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) out.nullValue();
            else out.value(value.toString());
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString());
        }
    }
}