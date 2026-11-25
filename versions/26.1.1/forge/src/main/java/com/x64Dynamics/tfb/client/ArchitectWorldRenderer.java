package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.construction.preview.PreviewVoxel;
import com.x64Dynamics.tfb.construction.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ArchitectWorldRenderer {
    private static final int MAX_PREVIEW_VOXELS = 8192;

    private ArchitectWorldRenderer() {}

    public static void renderGizmos() {
        ClientController controller = ClientController.getInstance();
        if (!controller.isPreviewActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        BoundingBox3i box = controller.model().boundingBox();
        AABB mainAabb = new AABB(
            box.min().x(), box.min().y(), box.min().z(),
            box.max().x() + 1.0, box.max().y() + 1.0, box.max().z() + 1.0
        );
        Gizmos.cuboid(mainAabb, GizmoStyle.stroke(0xFF38BDF8, 2.0f));

        Vec3i p1 = controller.model().selectionManager().pos1();
        if (p1 != null) {
            Gizmos.cuboid(new BlockPos(p1.x(), p1.y(), p1.z()), GizmoStyle.strokeAndFill(0xFF22C55E, 2.5f, 0x3322C55E));
        }

        Vec3i p2 = controller.model().selectionManager().pos2();
        if (p2 != null) {
            Gizmos.cuboid(new BlockPos(p2.x(), p2.y(), p2.z()), GizmoStyle.strokeAndFill(0xFFF59E0B, 2.5f, 0x33F59E0B));
        }

        WorldPreviewBuffer buffer = controller.model().previewBuffer();
        List<PreviewVoxel> snapshot = buffer.getFilteredVoxels();
        int count = 0;
        for (PreviewVoxel voxel : snapshot) {
            if (count++ >= MAX_PREVIEW_VOXELS) break;
            if (voxel.block().isAir()) continue;
            Vec3i pos = voxel.pos();
            int color = switch (voxel.role()) {
                case FOUNDATION, SUB_FOUNDATION -> 0xAA64748B;
                case ROOF_SLOPE, ROOF_RIDGE, ROOF_GABLE, ROOF_OVERHANG -> 0xAAB45309;
                case WINDOW -> 0x8838BDF8;
                case LIGHT_SOURCE -> 0xCCFACC15;
                default -> 0x88CBD5E1;
            };
            Gizmos.cuboid(new BlockPos(pos.x(), pos.y(), pos.z()), GizmoStyle.strokeAndFill(color, 1.0f, (color & 0x00FFFFFF) | 0x26000000));
        }

        var gp = controller.model().gridPlacement();
        if (controller.model().isInWorldGridMode() || !gp.registry().allBuildings().isEmpty()) {
            var batch = com.x64Dynamics.tfb.architectural.builds.grid.GridRendererModel.buildBatch(
                gp.grid(), gp.registry(), gp.selectedCell(), gp.selectedBuildingId(), gp.grid().origin().y()
            );
            for (var line : batch.lines()) {
                Gizmos.line(
                    new net.minecraft.world.phys.Vec3(line.start().x(), line.start().y() + 0.02, line.start().z()),
                    new net.minecraft.world.phys.Vec3(line.end().x(), line.end().y() + 0.02, line.end().z()),
                    line.argbColor(), line.width()
                );
            }
            if (controller.model().isInWorldGridMode() && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var mg = controller.model().modularGrid();
                var aimedCell = mg.worldToCell(new Vec3i(bhr.getBlockPos().getX(), bhr.getBlockPos().getY(), bhr.getBlockPos().getZ()));
                int cs = mg.cellSize();
                int ch = mg.cellHeight();
                int nextFloor = mg.highestFloorAt(aimedCell.gx(), aimedCell.gz()) + 1;
                int cx0 = mg.origin().x() + aimedCell.gx() * cs;
                int cy0 = mg.origin().y() + nextFloor * ch;
                int cz0 = mg.origin().z() + aimedCell.gz() * cs;
                Gizmos.cuboid(new net.minecraft.world.phys.AABB(cx0, cy0, cz0, cx0 + cs, cy0 + ch, cz0 + cs), GizmoStyle.stroke(0xFFFACC15, 2.0f));
            }
        }
    }
}
