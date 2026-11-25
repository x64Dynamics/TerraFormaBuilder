package com.x64Dynamics.tfb.architectural.assets;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.platform.ILevelAccess;

import java.util.ArrayList;
import java.util.List;

// Reusable architectural structure asset captured directly from a Minecraft world
// or studio design (inspired by minecraft-citygen's marker-based asset system).
public record AuthoredAsset(
    String id,
    String name,
    AssetCategory category,
    int sizeX,
    int sizeY,
    int sizeZ,
    int groundOffsetY,
    Direction frontFacing,
    boolean rotatable,
    List<RelativeBlock> blocks
) {
    public enum AssetCategory {
        BUILDING,
        STACKABLE_BOTTOM,
        STACKABLE_MIDDLE,
        STACKABLE_TOP,
        LANDMARK,
        ROAD_PIECE,
        PROP
    }

    public record RelativeBlock(int rx, int ry, int rz, String blockState, String roleName) {}

    public static AuthoredAsset captureFromWorld(
            String id,
            String name,
            BoundingBox3i bounds,
            int groundY,
            AssetCategory category,
            Direction frontFacing,
            ILevelAccess level) {
        List<RelativeBlock> captured = new ArrayList<>();
        Vec3i min = bounds.min();
        Vec3i max = bounds.max();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    Vec3i wp = new Vec3i(x, y, z);
                    BlockIdentifier state = level != null ? level.getBlockState(wp) : BlockIdentifier.AIR;
                    if (!state.isAir()) {
                        captured.add(new RelativeBlock(
                            x - min.x(), y - min.y(), z - min.z(),
                            state.asString(), MaterialRole.WALL_PRIMARY.name()
                        ));
                    }
                }
            }
        }
        return new AuthoredAsset(
            id, name, category,
            bounds.sizeX(), bounds.sizeY(), bounds.sizeZ(),
            groundY - min.y(), frontFacing != null ? frontFacing : Direction.SOUTH,
            true, captured
        );
    }

    public List<PlacementTarget> instantiateAt(Vec3i origin, Rotation rotation) {
        List<PlacementTarget> out = new ArrayList<>(blocks.size());
        Rotation rot = rotatable && rotation != null ? rotation : Rotation.NONE;
        for (RelativeBlock rb : blocks) {
            int rx = rb.rx(), rz = rb.rz();
            int tx, tz;
            switch (rot) {
                case CLOCKWISE_90 -> { tx = sizeZ - 1 - rz; tz = rx; }
                case CLOCKWISE_180 -> { tx = sizeX - 1 - rx; tz = sizeZ - 1 - rz; }
                case COUNTERCLOCKWISE_90 -> { tx = rz; tz = sizeX - 1 - rx; }
                default -> { tx = rx; tz = rz; }
            }
            MaterialRole role;
            try { role = MaterialRole.valueOf(rb.roleName()); } catch (Exception e) { role = MaterialRole.WALL_PRIMARY; }
            out.add(new PlacementTarget(
                new Vec3i(origin.x() + tx, origin.y() + rb.ry() - groundOffsetY, origin.z() + tz),
                BlockIdentifier.of(rb.blockState()),
                role
            ));
        }
        return out;
    }
}
