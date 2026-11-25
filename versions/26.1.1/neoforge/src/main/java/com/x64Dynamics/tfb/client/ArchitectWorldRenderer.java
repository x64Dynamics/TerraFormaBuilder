package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.preview.PreviewVoxel;
import com.x64Dynamics.tfb.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)
public final class ArchitectWorldRenderer {
    private static final RandomSource PREVIEW_RANDOM = RandomSource.create(42L);

    private ArchitectWorldRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        ClientController controller = ClientController.getInstance();
        if (!controller.isPreviewActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        if (camPos == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        BoundingBox3i box = controller.model().boundingBox();
        double minX = box.min().x() - camPos.x;
        double minY = box.min().y() - camPos.y;
        double minZ = box.min().z() - camPos.z;
        double sizeX = box.max().x() + 1.0 - box.min().x();
        double sizeY = box.max().y() + 1.0 - box.min().y();
        double sizeZ = box.max().z() + 1.0 - box.min().z();

        poseStack.pushPose();

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderTypes.lines());
        ShapeRenderer.renderShape(poseStack, lineConsumer, Shapes.create(0, 0, 0, sizeX, sizeY, sizeZ), minX, minY, minZ, 0xFF38BDF8, 2.0f);

        var gp = controller.model().gridPlacement();
        if (!gp.registry().allBuildings().isEmpty()) {
            for (var bld : gp.registry().allBuildings()) {
                BoundingBox3i gb = gp.grid().toWorldBounds(bld);
                boolean sel = bld.id().equals(gp.selectedBuildingId());
                ShapeRenderer.renderShape(poseStack, lineConsumer,
                    Shapes.create(0, 0, 0, gb.sizeX(), gb.sizeY(), gb.sizeZ()),
                    gb.min().x() - camPos.x, gb.min().y() - camPos.y, gb.min().z() - camPos.z,
                    sel ? 0xFFFACC15 : 0xFF34D399, 2.0f);
            }
        if (controller.model().isInWorldGridMode() || !gp.registry().allBuildings().isEmpty()) {
            var gridBatch = com.x64Dynamics.tfb.grid.GridRendererModel.buildBatch(
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
            double p1x = p1.x() - camPos.x, p1y = p1.y() - camPos.y, p1z = p1.z() - camPos.z;
            ShapeRenderer.renderShape(poseStack, lineConsumer, Shapes.create(-0.02, -0.02, -0.02, 1.02, 1.02, 1.02), p1x, p1y, p1z, 0xFF26F240, 2.5f);
        }

        Vec3i p2 = controller.model().selectionManager().pos2();
        if (p2 != null) {
            double p2x = p2.x() - camPos.x, p2y = p2.y() - camPos.y, p2z = p2.z() - camPos.z;
            ShapeRenderer.renderShape(poseStack, lineConsumer, Shapes.create(-0.02, -0.02, -0.02, 1.02, 1.02, 1.02), p2x, p2y, p2z, 0xFFFFA60D, 2.5f);
        }

        WorldPreviewBuffer previewBuffer = controller.model().previewBuffer();
        List<PreviewVoxel> voxels = previewBuffer.getFilteredVoxels();
        VertexConsumer blockConsumer = bufferSource.getBuffer(RenderTypes.cutoutMovingBlock());
        QuadInstance qi = new QuadInstance();
        qi.setColor(0xFFFFFFFF);
        qi.setLightCoords(0x00F000F0);
        qi.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        List<BlockStateModelPart> parts = new ArrayList<>();
        int maxRenderCount = 8192;
        int count = 0;

        for (PreviewVoxel voxel : voxels) {
            Vec3i pos = voxel.pos();
            double vx = pos.x() - camPos.x;
            double vy = pos.y() - camPos.y;
            double vz = pos.z() - camPos.z;
            if (vx * vx + vy * vy + vz * vz > 128.0 * 128.0) continue;

            if (voxel.invalidPlacement()) {
                ShapeRenderer.renderShape(poseStack, lineConsumer, Shapes.block(), vx, vy, vz, 0xF2FF2626, 2.0f);
            } else {
                BlockState state = BlockStateParser.parse(voxel.block());
                if (!state.isAir()) {
                    poseStack.pushPose();
                    poseStack.translate(vx, vy, vz);
                    BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
                    parts.clear();
                    model.collectParts(PREVIEW_RANDOM, parts);
                    for (BlockStateModelPart part : parts) {
                        for (Direction dir : Direction.values()) {
                            for (BakedQuad quad : part.getQuads(dir)) {
                                blockConsumer.putBakedQuad(poseStack.last(), quad, qi);
                            }
                        }
                        for (BakedQuad quad : part.getQuads(null)) {
                            blockConsumer.putBakedQuad(poseStack.last(), quad, qi);
                        }
                    }
                    poseStack.popPose();
                } else {
                    ShapeRenderer.renderShape(poseStack, lineConsumer, Shapes.create(0.05, 0.05, 0.05, 0.95, 0.95, 0.95), vx, vy, vz, 0x7359D9FF, 1.5f);
                }
            }
            if (++count >= maxRenderCount) break;
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }
}
