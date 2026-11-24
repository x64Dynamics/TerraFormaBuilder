package com.x64Dynamics.tfb.geometry;

public record Vec3i(int x, int y, int z) implements Comparable<Vec3i> {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    public Vec3i add(int dx, int dy, int dz) {
        return new Vec3i(x + dx, y + dy, z + dz);
    }

    public Vec3i add(Vec3i other) {
        return new Vec3i(x + other.x, y + other.y, z + other.z);
    }

    public Vec3i subtract(Vec3i other) {
        return new Vec3i(x - other.x, y - other.y, z - other.z);
    }

    public Vec3i multiply(int factor) {
        return new Vec3i(x * factor, y * factor, z * factor);
    }

    public Vec3i offset(Direction direction) {
        return offset(direction, 1);
    }

    public Vec3i offset(Direction direction, int distance) {
        return new Vec3i(x + direction.stepX() * distance, y + direction.stepY() * distance, z + direction.stepZ() * distance);
    }

    public int manhattanDistance(Vec3i other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }

    public double distance(Vec3i other) {
        int dx = x - other.x;
        int dy = y - other.y;
        int dz = z - other.z;
        return Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
    }

    public double distanceSq(Vec3i other) {
        int dx = x - other.x;
        int dy = y - other.y;
        int dz = z - other.z;
        return (double) dx * dx + (double) dy * dy + (double) dz * dz;
    }

    public Vec3d toDouble() {
        return new Vec3d(x, y, z);
    }

    public Vec3d toCenterDouble() {
        return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
    }

    @Override
    public int compareTo(Vec3i o) {
        if (y != o.y) return Integer.compare(y, o.y);
        if (z != o.z) return Integer.compare(z, o.z);
        return Integer.compare(x, o.x);
    }
}
