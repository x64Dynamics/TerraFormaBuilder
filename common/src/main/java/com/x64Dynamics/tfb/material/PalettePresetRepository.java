package com.x64Dynamics.tfb.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class PalettePresetRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File directory;
    private final Map<String, MaterialPalette> presets = new LinkedHashMap<>();

    public PalettePresetRepository(File baseDir) {
        this.directory = new File(baseDir, "palettes");
        if (!directory.exists()) directory.mkdirs();

        for (StylePreset p : StylePreset.values()) {
            presets.put(p.id(), p.createPalette());
        }
    }

    public MaterialPalette get(String id) {
        return presets.get(id);
    }

    public void register(MaterialPalette palette) {
        presets.put(palette.id(), palette);
    }

    public Collection<MaterialPalette> all() {
        return Collections.unmodifiableCollection(presets.values());
    }

    public void saveToFile(MaterialPalette palette) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", palette.id());
        JsonObject mappings = new JsonObject();
        for (Map.Entry<MaterialRole, BlockIdentifier> entry : palette.allMappings().entrySet()) {
            mappings.addProperty(entry.getKey().name(), entry.getValue().asString());
        }
        obj.add("mappings", mappings);

        File target = new File(directory, palette.id().replaceAll("[^a-zA-Z0-9_-]", "_") + ".json");
        Files.writeString(target.toPath(), GSON.toJson(obj), StandardCharsets.UTF_8);
    }

    public MaterialPalette loadFromFile(File file) throws IOException {
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String id = obj.get("id").getAsString();
        MaterialPalette palette = new MaterialPalette(id);

        JsonObject mappings = obj.getAsJsonObject("mappings");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : mappings.entrySet()) {
            try {
                MaterialRole role = MaterialRole.valueOf(entry.getKey());
                palette.set(role, BlockIdentifier.of(entry.getValue().getAsString()));
            } catch (IllegalArgumentException ignored) { }
        }
        register(palette);
        return palette;
    }
}
