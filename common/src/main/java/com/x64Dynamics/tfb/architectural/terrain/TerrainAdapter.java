package com.x64Dynamics.tfb.architectural.terrain;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class TerrainAdapter {
    private TerrainAdapter() { }

    public record TerrainAdaptationResult(
        List<PlacementTarget> foundationPlacements,
        List<Vec3i> excavations,
        List<PlacementTarget> fillPlacements
    ) { }

    public static TerrainAdaptationResult adapt(BoundingBox3i structureBounds, HeightmapGrid heightmap, FoundationStrategy strategy, MaterialPalette palette) {
        List<PlacementTarget> foundations = new ArrayList<>();
        List<Vec3i> excavations = new ArrayList<>();
        List<PlacementTarget> fills = new ArrayList<>();
        if (heightmap == null || structureBounds == null || palette == null) {
            return new TerrainAdaptationResult(foundations, excavations, fills);
        }

        int floorY = structureBounds.min().y();
        int minX = structureBounds.min().x(), maxX = structureBounds.max().x();
        int minZ = structureBounds.min().z(), maxZ = structureBounds.max().z();

        BlockIdentifier foundationBlock = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier subFoundationBlock = palette.get(MaterialRole.SUB_FOUNDATION);
        BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);

        switch (strategy) {
            case PILLARS_STILTS -> {
                // Place pillars at corners and perimeter intervals down to terrain elevation
                int spacing = 4;
                for (int x = minX; x <= maxX; x += Math.min(spacing, maxX - x == 0 ? 1 : maxX - x)) {
                    for (int z = minZ; z <= maxZ; z += Math.min(spacing, maxZ - z == 0 ? 1 : maxZ - z)) {
                        boolean onBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                        if (!onBorder) continue;

                        int groundY = heightmap.getElevation(x, z);
                        if (groundY < floorY) {
                            for (int y = groundY; y < floorY; y++) {
                                foundations.add(new PlacementTarget(new Vec3i(x, y, z), pillarBlock, MaterialRole.PILLAR));
                            }
                        }
                    }
                }
            }
            case SOLID_SUB_BASEMENT -> {
                // Extends continuous perimeter foundation wall downward to lowest terrain surface
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean onBorder = (x == minX || x == maxX || z == minZ || z == maxZ);
                        if (!onBorder) continue;

                        int groundY = heightmap.getElevation(x, z);
                        if (groundY < floorY) {
                            for (int y = groundY; y < floorY; y++) {
                                BlockIdentifier block = (y == floorY - 1) ? foundationBlock : subFoundationBlock;
                                foundations.add(new PlacementTarget(new Vec3i(x, y, z), block, MaterialRole.FOUNDATION));
                            }
                        }
                    }
                }
            }
            case CUT_AND_FILL -> {
                // Excavate everything inside bounds above floorY, fill voids below floorY
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int groundY = heightmap.getElevation(x, z);
                        if (groundY >= floorY) {
                            for (int y = floorY; y <= groundY; y++) {
                                excavations.add(new Vec3i(x, y, z));
                            }
                        } else {
                            for (int y = groundY; y < floorY; y++) {
                                fills.add(new PlacementTarget(new Vec3i(x, y, z), foundationBlock, MaterialRole.FOUNDATION));
                            }
                        }
                    }
                }
            }
            case CLIFF_CANTILEVER -> {
                // Generate diagonal support braces extending underneath floor
                for (int x = minX; x <= maxX; x += 3) {
                    for (int z = minZ; z <= maxZ; z += 3) {
                        int groundY = heightmap.getElevation(x, z);
                        int drop = floorY - groundY;
                        if (drop > 1 && drop <= 6) {
                            // Diagonal brace
                            for (int step = 1; step < drop; step++) {
                                foundations.add(new PlacementTarget(new Vec3i(x, floorY - step, z + step), pillarBlock, MaterialRole.BEAM));
                            }
                        }
                    }
                }
            }
            case MOUNTAIN_CAVE_EMBED -> {
                // Excavate full interior cave pocket into mountain
                for (int x = minX + 1; x < maxX; x++) {
                    for (int z = minZ + 1; z < maxZ; z++) {
                        for (int y = floorY; y <= structureBounds.max().y(); y++) {
                            excavations.add(new Vec3i(x, y, z));
                        }
                    }
                }
                // Frame the outer perimeter portal
                for (int x = minX; x <= maxX; x++) {
                    for (int y = floorY; y <= structureBounds.max().y(); y++) {
                        foundations.add(new PlacementTarget(new Vec3i(x, y, minZ), foundationBlock, MaterialRole.WALL_PRIMARY));
                    }
                }
            }
            case STEPPED_TERRACE -> {
                // Foundation terracing: fills steps to meet floor level
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int groundY = heightmap.getElevation(x, z);
                        if (groundY < floorY) {
                            int stepBase = Math.max(groundY, floorY - 2);
                            for (int y = stepBase; y < floorY; y++) {
                                foundations.add(new PlacementTarget(new Vec3i(x, y, z), foundationBlock, MaterialRole.FOUNDATION));
                            }
                        }
                    }
                }
            }
        }

        return new TerrainAdaptationResult(foundations, excavations, fills);
    }
}
