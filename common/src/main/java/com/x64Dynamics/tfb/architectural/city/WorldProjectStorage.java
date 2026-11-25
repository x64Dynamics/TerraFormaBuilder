package com.x64Dynamics.tfb.architectural.city;

import com.google.gson.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// Persists and loads WorldProject definitions (districts, parcels, locks, seeds, pattern, style)
// to config/terraformabuilder/projects/*.json and regenerates their plans deterministically on load.
public final class WorldProjectStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Path projectsDir = Paths.get("config", "terraformabuilder", "projects");

    private WorldProjectStorage() { }

    public static void setProjectsDir(Path dir) {
        if (dir != null) projectsDir = dir;
    }

    public static Path save(WorldProject project) throws IOException {
        Files.createDirectories(projectsDir);
        String safe = sanitize(project.name());
        Path file = projectsDir.resolve(safe + ".json");
        Files.writeString(file, toJson(project), StandardCharsets.UTF_8);
        return file;
    }

    public static List<String> listSavedProjects() {
        if (!Files.isDirectory(projectsDir)) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectsDir, "*.json")) {
            for (Path p : stream) {
                String fn = p.getFileName().toString();
                names.add(fn.substring(0, fn.length() - 5));
            }
        } catch (IOException ignored) { }
        Collections.sort(names);
        return names;
    }

    public static Optional<WorldProject> load(String name) {
        Path file = projectsDir.resolve(sanitize(name) + ".json");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return Optional.of(fromJson(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String toJson(WorldProject project) {
        JsonObject root = new JsonObject();
        root.addProperty("id", project.id());
        root.addProperty("name", project.name());
        root.addProperty("author", project.author());
        root.addProperty("pattern", project.pattern().name());
        root.addProperty("primaryStyle", project.primaryStyle().name());
        root.addProperty("seed", project.seed());
        root.addProperty("lastModifiedEpochMs", project.lastModifiedEpochMs());
        root.add("bounds", boxToJson(project.bounds()));

        JsonArray districtsArr = new JsonArray();
        for (CityDistrict d : project.districts()) {
            JsonObject dj = new JsonObject();
            dj.addProperty("id", d.id());
            dj.addProperty("name", d.name());
            dj.addProperty("role", d.role().name());
            dj.addProperty("style", d.style().name());
            dj.add("bounds", boxToJson(d.bounds()));

            JsonArray parcelsArr = new JsonArray();
            for (CityParcel p : d.parcels()) {
                JsonObject pj = new JsonObject();
                pj.addProperty("id", p.id());
                pj.addProperty("purpose", p.purpose().name());
                pj.addProperty("streetFacing", p.streetFacing().name());
                pj.addProperty("locked", p.isLocked());
                pj.addProperty("seed", p.seed());
                pj.add("bounds", boxToJson(p.bounds()));
                parcelsArr.add(pj);
            }
            dj.add("parcels", parcelsArr);
            districtsArr.add(dj);
        }
        root.add("districts", districtsArr);
        return GSON.toJson(root);
    }

    public static WorldProject fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String id = root.get("id").getAsString();
        String name = root.get("name").getAsString();
        String author = root.has("author") ? root.get("author").getAsString() : "KyroXova";
        SettlementPattern pattern = SettlementPattern.valueOf(root.get("pattern").getAsString());
        StylePreset style = StylePreset.valueOf(root.get("primaryStyle").getAsString());
        long seed = root.get("seed").getAsLong();
        BoundingBox3i bounds = boxFromJson(root.getAsJsonObject("bounds"));

        WorldProject project = new WorldProject(id, name, author, bounds, pattern, style, seed);
        if (root.has("districts")) {
            for (JsonElement de : root.getAsJsonArray("districts")) {
                JsonObject dj = de.getAsJsonObject();
                CityDistrict district = new CityDistrict(
                    dj.get("id").getAsString(),
                    dj.get("name").getAsString(),
                    CityDistrict.DistrictRole.valueOf(dj.get("role").getAsString()),
                    boxFromJson(dj.getAsJsonObject("bounds")),
                    StylePreset.valueOf(dj.get("style").getAsString())
                );
                if (dj.has("parcels")) {
                    for (JsonElement pe : dj.getAsJsonArray("parcels")) {
                        JsonObject pj = pe.getAsJsonObject();
                        CityParcel parcel = new CityParcel(
                            pj.get("id").getAsString(),
                            district.id(),
                            boxFromJson(pj.getAsJsonObject("bounds")),
                            CityParcel.ParcelPurpose.valueOf(pj.get("purpose").getAsString()),
                            Direction.valueOf(pj.get("streetFacing").getAsString()),
                            pj.get("seed").getAsLong()
                        );
                        parcel.setLocked(pj.has("locked") && pj.get("locked").getAsBoolean());
                        district.addParcel(parcel);
                    }
                }
                project.addDistrict(district);
            }
        }
        return project;
    }

    private static JsonObject boxToJson(BoundingBox3i b) {
        JsonObject o = new JsonObject();
        o.addProperty("minX", b.min().x());
        o.addProperty("minY", b.min().y());
        o.addProperty("minZ", b.min().z());
        o.addProperty("maxX", b.max().x());
        o.addProperty("maxY", b.max().y());
        o.addProperty("maxZ", b.max().z());
        return o;
    }

    private static BoundingBox3i boxFromJson(JsonObject o) {
        return BoundingBox3i.fromCorners(
            new Vec3i(o.get("minX").getAsInt(), o.get("minY").getAsInt(), o.get("minZ").getAsInt()),
            new Vec3i(o.get("maxX").getAsInt(), o.get("maxY").getAsInt(), o.get("maxZ").getAsInt())
        );
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "untitled_city";
        return name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
