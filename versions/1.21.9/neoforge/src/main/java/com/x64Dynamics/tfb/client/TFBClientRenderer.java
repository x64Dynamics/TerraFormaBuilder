package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.preview.PreviewVoxel;
import com.x64Dynamics.tfb.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.ui.TFBClientController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
public final class TFBClientRenderer {
    private TFBClientRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        TFBClientController controller = TFBClientController.getInstance();
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
        LevelRenderer.renderLineBox(poseStack, lineConsumer, minX, minY, minZ, maxX, maxY, maxZ, 0.22f, 0.74f, 0.97f, 1.0f);

        // 2. Render POS1 and POS2 distinct markers
        Vec3i p1 = controller.model().selectionManager().pos1();
        if (p1 != null) {
            double p1x = p1.x() - camPos.x;
            double p1y = p1.y() - camPos.y;
            double p1z = p1.z() - camPos.z;
            // Vibrant green marker for POS1
            LevelRenderer.renderLineBox(poseStack, lineConsumer, p1x - 0.02, p1y - 0.02, p1z - 0.02, p1x + 1.02, p1y + 1.02, p1z + 1.02, 0.15f, 0.95f, 0.25f, 1.0f);
        }

        Vec3i p2 = controller.model().selectionManager().pos2();
        if (p2 != null) {
            double p2x = p2.x() - camPos.x;
            double p2y = p2.y() - camPos.y;
            double p2z = p2.z() - camPos.z;
            // Vibrant amber/gold marker for POS2
            LevelRenderer.renderLineBox(poseStack, lineConsumer, p2x - 0.02, p2y - 0.02, p2z - 0.02, p2x + 1.02, p2y + 1.02, p2z + 1.02, 1.0f, 0.65f, 0.05f, 1.0f);
        }

        // 3. Render face handles on center of each face
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;
        double hSize = 0.35;

        // +X & -X handles (red accent)
        LevelRenderer.renderLineBox(poseStack, lineConsumer, maxX - hSize, midY - hSize, midZ - hSize, maxX + hSize, midY + hSize, midZ + hSize, 0.95f, 0.2f, 0.2f, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lineConsumer, minX - hSize, midY - hSize, midZ - hSize, minX + hSize, midY + hSize, midZ + hSize, 0.95f, 0.2f, 0.2f, 1.0f);

        // +Y & -Y handles (green accent)
        LevelRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, maxY - hSize, midZ - hSize, midX + hSize, maxY + hSize, midZ + hSize, 0.2f, 0.95f, 0.2f, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, minY - hSize, midZ - hSize, midX + hSize, minY + hSize, midZ + hSize, 0.2f, 0.95f, 0.2f, 1.0f);

        // +Z & -Z handles (blue accent)
        LevelRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, midY - hSize, maxZ - hSize, midX + hSize, midY + hSize, maxZ + hSize, 0.2f, 0.5f, 0.95f, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lineConsumer, midX - hSize, midY - hSize, minZ - hSize, midX + hSize, midY + hSize, minZ + hSize, 0.2f, 0.5f, 0.95f, 1.0f);

        // 4. Render ghost preview voxels from WorldPreviewBuffer
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
                LevelRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, r, g, b, a);
            } else {
                // Render real 3D textured block models with orientations, stairs, slabs, and decor
                BlockState state = BlockStateParser.parse(voxel.block());
                if (!state.isAir()) {
                    poseStack.pushPose();
                    poseStack.translate(vx, vy, vz);
                    mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                } else {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, r, g, b, a);
                }
            }
            if (++count >= maxRenderCount) break;
        }

        // Flush all batched block models to GPU
        bufferSource.endBatch();

        poseStack.popPose();
    }
}
