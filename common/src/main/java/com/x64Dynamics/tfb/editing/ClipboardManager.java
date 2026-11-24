package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipboardManager {
    public record ClipboardEntry(
        List<PlacementTarget> targets,
        Vec3i anchor,
        int width,
        int height,
        int depth,
        long copiedEpochMs
    ) { }

    private static final ClipboardManager INSTANCE = new ClipboardManager();
    private ClipboardEntry currentEntry = null;

    private ClipboardManager() { }

    public static ClipboardManager getInstance() {
        return INSTANCE;
    }

    public synchronized void copy(List<PlacementTarget> targets, Vec3i anchor) {
        if (targets == null || targets.isEmpty()) {
            this.currentEntry = null;
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        List<PlacementTarget> relativeTargets = new ArrayList<>(targets.size());
        for (PlacementTarget target : targets) {
            Vec3i relPos = target.pos().subtract(anchor);
            relativeTargets.add(new PlacementTarget(relPos, target.block(), target.role()));

            minX = Math.min(minX, relPos.x());
            maxX = Math.max(maxX, relPos.x());
            minY = Math.min(minY, relPos.y());
            maxY = Math.max(maxY, relPos.y());
            minZ = Math.min(minZ, relPos.z());
            maxZ = Math.max(maxZ, relPos.z());
        }

        this.currentEntry = new ClipboardEntry(
            Collections.unmodifiableList(relativeTargets),
            anchor,
            maxX - minX + 1,
            maxY - minY + 1,
            maxZ - minZ + 1,
            System.currentTimeMillis()
        );
    }

    public synchronized boolean hasContent() {
        return currentEntry != null && !currentEntry.targets().isEmpty();
    }

    public synchronized ClipboardEntry currentEntry() {
        return currentEntry;
    }

    public synchronized List<PlacementTarget> paste(Vec3i newAnchor, Rotation rotation, Axis mirror) {
        if (!hasContent()) return Collections.emptyList();

        List<PlacementTarget> pasted = new ArrayList<>(currentEntry.targets().size());
        for (PlacementTarget target : currentEntry.targets()) {
            pasted.add(new PlacementTarget(target.pos().add(newAnchor), target.block(), target.role()));
        }

        if (rotation != null && rotation != Rotation.NONE) {
            pasted = ConstructionTransforms.rotate(pasted, rotation, newAnchor);
        }
        if (mirror != null) {
            int planeCoord = (mirror == Axis.X) ? newAnchor.x() : ((mirror == Axis.Y) ? newAnchor.y() : newAnchor.z());
            pasted = ConstructionTransforms.mirror(pasted, mirror, planeCoord);
        }

        return pasted;
    }

    public static List<PlacementTarget> duplicate(List<PlacementTarget> targets, Vec3i offset) {
        List<PlacementTarget> result = new ArrayList<>(targets.size());
        for (PlacementTarget target : targets) {
            result.add(new PlacementTarget(target.pos().add(offset), target.block(), target.role()));
        }
        return result;
    }
}