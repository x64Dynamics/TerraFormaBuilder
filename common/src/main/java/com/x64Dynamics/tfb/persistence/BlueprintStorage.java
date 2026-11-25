package com.x64Dynamics.tfb.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Persists and loads ParametricBlueprint JSON files under the mod config directory.
 */
public final class BlueprintStorage {
    private static volatile Path storageDir = Paths.get("config", "terraformabuilder", "blueprints");

    private BlueprintStorage() { }

    public static void setStorageDir(Path dir) {
        if (dir != null) {
            storageDir = dir;
        }
    }

    public static Path getStorageDir() {
        return storageDir;
    }

    public static Path save(ParametricBlueprint blueprint) throws IOException {
        Files.createDirectories(storageDir);
        String safeName = sanitizeFilename(blueprint.metadata().name());
        Path file = storageDir.resolve(safeName + ".json");
        Files.writeString(file, blueprint.toJson(), StandardCharsets.UTF_8);
        return file;
    }

    public static List<String> listSavedNames() {
        if (!Files.isDirectory(storageDir)) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "*.json")) {
            for (Path entry : stream) {
                String fn = entry.getFileName().toString();
                names.add(fn.substring(0, fn.length() - 5));
            }
        } catch (IOException ignored) { }
        Collections.sort(names);
        return names;
    }

    public static Optional<ParametricBlueprint> load(String name) {
        String safeName = sanitizeFilename(name);
        Path file = storageDir.resolve(safeName + ".json");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return Optional.ofNullable(ParametricBlueprint.fromJson(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "untitled_home";
        return name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
