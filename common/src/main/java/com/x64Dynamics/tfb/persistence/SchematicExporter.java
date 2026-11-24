package com.x64Dynamics.tfb.persistence;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.*;

public final class SchematicExporter {
    private SchematicExporter() { }

    public record SchematicBlockData(int relX, int relY, int relZ, String blockState) { }

    public record SchematicPayload(
        int sizeX,
        int sizeY,
        int sizeZ,
        List<SchematicBlockData> blocks
    ) { }

    public static SchematicPayload exportRelative(BoundingBox3i bounds, List<PlacementTarget> targets) {
        Vec3i origin = bounds.min();
        List<SchematicBlockData> blocks = new ArrayList<>(targets.size());

        for (PlacementTarget target : targets) {
            Vec3i pos = target.pos();
            int rx = pos.x() - origin.x();
            int ry = pos.y() - origin.y();
            int rz = pos.z() - origin.z();
            blocks.add(new SchematicBlockData(rx, ry, rz, target.block().asString()));
        }

        return new SchematicPayload(bounds.sizeX(), bounds.sizeY(), bounds.sizeZ(), blocks);
    }
}
