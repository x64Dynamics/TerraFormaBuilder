package com.x64Dynamics.tfb.geometry;

public record Vec3d(double x, double y, double z) {
    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);

    public Vec3d add(double dx, double dy, double dz) {
        return new Vec3d(x + dx, y + dy, z + dz);
    }

    public Vec3d add(Vec3d other) {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d multiply(double factor) {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public Vec3d normalize() {
        double len = length();
        if (len < 1.0e-7) return ZERO;
        return new Vec3d(x / len, y / len, z / len);
    }

    public double dot(Vec3d other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3d cross(Vec3d other) {
        return new Vec3d(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }

    public Vec3d lerp(Vec3d other, double delta) {
        return new Vec3d(
            x + (other.x - x) * delta,
            y + (other.y - y) * delta,
            z + (other.z - z) * delta
        );
    }

    public Vec3i toFloorInt() {
        return new Vec3i((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
