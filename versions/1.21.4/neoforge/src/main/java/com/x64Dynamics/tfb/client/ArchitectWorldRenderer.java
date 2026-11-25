package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.construction.preview.PreviewVoxel;
import com.x64Dynamics.tfb.construction.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
public final class ArchitectWorldRenderer {
    private ArchitectWorldRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        ClientController controller = ClientController.getInstance();
        if (!controller.isPreviewActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        BoundingBox3i box = controller.model().boundingBox();
        double minX = box.min().x() - camPos.x;
        double minY = box.min().y() - camPos.y;
        double minZ = box.min().z() - camPos.z;
        double maxX = box.max().x() + 1.0 - camPos.x;
        double maxY = box.max().y() + 1.0 - camPos.y;
        double maxZ = box.max().z() + 1.0 - camPos.z;

        poseStack.pushPose();

        // 1. Render primary cyan bounding box wireframe
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, minX, minY, minZ, maxX, maxY, maxZ, 0.22f, 0.74f, 0.97f, 1.0f);

        // 2. Render POS1 and POS2 distinct markers
        var gp = controller.model().gridPlacement();
        if (!gp.registry().allBuildings().isEmpty()) {
            for (var bld : gp.registry().allBuildings()) {
                BoundingBox3i gb = gp.grid().toWorldBounds(bld);
                boolean sel = bld.id().equals(gp.selectedBuildingId());
                ShapeRenderer.renderLineBox(poseStack, lineConsumer, gb.min().x() - camPos.x, gb.min().y() - camPos.y, gb.min().z() - camPos.z, gb.max().x() + 1.0 - camPos.x, gb.max().y() + 1.0 - camPos.y, gb.max().z() + 1.0 - camPos.z, sel ? 0.98f : 0.2f, sel ? 0.8f : 0.85f, sel ? 0.08f : 0.45f, 1.0f);
            }
        if (controller.model().isInWorldGridMode() || !gp.registry().allBuildings().isEmpty()) {
            var gridBatch = com.x64Dynamics.tfb.architectural.builds.grid.GridRendererModel.buildBatch(
                gp.grid(), gp.registry(), gp.selectedCell(), gp.selectedBuildingId(), controller.model().origin().y()
            );
            for (var gl : gridBatch.lines()) {
                float lr = ((gl.argbColor() >> 16) & 0xFF) / 255.0f;
                float lg = ((gl.argbColor() >> 8) & 0xFF) / 255.0f;
                float lb = (gl.argbColor() & 0xFF) / 255.0f;
                float la = Math.max(0.25f, ((gl.argbColor() >>> 24) & 0xFF) / 255.0f);
                double gx0 = Math.min(gl.start().x(), gl.end().x()) - camPos.x;
                double gy0 = Math.min(gl.start().y(), gl.end().y()) - camPos.y;
                double gz0 = Math.min(gl.start().z(), gl.end().z()) - camPos.z;
                double gx1 = Math.max(gl.start().x(), gl.end().x()) - camPos.x + 0.02;
                double gy1 = Math.max(gl.start().y(), gl.end().y()) - camPos.y + 0.02;
                double gz1 = Math.max(gl.start().z(), gl.end().z()) - camPos.z + 0.02;
                LevelRenderer.renderLineBox(poseStack, lineConsumer, gx0, gy0, gz0, gx1, gy1, gz1, lr, lg, lb, la);
            }
            if (controller.model().isInWorldGridMode() && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var mg = controller.model().modularGrid();
                var aimedCell = mg.worldToCell(new Vec3i(bhr.getBlockPos().getX(), bhr.getBlockPos().getY(), bhr.getBlockPos().getZ()));
                int cs = mg.cellSize();
                int ch = mg.cellHeight();
                int nextFloor = mg.highestFloorAt(aimedCell.gx(), aimedCell.gz()) + 1;
                double cx0 = mg.origin().x() + aimedCell.gx() * cs - camPos.x;
                double cy0 = mg.origin().y() + nextFloor * ch - camPos.y;
                double cz0 = mg.origin().z() + aimedCell.gz() * cs - camPos.z;
                LevelRenderer.renderLineBox(poseStack, lineConsumer, cx0, cy0, cz0, cx0 + cs, cy0 + ch, cz0 + cs, 0.98f, 0.80f, 0.08f, 1.0f);
            }
        }
        }
        Vec3i p1 = controller.model().selectionManager().pos1();
        if (p1 != null) {
            double p1x = p1.x() - camPos.x;
            double p1y = p1.y() - camPos.y;
            double p1z = p1.z() - camPos.z;
            // Vibrant green marker for POS1
            ShapeRenderer.renderLineBox(poseStack, lineConsumer, p1x - 0.02, p1y - 0.02, p1z - 0.02, p1x + 1.02, p1y + 1.02, p1z + 1.02, 0.15f, 0.95f, 0.25f, 1.0f);
        }

        Vec3i p2 = controller.model().selectionManager().pos2();
        if (p2 != null) {
            double p2x = p2.x() - camPos.x;
            double p2y = p2.y() - camPos.y;
            double p2z = p2.z() - camPos.z;
            // Vibrant amber/gold marker for POS2
            ShapeRenderer.renderLineBox(poseStack, lineConsumer, p2x - 0.02, p2y - 0.02, p2z - 0.02, p2x + 1.02, p2y + 1.02, p2z + 1.02, 1.0f, 0.65f, 0.05f, 1.0f);
        }

        // 3. Render face handles on center of each face
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;
        double hSize = 0.35;

        // +X & -X handles (red accent)
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, maxX - hSize, midY - hSize, midZ - hSize, maxX + hSize, midY + hSize, midZ + hSize, 0.95f, 0.2f, 0.2f, 1.0f);
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, minX - hSize, midY - hSize, midZ - hSize, minX + hSize, midY + hSize, midZ + hSize, 0.95f, 0.2f, 0.2f, 1.0f);

        // +Y & -Y handles (green accent)
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, maxY - hSize, midZ - hSize, midX + hSize, maxY + hSize, midZ + hSize, 0.2f, 0.95f, 0.2f, 1.0f);
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, minY - hSize, midZ - hSize, midX + hSize, minY + hSize, midZ + hSize, 0.2f, 0.95f, 0.2f, 1.0f);

        // +Z & -Z handles (blue accent)
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, midY - hSize, maxZ - hSize, midX + hSize, midY + hSize, maxZ + hSize, 0.2f, 0.5f, 0.95f, 1.0f);
        ShapeRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, midY - hSize, minZ - hSize, midX + hSize, midY + hSize, minZ + hSize, 0.2f, 0.5f, 0.95f, 1.0f);

        // 4. Render ghost preview voxels from WorldPreviewBuffer
        if (controller.model().isShapeEditorMode() && controller.model().selectedSectionId() != null) {
            controller.model().activeArchProject().findSection(controller.model().selectedSectionId()).ifPresent(sec -> {
                BoundingBox3i sb = sec.bounds();
                double sx0 = sb.min().x() - camPos.x - 0.03;
                double sy0 = sb.min().y() - camPos.y - 0.03;
                double sz0 = sb.min().z() - camPos.z - 0.03;
                double sx1 = sb.max().x() + 1.03 - camPos.x;
                double sy1 = sb.min().y() + sec.stories() * 4.0 + 1.03 - camPos.y;
                double sz1 = sb.max().z() + 1.03 - camPos.z;
                if (sec.isLocked()) {
                    ShapeRenderer.renderLineBox(poseStack, lineConsumer, sx0, sy0, sz0, sx1, sy1, sz1, 1.0f, 0.78f, 0.1f, 1.0f);
                } else {
                    ShapeRenderer.renderLineBox(poseStack, lineConsumer, sx0, sy0, sz0, sx1, sy1, sz1, 0.18f, 0.95f, 0.45f, 1.0f);
                }
            });
        }

        WorldPreviewBuffer previewBuffer = controller.model().previewBuffer();
        List<PreviewVoxel> voxels = previewBuffer.getFilteredVoxels();

        int maxRenderCount = 8192; // Full architectural structure rendering budget
        int count = 0;
        for (PreviewVoxel voxel : voxels) {
            Vec3i pos = voxel.pos();
            double vx = pos.x() - camPos.x;
            double vy = pos.y() - camPos.y;
            double vz = pos.z() - camPos.z;

            // Distance cull (within 128 blocks)
            if (vx * vx + vy * vy + vz * vz > 128.0 * 128.0) continue;

            float r = 0.35f, g = 0.85f, b = 1.0f, a = 0.55f;
            if (voxel.invalidPlacement()) {
                r = 1.0f; g = 0.15f; b = 0.15f; a = 0.95f; // Red conflict highlight
                ShapeRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, r, g, b, a);
            } else {
                // Render real 3D textured block models with orientations, stairs, slabs, and decor
                BlockState state = BlockStateParser.parse(voxel.block());
                if (!state.isAir()) {
                    poseStack.pushPose();
                    poseStack.translate(vx, vy, vz);
                    mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                } else {
                    ShapeRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, r, g, b, a);
                }
            }
            if (++count >= maxRenderCount) break;
        }

        // Flush all batched block models to GPU
        bufferSource.endBatch();

        poseStack.popPose();
    }
}
