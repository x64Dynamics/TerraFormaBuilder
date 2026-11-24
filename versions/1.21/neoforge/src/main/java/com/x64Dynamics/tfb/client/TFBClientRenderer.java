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

        // 2. Render face handles on center of each face
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

        // 3. Render ghost preview voxels from WorldPreviewBuffer
        WorldPreviewBuffer previewBuffer = controller.model().previewBuffer();
        List<PreviewVoxel> voxels = previewBuffer.getFilteredVoxels();

        int maxRenderCount = 2048; // Budget to ensure smooth 120 FPS rendering
        int count = 0;
        for (PreviewVoxel voxel : voxels) {
            Vec3i pos = voxel.pos();
            double vx = pos.x() - camPos.x;
            double vy = pos.y() - camPos.y;
            double vz = pos.z() - camPos.z;

            // Distance cull (within 96 blocks)
            if (vx * vx + vy * vy + vz * vz > 96.0 * 96.0) continue;

            float r = 0.8f, g = 0.8f, b = 0.8f;
            if (voxel.invalidPlacement()) {
                r = 1.0f; g = 0.1f; b = 0.1f; // Red conflict highlight
            } else if (voxel.isModified()) {
                r = 1.0f; g = 0.8f; b = 0.1f; // Yellow modified highlight
            }

            LevelRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, r, g, b, 0.35f);
            if (++count >= maxRenderCount) break;
        }

        poseStack.popPose();
    }
}
