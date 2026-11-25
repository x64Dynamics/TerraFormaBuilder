package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import org.junit.Test;

import static org.junit.Assert.*;

public class LocalAIBuilderTest {
    private static ILevelAccess mockLevel(int baseY, String blockId, boolean slopeEast) {
        return new ILevelAccess() {
            @Override
            public int getSurfaceHeight(int x, int z) {
                return slopeEast && x > 4 ? baseY + 6 : baseY;
            }

            @Override
            public BlockIdentifier getBlockState(Vec3i pos) {
                return BlockIdentifier.of(blockId);
            }

            @Override
            public boolean setBlockState(Vec3i pos, BlockIdentifier state) {
                return true;
            }

            @Override
            public boolean isAir(Vec3i pos) {
                return pos.y() > baseY;
            }
        };
    }

    @Test
    public void detectsWaterfrontAndChoosesStiltPierHouse() {
        var res = LocalAIBuilder.designAndBuild(new Vec3i(0, 64, 0), "", mockLevel(62, "minecraft:water", false), 42L);
        assertEquals(AutonomousArchitect.DetectedBiomeTheme.COASTAL_WATERFRONT, res.context().biomeTheme());
        assertEquals(FoundationStrategy.PILLARS_STILTS, res.context().recommendedFoundation());
        assertEquals(LocalAIBuilder.Archetype.PIER_HOUSE, res.intent().archetype());
        assertFalse(res.targets().isEmpty());
    }

    @Test
    public void harmonizesWithNearbyDeepslateAndSlopedTerrain() {
        var ctx = LocalAIBuilder.analyzeLocalWorld(new Vec3i(0, 64, 0), mockLevel(64, "minecraft:deepslate_bricks", true));
        assertTrue(ctx.hasNearbyStructures());
        assertEquals(StylePreset.GOTHIC, ctx.harmonizedNeighborhoodStyle());
        assertEquals(FoundationStrategy.STEPPED_TERRACE, ctx.recommendedFoundation());
        assertNotEquals(Direction.EAST, ctx.bestApproachFacing());
    }

    @Test
    public void parsesNaturalLanguagePromptsIntoCompleteBuilds() {
        var manor = LocalAIBuilder.designAndBuild(new Vec3i(0, 64, 0), "3-story japanese manor with porch and chimney", null, 99L);
        assertEquals(StylePreset.JAPANESE, manor.intent().style());
        assertEquals(3, manor.intent().stories());
        assertEquals(LocalAIBuilder.Archetype.MANOR, manor.intent().archetype());
        assertTrue(manor.targets().stream().anyMatch(t -> t.role() == MaterialRole.DOOR));

        var tower = LocalAIBuilder.designAndBuild(new Vec3i(0, 64, 0), "tall gothic watchtower", null, 100L);
        assertEquals(LocalAIBuilder.Archetype.TOWER, tower.intent().archetype());
        assertEquals(StylePreset.GOTHIC, tower.intent().style());
        assertFalse(tower.targets().isEmpty());

        var modular = LocalAIBuilder.designAndBuild(new Vec3i(0, 64, 0), "modular connected compound", null, 101L);
        assertTrue(modular.intent().modularCompound());
        assertFalse(modular.targets().isEmpty());
    }
}
