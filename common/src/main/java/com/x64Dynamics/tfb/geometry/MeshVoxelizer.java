package com.x64Dynamics.tfb.geometry;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Triangle surface & ray-parity solid mesh voxelizer (combining vengi and ObjToSchematic techniques),
 * scaling meshes into target voxel heights and assigning Minecraft blocks via {@link BlockColorMatcher}.
 */
public final class MeshVoxelizer {
    private MeshVoxelizer() {}

    public static VoxelVolume voxelize(
            ObjMeshImporter.MeshData mesh,
            int targetHeight,
            boolean solidFill,
            float ditherMagnitude,
            Vec3i origin) {
        VoxelVolume volume = new VoxelVolume();
        if (mesh == null || mesh.triangles().isEmpty()) {
            return volume;
        }

        double spanX = Math.max(1e-5, mesh.maxBounds().x() - mesh.minBounds().x());
        double spanY = Math.max(1e-5, mesh.maxBounds().y() - mesh.minBounds().y());
        double spanZ = Math.max(1e-5, mesh.maxBounds().z() - mesh.minBounds().z());
        int clampedHeight = Math.max(2, Math.min(128, targetHeight));
        double scale = clampedHeight / spanY;

        int maxVoxX = Math.max(1, (int) Math.ceil(spanX * scale));
        int maxVoxY = clampedHeight;
        int maxVoxZ = Math.max(1, (int) Math.ceil(spanZ * scale));

        Vec3d minB = mesh.minBounds();
        Vec3i base = origin != null ? origin : Vec3i.ZERO;

        // 1. Surface triangle rasterization (barycentric sub-sampling inside triangle AABB)
        for (ObjMeshImporter.Triangle tri : mesh.triangles()) {
            Vec3d a = toGrid(tri.a(), minB, scale);
            Vec3d b = toGrid(tri.b(), minB, scale);
            Vec3d c = toGrid(tri.c(), minB, scale);

            double edgeLen = Math.max(dist(a, b), Math.max(dist(b, c), dist(c, a)));
            int steps = Math.max(2, Math.min(256, (int) Math.ceil(edgeLen * 2.0)));

            for (int i = 0; i <= steps; i++) {
                double u = (double) i / steps;
                for (int j = 0; j <= steps - i; j++) {
                    double v = (double) j / steps;
                    double w = 1.0 - u - v;
                    int vx = (int) Math.round(w * a.x() + u * b.x() + v * c.x());
                    int vy = (int) Math.round(w * a.y() + u * b.y() + v * c.y());
                    int vz = (int) Math.round(w * a.z() + u * b.z() + v * c.z());
                    Vec3i wp = base.add(vx, vy, vz);
                    BlockIdentifier blk = BlockColorMatcher.matchBlock(
                        tri.material().r(), tri.material().g(), tri.material().b(),
                        wp, ditherMagnitude, tri.material().translucent(), tri.material().emissive()
                    );
                    volume.set(wp, blk);
                }
            }
        }

        // 2. Optional solid interior fill via Moller-Trumbore Y-ray parity test
        if (solidFill && maxVoxX * maxVoxZ <= 65536) {
            List<Vec3d[]> scaledTris = new ArrayList<>(mesh.triangles().size());
            List<ObjMeshImporter.MaterialColor> triMats = new ArrayList<>(mesh.triangles().size());
            for (ObjMeshImporter.Triangle tri : mesh.triangles()) {
                scaledTris.add(new Vec3d[]{
                    toGrid(tri.a(), minB, scale),
                    toGrid(tri.b(), minB, scale),
                    toGrid(tri.c(), minB, scale)
                });
                triMats.add(tri.material());
            }

            for (int x = 0; x <= maxVoxX; x++) {
                for (int z = 0; z <= maxVoxZ; z++) {
                    List<Double> hitYs = new ArrayList<>();
                    ObjMeshImporter.MaterialColor lastMat = ObjMeshImporter.MaterialColor.DEFAULT;
                    for (int t = 0; t < scaledTris.size(); t++) {
                        Vec3d[] tri = scaledTris.get(t);
                        Double hitY = intersectVerticalRay(x + 0.5, z + 0.5, tri[0], tri[1], tri[2]);
                        if (hitY != null) {
                            hitYs.add(hitY);
                            lastMat = triMats.get(t);
                        }
                    }
                    if (hitYs.size() >= 2) {
                        hitYs.sort(Double::compareTo);
                        for (int k = 0; k + 1 < hitYs.size(); k += 2) {
                            int yStart = Math.max(0, (int) Math.ceil(hitYs.get(k)));
                            int yEnd = Math.min(maxVoxY, (int) Math.floor(hitYs.get(k + 1)));
                            for (int y = yStart; y <= yEnd; y++) {
                                Vec3i wp = base.add(x, y, z);
                                if (volume.get(wp).isAir()) {
                                    volume.set(wp, BlockColorMatcher.matchBlock(
                                        lastMat.r(), lastMat.g(), lastMat.b(),
                                        wp, ditherMagnitude, false, false
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }

        return volume;
    }

    private static Vec3d toGrid(Vec3d p, Vec3d minB, double scale) {
        return new Vec3d((p.x() - minB.x()) * scale, (p.y() - minB.y()) * scale, (p.z() - minB.z()) * scale);
    }

    private static double dist(Vec3d a, Vec3d b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Double intersectVerticalRay(double rx, double rz, Vec3d a, Vec3d b, Vec3d c) {
        double det = (b.z() - c.z()) * (a.x() - c.x()) + (c.x() - b.x()) * (a.z() - c.z());
        if (Math.abs(det) < 1e-8) return null;
        double l1 = ((b.z() - c.z()) * (rx - c.x()) + (c.x() - b.x()) * (rz - c.z())) / det;
        double l2 = ((c.z() - a.z()) * (rx - c.x()) + (a.x() - c.x()) * (rz - c.z())) / det;
        double l3 = 1.0 - l1 - l2;
        if (l1 >= -1e-5 && l2 >= -1e-5 && l3 >= -1e-5) {
            return l1 * a.y() + l2 * b.y() + l3 * c.y();
        }
        return null;
    }
}
