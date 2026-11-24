package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.*;
import org.junit.Test;

import static org.junit.Assert.*;

public final class MaterialTest {
    @Test
    public void testBlockIdentifierParsing() {
        BlockIdentifier simple = BlockIdentifier.of("minecraft:stone");
        assertEquals("minecraft", simple.namespace());
        assertEquals("stone", simple.path());
        assertTrue(simple.properties().isEmpty());
        assertEquals("minecraft:stone", simple.asString());

        BlockIdentifier withProps = BlockIdentifier.of("minecraft:oak_stairs[facing=north,half=bottom]");
        assertEquals("minecraft", withProps.namespace());
        assertEquals("oak_stairs", withProps.path());
        assertEquals("north", withProps.property("facing"));
        assertEquals("bottom", withProps.property("half"));
        assertEquals("minecraft:oak_stairs[facing=north,half=bottom]", withProps.asString());
    }

    @Test
    public void testFamilyResolver() {
        BlockIdentifier oakPlanks = BlockIdentifier.of("minecraft:oak_planks");
        MaterialFamily family = MaterialFamilyResolver.resolve(oakPlanks);
        assertEquals("oak_stairs", family.stair().path());
        assertEquals("oak_slab", family.slab().path());
        assertEquals("oak_fence", family.fence().path());
        assertEquals("oak_door", family.door().path());
    }

    @Test
    public void testStylePresets() {
        for (StylePreset preset : StylePreset.values()) {
            MaterialPalette palette = preset.createPalette();
            assertNotNull(palette.get(MaterialRole.WALL_PRIMARY));
            assertNotNull(palette.get(MaterialRole.ROOF_SLOPE));
            assertNotNull(palette.get(MaterialRole.FLOOR));
        }
    }
}
