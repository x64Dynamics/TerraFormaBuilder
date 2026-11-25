package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class ConstructionTransforms {
    private ConstructionTransforms() { }

    public static List<PlacementTarget> rotate(List<PlacementTarget> targets, Rotation rotation, Vec3i origin) {
        if (rotation == Rotation.NONE) return new ArrayList<>(targets);
        List<PlacementTarget> result = new ArrayList<>(targets.size());

        for (PlacementTarget target : targets) {
            Vec3i rel = target.pos().subtract(origin);
            Vec3i rotatedRel = rotation.rotate(rel);
            Vec3i newPos = origin.add(rotatedRel);

            BlockIdentifier block = target.block();
            String facing = block.property("facing");
            if (facing != null) {
                try {
                    Direction dir = Direction.valueOf(facing.toUpperCase(Locale.ROOT));
                    Direction rotatedDir = rotation.rotate(dir);
                    block = block.withProperty("facing", rotatedDir.name().toLowerCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) { }
            }
            if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
                String axis = block.property("axis");
                if ("x".equalsIgnoreCase(axis)) {
                    block = block.withProperty("axis", "z");
                } else if ("z".equalsIgnoreCase(axis)) {
                    block = block.withProperty("axis", "x");
                }
            }

            result.add(new PlacementTarget(newPos, block, target.role()));
        }
        return result;
    }

    public static List<PlacementTarget> mirror(List<PlacementTarget> targets, Axis axis, int planeCoord) {
        List<PlacementTarget> result = new ArrayList<>(targets.size());

        for (PlacementTarget target : targets) {
            Vec3i pos = target.pos();
            Vec3i mirroredPos = switch (axis) {
                case X -> new Vec3i(planeCoord - (pos.x() - planeCoord), pos.y(), pos.z());
                case Y -> new Vec3i(pos.x(), planeCoord - (pos.y() - planeCoord), pos.z());
                case Z -> new Vec3i(pos.x(), pos.y(), planeCoord - (pos.z() - planeCoord));
            };

            BlockIdentifier block = target.block();
            String facing = block.property("facing");
            if (facing != null) {
                try {
                    Direction dir = Direction.valueOf(facing.toUpperCase(Locale.ROOT));
                    if (dir.axis() == axis) {
                        block = block.withProperty("facing", dir.opposite().name().toLowerCase(Locale.ROOT));
                    }
                } catch (IllegalArgumentException ignored) { }
            }

            result.add(new PlacementTarget(mirroredPos, block, target.role()));
        }
        return result;
    }

    public static List<PlacementTarget> translate(List<PlacementTarget> targets, Vec3i offset) {
        List<PlacementTarget> result = new ArrayList<>(targets.size());
        for (PlacementTarget target : targets) {
            result.add(new PlacementTarget(target.pos().add(offset), target.block(), target.role()));
        }
        return result;
    }

    public static List<PlacementTarget> replaceMaterial(List<PlacementTarget> targets, String targetPath, BlockIdentifier replacement) {
        List<PlacementTarget> result = new ArrayList<>(targets.size());
        for (PlacementTarget target : targets) {
            if (target.block().path().equalsIgnoreCase(targetPath)) {
                result.add(new PlacementTarget(target.pos(), replacement, target.role()));
            } else {
                result.add(target);
            }
        }
        return result;
    }

    public static List<PlacementTarget> replaceRole(List<PlacementTarget> targets, MaterialRole role, BlockIdentifier newBlock) {
        List<PlacementTarget> result = new ArrayList<>(targets.size());
        for (PlacementTarget target : targets) {
            if (target.role() == role) {
                result.add(new PlacementTarget(target.pos(), newBlock, role));
            } else {
                result.add(target);
            }
        }
        return result;
    }
}
