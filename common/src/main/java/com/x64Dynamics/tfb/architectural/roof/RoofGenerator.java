package com.x64Dynamics.tfb.architectural.roof;

import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
public final class RoofGenerator {
    public enum RoofType {
        GABLE,
        HIP,
        MANSARD,
        SHED,
        FLAT,
        CONICAL,
        DOME
    }

    private RoofGenerator() { }

    public static List<PlacementTarget> generateRoof(BoundingBox3i baseBounds, RoofType type, Axis ridgeAxis, int overhang, MaterialPalette palette) {
        BoundingBox3i roofBounds = baseBounds.inflate(overhang);
        return switch (type) {
            case GABLE -> generateGable(roofBounds, ridgeAxis, palette);
            case HIP -> generateHip(roofBounds, palette);
            case MANSARD -> generateMansard(roofBounds, palette);
            case SHED -> generateShed(roofBounds, ridgeAxis, palette);
            case FLAT -> generateFlatWithParapet(roofBounds, palette);
            case CONICAL -> generateConical(roofBounds, palette);
            case DOME -> generateDome(roofBounds, palette);
        };
    }

    private static List<PlacementTarget> generateConical(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier slab = palette.get(MaterialRole.ROOF_RIDGE);
        int radiusX = bounds.sizeX() / 2;
        int radiusZ = bounds.sizeZ() / 2;
        int maxRadius = Math.max(radiusX, radiusZ);
        Vec3i center = new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), (bounds.min().z() + bounds.max().z()) / 2);
        if (maxRadius <= 0) {
            targets.add(new PlacementTarget(center, slab, MaterialRole.ROOF_RIDGE));
            return targets;
        }

        for (int r = maxRadius, y = 0; r >= 0; r--, y++) {
            int curRadX = Math.max(0, (int) Math.round((double) r * radiusX / maxRadius));
            int curRadZ = Math.max(0, (int) Math.round((double) r * radiusZ / maxRadius));
            for (int dz = -curRadZ; dz <= curRadZ; dz++) {
                for (int dx = -curRadX; dx <= curRadX; dx++) {
                    double normDist = (curRadX == 0 ? 0 : (dx * dx) / (double)(curRadX * curRadX)) +
                                      (curRadZ == 0 ? 0 : (dz * dz) / (double)(curRadZ * curRadZ));
                    if (normDist <= 1.0) {
                        double innerLimit = Math.pow(1.0 - (1.0 / Math.max(1, Math.min(curRadX, curRadZ))), 2.0);
                        if (r > 1 && normDist < innerLimit) continue;
                        Vec3i p = new Vec3i(center.x() + dx, center.y() + y, center.z() + dz);
                        targets.add(new PlacementTarget(p, r == 0 ? slab : stair, r == 0 ? MaterialRole.ROOF_RIDGE : MaterialRole.ROOF_SLOPE));
                    }
                }
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateDome(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier roofBlock = palette.get(MaterialRole.ROOF_SLOPE);
        Vec3i center = new Vec3i((bounds.min().x() + bounds.max().x()) / 2, bounds.min().y(), (bounds.min().z() + bounds.max().z()) / 2);
        int radX = bounds.sizeX() / 2;
        int radZ = bounds.sizeZ() / 2;
        int height = Math.min(radX, radZ);

        for (Vec3i p : com.x64Dynamics.tfb.geometry.ShapeRasterizer.sphere(center, radX, height, radZ, true)) {
            if (p.y() >= center.y()) {
                targets.add(new PlacementTarget(p, roofBlock, MaterialRole.ROOF_SLOPE));
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateGable(BoundingBox3i bounds, Axis ridgeAxis, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier ridgeSlab = palette.get(MaterialRole.ROOF_RIDGE);
        BlockIdentifier wallBlock = palette.get(MaterialRole.WALL_PRIMARY);

        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int baseY = bounds.min().y();

        if (ridgeAxis == Axis.X) {
            int span = maxZ - minZ + 1;
            int halfSpan = span / 2;

            for (int step = 0; step <= halfSpan; step++) {
                int y = baseY + step;
                int zLeft = minZ + step;
                int zRight = maxZ - step;

                if (zLeft < zRight) {
                    BlockIdentifier northStair = stair.withProperty("facing", "south").withProperty("half", "bottom");
                    BlockIdentifier southStair = stair.withProperty("facing", "north").withProperty("half", "bottom");

                    for (int x = minX; x <= maxX; x++) {
                        targets.add(new PlacementTarget(new Vec3i(x, y, zLeft), northStair, MaterialRole.ROOF_SLOPE));
                        targets.add(new PlacementTarget(new Vec3i(x, y, zRight), southStair, MaterialRole.ROOF_SLOPE));
                    }
                    // Fill gable pediments on both ends
                    if (maxX - minX >= 2) {
                        for (int z = zLeft + 1; z < zRight; z++) {
                            targets.add(new PlacementTarget(new Vec3i(minX + 1, y, z), wallBlock, MaterialRole.WALL_PRIMARY));
                            targets.add(new PlacementTarget(new Vec3i(maxX - 1, y, z), wallBlock, MaterialRole.WALL_PRIMARY));
                        }
                    }
                    // For even spans, cap the top ridge pair
                    if (zLeft == zRight - 1) {
                        for (int x = minX; x <= maxX; x++) {
                            targets.add(new PlacementTarget(new Vec3i(x, y + 1, zLeft), ridgeSlab, MaterialRole.ROOF_RIDGE));
                            targets.add(new PlacementTarget(new Vec3i(x, y + 1, zRight), ridgeSlab, MaterialRole.ROOF_RIDGE));
                        }
                    }
                } else if (zLeft == zRight) {
                    for (int x = minX; x <= maxX; x++) {
                        targets.add(new PlacementTarget(new Vec3i(x, y, zLeft), ridgeSlab, MaterialRole.ROOF_RIDGE));
                    }
                }
            }
        } else {
            int span = maxX - minX + 1;
            int halfSpan = span / 2;

            for (int step = 0; step <= halfSpan; step++) {
                int y = baseY + step;
                int xLeft = minX + step;
                int xRight = maxX - step;

                if (xLeft < xRight) {
                    BlockIdentifier westStair = stair.withProperty("facing", "east").withProperty("half", "bottom");
                    BlockIdentifier eastStair = stair.withProperty("facing", "west").withProperty("half", "bottom");

                    for (int z = minZ; z <= maxZ; z++) {
                        targets.add(new PlacementTarget(new Vec3i(xLeft, y, z), westStair, MaterialRole.ROOF_SLOPE));
                        targets.add(new PlacementTarget(new Vec3i(xRight, y, z), eastStair, MaterialRole.ROOF_SLOPE));
                    }
                    // Fill gable pediments on both ends
                    if (maxZ - minZ >= 2) {
                        for (int x = xLeft + 1; x < xRight; x++) {
                            targets.add(new PlacementTarget(new Vec3i(x, y, minZ + 1), wallBlock, MaterialRole.WALL_PRIMARY));
                            targets.add(new PlacementTarget(new Vec3i(x, y, maxZ - 1), wallBlock, MaterialRole.WALL_PRIMARY));
                        }
                    }
                    // For even spans, cap the top ridge pair
                    if (xLeft == xRight - 1) {
                        for (int z = minZ; z <= maxZ; z++) {
                            targets.add(new PlacementTarget(new Vec3i(xLeft, y + 1, z), ridgeSlab, MaterialRole.ROOF_RIDGE));
                            targets.add(new PlacementTarget(new Vec3i(xRight, y + 1, z), ridgeSlab, MaterialRole.ROOF_RIDGE));
                        }
                    }
                } else if (xLeft == xRight) {
                    for (int z = minZ; z <= maxZ; z++) {
                        targets.add(new PlacementTarget(new Vec3i(xLeft, y, z), ridgeSlab, MaterialRole.ROOF_RIDGE));
                    }
                }
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateHip(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        BlockIdentifier slab = palette.get(MaterialRole.ROOF_RIDGE);

        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int baseY = bounds.min().y();

        int maxInset = Math.min((maxX - minX) / 2, (maxZ - minZ) / 2);

        for (int inset = 0; inset <= maxInset; inset++) {
            int y = baseY + inset;
            int curMinX = minX + inset, curMaxX = maxX - inset;
            int curMinZ = minZ + inset, curMaxZ = maxZ - inset;

            if (curMinX >= curMaxX || curMinZ >= curMaxZ) {
                for (int x = curMinX; x <= curMaxX; x++) {
                    for (int z = curMinZ; z <= curMaxZ; z++) {
                        targets.add(new PlacementTarget(new Vec3i(x, y, z), slab, MaterialRole.ROOF_RIDGE));
                    }
                }
                break;
            }

            for (int x = curMinX; x <= curMaxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, y, curMinZ), stair.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
                targets.add(new PlacementTarget(new Vec3i(x, y, curMaxZ), stair.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
            }
            for (int z = curMinZ + 1; z < curMaxZ; z++) {
                targets.add(new PlacementTarget(new Vec3i(curMinX, y, z), stair.withProperty("facing", "east").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
                targets.add(new PlacementTarget(new Vec3i(curMaxX, y, z), stair.withProperty("facing", "west").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateMansard(BoundingBox3i bounds, MaterialPalette palette) {
        // Steep slope followed by flat top
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier primary = palette.get(MaterialRole.ROOF_SLOPE);
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int baseY = bounds.min().y();

        int step = 0;
        for (int y = baseY; y < baseY + 2; y++) {
            int curMinX = minX + step, curMaxX = maxX - step;
            int curMinZ = minZ + step, curMaxZ = maxZ - step;
            for (int x = curMinX + 1; x < curMaxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, y, curMinZ), primary.withProperty("facing", "south").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
                targets.add(new PlacementTarget(new Vec3i(x, y, curMaxZ), primary.withProperty("facing", "north").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
            }
            for (int z = curMinZ + 1; z < curMaxZ; z++) {
                targets.add(new PlacementTarget(new Vec3i(curMinX, y, z), primary.withProperty("facing", "east").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
                targets.add(new PlacementTarget(new Vec3i(curMaxX, y, z), primary.withProperty("facing", "west").withProperty("half", "bottom"), MaterialRole.ROOF_SLOPE));
            }
            // Solid corner blocks for clean mansard hips without stair gaps
            BlockIdentifier cornerMat = palette.get(MaterialRole.ROOF_OVERHANG);
            targets.add(new PlacementTarget(new Vec3i(curMinX, y, curMinZ), cornerMat, MaterialRole.ROOF_OVERHANG));
            targets.add(new PlacementTarget(new Vec3i(curMaxX, y, curMinZ), cornerMat, MaterialRole.ROOF_OVERHANG));
            targets.add(new PlacementTarget(new Vec3i(curMinX, y, curMaxZ), cornerMat, MaterialRole.ROOF_OVERHANG));
            targets.add(new PlacementTarget(new Vec3i(curMaxX, y, curMaxZ), cornerMat, MaterialRole.ROOF_OVERHANG));
            step++;
        }

        // Flat upper cap
        int topY = baseY + 2;
        int capMinX = minX + step, capMaxX = maxX - step;
        int capMinZ = minZ + step, capMaxZ = maxZ - step;
        BlockIdentifier slab = palette.get(MaterialRole.ROOF_RIDGE);
        for (int x = capMinX; x <= capMaxX; x++) {
            for (int z = capMinZ; z <= capMaxZ; z++) {
                targets.add(new PlacementTarget(new Vec3i(x, topY, z), slab, MaterialRole.ROOF_RIDGE));
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateShed(BoundingBox3i bounds, Axis slopeAxis, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        BlockIdentifier stair = palette.get(MaterialRole.ROOF_SLOPE);
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int baseY = bounds.min().y();

        if (slopeAxis == Axis.X) {
            for (int x = minX; x <= maxX; x++) {
                int y = baseY + (x - minX);
                BlockIdentifier directedStair = stair.withProperty("facing", "west").withProperty("half", "bottom");
                for (int z = minZ; z <= maxZ; z++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), directedStair, MaterialRole.ROOF_SLOPE));
                }
            }
        } else {
            for (int z = minZ; z <= maxZ; z++) {
                int y = baseY + (z - minZ);
                BlockIdentifier directedStair = stair.withProperty("facing", "north").withProperty("half", "bottom");
                for (int x = minX; x <= maxX; x++) {
                    targets.add(new PlacementTarget(new Vec3i(x, y, z), directedStair, MaterialRole.ROOF_SLOPE));
                }
            }
        }
        return targets;
    }

    private static List<PlacementTarget> generateFlatWithParapet(BoundingBox3i bounds, MaterialPalette palette) {
        List<PlacementTarget> targets = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int y = bounds.min().y();

        BlockIdentifier floor = palette.get(MaterialRole.FLOOR);
        BlockIdentifier parapet = palette.get(MaterialRole.FENCE);

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                targets.add(new PlacementTarget(new Vec3i(x, y, z), floor, MaterialRole.FLOOR));
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    targets.add(new PlacementTarget(new Vec3i(x, y + 1, z), parapet, MaterialRole.FENCE));
                }
            }
        }
        return targets;
    }
}
