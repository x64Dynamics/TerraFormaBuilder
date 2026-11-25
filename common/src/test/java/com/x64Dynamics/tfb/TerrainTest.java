package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.architectural.exterior.terrain.HeightmapGrid;
import com.x64Dynamics.tfb.architectural.exterior.terrain.TerrainAdapter;
import org.junit.Test;

import static org.junit.Assert.*;

public final class TerrainTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testHeightmapAnalysis() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 10, 0), 5, 5, 5);
        HeightmapGrid grid = new HeightmapGrid(box);
        grid.setElevation(0, 0, 5);
        grid.setElevation(4, 4, 15);

        assertEquals(5, grid.minY());
        assertEquals(15, grid.maxY());
        assertFalse(grid.isFlat());
    }

    @Test
    public void testPillarsAndStiltsStrategy() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 10, 0), 5, 5, 5);
        HeightmapGrid grid = new HeightmapGrid(box);
        // Sloped ground underneath: 5 at front, 8 at back
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                grid.setElevation(x, z, 5 + z);
            }
        }

        TerrainAdapter.TerrainAdaptationResult result = TerrainAdapter.adapt(box, grid, FoundationStrategy.PILLARS_STILTS, palette);
        assertFalse(result.foundationPlacements().isEmpty());
    }

    @Test
    public void testCutAndFillStrategy() {
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 10, 0), 3, 3, 3);
        HeightmapGrid grid = new HeightmapGrid(box);
        // Hill cutting through box
        grid.setElevation(1, 1, 12);
        grid.setElevation(0, 0, 8);

        TerrainAdapter.TerrainAdaptationResult result = TerrainAdapter.adapt(box, grid, FoundationStrategy.CUT_AND_FILL, palette);
        assertFalse(result.excavations().isEmpty());
        assertFalse(result.fillPlacements().isEmpty());
    }
}
