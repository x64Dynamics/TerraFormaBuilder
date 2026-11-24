package com.x64Dynamics.tfb.ui.interaction;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;

public class SelectionHandle {
    public enum HandleType {
        FACE_NORTH(Direction.NORTH),
        FACE_SOUTH(Direction.SOUTH),
        FACE_WEST(Direction.WEST),
        FACE_EAST(Direction.EAST),
        FACE_UP(Direction.UP),
        FACE_DOWN(Direction.DOWN),
        CORNER_MIN,
        CORNER_MAX;

        private final Direction direction;

        HandleType() { this.direction = null; }
        HandleType(Direction direction) { this.direction = direction; }
        public Direction direction() { return direction; }
    }

    private final HandleType type;
    private Vec3d position;
    private final float radius = 0.35f;

    public SelectionHandle(HandleType type, Vec3d position) {
        this.type = type;
        this.position = position;
    }

    public HandleType type() { return type; }
    public Vec3d position() { return position; }
    public void setPosition(Vec3d pos) { this.position = pos; }
    public float radius() { return radius; }

    public static SelectionHandle[] createHandles(BoundingBox3i box) {
        Vec3d center = new Vec3d(
            (box.min().x() + box.max().x() + 1) / 2.0,
            (box.min().y() + box.max().y() + 1) / 2.0,
            (box.min().z() + box.max().z() + 1) / 2.0
        );

        return new SelectionHandle[] {
            new SelectionHandle(HandleType.FACE_EAST, new Vec3d(box.max().x() + 1.0, center.y(), center.z())),
            new SelectionHandle(HandleType.FACE_WEST, new Vec3d(box.min().x(), center.y(), center.z())),
            new SelectionHandle(HandleType.FACE_UP, new Vec3d(center.x(), box.max().y() + 1.0, center.z())),
            new SelectionHandle(HandleType.FACE_DOWN, new Vec3d(center.x(), box.min().y(), center.z())),
            new SelectionHandle(HandleType.FACE_SOUTH, new Vec3d(center.x(), center.y(), box.max().z() + 1.0)),
            new SelectionHandle(HandleType.FACE_NORTH, new Vec3d(center.x(), center.y(), box.min().z())),
            new SelectionHandle(HandleType.CORNER_MIN, new Vec3d(box.min().x(), box.min().y(), box.min().z())),
            new SelectionHandle(HandleType.CORNER_MAX, new Vec3d(box.max().x() + 1.0, box.max().y() + 1.0, box.max().z() + 1.0))
        };
    }
}