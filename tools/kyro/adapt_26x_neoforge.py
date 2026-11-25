"""Adapt 26.1 - 26.3 NeoForge client and entrypoint classes to Minecraft 26.x APIs."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
VERSIONS_26 = ["26.1", "26.1.1", "26.1.2", "26.2", "26.3"]

BLOCK_STATE_PARSER_26 = """package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

public final class BlockStateParser {
    private BlockStateParser() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState parse(BlockIdentifier id) {
        if (id == null) return Blocks.AIR.defaultBlockState();

        Identifier loc = Identifier.fromNamespaceAndPath(id.namespace(), id.path());
        Block block = BuiltInRegistries.BLOCK.getValue(loc);
        if (block == null || (block == Blocks.AIR && !id.path().equals("air"))) {
            return Blocks.STONE.defaultBlockState();
        }

        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> entry : id.properties().entrySet()) {
            Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
            if (property != null) {
                Optional<?> val = property.getValue(entry.getValue());
                if (val.isPresent()) {
                    state = state.setValue((Property) property, (Comparable) val.get());
                }
            }
        }
        return state;
    }

    public static BlockIdentifier fromBlockState(BlockState state) {
        if (state == null) return BlockIdentifier.of("minecraft:air");
        Identifier loc = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockIdentifier id = BlockIdentifier.of(loc.toString());
        for (Property<?> prop : state.getProperties()) {
            id = id.withProperty(prop.getName(), state.getValue(prop).toString().toLowerCase());
        }
        return id;
    }
}
"""

TFB_KEY_MAPPINGS_26 = """package com.x64Dynamics.tfb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TFBKeyMappings {
    public static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath("terraformabuilder", "general"));

    public static final KeyMapping OPEN_WORKSPACE = new KeyMapping(
        "key.terraformabuilder.open_workspace", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping TOGGLE_PREVIEW = new KeyMapping(
        "key.terraformabuilder.toggle_preview", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);
    public static final KeyMapping EXECUTE_BUILD = new KeyMapping(
        "key.terraformabuilder.execute_build", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping UNDO = new KeyMapping(
        "key.terraformabuilder.undo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping REDO = new KeyMapping(
        "key.terraformabuilder.redo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping TOGGLE_FLIGHT = new KeyMapping(
        "key.terraformabuilder.toggle_flight", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping SPEED_UP = new KeyMapping(
        "key.terraformabuilder.speed_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);
    public static final KeyMapping SPEED_DOWN = new KeyMapping(
        "key.terraformabuilder.speed_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
    public static final KeyMapping ROTATE_TOOL = new KeyMapping(
        "key.terraformabuilder.rotate_tool", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping[] ALL = {
        OPEN_WORKSPACE, TOGGLE_PREVIEW, EXECUTE_BUILD, UNDO, REDO,
        TOGGLE_FLIGHT, SPEED_UP, SPEED_DOWN, ROTATE_TOOL
    };

    private TFBKeyMappings() {}
}
"""

TERRAFORMA_SCREEN_26 = """package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class TerraFormaScreen extends Screen {
    private final ArchitectWorkspaceScreen screen;

    public TerraFormaScreen() {
        super(Component.literal("TerraFormaBuilder Home Studio"));
        this.screen = TFBClientController.getInstance().screen();
    }

    @Override
    protected void init() {
        super.init();
        screen.setCloseCallback(this::onClose);
        screen.setBuildCallback(() -> {
            if (this.minecraft != null) {
                TFBClientEvents.ForgeEvents.executeBuild(this.minecraft);
            }
        });
        screen.init(this.width, this.height);
        TFBClientController.getInstance().setArchitectMode(true);
        if (this.minecraft != null && this.minecraft.player != null) {
            com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
            if (model.origin().x() == 0 && model.origin().y() == 64 && model.origin().z() == 0) {
                net.minecraft.core.BlockPos playerPos = this.minecraft.player.blockPosition();
                model.setOrigin(new com.x64Dynamics.tfb.geometry.Vec3i(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        IGuiGraphicsContext ctx = new IGuiGraphicsContext() {
            @Override public void fill(int x1, int y1, int x2, int y2, int color) { guiGraphics.fill(x1, y1, x2, y2, color); }
            @Override public void drawBorder(int x, int y, int w, int h, int color) {
                guiGraphics.fill(x, y, x + w, y + 1, color);
                guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
                guiGraphics.fill(x, y, x + 1, y + h, color);
                guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
            }
            @Override public void drawText(String text, int x, int y, int color, boolean shadow) { guiGraphics.text(font, text, x, y, color, shadow); }
            @Override public void drawCenteredText(String text, int cx, int y, int color) { guiGraphics.centeredText(font, text, cx, y, color); }
            @Override public void drawTooltip(String tooltip, int mx, int my) { guiGraphics.setTooltipForNextFrame(font, Component.literal(tooltip), mx, my); }
            @Override public void enableScissor(int x, int y, int w, int h) { guiGraphics.enableScissor(x, y, x + w, y + h); }
            @Override public void disableScissor() { guiGraphics.disableScissor(); }
        };
        screen.render(ctx, mouseX, mouseY);
    }

    @Override public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor guiGraphics) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (screen.mouseClicked((int) event.x(), (int) event.y(), event.button())) return true;
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (screen.mouseDragged((int) event.x(), (int) event.y(), event.button(), dragX, dragY)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (screen.mouseReleased((int) event.x(), (int) event.y(), event.button())) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (screen.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        if (this.minecraft != null && this.minecraft.hasControlDown()) {
            if (event.key() == 90) {
                if (this.minecraft.hasShiftDown()) TFBClientController.getInstance().model().redo();
                else TFBClientController.getInstance().model().undo();
                return true;
            } else if (event.key() == 89) {
                TFBClientController.getInstance().model().redo();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (screen.charTyped((char) event.codepoint(), 0)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
"""

def adapt():
    for v in VERSIONS_26:
        pkg = ROOT / f"versions/{v}/neoforge/src/main/java/com/x64Dynamics/tfb"
        cdir = pkg / "client"
        if not cdir.exists():
            continue

        (cdir / "BlockStateParser.java").write_text(BLOCK_STATE_PARSER_26, encoding="utf-8", newline="\n")
        (cdir / "TFBKeyMappings.java").write_text(TFB_KEY_MAPPINGS_26, encoding="utf-8", newline="\n")
        (cdir / "TerraFormaScreen.java").write_text(TERRAFORMA_SCREEN_26, encoding="utf-8", newline="\n")

        # Entrypoint.java
        ep = pkg / "Entrypoint.java"
        if ep.exists():
            t = ep.read_text(encoding="utf-8")
            t = t.replace("FMLEnvironment.dist != null ? FMLEnvironment.dist.name() : \"UNKNOWN\"", "FMLEnvironment.getDist().name()")
            t = t.replace("FMLEnvironment.dist == Dist.CLIENT", "FMLEnvironment.getDist() == Dist.CLIENT")
            ep.write_text(t, encoding="utf-8", newline="\n")

        # ArchitectFlightController.java
        fc = cdir / "ArchitectFlightController.java"
        if fc.exists():
            t = fc.read_text(encoding="utf-8")
            t = t.replace("Screen.hasAltDown()", "mc.hasAltDown()")
            t = t.replace("mc.player.hasImpulse = true;", "mc.player.needsSync = true;")
            fc.write_text(t, encoding="utf-8", newline="\n")

        # TFBInWorldHud.java
        hud = cdir / "TFBInWorldHud.java"
        if hud.exists():
            t = hud.read_text(encoding="utf-8")
            t = t.replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
            t = t.replace("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;")
            t = t.replace("GuiGraphics g", "GuiGraphicsExtractor g")
            t = t.replace("g.drawString(", "g.text(")
            t = t.replace("g.drawCenteredString(", "g.centeredText(")
            t = t.replace("g.renderItem(", "g.item(")
            t = t.replace("ResourceLocation", "Identifier")
            t = t.replace("BuiltInRegistries.ITEM.get(", "BuiltInRegistries.ITEM.getValue(")
            hud.write_text(t, encoding="utf-8", newline="\n")

        # TFBClientEvents.java
        ev = cdir / "TFBClientEvents.java"
        if ev.exists():
            t = ev.read_text(encoding="utf-8")
            t = t.replace("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;")
            t = t.replace("ResourceLocation", "Identifier")
            t = t.replace("VanillaGuiLayers.EXPERIENCE_BAR", "VanillaGuiLayers.EXPERIENCE_LEVEL")
            t = t.replace("Screen.hasShiftDown()", "mc.hasShiftDown()")
            t = t.replace("private static void executeBuild(", "public static void executeBuild(")
            t = t.replace(
                "for (var key : TFBKeyMappings.ALL) {",
                "event.registerCategory(TFBKeyMappings.CATEGORY);\n            for (var key : TFBKeyMappings.ALL) {"
            )
            t = t.replace("mc.player.displayClientMessage(", "mc.player.sendOverlayMessage(")
            t = re.sub(
                r'mc\.player\.sendOverlayMessage\(\s*(Component\.[^;]+?),\s*true\s*\)',
                r'mc.player.sendOverlayMessage(\1)',
                t,
                flags=re.DOTALL
            )
            t = re.sub(
                r'mc\.player\.sendOverlayMessage\(\s*(Component\.[^;]+?),\s*false\s*\)',
                r'mc.player.sendSystemMessage(\1)',
                t,
                flags=re.DOTALL
            )
            ev.write_text(t, encoding="utf-8", newline="\n")

        print(f"Adapted {v}/neoforge client & entrypoint classes to 26.x API.")

if __name__ == "__main__":
    adapt()
