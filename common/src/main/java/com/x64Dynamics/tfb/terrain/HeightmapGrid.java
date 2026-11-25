package com.x64Dynamics.tfb.terrain;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.Arrays;

public final class HeightmapGrid {
    private final BoundingBox3i bounds;
    private final int sizeX;
    private final int sizeZ;
    private final int[] elevations;

    public HeightmapGrid(BoundingBox3i bounds) {
        this.bounds = bounds;
        this.sizeX = bounds.sizeX();
        this.sizeZ = bounds.sizeZ();
        this.elevations = new int[sizeX * sizeZ];
        Arrays.fill(elevations, bounds.min().y());
    }

    public void setElevation(int worldX, int worldZ, int y) {
        int lx = worldX - bounds.min().x();
        int lz = worldZ - bounds.min().z();
        if (lx >= 0 && lx < sizeX && lz >= 0 && lz < sizeZ) {
            elevations[lz * sizeX + lx] = y;
        }
    }

    public int getElevation(int worldX, int worldZ) {
        int lx = worldX - bounds.min().x();
        int lz = worldZ - bounds.min().z();
        if (lx >= 0 && lx < sizeX && lz >= 0 && lz < sizeZ) {
            return elevations[lz * sizeX + lx];
        }
        return bounds.min().y();
    }

    public int minY() {
        int min = Integer.MAX_VALUE;
        for (int y : elevations) {
            if (y < min) min = y;
        }
        return min == Integer.MAX_VALUE ? bounds.min().y() : min;
    }

    public int maxY() {
        int max = Integer.MIN_VALUE;
        for (int y : elevations) {
            if (y > max) max = y;
        }
        return max == Integer.MIN_VALUE ? bounds.min().y() : max;
    }

    public double averageY() {
        if (elevations.length == 0) return bounds.min().y();
        long sum = 0;
        for (int y : elevations) sum += y;
        return (double) sum / elevations.length;
    }

    public boolean isFlat() {
        return minY() == maxY();
    }

    public BoundingBox3i bounds() {
        return bounds;
    }

    /**
     * Samples live world surface elevations across the XZ footprint of {@code bounds}.
     * Falls back to flat {@code bounds.min().y()} when {@code level} is null.
     */
    public static HeightmapGrid sample(BoundingBox3i bounds, com.x64Dynamics.tfb.platform.ILevelAccess level) {
        HeightmapGrid grid = new HeightmapGrid(bounds);
        if (level == null) {
            return grid;
        }
        for (int x = bounds.min().x(); x <= bounds.max().x(); x++) {
            for (int z = bounds.min().z(); z <= bounds.max().z(); z++) {
                int y = level.getSurfaceHeight(x, z);
                grid.setElevation(x, z, y);
            }
        }
        return grid;
    }

    /**
     * Samples live world surface elevations over {@code bounds} expanded horizontally by {@code padding}.
     */
    public static HeightmapGrid samplePadded(BoundingBox3i bounds, int padding, com.x64Dynamics.tfb.platform.ILevelAccess level) {
        BoundingBox3i padded = BoundingBox3i.fromCorners(
            new Vec3i(bounds.min().x() - padding, bounds.min().y(), bounds.min().z() - padding),
            new Vec3i(bounds.max().x() + padding, bounds.max().y(), bounds.max().z() + padding)
        );
        return sample(padded, level);
    }
}
