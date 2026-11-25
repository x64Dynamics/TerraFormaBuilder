package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.AutoArchitectEngine;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.grid.*;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import org.junit.Test;

import static org.junit.Assert.*;

public class AutoArchitectEngineTest {

    private static ILevelAccess flatWorldWithBlock(String blockId, int y) {
        return new ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return y; }
            @Override public BlockIdentifier getBlockState(Vec3i pos) {
                return pos.y() <= y ? BlockIdentifier.of(blockId) : BlockIdentifier.AIR;
            }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { return true; }
            @Override public boolean isAir(Vec3i pos) { return pos.y() > y; }
        };
    }

    @Test
    public void testZeroInputAutoDetectsBiomesAndDesignsCompleteHomes() {
        var snowReport = AutoArchitectEngine.autoDesign(
            new Vec3i(0, 64, 0), null, flatWorldWithBlock("minecraft:snow_block", 64), null, 42L
        );
        assertEquals(AutoArchitectEngine.DetectedBiomeTheme.SNOWY_ALPINE, snowReport.biomeTheme());
        assertEquals(StylePreset.NORDIC, snowReport.chosenStyle());
        assertTrue(snowReport.plan().validation().valid());
        assertTrue(snowReport.plan().totalRoomCount() >= 2);

        var desertReport = AutoArchitectEngine.autoDesign(
            new Vec3i(0, 64, 0), null, flatWorldWithBlock("minecraft:sand", 64), null, 42L
        );
        assertEquals(AutoArchitectEngine.DetectedBiomeTheme.DESERT_ARID, desertReport.biomeTheme());
        assertTrue(desertReport.plan().targets().stream().anyMatch(t -> t.block().path().contains("sandstone")));

        var cherryReport = AutoArchitectEngine.autoDesign(
            new Vec3i(0, 64, 0), null, flatWorldWithBlock("minecraft:grass_block", 64), "minecraft:cherry_grove", 42L
        );
        assertEquals(AutoArchitectEngine.DetectedBiomeTheme.CHERRY_EAST_ASIAN, cherryReport.biomeTheme());
        assertEquals(StylePreset.JAPANESE, cherryReport.chosenStyle());
    }

    @Test
    public void testAutoPopulateComplexWithZeroManualInput() {
        GridManager grid = GridManager.defaultProjectGrid(new Vec3i(0, 64, 0));
        StructurePlacementController ctrl = new StructurePlacementController(
            grid, new BuildingRegistry(),
            new PlacementRuleEngine(PlacementRuleEngine.PlacementRules.defaults()),
            new ConnectionEngine()
        );
        var rep = AutoArchitectEngine.autoPopulateComplex(
            new Vec3i(0, 64, 0), ctrl, flatWorldWithBlock("minecraft:grass_block", 64), null, 77L
        );
        assertFalse(ctrl.registry().allBuildings().isEmpty());
        var res = ctrl.generateComplex();
        assertTrue(res.targets().size() >= 300);
    }
}
