package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import com.x64Dynamics.tfb.TerraFormaBuilder;
public class TerraFormaScreen extends Screen {
    private final ArchitectWorkspaceScreen screen;

    public TerraFormaScreen() {
        super(Component.literal("TerraFormaBuilder Architect Workspace"));
        this.screen = ClientController.getInstance().screen();
    }

    @Override
    protected void init() {
        super.init();
        screen.setCloseCallback(this::onClose);
        screen.init(this.width, this.height);
        ClientController.getInstance().setArchitectMode(true);
        if (this.minecraft != null && this.minecraft.player != null) {
            com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel model = ClientController.getInstance().model();
            if (model.origin().x() == 0 && model.origin().y() == 64 && model.origin().z() == 0) {
                net.minecraft.core.BlockPos playerPos = this.minecraft.player.blockPosition();
                com.x64Dynamics.tfb.geometry.Vec3i newOrigin = new com.x64Dynamics.tfb.geometry.Vec3i(playerPos.getX(), playerPos.getY(), playerPos.getZ());
                model.setOrigin(newOrigin);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IGuiGraphicsContext ctx = new IGuiGraphicsContext() {
            @Override
            public void fill(int x1, int y1, int x2, int y2, int color) {
                guiGraphics.fill(x1, y1, x2, y2, color);
            }

            @Override
            public void drawBorder(int x, int y, int width, int height, int color) {
                guiGraphics.fill(x, y, x + width, y + 1, color);
                guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
                guiGraphics.fill(x, y + 1, x + 1, y + height, color);
                guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
            }

            @Override
            public void drawText(String text, int x, int y, int color, boolean shadow) {
                guiGraphics.drawString(font, text, x, y, color, shadow);
            }

            @Override
            public void drawCenteredText(String text, int centerX, int y, int color) {
                guiGraphics.drawCenteredString(font, text, centerX, y, color);
            }

            @Override
            public void drawTooltip(String tooltip, int mx, int my) {
                guiGraphics.setTooltipForNextFrame(font, Component.literal(tooltip), mx, my);
            }

            @Override
            public void enableScissor(int x, int y, int width, int height) {
                guiGraphics.enableScissor(x, y, x + width, y + height);
            }

            @Override
            public void disableScissor() {
                guiGraphics.disableScissor();
            }
        };

        screen.render(ctx, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately empty to keep Minecraft world and 3D preview completely clear
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (screen.mouseClicked((int) event.x(), (int) event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (screen.mouseDragged((int) event.x(), (int) event.y(), event.button(), dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (screen.mouseReleased((int) event.x(), (int) event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (screen.keyPressed(event.key(), event.scancode(), event.modifiers())) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.hasControlDown()) {
            if (event.key() == 90) {
                if (this.minecraft.hasShiftDown()) {
                    ClientController.getInstance().model().redo();
                } else {
                    ClientController.getInstance().model().undo();
                }
                return true;
            } else if (event.key() == 89) {
                ClientController.getInstance().model().redo();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (screen.charTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
