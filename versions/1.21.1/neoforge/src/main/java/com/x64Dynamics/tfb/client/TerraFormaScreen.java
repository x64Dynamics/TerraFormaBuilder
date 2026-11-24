package com.x64Dynamics.tfb.client;

import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TerraFormaScreen extends Screen {
    private final ArchitectWorkspaceScreen screen;

    public TerraFormaScreen() {
        super(Component.literal("TerraFormaBuilder Architect Workspace"));
        this.screen = TFBClientController.getInstance().screen();
    }

    @Override
    protected void init() {
        super.init();
        screen.init(this.width, this.height);
        TFBClientController.getInstance().setArchitectMode(true);
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
                guiGraphics.fill(x, y, x + 1, y + height, color);
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
                guiGraphics.renderTooltip(font, Component.literal(tooltip), mx, my);
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
        // Do not call super.render() which invokes renderBackground and triggers vanilla screen blur shaders.
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately empty: prevents vanilla Minecraft from applying the post-processing blur shader
        // so the in-game world, structures, and live 3D preview voxels remain completely clear and unblurred.
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
        // Deliberately empty to keep world transparent
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (screen.mouseClicked((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (screen.mouseDragged((int) mouseX, (int) mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (screen.mouseReleased((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (screen.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (hasControlDown()) {
            if (keyCode == 90) {
                if (hasShiftDown()) {
                    TFBClientController.getInstance().model().redo();
                } else {
                    TFBClientController.getInstance().model().undo();
                }
                return true;
            } else if (keyCode == 89) {
                TFBClientController.getInstance().model().redo();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (screen.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
