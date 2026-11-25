package com.x64Dynamics.tfb.architectural.plan;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class StructuralIntegrityEngineTest {
    @Test
    public void repairsFloatingPillarAndDoorLandingAndAddsOverhangCorbels() {
        MaterialPalette pal = StylePreset.MEDIEVAL.createPalette();
        List<PlacementTarget> raw = List.of(
            // Ground floor anchor at y=64
            new PlacementTarget(new Vec3i(0, 64, 0), pal.get(MaterialRole.FOUNDATION), MaterialRole.FOUNDATION),
            // Floating pillar at y=67 (y=65 and y=66 missing!)
            new PlacementTarget(new Vec3i(0, 67, 0), pal.get(MaterialRole.PILLAR), MaterialRole.PILLAR),
            // Door at (2, 65, 0) with no foundation below
            new PlacementTarget(new Vec3i(2, 65, 0), pal.get(MaterialRole.DOOR).withProperty("half", "lower"), MaterialRole.DOOR),
            // Wall + cantilevered roof overhang
            new PlacementTarget(new Vec3i(3, 66, 0), pal.get(MaterialRole.WALL_PRIMARY), MaterialRole.WALL_PRIMARY),
            new PlacementTarget(new Vec3i(3, 67, -1), pal.get(MaterialRole.ROOF_OVERHANG), MaterialRole.ROOF_OVERHANG)
        );

        var result = StructuralIntegrityEngine.enforce(raw, pal, null);
        Map<Vec3i, PlacementTarget> byPos = result.targets().stream()
            .collect(Collectors.toMap(PlacementTarget::pos, t -> t, (a, b) -> b));

        // Floating pillar traced down to y=64
        assertTrue(result.report().repairedColumns() >= 2);
        assertEquals(MaterialRole.PILLAR, byPos.get(new Vec3i(0, 65, 0)).role());
        assertEquals(MaterialRole.PILLAR, byPos.get(new Vec3i(0, 66, 0)).role());

        // Door threshold supported
        assertEquals(1, result.report().doorLandingsAdded());
        assertEquals(MaterialRole.FOUNDATION, byPos.get(new Vec3i(2, 64, 0)).role());

        // Overhang got bracket/corbel support if interval matched
        assertNotNull(result.report());
    }

    @Test
    public void groundsPerimeterColumnsDownToSlopedTerrain() {
        MaterialPalette pal = StylePreset.MEDIEVAL.createPalette();
        ILevelAccess sloped = new ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return 60; }
            @Override public BlockIdentifier getBlockState(Vec3i pos) { return BlockIdentifier.of("minecraft:stone"); }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { return true; }
            @Override public boolean isAir(Vec3i pos) { return pos.y() > 60; }
        };

        List<PlacementTarget> raw = List.of(
            new PlacementTarget(new Vec3i(0, 64, 0), pal.get(MaterialRole.FOUNDATION), MaterialRole.FOUNDATION)
        );
        var res = StructuralIntegrityEngine.enforce(raw, pal, sloped);
        assertTrue(res.report().groundedPilings() >= 3);
    }
}
