package com.x64Dynamics.tfb.geometry;

public record BoundingBox3i(Vec3i min, Vec3i max) {
    public BoundingBox3i {
        int minX = Math.min(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxX = Math.max(min.x(), max.x());
        int maxY = Math.max(min.y(), max.y());
        int maxZ = Math.max(min.z(), max.z());
        min = new Vec3i(minX, minY, minZ);
        max = new Vec3i(maxX, maxY, maxZ);
    }

    public static BoundingBox3i fromCorners(Vec3i first, Vec3i second) {
        return new BoundingBox3i(first, second);
    }

    public static BoundingBox3i fromOriginAndSize(Vec3i origin, int sizeX, int sizeY, int sizeZ) {
        return new BoundingBox3i(origin, new Vec3i(origin.x() + Math.max(0, sizeX - 1), origin.y() + Math.max(0, sizeY - 1), origin.z() + Math.max(0, sizeZ - 1)));
    }

    public int sizeX() { return max.x() - min.x() + 1; }
    public int sizeY() { return max.y() - min.y() + 1; }
    public int sizeZ() { return max.z() - min.z() + 1; }
    public long volume() { return (long) sizeX() * sizeY() * sizeZ(); }

    public boolean contains(Vec3i point) {
        return point.x() >= min.x() && point.x() <= max.x() &&
               point.y() >= min.y() && point.y() <= max.y() &&
               point.z() >= min.z() && point.z() <= max.z();
    }

    public boolean intersects(BoundingBox3i other) {
        return min.x() <= other.max.x() && max.x() >= other.min.x() &&
               min.y() <= other.max.y() && max.y() >= other.min.y() &&
               min.z() <= other.max.z() && max.z() >= other.min.z();
    }

    public BoundingBox3i offset(Vec3i offset) {
        return new BoundingBox3i(min.add(offset), max.add(offset));
    }

    public BoundingBox3i inflate(int amount) {
        return new BoundingBox3i(min.add(-amount, -amount, -amount), max.add(amount, amount, amount));
    }

    public Vec3d center() {
        return new Vec3d((min.x() + max.x() + 1) * 0.5, (min.y() + max.y() + 1) * 0.5, (min.z() + max.z() + 1) * 0.5);
    }
}
