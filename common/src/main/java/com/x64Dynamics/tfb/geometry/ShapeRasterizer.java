package com.x64Dynamics.tfb.geometry;

import java.util.*;

public final class ShapeRasterizer {
    private ShapeRasterizer() { }

    public static List<Vec3i> box(BoundingBox3i bounds, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minY = bounds.min().y(), maxY = bounds.max().y();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (hollow) {
                        boolean onEdge = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                        if (!onEdge) continue;
                    }
                    positions.add(new Vec3i(x, y, z));
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> cylinder(Vec3i centerBottom, int radiusX, int radiusZ, int height, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        if (radiusX <= 0 || radiusZ <= 0 || height <= 0) return positions;

        double invRadXSq = 1.0 / ((double) radiusX * radiusX);
        double invRadZSq = 1.0 / ((double) radiusZ * radiusZ);
        double innerFactor = hollow ? 1.0 - (1.0 / Math.min(radiusX, radiusZ)) : -1.0;
        double innerLimit = innerFactor * innerFactor;

        for (int y = 0; y < height; y++) {
            int currentY = centerBottom.y() + y;
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                for (int dx = -radiusX; dx <= radiusX; dx++) {
                    double normDist = (dx * dx) * invRadXSq + (dz * dz) * invRadZSq;
                    if (normDist <= 1.0) {
                        if (hollow && y > 0 && y < height - 1 && normDist < innerLimit) {
                            continue;
                        }
                        positions.add(new Vec3i(centerBottom.x() + dx, currentY, centerBottom.z() + dz));
                    }
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> sphere(Vec3i center, int radiusX, int radiusY, int radiusZ, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        if (radiusX <= 0 || radiusY <= 0 || radiusZ <= 0) return positions;

        double invX = 1.0 / ((double) radiusX * radiusX);
        double invY = 1.0 / ((double) radiusY * radiusY);
        double invZ = 1.0 / ((double) radiusZ * radiusZ);
        double minRadius = Math.min(Math.min(radiusX, radiusY), radiusZ);
        double innerLimit = hollow ? Math.pow(1.0 - (1.0 / minRadius), 2.0) : -1.0;

        for (int dy = -radiusY; dy <= radiusY; dy++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                for (int dx = -radiusX; dx <= radiusX; dx++) {
                    double dist = (dx * dx) * invX + (dy * dy) * invY + (dz * dz) * invZ;
                    if (dist <= 1.0) {
                        if (hollow && dist < innerLimit) continue;
                        positions.add(new Vec3i(center.x() + dx, center.y() + dy, center.z() + dz));
                    }
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> bresenhamLine(Vec3i start, Vec3i end) {
        List<Vec3i> points = new ArrayList<>();
        int x0 = start.x(), y0 = start.y(), z0 = start.z();
        int x1 = end.x(), y1 = end.y(), z1 = end.z();

        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int dz = Math.abs(z1 - z0), sz = z0 < z1 ? 1 : -1;

        int dm = Math.max(dx, Math.max(dy, dz));
        int i = dm;
        int x = x0, y = y0, z = z0;
        int ex = dm / 2, ey = dm / 2, ez = dm / 2;

        while (i-- >= 0) {
            points.add(new Vec3i(x, y, z));
            ex -= dx; if (ex < 0) { ex += dm; x += sx; }
            ey -= dy; if (ey < 0) { ey += dm; y += sy; }
            ez -= dz; if (ez < 0) { ez += dm; z += sz; }
        }
        return points;
    }

    public static List<Vec3i> regularPolygon(Vec3i centerBottom, int radius, int sides, int height, double rotationRadians, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        if (radius <= 0 || sides < 3 || height <= 0) return positions;

        // Calculate 2D vertices on XZ plane
        double[] vx = new double[sides];
        double[] vz = new double[sides];
        for (int i = 0; i < sides; i++) {
            double angle = rotationRadians + (2.0 * Math.PI * i) / sides;
            vx[i] = centerBottom.x() + radius * Math.cos(angle);
            vz[i] = centerBottom.z() + radius * Math.sin(angle);
        }

        int minX = (int) Math.floor(centerBottom.x() - radius - 1);
        int maxX = (int) Math.ceil(centerBottom.x() + radius + 1);
        int minZ = (int) Math.floor(centerBottom.z() - radius - 1);
        int maxZ = (int) Math.ceil(centerBottom.z() + radius + 1);

        for (int y = 0; y < height; y++) {
            int currentY = centerBottom.y() + y;
            boolean isCap = (y == 0 || y == height - 1);

            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (isPointInPolygon(x + 0.5, z + 0.5, vx, vz, sides)) {
                        if (hollow && !isCap) {
                            // Check if near perimeter (distance to any polygon edge < 1.0)
                            double minDist = minDistanceToPolygonEdge(x + 0.5, z + 0.5, vx, vz, sides);
                            if (minDist > 1.0) {
                                continue;
                            }
                        }
                        positions.add(new Vec3i(x, currentY, z));
                    }
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> roundedBox(BoundingBox3i bounds, int cornerRadius, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minY = bounds.min().y(), maxY = bounds.max().y();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int r = Math.max(1, Math.min(cornerRadius, Math.min(bounds.sizeX(), bounds.sizeZ()) / 2 - 1));

        for (int y = minY; y <= maxY; y++) {
            boolean isCap = (y == minY || y == maxY);
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    // Check 4 rounded corners
                    boolean inCorner = false;
                    double distSq = 0;
                    if (x < minX + r && z < minZ + r) { // MinX, MinZ corner
                        distSq = Math.pow(x - (minX + r), 2) + Math.pow(z - (minZ + r), 2);
                        inCorner = true;
                    } else if (x > maxX - r && z < minZ + r) { // MaxX, MinZ corner
                        distSq = Math.pow(x - (maxX - r), 2) + Math.pow(z - (minZ + r), 2);
                        inCorner = true;
                    } else if (x < minX + r && z > maxZ - r) { // MinX, MaxZ corner
                        distSq = Math.pow(x - (minX + r), 2) + Math.pow(z - (maxZ - r), 2);
                        inCorner = true;
                    } else if (x > maxX - r && z > maxZ - r) { // MaxX, MaxZ corner
                        distSq = Math.pow(x - (maxX - r), 2) + Math.pow(z - (maxZ - r), 2);
                        inCorner = true;
                    }

                    if (inCorner && distSq > r * r) {
                        continue; // Outside rounded corner
                    }

                    if (hollow && !isCap) {
                        boolean onStraightEdge = (x == minX || x == maxX || z == minZ || z == maxZ);
                        boolean onCornerPerimeter = inCorner && distSq >= Math.pow(r - 1.0, 2);
                        if (!onStraightEdge && !onCornerPerimeter) {
                            continue;
                        }
                    }
                    positions.add(new Vec3i(x, y, z));
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> rotatedBox(Vec3i origin, int sizeX, int sizeY, int sizeZ, double yawDegrees, boolean hollow) {
        List<Vec3i> positions = new ArrayList<>();
        double rad = Math.toRadians(-yawDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        // Bounding box in world coordinates
        double halfX = sizeX / 2.0;
        double halfZ = sizeZ / 2.0;
        int maxReach = (int) Math.ceil(Math.hypot(sizeX, sizeZ)) + 2;

        for (int y = 0; y < sizeY; y++) {
            boolean isCap = (y == 0 || y == sizeY - 1);
            int currentY = origin.y() + y;

            for (int dz = -maxReach; dz <= maxReach; dz++) {
                for (int dx = -maxReach; dx <= maxReach; dx++) {
                    // Rotate world offset into box local coordinates
                    double localX = dx * cos - dz * sin + halfX;
                    double localZ = dx * sin + dz * cos + halfZ;

                    if (localX >= 0 && localX < sizeX && localZ >= 0 && localZ < sizeZ) {
                        if (hollow && !isCap) {
                            boolean onLocalEdge = (localX < 1.0 || localX >= sizeX - 1.0 || localZ < 1.0 || localZ >= sizeZ - 1.0);
                            if (!onLocalEdge) continue;
                        }
                        positions.add(new Vec3i(origin.x() + dx, currentY, origin.z() + dz));
                    }
                }
            }
        }
        return positions;
    }

    public static List<Vec3i> cubicBezierPath(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, int samples, int width) {
        List<Vec3i> result = new ArrayList<>();
        Set<Vec3i> deduplicated = new HashSet<>();
        int halfW = Math.max(1, width / 2);

        for (int s = 0; s <= samples; s++) {
            double t = (double) s / (double) samples;
            Vec3d pt = interpolateCubicBezier(p0, p1, p2, p3, t);
            Vec3i center = pt.toFloorInt();

            for (int dx = -halfW; dx <= halfW; dx++) {
                for (int dz = -halfW; dz <= halfW; dz++) {
                    if (dx * dx + dz * dz <= halfW * halfW) {
                        Vec3i surface = center.add(dx, 0, dz);
                        if (deduplicated.add(surface)) {
                            result.add(surface);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static Vec3d interpolateCubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double u = 1.0 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * p0.x() + 3 * uu * t * p1.x() + 3 * u * tt * p2.x() + ttt * p3.x();
        double y = uuu * p0.y() + 3 * uu * t * p1.y() + 3 * u * tt * p2.y() + ttt * p3.y();
        double z = uuu * p0.z() + 3 * uu * t * p1.z() + 3 * u * tt * p2.z() + ttt * p3.z();
        return new Vec3d(x, y, z);
    }

    private static boolean isPointInPolygon(double x, double z, double[] vx, double[] vz, int n) {
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (((vz[i] > z) != (vz[j] > z)) &&
                (x < (vx[j] - vx[i]) * (z - vz[i]) / (vz[j] - vz[i]) + vx[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double minDistanceToPolygonEdge(double x, double z, double[] vx, double[] vz, int n) {
        double minDistSq = Double.MAX_VALUE;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double distSq = distToSegmentSquared(x, z, vx[i], vz[i], vx[j], vz[j]);
            if (distSq < minDistSq) {
                minDistSq = distSq;
            }
        }
        return Math.sqrt(minDistSq);
    }

    private static double distToSegmentSquared(double px, double pz, double x1, double z1, double x2, double z2) {
        double l2 = (x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1);
        if (l2 == 0) return (px - x1) * (px - x1) + (pz - z1) * (pz - z1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (pz - z1) * (z2 - z1)) / l2));
        double projX = x1 + t * (x2 - x1);
        double projZ = z1 + t * (z2 - z1);
        return (px - projX) * (px - projX) + (pz - projZ) * (pz - projZ);
    }

    public static List<Vec3i> arch(Vec3i leftBase, Vec3i rightBase, int archHeight, int depth, Direction facing) {
        List<Vec3i> blocks = new ArrayList<>();
        int span = (int) Math.round(leftBase.distance(rightBase));
        if (span <= 0 || archHeight <= 0) return blocks;

        // Okey, this works, but idk how. Maybe magic?
        Vec3d left = leftBase.toDouble();
        Vec3d right = rightBase.toDouble();

        for (int step = 0; step <= span; step++) {
            double t = (double) step / (double) span;
            double normX = 2.0 * t - 1.0;
            double profile = Math.sin(t * Math.PI);
            int yOffset = (int) Math.round(profile * archHeight);

            Vec3d basePos = left.lerp(right, t);
            Vec3i archPt = new Vec3i((int) Math.round(basePos.x()), (int) Math.round(basePos.y()) + yOffset, (int) Math.round(basePos.z()));

            for (int d = 0; d < depth; d++) {
                blocks.add(archPt.offset(facing, d));
            }
        }
        return blocks;
    }

    public static List<Vec3i> catmullRomPath(List<Vec3i> controlPoints, int samplesPerSegment, int width) {
        if (controlPoints.size() < 2) return new ArrayList<>(controlPoints);
        List<Vec3i> result = new ArrayList<>();
        Set<Vec3i> deduplicated = new HashSet<>();

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vec3d p0 = (i == 0 ? controlPoints.get(0).toDouble() : controlPoints.get(i - 1).toDouble());
            Vec3d p1 = controlPoints.get(i).toDouble();
            Vec3d p2 = controlPoints.get(i + 1).toDouble();
            Vec3d p3 = (i + 2 < controlPoints.size() ? controlPoints.get(i + 2).toDouble() : p2);

            for (int s = 0; s <= samplesPerSegment; s++) {
                double t = (double) s / (double) samplesPerSegment;
                Vec3d splinePt = interpolateCatmullRom(p0, p1, p2, p3, t);
                Vec3i center = splinePt.toFloorInt();

                int halfW = width / 2;
                for (int dx = -halfW; dx <= halfW; dx++) {
                    for (int dz = -halfW; dz <= halfW; dz++) {
                        if (dx * dx + dz * dz <= halfW * halfW) {
                            Vec3i surface = center.add(dx, 0, dz);
                            if (deduplicated.add(surface)) {
                                result.add(surface);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static Vec3d interpolateCatmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        // Standard Catmull-Rom spline formulation
        double x = 0.5 * ((2.0 * p1.x()) + (-p0.x() + p2.x()) * t + (2.0 * p0.x() - 5.0 * p1.x() + 4.0 * p2.x() - p3.x()) * t2 + (-p0.x() + 3.0 * p1.x() - 3.0 * p2.x() + p3.x()) * t3);
        double y = 0.5 * ((2.0 * p1.y()) + (-p0.y() + p2.y()) * t + (2.0 * p0.y() - 5.0 * p1.y() + 4.0 * p2.y() - p3.y()) * t2 + (-p0.y() + 3.0 * p1.y() - 3.0 * p2.y() + p3.y()) * t3);
        double z = 0.5 * ((2.0 * p1.z()) + (-p0.z() + p2.z()) * t + (2.0 * p0.z() - 5.0 * p1.z() + 4.0 * p2.z() - p3.z()) * t2 + (-p0.z() + 3.0 * p1.z() - 3.0 * p2.z() + p3.z()) * t3);
        return new Vec3d(x, y, z);
    }
}
