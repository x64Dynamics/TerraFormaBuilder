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

import com.x64Dynamics.tfb.geometry.Direction;
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
