package com.x64Dynamics.tfb.settlement;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class ProceduralSettlementGenerator {
    private ProceduralSettlementGenerator() { }

    public static SettlementPlan generateSettlement(Vec3i center, int radius, int buildingCount, MaterialPalette palette) {
        BoundingBox3i bounds = BoundingBox3i.fromCorners(
            center.add(-radius, -2, -radius),
            center.add(radius, 15, radius)
        );

        List<SettlementBuildingPlot> plots = new ArrayList<>();
        List<Vec3i> roadPath = new ArrayList<>();
        List<PlacementTarget> decorations = new ArrayList<>();

        int y = center.y();

        // 1. Central Town Square / Well
        Vec3i wellCenter = center;
        BlockIdentifier stone = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier water = BlockIdentifier.of("minecraft:water");
        BlockIdentifier wall = palette.get(MaterialRole.WALL_PRIMARY);

        // Square plaza paving
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                roadPath.add(new Vec3i(wellCenter.x() + dx, y, wellCenter.z() + dz));
            }
        }

        // Central well basin
        decorations.add(new PlacementTarget(wellCenter, water, MaterialRole.DECORATION));
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            decorations.add(new PlacementTarget(wellCenter.offset(d), stone, MaterialRole.FOUNDATION));
            decorations.add(new PlacementTarget(wellCenter.offset(d).add(0, 1, 0), wall, MaterialRole.FENCE));
        }

        // 2. Cardinal branching roads
        int branchLen = radius - 6;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            for (int dist = 4; dist <= branchLen; dist++) {
                Vec3i pt = center.offset(d, dist);
                roadPath.add(pt);
                roadPath.add(pt.offset(d.rotateY())); // 2-block wide road

                // Streetlamps every 6 blocks
                if (dist % 6 == 0) {
                    Vec3i lampPos = pt.offset(d.rotateY(), 2);
                    decorations.add(new PlacementTarget(lampPos, palette.get(MaterialRole.FENCE), MaterialRole.FENCE));
                    decorations.add(new PlacementTarget(lampPos.add(0, 1, 0), palette.get(MaterialRole.FENCE), MaterialRole.FENCE));
                    decorations.add(new PlacementTarget(lampPos.add(0, 2, 0), palette.get(MaterialRole.LIGHT_SOURCE), MaterialRole.LIGHT_SOURCE));
                }
            }
        }

        // 3. Building Plots alongside roads
        String[] types = {"house", "house", "barn", "stable", "tower"};
        int plotWidth = 9, plotDepth = 9, plotHeight = 7;

        int index = 0;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (plots.size() >= buildingCount) break;

            Vec3i leftPlotOrigin = center.offset(d, 8).offset(d.rotateY(), 4);
            BoundingBox3i plot1 = BoundingBox3i.fromOriginAndSize(leftPlotOrigin, plotWidth, plotHeight, plotDepth);
            plots.add(new SettlementBuildingPlot(plot1, types[index % types.length], d.rotateY().opposite()));
            index++;

            if (plots.size() >= buildingCount) break;

            Vec3i rightPlotOrigin = center.offset(d, 8).offset(d.rotateY().opposite(), 4 + plotWidth);
            BoundingBox3i plot2 = BoundingBox3i.fromOriginAndSize(rightPlotOrigin, plotWidth, plotHeight, plotDepth);
            plots.add(new SettlementBuildingPlot(plot2, types[index % types.length], d.rotateY()));
            index++;
        }

        return new SettlementPlan(bounds, plots, roadPath, decorations);
    }
}
