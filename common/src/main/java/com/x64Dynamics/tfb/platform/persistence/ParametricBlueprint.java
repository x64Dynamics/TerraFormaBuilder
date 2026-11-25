package com.x64Dynamics.tfb.platform.persistence;

import com.google.gson.*;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.architectural.terrain.FoundationStrategy;

import java.util.*;

public final class ParametricBlueprint {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final BlueprintMetadata metadata;
    private final BoundingBox3i bounds;
    private final String brushId;
    private final String stylePreset;
    private final FoundationStrategy foundationStrategy;
    private final Map<String, String> paletteOverrides;
    private final Map<String, Object> properties;
    private final List<Vec3i> pathPoints;

    public ParametricBlueprint(
        BlueprintMetadata metadata,
        BoundingBox3i bounds,
        String brushId,
        String stylePreset,
        FoundationStrategy foundationStrategy,
        Map<String, String> paletteOverrides,
        Map<String, Object> properties,
        List<Vec3i> pathPoints
    ) {
        this.metadata = metadata;
        this.bounds = bounds;
        this.brushId = brushId;
        this.stylePreset = stylePreset;
        this.foundationStrategy = foundationStrategy == null ? FoundationStrategy.PILLARS_STILTS : foundationStrategy;
        this.paletteOverrides = paletteOverrides == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(paletteOverrides));
        this.properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(properties));
        this.pathPoints = pathPoints == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(pathPoints));
    }

    public BlueprintMetadata metadata() { return metadata; }
    public BoundingBox3i bounds() { return bounds; }
    public String brushId() { return brushId; }
    public String stylePreset() { return stylePreset; }
    public FoundationStrategy foundationStrategy() { return foundationStrategy; }
    public Map<String, String> paletteOverrides() { return paletteOverrides; }
    public Map<String, Object> properties() { return properties; }
    public List<Vec3i> pathPoints() { return pathPoints; }

    public MaterialPalette reconstructPalette() {
        StylePreset preset;
        try {
            preset = StylePreset.valueOf(stylePreset.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            preset = StylePreset.MEDIEVAL;
        }
        MaterialPalette palette = preset.createPalette();
        for (Map.Entry<String, String> entry : paletteOverrides.entrySet()) {
            try {
                MaterialRole role = MaterialRole.valueOf(entry.getKey());
                palette.set(role, BlockIdentifier.of(entry.getValue()));
            } catch (IllegalArgumentException ignored) { }
        }
        return palette;
    }

    public String toJson() {
        JsonObject root = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("name", metadata.name());
        meta.addProperty("author", metadata.author());
        meta.addProperty("creationEpochMs", metadata.creationEpochMs());
        meta.addProperty("brushId", metadata.brushId());
        meta.addProperty("stylePreset", metadata.stylePreset());
        root.add("metadata", meta);

        JsonObject b = new JsonObject();
        b.addProperty("minX", bounds.min().x());
        b.addProperty("minY", bounds.min().y());
        b.addProperty("minZ", bounds.min().z());
        b.addProperty("maxX", bounds.max().x());
        b.addProperty("maxY", bounds.max().y());
        b.addProperty("maxZ", bounds.max().z());
        root.add("bounds", b);

        root.addProperty("brushId", brushId);
        root.addProperty("stylePreset", stylePreset);
        root.addProperty("foundationStrategy", foundationStrategy.id());

        JsonObject overrides = new JsonObject();
        for (Map.Entry<String, String> entry : paletteOverrides.entrySet()) {
            overrides.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("paletteOverrides", overrides);

        JsonArray pts = new JsonArray();
        for (Vec3i pt : pathPoints) {
            JsonObject p = new JsonObject();
            p.addProperty("x", pt.x());
            p.addProperty("y", pt.y());
            p.addProperty("z", pt.z());
            pts.add(p);
        }
        root.add("pathPoints", pts);

        return GSON.toJson(root);
    }

    public static ParametricBlueprint fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonObject metaObj = root.getAsJsonObject("metadata");
        String name = metaObj.get("name").getAsString();
        String author = metaObj.get("author").getAsString();
        long creation = metaObj.get("creationEpochMs").getAsLong();
        String metaBrushId = metaObj.get("brushId").getAsString();
        String metaStyle = metaObj.get("stylePreset").getAsString();

        JsonObject bObj = root.getAsJsonObject("bounds");
        Vec3i min = new Vec3i(bObj.get("minX").getAsInt(), bObj.get("minY").getAsInt(), bObj.get("minZ").getAsInt());
        Vec3i max = new Vec3i(bObj.get("maxX").getAsInt(), bObj.get("maxY").getAsInt(), bObj.get("maxZ").getAsInt());
        BoundingBox3i bounds = BoundingBox3i.fromCorners(min, max);

        BlueprintMetadata metadata = new BlueprintMetadata(name, author, creation, metaBrushId, bounds, metaStyle);

        String brushId = root.get("brushId").getAsString();
        String stylePreset = root.get("stylePreset").getAsString();

        String stratId = root.has("foundationStrategy") ? root.get("foundationStrategy").getAsString() : "pillars_stilts";
        FoundationStrategy strategy = FoundationStrategy.PILLARS_STILTS;
        for (FoundationStrategy s : FoundationStrategy.values()) {
            if (s.id().equalsIgnoreCase(stratId)) {
                strategy = s;
                break;
            }
        }

        Map<String, String> overrides = new LinkedHashMap<>();
        if (root.has("paletteOverrides")) {
            JsonObject oObj = root.getAsJsonObject("paletteOverrides");
            for (Map.Entry<String, JsonElement> entry : oObj.entrySet()) {
                overrides.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        List<Vec3i> points = new ArrayList<>();
        if (root.has("pathPoints")) {
            JsonArray arr = root.getAsJsonArray("pathPoints");
            for (JsonElement el : arr) {
                JsonObject ptObj = el.getAsJsonObject();
                points.add(new Vec3i(ptObj.get("x").getAsInt(), ptObj.get("y").getAsInt(), ptObj.get("z").getAsInt()));
            }
        }

        return new ParametricBlueprint(metadata, bounds, brushId, stylePreset, strategy, overrides, Collections.emptyMap(), points);
    }
}
