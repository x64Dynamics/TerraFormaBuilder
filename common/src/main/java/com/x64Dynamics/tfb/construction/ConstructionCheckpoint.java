package com.x64Dynamics.tfb.construction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public record ConstructionCheckpoint(
    UUID sessionId,
    int totalBlocks,
    int placedBlocks,
    long timestampMs,
    boolean isPaused
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("sessionId", sessionId.toString());
        obj.addProperty("totalBlocks", totalBlocks);
        obj.addProperty("placedBlocks", placedBlocks);
        obj.addProperty("timestampMs", timestampMs);
        obj.addProperty("isPaused", isPaused);
        return GSON.toJson(obj);
    }

    public static ConstructionCheckpoint fromJson(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return new ConstructionCheckpoint(
            UUID.fromString(obj.get("sessionId").getAsString()),
            obj.get("totalBlocks").getAsInt(),
            obj.get("placedBlocks").getAsInt(),
            obj.get("timestampMs").getAsLong(),
            obj.get("isPaused").getAsBoolean()
        );
    }

    public void save(File file) throws IOException {
        Files.writeString(file.toPath(), toJson(), StandardCharsets.UTF_8);
    }

    public static ConstructionCheckpoint load(File file) throws IOException {
        return fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }
}
