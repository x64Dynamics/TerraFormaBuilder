package com.x64Dynamics.tfb.platform.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BlueprintManager {
    private final File storageDirectory;

    public BlueprintManager(File baseDir) {
        this.storageDirectory = new File(baseDir, "blueprints");
        if (!storageDirectory.exists()) {
            storageDirectory.mkdirs();
        }
    }

    public BlueprintManager(Path basePath) {
        this(basePath.toFile());
    }

    public File storageDirectory() {
        return storageDirectory;
    }

    public void saveBlueprint(ParametricBlueprint blueprint) throws IOException {
        String safeName = blueprint.metadata().name().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        File targetFile = new File(storageDirectory, safeName + ".tfb.json");
        Files.writeString(targetFile.toPath(), blueprint.toJson(), StandardCharsets.UTF_8);
    }

    public ParametricBlueprint loadBlueprint(String name) throws IOException {
        String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        File targetFile = new File(storageDirectory, safeName + ".tfb.json");
        if (!targetFile.exists()) {
            throw new IOException("Blueprint file not found: " + targetFile.getName());
        }
        String json = Files.readString(targetFile.toPath(), StandardCharsets.UTF_8);
        return ParametricBlueprint.fromJson(json);
    }

    public List<String> listAvailableBlueprints() {
        List<String> names = new ArrayList<>();
        File[] files = storageDirectory.listFiles((dir, name) -> name.endsWith(".tfb.json"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                names.add(name.substring(0, name.length() - ".tfb.json".length()));
            }
        }
        return names;
    }

    public boolean deleteBlueprint(String name) {
        String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        File targetFile = new File(storageDirectory, safeName + ".tfb.json");
        return targetFile.delete();
    }
}
