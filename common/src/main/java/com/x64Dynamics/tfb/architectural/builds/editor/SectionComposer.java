package com.x64Dynamics.tfb.architectural.builds.editor;

import com.x64Dynamics.tfb.architectural.exterior.roof.RoofFeatures;
import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.architectural.exterior.terrain.HeightmapGrid;
import com.x64Dynamics.tfb.architectural.exterior.terrain.TerrainAdapter;

import java.util.*;

// Generates VoxelDreamer-inspired architectural geometry and adaptive details
// for an individual {@link EditableBuildingSection}, responding to adjacent sections
// (walkable rooftop terraces, connecting doors) and live terrain.
public final class SectionComposer {
    private SectionComposer() {}

    public static List<PlacementTarget> compose(
            EditableBuildingSection section,
            List<EditableBuildingSection> allSections,
            boolean hasTallerNeighbor,
            HeightmapGrid heightmap,
            FoundationStrategy foundationStrategy) {
        BoundingBox3i b = section.bounds();
        MaterialPalette palette = section.style().createPalette();
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();

        if (section.kind() == EditableBuildingSection.SectionKind.PORCH) {
            composePorch(b, section.facing(), palette, map);
            return new ArrayList<>(map.values());
        }
        if (section.kind() == EditableBuildingSection.SectionKind.STAIR_APPROACH) {
            composeStairApproach(b, section.facing(), palette, heightmap, map);
            return new ArrayList<>(map.values());
        }

        int minX = b.min().x(), maxX = b.max().x();
        int minY = b.min().y();
        int minZ = b.min().z(), maxZ = b.max().z();
        int width = Math.max(5, b.sizeX());
        int depth = Math.max(5, b.sizeZ());
        maxX = minX + width - 1;
        maxZ = minZ + depth - 1;

        int stories = section.stories();
        var styleRules = com.x64Dynamics.tfb.architectural.builds.style.StyleRuleSetRegistry.forPreset(section.style());
        int storyHeight = (section.style() == com.x64Dynamics.tfb.material.StylePreset.MODERN
            || section.style() == com.x64Dynamics.tfb.material.StylePreset.FANTASY) ? 5 : 4;
        int pillarStep = Math.max(3, styleRules.windowSpacing() + 1);
        int windowStep = Math.max(2, styleRules.windowSpacing());
        int wallTopY = minY + stories * storyHeight;

        BlockIdentifier groundWall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier upperWall = palette.get(MaterialRole.WALL_ACCENT);
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        BlockIdentifier beam = palette.get(MaterialRole.BEAM);
        BlockIdentifier floorMat = palette.get(MaterialRole.FLOOR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        BlockIdentifier glass = palette.get(MaterialRole.WINDOW);
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);
        BlockIdentifier doorBase = palette.get(MaterialRole.DOOR).withProperty("facing", section.facing().name().toLowerCase(Locale.ROOT));
        BlockIdentifier shutterBase = palette.get(MaterialRole.FRAME);

        // 1. Multi-story shell with story-based material zoning & timber framing
        for (int s = 0; s < stories; s++) {
            int y0 = minY + 1 + s * storyHeight;
            int y1 = y0 + storyHeight - 1;
            BlockIdentifier storyWall = (s == 0) ? groundWall : upperWall;

            // Floor deck for this story
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean border = (x == minX || x == maxX || z == minZ || z == maxZ);
                    put(map, new Vec3i(x, y0 - 1, z), border ? beam : floorMat, border ? MaterialRole.BEAM : MaterialRole.FLOOR);
                }
            }

            // Perimeter walls + corner/bay pillars
            for (int y = y0; y <= y1; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean onX = (x == minX || x == maxX);
                        boolean onZ = (z == minZ || z == maxZ);
                        if (!onX && !onZ) {
                            putAir(map, new Vec3i(x, y, z));
                            continue;
                        }
                        boolean corner = onX && onZ;
                        boolean bayPost = styleRules.allowPilasters() && ((onZ && (x - minX) % pillarStep == 0) || (onX && (z - minZ) % pillarStep == 0));
                        if (corner || bayPost) {
                            put(map, new Vec3i(x, y, z), pillar, MaterialRole.PILLAR);
                        } else if (y == y1 && s < stories - 1) {
                            put(map, new Vec3i(x, y, z), beam, MaterialRole.BEAM);
                        } else {
                            put(map, new Vec3i(x, y, z), storyWall, s == 0 ? MaterialRole.WALL_PRIMARY : MaterialRole.WALL_ACCENT);
                        }
                    }
                }
            }

            // Rhythmic windows with slab lintels & trapdoor shutters on all 4 facades
            int winY = y0 + 1;
            for (int x = minX + 2; x <= maxX - 2; x += windowStep) {
                if (styleRules.allowPilasters() && (x - minX) % pillarStep == 0) continue;
                placeWindowWithShutters(map, x, winY, minZ, Direction.NORTH, glass, slab, shutterBase);
                placeWindowWithShutters(map, x, winY, maxZ, Direction.SOUTH, glass, slab, shutterBase);
            }
            for (int z = minZ + 2; z <= maxZ - 2; z += windowStep) {
                if (styleRules.allowPilasters() && (z - minZ) % pillarStep == 0) continue;
                placeWindowWithShutters(map, minX, winY, z, Direction.WEST, glass, slab, shutterBase);
                placeWindowWithShutters(map, maxX, winY, z, Direction.EAST, glass, slab, shutterBase);
            }
        }

        // 2. Interior stairwell connecting floors
        if (stories > 1 && width >= 6 && depth >= 6) {
            int sx = minX + 2;
            int sz = minZ + 2;
            for (int s = 0; s < stories - 1; s++) {
                int baseFloorY = minY + 1 + s * storyHeight;
                for (int step = 0; step < storyHeight; step++) {
                    Vec3i stPos = new Vec3i(sx + step, baseFloorY + step, sz);
                    if (stPos.x() < maxX) {
                        put(map, stPos, stair.withProperty("facing", "east"), MaterialRole.STAIR);
                        putAir(map, stPos.add(0, 1, 0));
                        putAir(map, stPos.add(0, 2, 0));
                        putAir(map, stPos.add(0, 3, 0));
                    }
                }
            }
        }

        // 3. Main entrance door on facing facade
        int doorX = (minX + maxX) / 2;
        int doorZ = section.facing() == Direction.NORTH ? minZ : maxZ;
        Vec3i doorPos = new Vec3i(doorX, minY + 1, doorZ);
        put(map, doorPos, doorBase.withProperty("half", "lower"), MaterialRole.DOOR);
        put(map, doorPos.add(0, 1, 0), doorBase.withProperty("half", "upper"), MaterialRole.DOOR);
        int outDz = section.facing() == Direction.NORTH ? -1 : 1;
        put(map, doorPos.add(0, 2, outDz), slab.withProperty("type", "top"), MaterialRole.SLAB);
        put(map, doorPos.add(-1, 1, outDz), lantern.withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);

        // 4. Roof OR Walkable Rooftop Terrace (when a 1-story wing abuts a taller section)
        boolean makeTerrace = section.kind() == EditableBuildingSection.SectionKind.ROOFTOP_TERRACE
            || (section.kind() == EditableBuildingSection.SectionKind.SIDE_WING && stories == 1 && hasTallerNeighbor);

        if (makeTerrace) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean border = (x == minX || x == maxX || z == minZ || z == maxZ);
                    put(map, new Vec3i(x, wallTopY, z), floorMat, MaterialRole.FLOOR);
                    if (border) {
                        boolean corner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
                        put(map, new Vec3i(x, wallTopY + 1, z), corner ? pillar : fence, corner ? MaterialRole.PILLAR : MaterialRole.FENCE);
                        if (corner) {
                            put(map, new Vec3i(x, wallTopY + 2, z), lantern, MaterialRole.LIGHT_SOURCE);
                        }
                    }
                }
            }
            // Terrace barrel & flower pot accent
            put(map, new Vec3i(minX + 1, wallTopY + 1, minZ + 1), BlockIdentifier.of("minecraft:barrel"), MaterialRole.DECORATION);
        } else {
            BoundingBox3i roofBounds = BoundingBox3i.fromCorners(
                new Vec3i(minX, wallTopY, minZ),
                new Vec3i(maxX, wallTopY + 4, maxZ)
            );
            Axis ridgeAxis = width >= depth ? Axis.X : Axis.Z;
            for (PlacementTarget rt : RoofGenerator.generateRoof(roofBounds, section.roofType(), ridgeAxis, 4, palette)) {
                put(map, rt.pos(), rt.block(), rt.role());
            }
            // Stepped gable parapets on gable ends for VoxelDreamer silhouette
            if (section.roofType() == RoofGenerator.RoofType.GABLE && width >= 7) {
                int halfSpan = (ridgeAxis == Axis.X ? depth : width) / 2;
                if (ridgeAxis == Axis.X) {
                    for (int gx : new int[]{minX, maxX}) {
                        for (int dz = 0; dz <= halfSpan; dz++) {
                            put(map, new Vec3i(gx, wallTopY + dz + 1, minZ + dz), upperWall, MaterialRole.WALL_ACCENT);
                            put(map, new Vec3i(gx, wallTopY + dz + 1, maxZ - dz), upperWall, MaterialRole.WALL_ACCENT);
                        }
                    }
                }
            }
            if (width >= 11 && section.roofType() == RoofGenerator.RoofType.GABLE) {
                Vec3i dormerPos = new Vec3i(doorX, wallTopY + 2, maxZ - 1);
                for (PlacementTarget dt : RoofFeatures.generateDormer(dormerPos, Direction.SOUTH, 3, 2, palette)) {
                    put(map, dt.pos(), dt.block(), dt.role());
                }
            }
            if (section.kind() == EditableBuildingSection.SectionKind.MAIN_WING) {
                for (PlacementTarget ct : RoofFeatures.generateChimney(new Vec3i(maxX - 2, wallTopY, minZ + 2), 4, palette)) {
                    put(map, ct.pos(), ct.block(), ct.role());
                }
            }
        }

        // 5. Corner barrels, leaf bushes & terrain-aware foundation
        int[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        BlockIdentifier leaves = BlockIdentifier.of("minecraft:azalea_leaves");
        BlockIdentifier barrel = BlockIdentifier.of("minecraft:barrel");
        for (int i = 0; i < corners.length; i++) {
            int cx = corners[i][0] + (corners[i][0] == minX ? -1 : 1);
            int cz = corners[i][1] + (corners[i][1] == minZ ? -1 : 1);
            int gy = heightmap != null ? heightmap.getElevation(cx, cz) : minY;
            put(map, new Vec3i(cx, gy + 1, cz), (i % 2 == 0) ? leaves : barrel, MaterialRole.DECORATION);
        }

        if (heightmap != null) {
            var terrainResult = TerrainAdapter.adapt(
                b, heightmap,
                foundationStrategy != null ? foundationStrategy : FoundationStrategy.STEPPED_TERRACE,
                palette
            );
            for (PlacementTarget ft : terrainResult.foundationPlacements()) {
                map.putIfAbsent(ft.pos(), ft);
            }
            for (PlacementTarget fill : terrainResult.fillPlacements()) {
                map.putIfAbsent(fill.pos(), fill);
            }
        }

        return new ArrayList<>(map.values());
    }

    private static void placeWindowWithShutters(
            Map<Vec3i, PlacementTarget> map,
            int x, int y, int z,
            Direction outDir,
            BlockIdentifier glass,
            BlockIdentifier slab,
            BlockIdentifier shutterBase) {
        Vec3i wp = new Vec3i(x, y, z);
        put(map, wp, glass, MaterialRole.WINDOW);
        int ox = outDir == Direction.EAST ? 1 : outDir == Direction.WEST ? -1 : 0;
        int oz = outDir == Direction.SOUTH ? 1 : outDir == Direction.NORTH ? -1 : 0;
        // Slab hood lintel above window
        put(map, wp.add(ox, 1, oz), slab.withProperty("type", "top"), MaterialRole.SLAB);
        // Open trapdoor shutters on left & right of window
        BlockIdentifier shutter = shutterBase
            .withProperty("facing", outDir.name().toLowerCase(Locale.ROOT))
            .withProperty("open", "true")
            .withProperty("half", "bottom");
        if (ox == 0) {
            put(map, wp.add(-1, 0, oz), shutter, MaterialRole.FRAME);
            put(map, wp.add(1, 0, oz), shutter, MaterialRole.FRAME);
        } else {
            put(map, wp.add(ox, 0, -1), shutter, MaterialRole.FRAME);
            put(map, wp.add(ox, 0, 1), shutter, MaterialRole.FRAME);
        }
    }

    private static void composePorch(
            BoundingBox3i b,
            Direction facing,
            MaterialPalette palette,
            Map<Vec3i, PlacementTarget> map) {
        BlockIdentifier floorMat = palette.get(MaterialRole.FLOOR);
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);
        int y = b.min().y();
        int roofY = y + 3;
        for (int x = b.min().x(); x <= b.max().x(); x++) {
            for (int z = b.min().z(); z <= b.max().z(); z++) {
                boolean corner = (x == b.min().x() || x == b.max().x()) && (z == b.min().z() || z == b.max().z());
                put(map, new Vec3i(x, y, z), floorMat, MaterialRole.FLOOR);
                put(map, new Vec3i(x, roofY, z), slab.withProperty("type", "bottom"), MaterialRole.SLAB);
                if (corner) {
                    put(map, new Vec3i(x, y + 1, z), fence, MaterialRole.FENCE);
                    put(map, new Vec3i(x, y + 2, z), fence, MaterialRole.FENCE);
                    put(map, new Vec3i(x, y + 3, z), fence, MaterialRole.FENCE);
                }
            }
        }
        int cx = (b.min().x() + b.max().x()) / 2;
        int cz = (b.min().z() + b.max().z()) / 2;
        put(map, new Vec3i(cx, roofY - 1, cz), lantern.withProperty("hanging", "true"), MaterialRole.LIGHT_SOURCE);
    }

    private static void composeStairApproach(
            BoundingBox3i b,
            Direction facing,
            MaterialPalette palette,
            HeightmapGrid heightmap,
            Map<Vec3i, PlacementTarget> map) {
        BlockIdentifier stair = palette.get(MaterialRole.STAIR).withProperty("facing", facing.name().toLowerCase(Locale.ROOT));
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);
        int baseY = b.min().y();
        int len = Math.max(2, Math.min(6, b.sizeZ()));
        for (int i = 0; i < len; i++) {
            int y = baseY + i;
            for (int x = b.min().x(); x <= b.max().x(); x++) {
                int z = b.min().z() + i;
                put(map, new Vec3i(x, y, z), stair, MaterialRole.STAIR);
                int gy = heightmap != null ? heightmap.getElevation(x, z) : baseY;
                for (int fy = gy; fy < y; fy++) {
                    put(map, new Vec3i(x, fy, z), foundation, MaterialRole.FOUNDATION);
                }
            }
        }
    }

    private static void put(Map<Vec3i, PlacementTarget> map, Vec3i pos, BlockIdentifier block, MaterialRole role) {
        map.put(pos, new PlacementTarget(pos, block, role));
    }

    private static void putAir(Map<Vec3i, PlacementTarget> map, Vec3i pos) {
        map.put(pos, new PlacementTarget(pos, BlockIdentifier.AIR, MaterialRole.AIR));
    }
}
