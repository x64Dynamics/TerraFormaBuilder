package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class InteriorFurnishingGenerator {
    private InteriorFurnishingGenerator() { }

    public static List<PlacementTarget> furnishRoom(InteriorRoom room, MaterialPalette palette) {
        return furnishRoom(room, palette, Collections.emptySet(), 1.0f);
    }

    public static List<PlacementTarget> furnishRoom(
        InteriorRoom room,
        MaterialPalette palette,
        Set<Vec3i> clearanceZones,
        float density
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        Map<Vec3i, PlacementTarget> map = new HashMap<>();

        BoundingBox3i b = room.bounds();
        int y = b.min().y();

        int centerX = (b.min().x() + b.max().x()) / 2;
        int centerZ = (b.min().z() + b.max().z()) / 2;

        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier light = palette.get(MaterialRole.LIGHT_SOURCE);

        switch (room.type()) {
            case LIVING_HALL -> {
                // 1. Central sitting arrangement: Coffee table with sofa/chairs
                Vec3i tablePos = new Vec3i(centerX, y, centerZ);
                safePut(tablePos, fence, MaterialRole.DECORATION, clearanceZones, map);
                safePut(tablePos.add(0, 1, 0), slab, MaterialRole.DECORATION, clearanceZones, map);

                // Sofa chairs flanking table
                safePut(tablePos.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(tablePos.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(tablePos.add(-1, 0, 0), stair.withProperty("facing", "east").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);

                // Rug under seating
                BlockIdentifier redCarpet = BlockIdentifier.of("minecraft:red_carpet");
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        safePut(tablePos.add(dx, 0, dz), redCarpet, MaterialRole.DECORATION, clearanceZones, map);
                    }
                }

                // 2. Hanging Chandelier
                Vec3i ceilingCenter = new Vec3i(centerX, b.max().y(), centerZ);
                map.put(ceilingCenter, new PlacementTarget(ceilingCenter, BlockIdentifier.of("minecraft:chain"), MaterialRole.DECORATION));
                map.put(ceilingCenter.add(0, -1, 0), new PlacementTarget(ceilingCenter.add(0, -1, 0), light, MaterialRole.LIGHT_SOURCE));
            }
            case BEDROOM -> {
                // Canopy bed in quiet corner
                Vec3i bedHead = new Vec3i(b.min().x() + 1, y, b.min().z() + 1);
                Vec3i bedFoot = bedHead.add(0, 0, 1);
                safePut(bedHead, BlockIdentifier.of("minecraft:red_bed[facing=south,part=head]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(bedFoot, BlockIdentifier.of("minecraft:red_bed[facing=south,part=foot]"), MaterialRole.DECORATION, clearanceZones, map);

                // Nightstand with lantern
                Vec3i nightstand = bedHead.add(1, 0, 0);
                safePut(nightstand, BlockIdentifier.of("minecraft:oak_slab[half=top]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(nightstand.add(0, 1, 0), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);

                // Wardrobe cabinet on opposite wall
                Vec3i wardrobeBase = new Vec3i(b.max().x() - 1, y, b.min().z() + 1);
                safePut(wardrobeBase, BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(wardrobeBase.add(0, 1, 0), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case DINING_HALL -> {
                // Long banquet dining table
                for (int x = b.min().x() + 2; x <= b.max().x() - 2; x++) {
                    Vec3i tPos = new Vec3i(x, y, centerZ);
                    safePut(tPos, slab, MaterialRole.SLAB, clearanceZones, map);
                    safePut(tPos.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(tPos.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                }
                // Overhead lighting
                Vec3i light1 = new Vec3i(b.min().x() + 2, b.max().y(), centerZ);
                Vec3i light2 = new Vec3i(b.max().x() - 2, b.max().y(), centerZ);
                map.put(light1, new PlacementTarget(light1, light, MaterialRole.LIGHT_SOURCE));
                map.put(light2, new PlacementTarget(light2, light, MaterialRole.LIGHT_SOURCE));
            }
            case KITCHEN_HEARTH -> {
                // Fireplace hearth along back wall
                Vec3i hearth = new Vec3i(centerX, y, b.max().z() - 1);
                safePut(hearth, BlockIdentifier.of("minecraft:campfire"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(hearth.add(-1, 0, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY, clearanceZones, map);
                safePut(hearth.add(1, 0, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY, clearanceZones, map);
                safePut(hearth.add(-1, 1, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY, clearanceZones, map);
                safePut(hearth.add(1, 1, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY, clearanceZones, map);
                safePut(hearth.add(0, 2, 0), BlockIdentifier.of("minecraft:brick_slab"), MaterialRole.SLAB, clearanceZones, map);

                // Kitchen appliances: Smoker, Cauldron sink, and prep counter
                safePut(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:smoker"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 3, y, b.min().z() + 1), BlockIdentifier.of("minecraft:blast_furnace"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case LIBRARY_STUDY -> {
                // Bookshelves line the back wall
                int zBack = b.max().z() - 1;
                for (int x = b.min().x() + 1; x <= b.max().x() - 1; x++) {
                    safePut(new Vec3i(x, y, zBack), BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(new Vec3i(x, y + 1, zBack), BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION, clearanceZones, map);
                }
                // Writing study desk
                Vec3i desk = new Vec3i(centerX, y, centerZ);
                safePut(desk, fence, MaterialRole.DECORATION, clearanceZones, map);
                safePut(desk.add(0, 1, 0), BlockIdentifier.of("minecraft:oak_pressure_plate"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(desk.add(1, 1, 0), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);
                safePut(desk.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case WORKSHOP_SMITHY -> {
                safePut(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:anvil"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:blast_furnace"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 3, y, b.min().z() + 1), BlockIdentifier.of("minecraft:stonecutter"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.max().x() - 1, y, b.max().z() - 1), BlockIdentifier.of("minecraft:crafting_table"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.max().x() - 2, y, b.max().z() - 1), BlockIdentifier.of("minecraft:smithing_table"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case STORAGE_PANTRY -> {
                for (int x = b.min().x() + 1; x <= b.max().x() - 1; x += 2) {
                    safePut(new Vec3i(x, y, b.min().z() + 1), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(new Vec3i(x, y + 1, b.min().z() + 1), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                }
            }
            case BATHROOM -> {
                safePut(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:smooth_quartz_slab"), MaterialRole.SLAB, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y + 1, b.min().z() + 1), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);
            }
            case ARMORY -> {
                safePut(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:armor_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:armor_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.max().x() - 1, y, b.max().z() - 1), BlockIdentifier.of("minecraft:grindstone"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.max().x() - 2, y, b.max().z() - 1), BlockIdentifier.of("minecraft:chest"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case ALCHEMICAL_LAB -> {
                safePut(new Vec3i(centerX, y, centerZ), BlockIdentifier.of("minecraft:enchanting_table"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:brewing_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
            }
        }

        targets.addAll(map.values());
        return targets;
    }

    private static void safePut(
        Vec3i pos,
        BlockIdentifier block,
        MaterialRole role,
        Set<Vec3i> clearanceZones,
        Map<Vec3i, PlacementTarget> map
    ) {
        if (!clearanceZones.contains(pos)) {
            map.put(pos, new PlacementTarget(pos, block, role));
        }
    }
}
