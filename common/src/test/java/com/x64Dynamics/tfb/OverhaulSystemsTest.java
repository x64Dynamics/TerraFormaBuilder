package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.assets.AuthoredAsset;
import com.x64Dynamics.tfb.architectural.assets.AuthoredAssetRegistry;
import com.x64Dynamics.tfb.architectural.wfc.WfcSolver;
import com.x64Dynamics.tfb.architectural.city.TerrainRoadPathfinder;
import com.x64Dynamics.tfb.geometry.*;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.persistence.SchematicExporter;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.architectural.terrain.HeightmapGrid;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class OverhaulSystemsTest {

    @Test
    public void testVoxelVolumeBooleansConnectedComponentsAndFaceVisibility() {
        VoxelVolume a = new VoxelVolume();
        a.set(new Vec3i(0, 0, 0), BlockIdentifier.of("minecraft:stone"));
        a.set(new Vec3i(1, 0, 0), BlockIdentifier.of("minecraft:stone"));
        a.set(new Vec3i(5, 0, 5), BlockIdentifier.of("minecraft:oak_planks"));

        assertEquals(3, a.size());
        assertEquals(2, a.connectedComponents().size());
        assertEquals(63, a.visibleFacesMask(new Vec3i(5, 0, 5)));
        assertTrue(a.visibleFacesMask(new Vec3i(0, 0, 0)) < 63);

        VoxelVolume b = new VoxelVolume();
        b.set(new Vec3i(1, 0, 0), BlockIdentifier.of("minecraft:stone"));
        VoxelVolume inter = a.intersect(b);
        assertEquals(1, inter.size());
        a.subtract(b);
        assertEquals(2, a.size());
    }

    @Test
    public void testObjMeshImporterMeshVoxelizerAndColorMatcher() {
        String cubeObj = """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            v 0 0 1
            v 1 0 1
            v 1 1 1
            v 0 1 1
            f 1 2 3 4
            f 5 6 7 8
            f 1 2 6 5
            f 2 3 7 6
            f 3 4 8 7
            f 4 1 5 8
            """;
        ObjMeshImporter.MeshData mesh = ObjMeshImporter.parseObjString(cubeObj);
        assertEquals(12, mesh.triangles().size());

        VoxelVolume vol = MeshVoxelizer.voxelize(mesh, 6, true, 12.0f, Vec3i.ZERO);
        assertTrue("Voxelized cube should have >= 200 blocks", vol.size() >= 200);
        assertFalse(BlockColorMatcher.matchBlock(120, 120, 120, Vec3i.ZERO, 8.0f, false, false).isAir());
    }

    @Test
    public void testWfcSolverDeterministicAndLockedCells() {
        var rules = WfcSolver.defaultArchitecturalGrammar();
        var res = WfcSolver.solve(4, 2, 4, rules, Map.of("0,0,0", new WfcSolver.ModuleVariant("corner_pillar", 0)), 42L, 8);
        assertTrue("WFC solver should succeed with architectural grammar", res.success());
        assertEquals("corner_pillar", res.grid()[0][0][0].moduleId());
    }

    @Test
    public void testAuthoredAssetCaptureAndRegistryPersistence() throws Exception {
        ILevelAccess mockWorld = new ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return 64; }
            @Override public BlockIdentifier getBlockState(Vec3i pos) {
                return pos.y() == 64 ? BlockIdentifier.of("minecraft:stone_bricks") : BlockIdentifier.AIR;
            }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { return true; }
            @Override public boolean isAir(Vec3i pos) { return pos.y() != 64; }
        };

        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(10, 64, 10), 4, 3, 4);
        AuthoredAsset asset = AuthoredAsset.captureFromWorld(
            "plaza_pad", "Plaza Pad", box, 64, AuthoredAsset.AssetCategory.BUILDING, Direction.SOUTH, mockWorld
        );
        assertEquals(16, asset.blocks().size());

        Path dir = Files.createTempDirectory("tfb_assets_test");
        AuthoredAssetRegistry.getInstance().setStorageDir(dir);
        AuthoredAssetRegistry.getInstance().save(asset);
        var loaded = AuthoredAssetRegistry.getInstance().load("plaza_pad").orElseThrow();
        assertEquals(16, loaded.blocks().size());
        assertEquals(16, loaded.instantiateAt(new Vec3i(0, 70, 0), Rotation.CLOCKWISE_90).size());
    }

    @Test
    public void testTerrainRoadPathfinderAStarAndBridgeDetection() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 32, 16, 32);
        HeightmapGrid grid = new HeightmapGrid(bounds);
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                // River valley at x=14..17
                grid.setElevation(x, z, (x >= 14 && x <= 17) ? 60 : 65);
            }
        }
        List<TerrainRoadPathfinder.RoadNode> path = TerrainRoadPathfinder.findPath(
            new Vec3i(2, 65, 16), new Vec3i(28, 65, 16), grid, 63, 4096
        );
        assertFalse("A* must find a path across the river valley", path.isEmpty());
        assertTrue("River crossing should be tagged as BRIDGE", path.stream().anyMatch(n -> n.spanType() == TerrainRoadPathfinder.SpanType.BRIDGE));
        List<PlacementTarget> targets = TerrainRoadPathfinder.buildRoadTargets(path, 1, StylePreset.MEDIEVAL.createPalette());
        assertTrue("Bridge should include fence railings", targets.stream().anyMatch(t -> t.role() == MaterialRole.FENCE));
    }

    @Test
    public void testSchematicExporterJsonAndGzip() throws Exception {
        Path tmp = Files.createTempDirectory("tfb_schem_test");
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 2, 2, 2);
        List<PlacementTarget> targets = List.of(new PlacementTarget(Vec3i.ZERO, BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY));
        Path jsonPath = tmp.resolve("test.json");
        Path gzPath = tmp.resolve("test.schem.gz");
        SchematicExporter.saveJson(jsonPath, box, targets);
        SchematicExporter.saveCompressedSchematic(gzPath, box, targets);
        assertEquals(1, SchematicExporter.loadJson(jsonPath).blocks().size());
        assertTrue(Files.size(gzPath) > 0);
    }
}
