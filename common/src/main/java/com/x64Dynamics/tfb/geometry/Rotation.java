package com.x64Dynamics.tfb.geometry;

public enum Rotation {
    NONE(0),
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    COUNTERCLOCKWISE_90(270);

    private final int degrees;

    Rotation(int degrees) {
        this.degrees = degrees;
    }

    public int degrees() { return degrees; }

    public Direction rotate(Direction direction) {
        if (direction.axis() == Axis.Y) return direction;
        return switch (this) {
            case NONE -> direction;
            case CLOCKWISE_90 -> direction.rotateY();
            case CLOCKWISE_180 -> direction.opposite();
            case COUNTERCLOCKWISE_90 -> direction.rotateYCounterClockwise();
        };
    }

    public Vec3i rotate(Vec3i vec) {
        return switch (this) {
            case NONE -> vec;
            case CLOCKWISE_90 -> new Vec3i(-vec.z(), vec.y(), vec.x());
            case CLOCKWISE_180 -> new Vec3i(-vec.x(), vec.y(), -vec.z());
            case COUNTERCLOCKWISE_90 -> new Vec3i(vec.z(), vec.y(), -vec.x());
        };
    }
}
