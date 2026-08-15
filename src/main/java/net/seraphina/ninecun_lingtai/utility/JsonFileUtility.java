package net.seraphina.ninecun_lingtai.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class JsonFileUtility {
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private JsonFileUtility() {
    }

    public static JsonElement read(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath");
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    public static void write(Path filePath, JsonElement jsonElement) throws IOException {
        Objects.requireNonNull(filePath, "filePath");
        Objects.requireNonNull(jsonElement, "jsonElement");

        Path normalizedFilePath = filePath.toAbsolutePath().normalize();
        Path parentPath = normalizedFilePath.getParent();
        if (parentPath == null) {
            throw new IOException("JSON file must have a parent directory: " + filePath);
        }

        Files.createDirectories(parentPath);
        Path temporaryFilePath = Files.createTempFile(
                parentPath,
                normalizedFilePath.getFileName().toString() + ".",
                ".tmp"
        );
        try {
            try (Writer writer = Files.newBufferedWriter(
                    temporaryFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                PRETTY_GSON.toJson(jsonElement, writer);
            }
            moveAtomically(temporaryFilePath, normalizedFilePath);
        } finally {
            Files.deleteIfExists(temporaryFilePath);
        }
    }

    private static void moveAtomically(Path temporaryFilePath, Path targetFilePath) throws IOException {
        try {
            Files.move(
                    temporaryFilePath,
                    targetFilePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
