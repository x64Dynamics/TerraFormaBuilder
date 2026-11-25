package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

/**
 * Generates plazas, market squares, parks, gardens, fountains, stalls, benches, and street lamps.
 */
public final class PublicSpaceGenerator {
    private PublicSpaceGenerator() { }

    public static List<PlacementTarget> generate(PublicSpace space, MaterialPalette palette, long seed) {
        return switch (space.type()) {
            case PLAZA, MARKET_SQUARE -> generateMarketPlaza(space.bounds(), space.type() == PublicSpace.PublicSpaceType.MARKET_SQUARE, palette, seed);
            case PARK_GARDEN -> generateParkGarden(space.bounds(), palette, seed);
            case WATERFRONT_DOCK -> generateWaterfrontDock(space.bounds(), palette);
        };
    }

    private static List<PlacementTarget> generateWaterfrontDock(BoundingBox3i box, MaterialPalette palette) {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        int minX = box.min().x(), maxX = box.max().x();
        int minZ = box.min().z(), maxZ = box.max().z();
        int y = box.min().y();

        BlockIdentifier planks = BlockIdentifier.of("minecraft:spruce_planks");
        BlockIdentifier slab = BlockIdentifier.of("minecraft:spruce_slab[type=bottom]");
        BlockIdentifier log = BlockIdentifier.of("minecraft:oak_log[axis=y]");
        BlockIdentifier fence = BlockIdentifier.of("minecraft:spruce_fence");
        BlockIdentifier lantern = BlockIdentifier.of("minecraft:lantern");
        BlockIdentifier barrel = BlockIdentifier.of("minecraft:barrel[facing=up]");

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isPiling = ((x - minX) % 4 == 0) && ((z - minZ) % 4 == 0);
                if (isPiling) {
                    for (int py = y - 3; py <= y + 1; py++) {
                        put(map, new Vec3i(x, py, z), log, MaterialRole.PILLAR);
                    }
                    if (x == minX || x == maxX || z == minZ || z == maxZ) {
                        put(map, new Vec3i(x, y + 2, z), fence, MaterialRole.FENCE);
                        put(map, new Vec3i(x, y + 3, z), lantern, MaterialRole.LIGHT_SOURCE);
                    }
                } else {
                    put(map, new Vec3i(x, y, z), ((x + z) % 5 == 0) ? slab : planks, MaterialRole.FLOOR);
                }
            }
        }
        // Cargo crates on pier
        put(map, new Vec3i(minX + 1, y + 1, minZ + 1), barrel, MaterialRole.DECORATION);
        put(map, new Vec3i(minX + 2, y + 1, minZ + 1), barrel, MaterialRole.DECORATION);
        return new ArrayList<>(map.values());
    }

    private static List<PlacementTarget> generateMarketPlaza(
        BoundingBox3i box,
        boolean addMarketStalls,
        MaterialPalette palette,
        long seed
    ) {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        int minX = box.min().x(), maxX = box.max().x();
        int minZ = box.min().z(), maxZ = box.max().z();
        int y = box.min().y();
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;

        BlockIdentifier primaryPave = BlockIdentifier.of("minecraft:stone_bricks");
        BlockIdentifier trimPave = BlockIdentifier.of("minecraft:cobblestone");
        BlockIdentifier accentPave = BlockIdentifier.of("minecraft:polished_andesite");

        // 1. Patterned plaza paving
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isEdge = (x == minX || x == maxX || z == minZ || z == maxZ);
                boolean isAxis = (x == midX || z == midZ);
                BlockIdentifier blk = isEdge ? trimPave : (isAxis ? accentPave : primaryPave);
                put(map, new Vec3i(x, y, z), blk, MaterialRole.FLOOR);
            }
        }

        // 2. Central fountain monument
        if (box.sizeX() >= 7 && box.sizeZ() >= 7) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Vec3i rim = new Vec3i(midX + dx, y + 1, midZ + dz);
                    if (dx == 0 && dz == 0) {
                        put(map, rim, BlockIdentifier.of("minecraft:stone_brick_wall"), MaterialRole.PILLAR);
                        put(map, rim.add(0, 1, 0), BlockIdentifier.of("minecraft:lantern"), MaterialRole.LIGHT_SOURCE);
                    } else {
                        put(map, rim, BlockIdentifier.of("minecraft:stone_brick_slab[type=bottom]"), MaterialRole.SLAB);
                    }
                }
            }
        }

        // 3. Corner lampposts
        for (int[] corner : new int[][]{{minX + 1, minZ + 1}, {maxX - 1, minZ + 1}, {minX + 1, maxZ - 1}, {maxX - 1, maxZ - 1}}) {
            Vec3i base = new Vec3i(corner[0], y + 1, corner[1]);
            put(map, base, palette.get(MaterialRole.FENCE), MaterialRole.PILLAR);
            put(map, base.add(0, 1, 0), palette.get(MaterialRole.FENCE), MaterialRole.PILLAR);
            put(map, base.add(0, 2, 0), palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE);
        }

        // 4. Market stalls along North/South edges if requested
        if (addMarketStalls && box.sizeX() >= 11 && box.sizeZ() >= 9) {
            String[] colors = {"red", "blue", "yellow", "green"};
            Random rng = new Random(seed);
            String c1 = colors[rng.nextInt(colors.length)];
            String c2 = colors[rng.nextInt(colors.length)];
            addStall(map, new Vec3i(minX + 3, y + 1, minZ + 2), c1);
            addStall(map, new Vec3i(maxX - 3, y + 1, minZ + 2), c2);
        }

        return new ArrayList<>(map.values());
    }

    private static void addStall(Map<Vec3i, PlacementTarget> map, Vec3i center, String woolColor) {
        BlockIdentifier fence = BlockIdentifier.of("minecraft:oak_fence");
        BlockIdentifier awning = BlockIdentifier.of("minecraft:" + woolColor + "_wool");
        BlockIdentifier whiteAwning = BlockIdentifier.of("minecraft:white_wool");
        for (int dx = -1; dx <= 1; dx++) {
            if (dx != 0) {
                put(map, center.add(dx, 0, 0), fence, MaterialRole.FENCE);
                put(map, center.add(dx, 1, 0), fence, MaterialRole.FENCE);
            } else {
                put(map, center, BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION);
            }
            put(map, center.add(dx, 2, 0), (dx == 0) ? whiteAwning : awning, MaterialRole.DECORATION);
            put(map, center.add(dx, 2, 1), (dx == 0) ? whiteAwning : awning, MaterialRole.DECORATION);
        }
    }

    private static List<PlacementTarget> generateParkGarden(BoundingBox3i box, MaterialPalette palette, long seed) {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        int minX = box.min().x(), maxX = box.max().x();
        int minZ = box.min().z(), maxZ = box.max().z();
        int y = box.min().y();
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;

        BlockIdentifier grass = BlockIdentifier.of("minecraft:grass_block");
        BlockIdentifier path = BlockIdentifier.of("minecraft:dirt_path");
        BlockIdentifier cobble = BlockIdentifier.of("minecraft:cobblestone");
        Random rng = new Random(seed);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                boolean isWalkway = (Math.abs(x - midX) <= 1 || Math.abs(z - midZ) <= 1);
                BlockIdentifier blk = isBorder ? cobble : (isWalkway ? path : grass);
                put(map, new Vec3i(x, y, z), blk, MaterialRole.FLOOR);

                if (!isBorder && !isWalkway && rng.nextInt(10) < 2) {
                    String flower = rng.nextBoolean() ? "minecraft:poppy" : "minecraft:dandelion";
                    put(map, new Vec3i(x, y + 1, z), BlockIdentifier.of(flower), MaterialRole.DECORATION);
                }
            }
        }

        // Add small stylized garden trees in quadrants if large enough
        if (box.sizeX() >= 9 && box.sizeZ() >= 9) {
            addGardenTree(map, new Vec3i(minX + 2, y + 1, minZ + 2));
            addGardenTree(map, new Vec3i(maxX - 2, y + 1, maxZ - 2));
        }
        return new ArrayList<>(map.values());
    }

    private static void addGardenTree(Map<Vec3i, PlacementTarget> map, Vec3i base) {
        BlockIdentifier log = BlockIdentifier.of("minecraft:oak_log[axis=y]");
        BlockIdentifier leaves = BlockIdentifier.of("minecraft:oak_leaves[persistent=true]");
        for (int i = 0; i < 3; i++) {
            put(map, base.add(0, i, 0), log, MaterialRole.PILLAR);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 2; dy <= 3; dy++) {
                    if (dx == 0 && dz == 0 && dy == 2) continue;
                    put(map, base.add(dx, dy, dz), leaves, MaterialRole.DECORATION);
                }
            }
        }
        put(map, base.add(0, 4, 0), leaves, MaterialRole.DECORATION);
    }

    private static void put(Map<Vec3i, PlacementTarget> map, Vec3i pos, BlockIdentifier blk, MaterialRole role) {
        map.put(pos, new PlacementTarget(pos, blk, role));
    }
}
