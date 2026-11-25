package com.x64Dynamics.tfb.geometry;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

/**
 * Editable sparse voxel volume (inspired by vengi's RawVolume/SparseVolume)
 * supporting O(1) voxel access, boolean operations, connected-component
 * detection, surface extraction, and geometric transforms.
 */
public final class VoxelVolume {
    private final Map<Vec3i, BlockIdentifier> voxels = new LinkedHashMap<>();

    public void set(Vec3i pos, BlockIdentifier block) {
        if (pos == null) return;
        if (block == null || block.isAir()) {
            voxels.remove(pos);
        } else {
            voxels.put(pos, block);
        }
    }

    public BlockIdentifier get(Vec3i pos) {
        return voxels.getOrDefault(pos, BlockIdentifier.AIR);
    }

    public boolean isEmpty() {
        return voxels.isEmpty();
    }

    public int size() {
        return voxels.size();
    }

    public Set<Vec3i> positions() {
        return Collections.unmodifiableSet(voxels.keySet());
    }

    public BoundingBox3i bounds() {
        if (voxels.isEmpty()) {
            return BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 1, 1, 1);
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Vec3i p : voxels.keySet()) {
            if (p.x() < minX) minX = p.x();
            if (p.y() < minY) minY = p.y();
            if (p.z() < minZ) minZ = p.z();
            if (p.x() > maxX) maxX = p.x();
            if (p.y() > maxY) maxY = p.y();
            if (p.z() > maxZ) maxZ = p.z();
        }
        return BoundingBox3i.fromCorners(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
    }

    public void union(VoxelVolume other) {
        if (other == null) return;
        voxels.putAll(other.voxels);
    }

    public void subtract(VoxelVolume other) {
        if (other == null) return;
        for (Vec3i p : other.voxels.keySet()) {
            voxels.remove(p);
        }
    }

    public VoxelVolume intersect(VoxelVolume other) {
        VoxelVolume out = new VoxelVolume();
        if (other == null) return out;
        for (Map.Entry<Vec3i, BlockIdentifier> e : voxels.entrySet()) {
            if (other.voxels.containsKey(e.getKey())) {
                out.voxels.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /**
     * Finds 6-connected components in the volume (useful for structural integrity
     * and floating-fragment detection).
     */
    public List<Set<Vec3i>> connectedComponents() {
        List<Set<Vec3i>> components = new ArrayList<>();
        Set<Vec3i> visited = new HashSet<>();
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};

        for (Vec3i start : voxels.keySet()) {
            if (!visited.add(start)) continue;
            Set<Vec3i> comp = new LinkedHashSet<>();
            ArrayDeque<Vec3i> queue = new ArrayDeque<>();
            queue.add(start);
            comp.add(start);
            while (!queue.isEmpty()) {
                Vec3i cur = queue.poll();
                for (int[] d : dirs) {
                    Vec3i nxt = cur.add(d[0], d[1], d[2]);
                    if (voxels.containsKey(nxt) && visited.add(nxt)) {
                        comp.add(nxt);
                        queue.add(nxt);
                    }
                }
            }
            components.add(comp);
        }
        return components;
    }

    /**
     * Computes 6-neighbor face visibility bitmask (matching ObjToSchematic's EFaceVisibility).
     */
    public int visibleFacesMask(Vec3i pos) {
        int mask = 0;
        if (!voxels.containsKey(pos.add(0, 1, 0))) mask |= 1 << 0;  // UP
        if (!voxels.containsKey(pos.add(0, -1, 0))) mask |= 1 << 1; // DOWN
        if (!voxels.containsKey(pos.add(0, 0, -1))) mask |= 1 << 2; // NORTH
        if (!voxels.containsKey(pos.add(1, 0, 0))) mask |= 1 << 3;  // EAST
        if (!voxels.containsKey(pos.add(0, 0, 1))) mask |= 1 << 4;  // SOUTH
        if (!voxels.containsKey(pos.add(-1, 0, 0))) mask |= 1 << 5; // WEST
        return mask;
    }

    public VoxelVolume translate(Vec3i delta) {
        VoxelVolume out = new VoxelVolume();
        for (Map.Entry<Vec3i, BlockIdentifier> e : voxels.entrySet()) {
            out.voxels.put(e.getKey().add(delta), e.getValue());
        }
        return out;
    }

    public List<PlacementTarget> toPlacementTargets(MaterialRole defaultRole) {
        List<PlacementTarget> out = new ArrayList<>(voxels.size());
        MaterialRole role = defaultRole != null ? defaultRole : MaterialRole.WALL_PRIMARY;
        for (Map.Entry<Vec3i, BlockIdentifier> e : voxels.entrySet()) {
            out.add(new PlacementTarget(e.getKey(), e.getValue(), role));
        }
        return out;
    }

    public static VoxelVolume fromPlacementTargets(List<PlacementTarget> targets) {
        VoxelVolume vol = new VoxelVolume();
        if (targets != null) {
            for (PlacementTarget t : targets) {
                vol.set(t.pos(), t.block());
            }
        }
        return vol;
    }
}
