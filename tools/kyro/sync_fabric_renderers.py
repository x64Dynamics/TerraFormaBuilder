from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS_DIR = ROOT / "versions"

TFB_CLIENT_RENDERER = """package com.x64Dynamics.tfb.client;

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

import java.util.List;

public final class TFBClientRenderer {
    private TFBClientRenderer() {}

    public static void renderLevel(PoseStack poseStack, Camera camera) {
        TFBClientController controller = TFBClientController.getInstance();
        if (!controller.isPreviewActive() || poseStack == null || camera == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        BoundingBox3i box = controller.model().boundingBox();
        double minX = box.min().x() - camPos.x;
        double minY = box.min().y() - camPos.y;
        double minZ = box.min().z() - camPos.z;
        double maxX = box.max().x() + 1.0 - camPos.x;
        double maxY = box.max().y() + 1.0 - camPos.y;
        double maxZ = box.max().z() + 1.0 - camPos.z;

        poseStack.pushPose();

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lineConsumer, minX, minY, minZ, maxX, maxY, maxZ, 0.22f, 0.74f, 0.97f, 1.0f);

        Vec3i p1 = controller.model().selectionManager().pos1();
        if (p1 != null) {
            double p1x = p1.x() - camPos.x, p1y = p1.y() - camPos.y, p1z = p1.z() - camPos.z;
            LevelRenderer.renderLineBox(poseStack, lineConsumer, p1x - 0.02, p1y - 0.02, p1z - 0.02, p1x + 1.02, p1y + 1.02, p1z + 1.02, 0.15f, 0.95f, 0.25f, 1.0f);
        }

        Vec3i p2 = controller.model().selectionManager().pos2();
        if (p2 != null) {
            double p2x = p2.x() - camPos.x, p2y = p2.y() - camPos.y, p2z = p2.z() - camPos.z;
            LevelRenderer.renderLineBox(poseStack, lineConsumer, p2x - 0.02, p2y - 0.02, p2z - 0.02, p2x + 1.02, p2y + 1.02, p2z + 1.02, 1.0f, 0.65f, 0.05f, 1.0f);
        }

        WorldPreviewBuffer previewBuffer = controller.model().previewBuffer();
        List<PreviewVoxel> voxels = previewBuffer.getFilteredVoxels();
        int maxRenderCount = 8192;
        int count = 0;

        for (PreviewVoxel voxel : voxels) {
            Vec3i pos = voxel.pos();
            double vx = pos.x() - camPos.x;
            double vy = pos.y() - camPos.y;
            double vz = pos.z() - camPos.z;
            if (vx * vx + vy * vy + vz * vz > 128.0 * 128.0) continue;

            if (voxel.invalidPlacement()) {
                LevelRenderer.renderLineBox(poseStack, lineConsumer, vx, vy, vz, vx + 1.0, vy + 1.0, vz + 1.0, 1.0f, 0.15f, 0.15f, 0.95f);
            } else {
                BlockState state = BlockStateParser.parse(voxel.block());
                if (!state.isAir()) {
                    poseStack.pushPose();
                    poseStack.translate(vx, vy, vz);
                    mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                } else {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, vx + 0.05, vy + 0.05, vz + 0.05, vx + 0.95, vy + 0.95, vz + 0.95, 0.35f, 0.85f, 1.0f, 0.45f);
                }
            }
            if (++count >= maxRenderCount) break;
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }
}
"""

def level_renderer_mixin_for(ver: str) -> str:
    if ver in {"1.18.2", "1.19", "1.19.1", "1.19.2"}:
        return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""
    if ver in {"1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"}:
        return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""
    if ver in {"1.20.5", "1.20.6"}:
        return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""
    if ver in {"1.21", "1.21.1"}:
        return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""
    if ver in {"1.21.2", "1.21.3"}:
        return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""
    return """package com.x64Dynamics.tfb.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.x64Dynamics.tfb.client.TFBClientRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tfb$onRenderLevel(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        TFBClientRenderer.renderLevel(poseStack, camera);
    }
}
"""

ON_MOUSE_PRESS_MIXIN = """
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void tfb$onMousePress(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (ClientEntrypoint.onMouseButton(button, action)) {
            ci.cancel();
        }
    }
"""

def on_mouse_button_method(use_null_to_empty: bool) -> str:
    comp_fn = "Component.nullToEmpty" if use_null_to_empty else "Component.literal"
    return f"""
    public static boolean onMouseButton(int button, int action) {{
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.level == null) return false;
        TFBClientController controller = TFBClientController.getInstance();
        if (!controller.isArchitectMode()) return false;
        if (action != 1) return false;

        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {{
            BlockPos clicked = bhr.getBlockPos();
            Vec3i pos = new Vec3i(clicked.getX(), clicked.getY(), clicked.getZ());
            if (button == 0) {{
                controller.model().setPos1(pos);
                mc.player.displayClientMessage(
                    {comp_fn}("§a[TFB] POS1 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ")"),
                    true
                );
                return true;
            }} else if (button == 1) {{
                controller.model().setPos2(pos);
                ArchitectWorkspaceModel m = controller.model();
                mc.player.displayClientMessage(
                    {comp_fn}("§6[TFB] POS2 set to (" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") §8| §b"
                        + m.width() + "x" + m.height() + "x" + m.depth()
                        + " (" + ((long) m.width() * m.height() * m.depth()) + " blocks)"),
                    true
                );
                return true;
            }}
        }}
        return false;
    }}
"""

def main() -> None:
    updated = 0
    for vdir in sorted(VERSIONS_DIR.iterdir()):
        ver = vdir.name
        if not vdir.is_dir() or ver.startswith("26."):
            continue
        fabric_dir = vdir / "fabric"
        if not fabric_dir.exists():
            continue

        client_dir = fabric_dir / "src/main/java/com/x64Dynamics/tfb/client"
        mixin_dir = fabric_dir / "src/main/java/com/x64Dynamics/tfb/mixin"
        mixins_json = fabric_dir / "src/main/resources/terraformabuilder.mixins.json"

        # 1. TFBClientRenderer.java
        (client_dir / "TFBClientRenderer.java").write_text(TFB_CLIENT_RENDERER, encoding="utf-8")

        # 2. LevelRendererMixin.java
        (mixin_dir / "LevelRendererMixin.java").write_text(level_renderer_mixin_for(ver), encoding="utf-8")

        # 3. terraformabuilder.mixins.json
        if mixins_json.exists():
            data = json.loads(mixins_json.read_text(encoding="utf-8"))
            client_list = data.setdefault("client", [])
            if "LevelRendererMixin" not in client_list:
                client_list.append("LevelRendererMixin")
                mixins_json.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

        # 4. MouseHandlerMixin.java
        mh_path = mixin_dir / "MouseHandlerMixin.java"
        if mh_path.exists():
            mh = mh_path.read_text(encoding="utf-8")
            if "tfb$onMousePress" not in mh:
                mh = mh.replace("    @Inject(method = \"onScroll\"", ON_MOUSE_PRESS_MIXIN.lstrip("\n") + "\n    @Inject(method = \"onScroll\"")
                mh_path.write_text(mh, encoding="utf-8")

        # 5. ClientEntrypoint.java
        ce_path = client_dir / "ClientEntrypoint.java"
        if ce_path.exists():
            ce = ce_path.read_text(encoding="utf-8")
            if "onMouseButton(" not in ce:
                use_null = "Component.literal(" not in ce
                ce = re.sub(
                    r"(    (?:public|private) static void executeBuild\(Minecraft mc\))",
                    lambda m: on_mouse_button_method(use_null).lstrip("\n") + "\n" + m.group(1),
                    ce,
                    count=1,
                )
                ce_path.write_text(ce, encoding="utf-8")

        updated += 1
        print(f"Synced Fabric renderer & mixins for {ver}")
    print(f"Done: {updated} Fabric targets updated.")

if __name__ == "__main__":
    main()
