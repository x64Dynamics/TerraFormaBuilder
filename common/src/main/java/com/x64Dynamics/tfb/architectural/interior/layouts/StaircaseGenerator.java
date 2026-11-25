package com.x64Dynamics.tfb.architectural.interior.layouts;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
public final class StaircaseGenerator {
    public enum StaircaseStyle {
        STRAIGHT,
        SPIRAL,
        DOG_LEG,
        BIFURCATED_IMPERIAL
    }

    private StaircaseGenerator() { }

    public static List<PlacementTarget> generate(
        StaircaseStyle style,
        BoundingBox3i bounds,
        Direction ascendDirection,
        MaterialPalette palette
    ) {
        Direction dir = ascendDirection != null ? ascendDirection : Direction.NORTH;
        int height = Math.max(2, bounds.sizeY());
        int spanX = Math.max(1, bounds.sizeX());
        return switch (style) {
            case STRAIGHT -> generateStraight(bounds.min(), dir, height, spanX, palette);
            case SPIRAL -> generateSpiral(
                new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), (bounds.min().z() + bounds.max().z()) / 2),
                height, palette
            );
            case DOG_LEG -> generateDogLeg(bounds.min(), dir, height, Math.max(1, (spanX - 1) / 2), palette);
            case BIFURCATED_IMPERIAL -> generateBifurcatedImperial(
                new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), bounds.min().z()),
                dir, height, Math.max(2, spanX / 3), palette
            );
        };
    }

    public static List<PlacementTarget> generateStraight(Vec3i bottom, Direction ascendDirection, int height, int width, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);
        Direction stairFacing = ascendDirection;
        Direction cross = ascendDirection.rotateY();

        BlockIdentifier orientedStair = stair.withProperty("facing", stairFacing.name().toLowerCase()).withProperty("half", "bottom");

        for (int step = 0; step < height; step++) {
            Vec3i stepPos = bottom.offset(ascendDirection, step).add(0, step, 0);
            for (int w = 0; w < width; w++) {
                Vec3i targetPos = stepPos.offset(cross, w);
                targets.add(new PlacementTarget(targetPos, orientedStair, MaterialRole.STAIR));
            }
        }
        return targets;
    }

    public static List<PlacementTarget> generateSpiral(Vec3i centerBottom, int height, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier pillar = palette.get(MaterialRole.PILLAR);
        BlockIdentifier stair = palette.get(MaterialRole.STAIR);

        Direction[] cycle = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        for (int y = 0; y < height; y++) {
            // Central core
            targets.add(new PlacementTarget(centerBottom.add(0, y, 0), pillar, MaterialRole.PILLAR));

            // Spiral tread
            Direction dir = cycle[y % 4];
            Direction facing = dir.rotateY();
            BlockIdentifier tread = stair.withProperty("facing", facing.name().toLowerCase()).withProperty("half", "bottom");

            targets.add(new PlacementTarget(centerBottom.add(0, y, 0).offset(dir), tread, MaterialRole.STAIR));
        }
        return targets;
    }

    // Generates a 180-degree dog-leg staircase with an intermediate landing and side balustrades.
    public static List<PlacementTarget> generateDogLeg(Vec3i bottom, Direction ascendDirection, int height, int runWidth, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int w = Math.max(1, runWidth);
        int firstFlight = Math.max(1, height / 2);
        int secondFlight = Math.max(1, height - firstFlight);
        Direction cross = ascendDirection.rotateY();

        // 1. Lower flight
        targets.addAll(generateStraight(bottom, ascendDirection, firstFlight, w, palette));

        // 2. Intermediate landing at top of first flight
        int landingY = bottom.y() + firstFlight - 1;
        BlockIdentifier floor = palette.get(MaterialRole.FLOOR);
        BlockIdentifier rail = palette.get(MaterialRole.BALUSTRADE);
        int totalWidth = w * 2 + 1;

        for (int d = 0; d < 2; d++) {
            for (int cw = 0; cw < totalWidth; cw++) {
                Vec3i lp = new Vec3i(bottom.x(), landingY, bottom.z())
                    .offset(ascendDirection, firstFlight + d)
                    .offset(cross, cw);
                targets.add(new PlacementTarget(lp, floor, MaterialRole.FLOOR));
                if (d == 1) {
                    targets.add(new PlacementTarget(lp.add(0, 1, 0), rail, MaterialRole.BALUSTRADE));
                }
            }
        }

        // 3. Upper flight (180-degree return)
        Vec3i secondStart = new Vec3i(bottom.x(), bottom.y() + firstFlight, bottom.z())
            .offset(ascendDirection, firstFlight - 1)
            .offset(cross, w + 1);
        targets.addAll(generateStraight(secondStart, ascendDirection.opposite(), secondFlight, w, palette));

        return targets;
    }

    // Generates a grand bifurcated imperial staircase: a wide central flight to a mid-landing,
    // splitting symmetrically into left and right upper wings.
    public static List<PlacementTarget> generateBifurcatedImperial(Vec3i centerBottom, Direction ascendDirection, int height, int centralWidth, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int cw = Math.max(2, centralWidth);
        int wingW = Math.max(1, cw / 2);
        int firstFlight = Math.max(1, height / 2);
        int secondFlight = Math.max(1, height - firstFlight);
        Direction cross = ascendDirection.rotateY();

        // 1. Central lower flight
        Vec3i lowerOrigin = centerBottom.offset(cross, -(cw / 2));
        targets.addAll(generateStraight(lowerOrigin, ascendDirection, firstFlight, cw, palette));

        // 2. Mid-level imperial landing
        int landingY = centerBottom.y() + firstFlight - 1;
        BlockIdentifier floor = palette.get(MaterialRole.FLOOR);
        BlockIdentifier rail = palette.get(MaterialRole.BALUSTRADE);
        int halfSpan = cw / 2 + 1;

        for (int d = 0; d < 2; d++) {
            for (int offset = -halfSpan; offset <= halfSpan; offset++) {
                Vec3i lp = new Vec3i(centerBottom.x(), landingY, centerBottom.z())
                    .offset(ascendDirection, firstFlight + d)
                    .offset(cross, offset);
                targets.add(new PlacementTarget(lp, floor, MaterialRole.FLOOR));
                if (d == 1) {
                    targets.add(new PlacementTarget(lp.add(0, 1, 0), rail, MaterialRole.BALUSTRADE));
                }
            }
        }

        // 3. Left & Right symmetrical upper wings
        Vec3i leftStart = new Vec3i(centerBottom.x(), centerBottom.y() + firstFlight, centerBottom.z())
            .offset(ascendDirection, firstFlight)
            .offset(cross, -(halfSpan + 1));
        Vec3i rightStart = new Vec3i(centerBottom.x(), centerBottom.y() + firstFlight, centerBottom.z())
            .offset(ascendDirection, firstFlight)
            .offset(cross, halfSpan + 1);

        targets.addAll(generateStraight(leftStart, cross.opposite(), secondFlight, wingW, palette));
        targets.addAll(generateStraight(rightStart, cross, secondFlight, wingW, palette));

        return targets;
    }
}
