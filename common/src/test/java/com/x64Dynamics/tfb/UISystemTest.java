package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.brush.ArchitecturalBrushes;
import com.x64Dynamics.tfb.architectural.builds.brush.BrushCategory;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3d;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.platform.persistence.ParametricBlueprint;
import com.x64Dynamics.tfb.ui.controller.ClientController;
import com.x64Dynamics.tfb.ui.interaction.RaycastUtil;
import com.x64Dynamics.tfb.ui.interaction.SelectionHandle;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import com.x64Dynamics.tfb.ui.model.WorkspaceTab;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.screen.ArchitectWorkspaceScreen;
import com.x64Dynamics.tfb.ui.widget.UIBounds;
import com.x64Dynamics.tfb.ui.widget.UIButton;
import com.x64Dynamics.tfb.ui.widget.UIDropdown;
import com.x64Dynamics.tfb.ui.widget.UIProgressBar;
import com.x64Dynamics.tfb.ui.widget.UIScrollList;
import com.x64Dynamics.tfb.ui.widget.UISlider;
import com.x64Dynamics.tfb.ui.widget.UITabBar;
import com.x64Dynamics.tfb.ui.widget.UITextField;
import com.x64Dynamics.tfb.ui.widget.UIToggleButton;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UISystemTest {

    private static class DummyGraphics implements IGuiGraphicsContext {
        public void fill(int x1, int y1, int x2, int y2, int color) {}
        public void drawBorder(int x, int y, int width, int height, int color) {}
        public void drawText(String text, int x, int y, int color, boolean shadow) {}
        public void drawCenteredText(String text, int centerX, int y, int color) {}
        public void drawTooltip(String tooltip, int mouseX, int mouseY) {}
        public void enableScissor(int x, int y, int width, int height) {}
        public void disableScissor() {}
    }

    @Test
    public void testUIBounds() {
        UIBounds bounds = new UIBounds(10, 20, 100, 50);
        assertTrue(bounds.contains(15, 25));
        assertTrue(bounds.contains(10, 20));
        assertFalse(bounds.contains(9, 20));
        assertFalse(bounds.contains(110, 50));
    }

    @Test
    public void testWidgets() {
        DummyGraphics g = new DummyGraphics();

        // Button
        AtomicBoolean clicked = new AtomicBoolean(false);
        UIButton btn = new UIButton(new UIBounds(0, 0, 50, 20), "Test", b -> clicked.set(true));
        btn.render(g, 10, 10);
        assertTrue(btn.mouseClicked(10, 10, 0));
        assertTrue(clicked.get());

        // Slider
        AtomicInteger val = new AtomicInteger(5);
        UISlider slider = new UISlider(new UIBounds(0, 0, 100, 20), "Val", 0, 10, 5, val::set);
        slider.render(g, 0, 0);
        slider.mouseClicked(80, 10, 0);
        assertEquals(8, val.get());

        // Dropdown
        UIDropdown<String> dd = new UIDropdown<>(new UIBounds(0, 0, 100, 20), "List", Arrays.asList("A", "B", "C"), 0, null);
        dd.render(g, 0, 0);
        dd.mouseClicked(10, 10, 0); // open
        assertTrue(dd.isOpen());
        dd.mouseClicked(10, 30, 0); // select item 0
        assertEquals("A", dd.selected());

        // TabBar
        UITabBar tabBar = new UITabBar(new UIBounds(0, 0, 800, 20), null);
        tabBar.render(g, 0, 0);
        tabBar.setActiveTab(WorkspaceTab.MATERIALS);
        assertEquals(WorkspaceTab.MATERIALS, tabBar.activeTab());

        // Toggle
        AtomicBoolean toggleVal = new AtomicBoolean(false);
        UIToggleButton toggle = new UIToggleButton(new UIBounds(0, 0, 50, 20), "Toggle", false, toggleVal::set);
        toggle.mouseClicked(10, 10, 0);
        assertTrue(toggleVal.get());

        // Progress
        UIProgressBar bar = new UIProgressBar(new UIBounds(0, 0, 100, 10));
        bar.setProgress(0.75f);
        assertEquals(0.75f, bar.progress(), 0.001f);

        // TextField
        UITextField field = new UITextField(new UIBounds(0, 0, 100, 20), "Hi", "Placeholder", null);
        field.mouseClicked(10, 10, 0);
        assertTrue(field.isFocused());
        field.charTyped('!', 0);
        assertEquals("Hi!", field.text());
        field.keyPressed(259, 0, 0); // backspace
        assertEquals("Hi", field.text());

        // ScrollList
        UIScrollList<String> list = new UIScrollList<>(new UIBounds(0, 0, 100, 100), 20, Arrays.asList("1", "2", "3"), null);
        list.render(g, 0, 0);
        list.mouseClicked(10, 30, 0);
        assertEquals(1, list.selectedIndex());
        assertEquals("2", list.selectedItem());
    }

    @Test
    public void testRaycastAndHandles() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(0, 0, 0), new Vec3i(10, 10, 10));
        SelectionHandle[] handles = SelectionHandle.createHandles(box);
        assertEquals(8, handles.length);

        Double hit = RaycastUtil.intersectRayBox(new Vec3d(5, 5, -5), new Vec3d(0, 0, 1), box);
        assertNotNull(hit);
        assertEquals(5.0, hit, 0.01);

        Double sphereHit = RaycastUtil.intersectRaySphere(new Vec3d(0, 0, -5), new Vec3d(0, 0, 1), new Vec3d(0, 0, 0), 1.0);
        assertNotNull(sphereHit);
        assertEquals(4.0, sphereHit, 0.01);
    }

    @Test
    public void testArchitectWorkspaceModel() {
        ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
        assertNotNull(model.activeBrush());
        assertTrue(model.totalBlockCount() > 0);

        // Test dimension changes
        model.setWidth(20);
        model.setHeight(12);
        model.setDepth(20);
        assertEquals(20, model.width());
        assertEquals(12, model.height());
        assertEquals(20, model.depth());
        assertTrue(model.totalBlockCount() > 0);

        // Test style preset
        model.setStylePreset(StylePreset.RUSTIC);
        assertEquals(StylePreset.RUSTIC, model.stylePreset());

        // Test history undo/redo
        model.pushHistorySnapshot();
        model.setWidth(30);
        model.pushHistorySnapshot();
        assertTrue(model.undo());
        assertEquals(20, model.width());
        assertTrue(model.redo());
        assertEquals(30, model.width());

        // Test blueprint serialization
        ParametricBlueprint bp = model.toBlueprint("Test Villa", "KyroXova");
        assertEquals("Test Villa", bp.metadata().name());
        assertEquals("KyroXova", bp.metadata().author());

        model.loadBlueprint(bp);
        assertEquals("Test Villa", bp.metadata().name());
    }

    @Test
    public void testArchitectWorkspaceScreen() {
        ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
        ArchitectWorkspaceScreen screen = new ArchitectWorkspaceScreen(model);
        screen.init(800, 600);

        DummyGraphics g = new DummyGraphics();
        screen.render(g, 100, 100);

        // Click a tab
        screen.mouseClicked(150, 30, 0);
        // Drag
        screen.mouseDragged(100, 100, 0, 5, 5);
        // Release
        screen.mouseReleased(100, 100, 0);

        assertNotNull(screen.model());
    }

    @Test
    public void testClientController() {
        ClientController controller = ClientController.getInstance();
        assertNotNull(controller.model());
        assertNotNull(controller.screen());
        assertTrue(controller.isPreviewActive());
        controller.togglePreview();
        assertFalse(controller.isPreviewActive());
        controller.togglePreview();
        assertTrue(controller.isPreviewActive());
        controller.setTargetPosition(new Vec3i(100, 70, 100));
        assertEquals(new Vec3i(100, 70, 100), controller.model().origin());
    }

    @Test
    public void testLocalizationAndAllStudioTabs() {
        assertTrue(com.x64Dynamics.tfb.platform.i18n.Localization.hasKey("tfb.home.welcome"));
        assertTrue(com.x64Dynamics.tfb.platform.i18n.Localization.hasKey("tfb.nav.home"));
        assertTrue(com.x64Dynamics.tfb.platform.i18n.Localization.hasKey("tfb.room.living_hall"));
        assertEquals("Design", com.x64Dynamics.tfb.platform.i18n.Localization.tr("tfb.nav.design"));

        ArchitectWorkspaceModel model = new ArchitectWorkspaceModel();
        ArchitectWorkspaceScreen screen = new ArchitectWorkspaceScreen(model);
        DummyGraphics g = new DummyGraphics();

        for (WorkspaceTab tab : WorkspaceTab.values()) {
            model.setActiveTab(tab);
            screen.init(854, 480);
            screen.render(g, 200, 200);
            screen.init(1280, 720);
            screen.render(g, 400, 300);
        }
    }
}