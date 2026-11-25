package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.platform.persistence.*;
import com.x64Dynamics.tfb.architectural.terrain.FoundationStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class PersistenceTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testBlueprintJsonSerialization() throws IOException {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 10, 5, 10);
        BlueprintMetadata meta = new BlueprintMetadata("Tower 1", "x64Dynamics", System.currentTimeMillis(), "tower", box, "medieval");
        ParametricBlueprint blueprint = new ParametricBlueprint(
            meta, box, "tower", "medieval", FoundationStrategy.PILLARS_STILTS,
            Map.of("WALL_PRIMARY", "minecraft:stone_bricks"),
            Map.of("stories", 2),
            List.of(new Vec3i(0, 0, 0))
        );

        String json = blueprint.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Tower 1"));

        ParametricBlueprint restored = ParametricBlueprint.fromJson(json);
        assertEquals("Tower 1", restored.metadata().name());
        assertEquals("tower", restored.brushId());
        assertEquals("medieval", restored.stylePreset());
    }

    @Test
    public void testBlueprintManagerSaveLoad() throws IOException {
        File dir = tempFolder.newFolder("tfb_test");
        BlueprintManager manager = new BlueprintManager(dir);

        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 5, 5, 5);
        BlueprintMetadata meta = new BlueprintMetadata("House Alpha", "Builder", System.currentTimeMillis(), "house", box, "rustic");
        ParametricBlueprint blueprint = new ParametricBlueprint(meta, box, "house", "rustic", FoundationStrategy.CUT_AND_FILL, null, null, null);

        manager.saveBlueprint(blueprint);
        List<String> list = manager.listAvailableBlueprints();
        assertEquals(1, list.size());
        assertEquals("house_alpha", list.get(0));

        ParametricBlueprint loaded = manager.loadBlueprint("house_alpha");
        assertEquals("House Alpha", loaded.metadata().name());
    }

    @Test
    public void testSchematicExport() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(100, 64, 100), 2, 2, 2);
        List<PlacementTarget> targets = List.of(
            new PlacementTarget(new Vec3i(100, 64, 100), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY),
            new PlacementTarget(new Vec3i(101, 65, 101), BlockIdentifier.of("minecraft:glass"), MaterialRole.WINDOW)
        );

        SchematicExporter.SchematicPayload payload = SchematicExporter.exportRelative(box, targets);
        assertEquals(2, payload.sizeX());
        assertEquals(2, payload.blocks().size());
        assertEquals(0, payload.blocks().get(0).relX());
        assertEquals(1, payload.blocks().get(1).relX());
    }
}
