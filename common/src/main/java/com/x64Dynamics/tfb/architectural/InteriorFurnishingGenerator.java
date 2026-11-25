package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.BlockStateNormalizer;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

/**
 * Generates realistic human-scale interior layouts and furnishings for every RoomType,
 * using exact interior room bounds (without double-shrinking walls), height-aware
 * ceiling lighting, style-harmonized rugs/beds, and clearance-safe placement.
 */
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
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        BoundingBox3i b = room.bounds();
        int y = b.min().y();
        int minX = b.min().x(), maxX = b.max().x();
        int minZ = b.min().z(), maxZ = b.max().z();
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;

        if (b.sizeX() < 3 || b.sizeZ() < 3) {
            return Collections.emptyList();
        }

        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        BlockIdentifier slab = palette.get(MaterialRole.SLAB);
        BlockIdentifier fence = palette.get(MaterialRole.FENCE);
        BlockIdentifier light = palette.get(MaterialRole.LIGHT_SOURCE);
        String floorPath = palette.get(MaterialRole.FLOOR).path();
        String carpetId = floorPath.contains("warped") ? "minecraft:cyan_carpet"
            : floorPath.contains("birch") ? "minecraft:light_gray_carpet"
            : floorPath.contains("dark_oak") ? "minecraft:brown_carpet"
            : "minecraft:red_carpet";
        String bedId = floorPath.contains("warped") ? "minecraft:cyan_bed"
            : floorPath.contains("birch") ? "minecraft:white_bed"
            : floorPath.contains("dark_oak") ? "minecraft:brown_bed"
            : "minecraft:red_bed";

        switch (room.type()) {
            case LIVING_HALL -> {
                Vec3i tablePos = new Vec3i(centerX, y, centerZ);
                safePut(tablePos, fence, MaterialRole.DECORATION, clearanceZones, map);
                safePut(tablePos.add(0, 1, 0), BlockStateNormalizer.normalize(slab.withProperty("type", "bottom"), Direction.SOUTH), MaterialRole.DECORATION, clearanceZones, map);
                if (b.sizeY() >= 4) {
                    safePut(tablePos.add(0, 2, 0), BlockIdentifier.of("minecraft:flower_pot"), MaterialRole.DECORATION, clearanceZones, map);
                }

                if (b.sizeX() >= 5 && b.sizeZ() >= 5) {
                    safePut(tablePos.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(tablePos.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(tablePos.add(-1, 0, 0), stair.withProperty("facing", "east").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                }

                BlockIdentifier carpet = BlockIdentifier.of(carpetId);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Vec3i rp = tablePos.add(dx, 0, dz);
                        if (rp.x() >= minX && rp.x() <= maxX && rp.z() >= minZ && rp.z() <= maxZ && !map.containsKey(rp)) {
                            safePut(rp, carpet, MaterialRole.DECORATION, clearanceZones, map);
                        }
                    }
                }

                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(minX, y + 1, minZ), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case BEDROOM -> {
                Vec3i bedHead = new Vec3i(minX, y, minZ);
                Vec3i bedFoot = bedHead.add(0, 0, 1);
                safePut(bedHead, BlockIdentifier.of(bedId + "[facing=south,part=head]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(bedFoot, BlockIdentifier.of(bedId + "[facing=south,part=foot]"), MaterialRole.DECORATION, clearanceZones, map);

                Vec3i nightstand = bedHead.add(1, 0, 0);
                safePut(nightstand, BlockStateNormalizer.normalize(slab.withProperty("type", "top"), Direction.SOUTH), MaterialRole.DECORATION, clearanceZones, map);
                safePut(nightstand.add(0, 1, 0), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);
                safePut(bedFoot.add(1, 0, 0), BlockIdentifier.of(carpetId), MaterialRole.DECORATION, clearanceZones, map);

                Vec3i wardrobeBase = new Vec3i(maxX, y, minZ);
                safePut(wardrobeBase, BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(wardrobeBase.add(0, 1, 0), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(maxX, y, maxZ), BlockIdentifier.of("minecraft:chest[facing=west]"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case DINING_HALL -> {
                int startX = b.sizeX() >= 5 ? minX + 1 : minX;
                int endX = b.sizeX() >= 5 ? maxX - 1 : maxX;
                for (int x = startX; x <= endX; x++) {
                    Vec3i tPos = new Vec3i(x, y, centerZ);
                    safePut(tPos, BlockStateNormalizer.normalize(slab.withProperty("type", "top"), Direction.SOUTH), MaterialRole.SLAB, clearanceZones, map);
                    if (x == centerX) {
                        safePut(tPos.add(0, 1, 0), BlockIdentifier.of("minecraft:candle[lit=true,candles=2]"), MaterialRole.LIGHT_SOURCE, clearanceZones, map);
                    }
                    if (centerZ - 1 >= minZ) {
                        safePut(tPos.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                    }
                    if (centerZ + 1 <= maxZ) {
                        safePut(tPos.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                    }
                }
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case KITCHEN_HEARTH -> {
                Vec3i hearth = new Vec3i(centerX, y, maxZ);
                safePut(hearth, BlockIdentifier.of("minecraft:campfire[lit=true]"), MaterialRole.DECORATION, clearanceZones, map);
                BlockIdentifier masonry = palette.get(MaterialRole.FOUNDATION);
                for (int dx = -1; dx <= 1; dx += 2) {
                    if (centerX + dx >= minX && centerX + dx <= maxX) {
                        safePut(hearth.add(dx, 0, 0), masonry, MaterialRole.FOUNDATION, clearanceZones, map);
                        safePut(hearth.add(dx, 1, 0), masonry, MaterialRole.FOUNDATION, clearanceZones, map);
                    }
                }
                safePut(hearth.add(0, 2, 0), BlockStateNormalizer.normalize(slab.withProperty("type", "bottom"), Direction.SOUTH), MaterialRole.SLAB, clearanceZones, map);

                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:smoker[facing=south]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y, minZ), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 2), y, minZ), BlockIdentifier.of("minecraft:crafting_table"), MaterialRole.DECORATION, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case LIBRARY_STUDY -> {
                for (int x = minX; x <= maxX; x++) {
                    safePut(new Vec3i(x, y, maxZ), BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(new Vec3i(x, y + 1, maxZ), BlockIdentifier.of("minecraft:bookshelf"), MaterialRole.DECORATION, clearanceZones, map);
                }
                Vec3i desk = new Vec3i(centerX, y, centerZ);
                safePut(desk, fence, MaterialRole.DECORATION, clearanceZones, map);
                safePut(desk.add(0, 1, 0), BlockIdentifier.of("minecraft:oak_pressure_plate"), MaterialRole.DECORATION, clearanceZones, map);
                if (centerX + 1 <= maxX) {
                    safePut(desk.add(1, 0, 0), BlockIdentifier.of("minecraft:lectern[facing=north]"), MaterialRole.DECORATION, clearanceZones, map);
                }
                if (centerZ - 1 >= minZ) {
                    safePut(desk.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION, clearanceZones, map);
                }
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case WORKSHOP_SMITHY -> {
                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:anvil[facing=south]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y, minZ), BlockIdentifier.of("minecraft:blast_furnace[facing=south]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 2), y, minZ), BlockIdentifier.of("minecraft:stonecutter[facing=south]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(maxX, y, maxZ), BlockIdentifier.of("minecraft:crafting_table"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.max(minX, maxX - 1), y, maxZ), BlockIdentifier.of("minecraft:smithing_table"), MaterialRole.DECORATION, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case STORAGE_PANTRY -> {
                for (int x = minX; x <= maxX; x += 2) {
                    safePut(new Vec3i(x, y, minZ), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                    safePut(new Vec3i(x, y + 1, minZ), BlockIdentifier.of("minecraft:barrel[facing=up]"), MaterialRole.DECORATION, clearanceZones, map);
                }
                safePut(new Vec3i(maxX, y, maxZ), BlockIdentifier.of("minecraft:chest[facing=north]"), MaterialRole.DECORATION, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case BATHROOM -> {
                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y, minZ), BlockIdentifier.of("minecraft:smooth_quartz_slab[type=top]"), MaterialRole.SLAB, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y + 1, minZ), light, MaterialRole.LIGHT_SOURCE, clearanceZones, map);
                safePut(new Vec3i(maxX, y, maxZ), BlockIdentifier.of("minecraft:white_carpet"), MaterialRole.DECORATION, clearanceZones, map);
            }
            case ARMORY -> {
                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:armor_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y, minZ), BlockIdentifier.of("minecraft:armor_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(maxX, y, maxZ), BlockIdentifier.of("minecraft:grindstone"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.max(minX, maxX - 1), y, maxZ), BlockIdentifier.of("minecraft:chest[facing=north]"), MaterialRole.DECORATION, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
            case ALCHEMICAL_LAB -> {
                safePut(new Vec3i(centerX, y, centerZ), BlockIdentifier.of("minecraft:enchanting_table"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(minX, y, minZ), BlockIdentifier.of("minecraft:brewing_stand"), MaterialRole.DECORATION, clearanceZones, map);
                safePut(new Vec3i(Math.min(maxX, minX + 1), y, minZ), BlockIdentifier.of("minecraft:water_cauldron[level=3]"), MaterialRole.DECORATION, clearanceZones, map);
                addCeilingChandelier(centerX, b.max().y(), b.sizeY(), centerZ, light, map);
            }
        }

        return new ArrayList<>(map.values());
    }

    private static void addCeilingChandelier(int x, int ceilingY, int roomHeight, int z, BlockIdentifier light, Map<Vec3i, PlacementTarget> map) {
        if (roomHeight >= 4) {
            Vec3i top = new Vec3i(x, ceilingY, z);
            map.put(top, new PlacementTarget(top, BlockIdentifier.of("minecraft:chain[axis=y]"), MaterialRole.DECORATION));
            Vec3i lamp = top.add(0, -1, 0);
            map.put(lamp, new PlacementTarget(lamp, BlockStateNormalizer.normalize(light.withProperty("hanging", "true"), Direction.SOUTH), MaterialRole.LIGHT_SOURCE));
        } else {
            Vec3i lamp = new Vec3i(x, ceilingY, z);
            map.put(lamp, new PlacementTarget(lamp, BlockStateNormalizer.normalize(light.withProperty("hanging", "true"), Direction.SOUTH), MaterialRole.LIGHT_SOURCE));
        }
    }

    private static void safePut(
        Vec3i pos,
        BlockIdentifier block,
        MaterialRole role,
        Set<Vec3i> clearanceZones,
        Map<Vec3i, PlacementTarget> map
    ) {
        if (!clearanceZones.contains(pos)) {
            map.put(pos, new PlacementTarget(pos, BlockStateNormalizer.normalize(block, Direction.SOUTH), role));
        }
    }
}
