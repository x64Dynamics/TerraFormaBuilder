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
        List<PlacementTarget> targets = new ArrayList<>();
        BoundingBox3i b = room.bounds();
        int y = b.min().y();

        int centerX = (b.min().x() + b.max().x()) / 2;
        int centerZ = (b.min().z() + b.max().z()) / 2;

        switch (room.type()) {
            case LIVING_HALL -> {
                // Dining / meeting table at center
                BlockIdentifier fence = palette.get(MaterialRole.FENCE);
                BlockIdentifier slab = palette.get(MaterialRole.SLAB);
                BlockIdentifier stair = palette.get(MaterialRole.STAIR);

                Vec3i table1 = new Vec3i(centerX, y, centerZ);
                Vec3i table2 = new Vec3i(centerX + 1, y, centerZ);
                targets.add(new PlacementTarget(table1, fence, MaterialRole.DECORATION));
                targets.add(new PlacementTarget(table2, fence, MaterialRole.DECORATION));
                targets.add(new PlacementTarget(table1.add(0, 1, 0), slab, MaterialRole.DECORATION));
                targets.add(new PlacementTarget(table2.add(0, 1, 0), slab, MaterialRole.DECORATION));

                // Chairs on sides
                targets.add(new PlacementTarget(table1.add(0, 0, -1), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(table2.add(0, 0, 1), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.DECORATION));

                // Chandelier hanging from ceiling
                Vec3i ceilingCenter = new Vec3i(centerX, b.max().y(), centerZ);
                targets.add(new PlacementTarget(ceilingCenter, BlockIdentifier.of("minecraft:chain"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(ceilingCenter.add(0, -1, 0), palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE));
            }
            case BEDROOM -> {
                // Bed in the corner away from doorway
                Vec3i corner = new Vec3i(b.min().x() + 1, y, b.min().z() + 1);
                targets.add(new PlacementTarget(corner, BlockIdentifier.of("minecraft:red_bed[facing=south,part=head]"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(corner.add(0, 0, 1), BlockIdentifier.of("minecraft:red_bed[facing=south,part=foot]"), MaterialRole.DECORATION));

                // Bedside nightstand with lantern
                Vec3i nightstand = corner.add(1, 0, 0);
                targets.add(new PlacementTarget(nightstand, BlockIdentifier.of("minecraft:oak_slab[half=top]"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(nightstand.add(0, 1, 0), palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE));
            }
            case LIBRARY_STUDY -> {
                // Bookshelves lining back wall
                BlockIdentifier bookshelf = BlockIdentifier.of("minecraft:bookshelf");
                int zBack = b.max().z() - 1;
                for (int x = b.min().x() + 1; x <= b.max().x() - 1; x++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, zBack), bookshelf, MaterialRole.DECORATION));
                    targets.add(new PlacementTarget(new Vec3i(x, y + 1, zBack), bookshelf, MaterialRole.DECORATION));
                }

                // Reading desk & chair
                Vec3i desk = new Vec3i(centerX, y, centerZ);
                targets.add(new PlacementTarget(desk, palette.get(MaterialRole.FENCE), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(desk.add(0, 1, 0), BlockIdentifier.of("minecraft:oak_pressure_plate"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(desk.add(1, 1, 0), palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE));
            }
            case KITCHEN_HEARTH -> {
                // Fireplace hearth
                Vec3i hearthCenter = new Vec3i(centerX, y, b.max().z() - 1);
                targets.add(new PlacementTarget(hearthCenter, BlockIdentifier.of("minecraft:campfire"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(hearthCenter.add(-1, 0, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY));
                targets.add(new PlacementTarget(hearthCenter.add(1, 0, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY));
                targets.add(new PlacementTarget(hearthCenter.add(-1, 1, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY));
                targets.add(new PlacementTarget(hearthCenter.add(1, 1, 0), BlockIdentifier.of("minecraft:bricks"), MaterialRole.WALL_PRIMARY));
                targets.add(new PlacementTarget(hearthCenter.add(0, 2, 0), BlockIdentifier.of("minecraft:brick_slab"), MaterialRole.SLAB));

                // Smoker & cauldron basin
                targets.add(new PlacementTarget(new Vec3i(b.min().x() + 1, y, b.min().z() + 1), BlockIdentifier.of("minecraft:smoker"), MaterialRole.DECORATION));
                targets.add(new PlacementTarget(new Vec3i(b.min().x() + 2, y, b.min().z() + 1), BlockIdentifier.of("minecraft:cauldron"), MaterialRole.DECORATION));
            }
            case STORAGE_PANTRY -> {
                // Stacked storage barrels & chests along walls
                BlockIdentifier barrel = BlockIdentifier.of("minecraft:barrel");
                for (int x = b.min().x() + 1; x <= b.max().x() - 1; x += 2) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, b.min().z() + 1), barrel, MaterialRole.DECORATION));
                    targets.add(new PlacementTarget(new Vec3i(x, y + 1, b.min().z() + 1), barrel, MaterialRole.DECORATION));
                }
            }
        }
        return targets;
    }
}
