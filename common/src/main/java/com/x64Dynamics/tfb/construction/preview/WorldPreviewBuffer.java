package com.x64Dynamics.tfb.construction.preview;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.construction.ChunkCoordinate;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.platform.ILevelAccess;

import java.util.*;

public class WorldPreviewBuffer {
    private PreviewFilterMode filterMode = PreviewFilterMode.ALL;
    private float globalAlpha = 0.65f;
    private int layerFilterMinY = Integer.MIN_VALUE;
    private int layerFilterMaxY = Integer.MAX_VALUE;

    private final Map<ChunkCoordinate, List<PreviewVoxel>> chunkVoxels = new HashMap<>();
    private final List<PreviewVoxel> allVoxels = new ArrayList<>();
    private int invalidVoxelCount = 0;
    private BoundingBox3i cachedBounds = null;

    public WorldPreviewBuffer() {}

    public PreviewFilterMode filterMode() { return filterMode; }
    public void setFilterMode(PreviewFilterMode mode) { this.filterMode = mode; }

    public float globalAlpha() { return globalAlpha; }
    public void setGlobalAlpha(float alpha) { this.globalAlpha = Math.max(0.1f, Math.min(1.0f, alpha)); }

    public void setLayerFilter(int minY, int maxY) {
        this.layerFilterMinY = minY;
        this.layerFilterMaxY = maxY;
    }

    public void clearLayerFilter() {
        this.layerFilterMinY = Integer.MIN_VALUE;
        this.layerFilterMaxY = Integer.MAX_VALUE;
    }

    public synchronized void updateFromTargets(List<PlacementTarget> targets, ILevelAccess level) {
        chunkVoxels.clear();
        allVoxels.clear();
        invalidVoxelCount = 0;

        if (targets == null || targets.isEmpty()) {
            cachedBounds = null;
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (PlacementTarget target : targets) {
            Vec3i pos = target.pos();
            minX = Math.min(minX, pos.x());
            maxX = Math.max(maxX, pos.x());
            minY = Math.min(minY, pos.y());
            maxY = Math.max(maxY, pos.y());
            minZ = Math.min(minZ, pos.z());
            maxZ = Math.max(maxZ, pos.z());

            // Check collision/validity if level is accessible
            boolean invalid = false;
            if (level != null) {
                if (!level.isAir(pos)) {
                    invalid = true;
                }
            }

            if (invalid) invalidVoxelCount++;

            PreviewVoxel voxel = new PreviewVoxel(pos, target.block(), target.role(), invalid, false, globalAlpha);
            allVoxels.add(voxel);

            ChunkCoordinate chunk = ChunkCoordinate.fromBlock(pos);
            chunkVoxels.computeIfAbsent(chunk, k -> new ArrayList<>()).add(voxel);
        }

        cachedBounds = new BoundingBox3i(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
    }

    public synchronized List<PreviewVoxel> getFilteredVoxels() {
        if (allVoxels.isEmpty()) return Collections.emptyList();

        List<PreviewVoxel> result = new ArrayList<>();
        for (PreviewVoxel voxel : allVoxels) {
            int y = voxel.pos().y();
            if (y < layerFilterMinY || y > layerFilterMaxY) continue;

            if (filterMode == PreviewFilterMode.CONFLICTS_ONLY) {
                if (voxel.invalidPlacement()) result.add(voxel);
                continue;
            }

            MaterialRole role = voxel.role();
            boolean passes = switch (filterMode) {
                case ALL -> true;
                case FOUNDATIONS_ONLY -> role == MaterialRole.FOUNDATION || role == MaterialRole.SUB_FOUNDATION;
                case STRUCTURAL_FRAME -> role == MaterialRole.PILLAR || role == MaterialRole.BEAM || role == MaterialRole.ARCH;
                case WALLS_OPENINGS -> role == MaterialRole.WALL_PRIMARY || role == MaterialRole.WALL_ACCENT || role == MaterialRole.WALL_CORNER || role == MaterialRole.DOOR || role == MaterialRole.WINDOW;
                case ROOF_FACADE -> role == MaterialRole.ROOF_SLOPE || role == MaterialRole.ROOF_RIDGE || role == MaterialRole.ROOF_GABLE || role == MaterialRole.ROOF_OVERHANG;
                case INTERIORS_ONLY -> role == MaterialRole.FLOOR || role == MaterialRole.CEILING || role == MaterialRole.DECORATION || role == MaterialRole.LIGHT_SOURCE;
                case CONFLICTS_ONLY -> voxel.invalidPlacement();
            };

            if (passes) {
                result.add(voxel);
            }
        }
        return result;
    }

    public List<PreviewVoxel> getVoxelsInChunk(ChunkCoordinate chunk) {
        return chunkVoxels.getOrDefault(chunk, Collections.emptyList());
    }

    public int totalVoxelCount() { return allVoxels.size(); }
    public int invalidVoxelCount() { return invalidVoxelCount; }
    public BoundingBox3i cachedBounds() { return cachedBounds; }
}