package com.x64Dynamics.tfb.ui.screen;

import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.brush.ArchitecturalBrushes;
import com.x64Dynamics.tfb.brush.BrushCategory;
import com.x64Dynamics.tfb.brush.BrushRegistry;
import com.x64Dynamics.tfb.brush.IConstructionBrush;
import com.x64Dynamics.tfb.editing.WorldSelectionManager;
import com.x64Dynamics.tfb.i18n.TFBLocalization;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.persistence.BlueprintStorage;
import com.x64Dynamics.tfb.persistence.ParametricBlueprint;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyEntry;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyRegistry;
import com.x64Dynamics.tfb.taxonomy.SearchableTaxonomyBrowser;
import com.x64Dynamics.tfb.ui.TFBClientController;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import com.x64Dynamics.tfb.ui.model.WorkspaceTab;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.widget.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * TerraFormaBuilder Studio UI featuring a Home Dashboard, live Building Hierarchy,
 * contextual Design/Materials/Library/Projects/Settings editors, and full localization.
 */
public class ArchitectWorkspaceScreen {
    private final ArchitectWorkspaceModel model;
    private final List<UIWidget> widgets = new ArrayList<>();
    private final List<UIWidget> tabWidgets = new ArrayList<>();

    private int width;
    private int height;

    // Header & Global Widgets
    private UITabBar tabBar;
    private UIButton flightButton;
    private UIButton previewButton;
    private UIButton instantBuildButton;
    private UIButton undoButton;
    private UIButton redoButton;
    private UIButton closeButton;

    // Left Sidebar Widgets
    private UIScrollList<String> hierarchyList;
    private UIScrollList<IConstructionBrush> brushList;
    private final List<UIWidget> libraryFilterButtons = new ArrayList<>();

    // Construction progress
    private UIProgressBar constructionProgressBar;

    // Search & Blueprint fields
    private UITextField blueprintNameField;
    private UITextField blueprintAuthorField;
    private UITextField materialSearchField;

    // Callbacks
    private Runnable closeCallback;
    private Runnable buildCallback;

    // Status message
    private String statusMessage = TFBLocalization.tr("tfb.status.ready");
    private long statusMessageExpiry = 0;

    public ArchitectWorkspaceScreen(ArchitectWorkspaceModel model) {
        this.model = model;
    }

    public ArchitectWorkspaceModel model() { return model; }
    public void setCloseCallback(Runnable cb) { this.closeCallback = cb; }
    public void setBuildCallback(Runnable cb) { this.buildCallback = cb; }

    public void init(int screenWidth, int screenHeight) {
        this.width = screenWidth;
        this.height = screenHeight;
        this.widgets.clear();
        this.tabWidgets.clear();
        this.libraryFilterButtons.clear();

        int headerHeight = 24;
        int tabBarHeight = 20;
        int sidebarWidth = Math.max(132, width / 5);
        int footerHeight = 18;

        // 1. Top Header Actions
        int actionBtnWidth = 48;
        int actionBtnHeight = 16;
        int actionY = 4;

        boolean flightOn = TFBClientController.getInstance().isFlightActive();
        flightButton = new UIButton(
            new UIBounds(width - (actionBtnWidth * 6 + 28), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr(flightOn ? "tfb.ui.flight_on" : "tfb.ui.flight_off"),
            b -> {
                TFBClientController.getInstance().toggleFlight();
                boolean state = TFBClientController.getInstance().isFlightActive();
                b.setLabel(TFBLocalization.tr(state ? "tfb.ui.flight_on" : "tfb.ui.flight_off"));
                setStatus(state ? "Architect Flight enabled." : "Architect Flight disabled.");
            }
        );

        previewButton = new UIButton(
            new UIBounds(width - (actionBtnWidth * 5 + 24), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr("tfb.ui.preview"),
            b -> {
                model.generatePreview();
                refreshHierarchy();
                setStatus("Preview generated (" + model.totalBlockCount() + " blocks).");
            }
        );
        instantBuildButton = new UIButton(
            new UIBounds(width - (actionBtnWidth * 4 + 20), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr("tfb.ui.build"),
            b -> {
                if (buildCallback != null) {
                    buildCallback.run();
                } else {
                    setStatus("Started construction (" + model.totalBlockCount() + " blocks).");
                }
            }
        );
        undoButton = new UIButton(
            new UIBounds(width - (actionBtnWidth * 3 + 16), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr("tfb.ui.undo"),
            b -> {
                if (model.undo()) {
                    refreshHierarchy();
                    setStatus("Reverted last edit.");
                } else {
                    setStatus("Nothing to undo.");
                }
            }
        );
        redoButton = new UIButton(
            new UIBounds(width - (actionBtnWidth * 2 + 12), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr("tfb.ui.redo"),
            b -> {
                if (model.redo()) {
                    refreshHierarchy();
                    setStatus("Redone edit.");
                } else {
                    setStatus("Nothing to redo.");
                }
            }
        );
        closeButton = new UIButton(
            new UIBounds(width - (actionBtnWidth + 8), actionY, actionBtnWidth, actionBtnHeight),
            TFBLocalization.tr("tfb.ui.close"),
            b -> {
                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
        );

        widgets.add(flightButton);
        widgets.add(previewButton);
        widgets.add(instantBuildButton);
        widgets.add(undoButton);
        widgets.add(redoButton);
        widgets.add(closeButton);

        // 2. Studio Navigation Bar
        tabBar = new UITabBar(new UIBounds(sidebarWidth + 4, headerHeight + 2, width - sidebarWidth - 8, tabBarHeight), tab -> {
            model.setActiveTab(tab);
            rebuildTabWidgets();
        });
        tabBar.setActiveTab(model.activeTab());
        widgets.add(tabBar);

        // 3. Left Sidebar: Building Hierarchy + Tool Library
        int listY = headerHeight + 20;
        int listHeight = height - listY - footerHeight - 4;

        hierarchyList = new UIScrollList<>(new UIBounds(6, listY, sidebarWidth - 8, listHeight), 15, buildHierarchyItems(), (item, idx) -> {
            String trimmed = item.trim();
            if (model.isShapeEditorMode()) {
                for (var s : model.activeArchProject().sections()) {
                    if (trimmed.contains("[" + s.id() + "]")) {
                        model.setSelectedSectionId(s.id());
                        rebuildTabWidgets();
                        setStatus("Selected " + s.id() + " (" + s.kind().name() + ", " + s.stories() + "F)");
                        return;
                    }
                }
            }
            if (model.isCityMode() && model.activeCityProject() != null) {
                for (var d : model.activeCityProject().districts()) {
                    for (var p : d.parcels()) {
                        if (trimmed.contains("[" + p.id() + "]")) {
                            model.setSelectedParcelId(p.id());
                            model.setSelectedDistrictId(d.id());
                            setStatus("Selected Parcel " + p.id() + " (" + p.purpose().name() + (p.isLocked() ? ", LOCKED" : "") + ")");
                            return;
                        }
                    }
                }
            }
            setStatus(trimmed);
        });
        widgets.add(hierarchyList);

        int catBtnY = headerHeight + 18;
        int catBtnW = (sidebarWidth - 14) / 4;
        int catBtnH = 16;

        UIButton btnAll = new UIButton(new UIBounds(6, catBtnY, catBtnW, catBtnH), "ALL", b -> filterBrushes(null));
        UIButton btnArc = new UIButton(new UIBounds(6 + catBtnW + 2, catBtnY, catBtnW, catBtnH), "ARC", b -> filterBrushes(BrushCategory.ARCHITECTURAL));
        UIButton btnStr = new UIButton(new UIBounds(6 + (catBtnW + 2) * 2, catBtnY, catBtnW, catBtnH), "STR", b -> filterBrushes(BrushCategory.STRUCTURAL));
        UIButton btnInf = new UIButton(new UIBounds(6 + (catBtnW + 2) * 3, catBtnY, catBtnW, catBtnH), "INF", b -> filterBrushes(BrushCategory.INFRASTRUCTURE));
        libraryFilterButtons.addAll(Arrays.asList(btnAll, btnArc, btnStr, btnInf));
        widgets.addAll(libraryFilterButtons);

        int brushListY = headerHeight + 38;
        int brushListHeight = height - brushListY - footerHeight - 4;
        List<IConstructionBrush> allBrushes = new ArrayList<>(BrushRegistry.all());
        brushList = new UIScrollList<>(new UIBounds(6, brushListY, sidebarWidth - 8, brushListHeight), 16, allBrushes, (brush, idx) -> {
            model.setActiveBrush(brush);
            refreshHierarchy();
            setStatus("Selected tool: " + brush.displayName());
        });
        brushList.setLabelProvider(brush -> "[" + brush.category().name().substring(0, Math.min(3, brush.category().name().length())) + "] " + brush.displayName());
        brushList.setSelectedIndex(allBrushes.indexOf(model.activeBrush()));
        widgets.add(brushList);

        // 4. Construction progress bar in footer
        constructionProgressBar = new UIProgressBar(new UIBounds(width - 180, height - footerHeight + 2, 172, footerHeight - 4));
        constructionProgressBar.setProgress(1.0f);
        constructionProgressBar.setStatusText(TFBLocalization.tr("tfb.status.preview_ready"));
        widgets.add(constructionProgressBar);

        rebuildTabWidgets();
    }

    private void filterBrushes(BrushCategory category) {
        List<IConstructionBrush> list = category == null
            ? new ArrayList<>(BrushRegistry.all())
            : BrushRegistry.byCategory(category);
        brushList.setItems(list);
        if (!list.isEmpty()) {
            model.setActiveBrush(list.get(0));
            brushList.setSelectedIndex(0);
        }
    }

    private void refreshHierarchy() {
        if (hierarchyList != null) {
            hierarchyList.setItems(buildHierarchyItems());
        }
    }

    private List<String> buildHierarchyItems() {
        List<String> items = new ArrayList<>();
        if (model.isShapeEditorMode()) {
            var proj = model.activeArchProject();
            items.add("Arch: " + proj.name() + " (" + proj.sections().size() + " sec)");
            for (var s : proj.sections()) {
                String sel = s.id().equals(model.selectedSectionId()) ? "> " : "  ";
                String lock = s.isLocked() ? " *LOCK*" : "";
                items.add(sel + "[" + s.id() + "] " + s.kind().name() + " " + s.stories() + "F" + lock);
            }
            return items;
        }
        if (model.isCityMode() && model.activeCityProject() != null) {
            var city = model.activeCityProject();
            items.add("City: " + city.name() + " (" + city.totalParcelCount() + " lots)");
            items.add("  Roads: " + city.roadNetwork().segments().size() + " | Spaces: " + city.publicSpaces().size());
            for (var d : city.districts()) {
                items.add("  [" + d.role().name().substring(0, 3) + "] " + d.name());
                for (var p : d.parcels()) {
                    String lock = p.isLocked() ? " *LOCK*" : "";
                    String sel = p.id().equals(model.selectedParcelId()) ? "> " : "    ";
                    items.add(sel + "[" + p.id() + "] " + p.purpose().name() + lock);
                }
            }
            for (var ps : city.publicSpaces()) {
                items.add("  [PUB] " + ps.name());
            }
            return items;
        }

        BuildingPlan plan = model.latestPlan();
        if (plan == null) {
            items.add("Home: " + model.width() + "x" + model.depth());
            items.add("  Tool: " + (model.activeBrush() != null ? model.activeBrush().displayName() : "None"));
            return items;
        }

        items.add("Home (" + model.width() + "x" + model.depth() + ")");
        for (BuildingPlan.FloorPlan fp : plan.floors()) {
            items.add("  " + TFBLocalization.tr("tfb.hierarchy.floor", fp.floorIndex() + 1, fp.rooms().size()));
            for (BuildingPlan.PlannedRoom room : fp.rooms()) {
                String roomName = TFBLocalization.tr("tfb.room." + room.type().name().toLowerCase(Locale.ROOT));
                items.add("    - " + roomName + " (" + room.bounds().sizeX() + "x" + room.bounds().sizeZ() + ")");
            }
        }
        for (BuildingPlan.StairwellPlan sw : plan.stairwells()) {
            items.add("  " + TFBLocalization.tr("tfb.hierarchy.stairwell", sw.fromFloor() + 1, sw.toFloor() + 1));
        }
        items.add("  " + TFBLocalization.tr("tfb.hierarchy.roof", plan.definition().roofType().name()));
        if (plan.definition().addPorchAndGarden()) {
            items.add("  " + TFBLocalization.tr("tfb.hierarchy.porch"));
        }
        return items;
    }

    private void rebuildTabWidgets() {
        tabWidgets.clear();
        refreshHierarchy();

        int sidebarWidth = Math.max(132, width / 5);
        int contentX = sidebarWidth + 12;
        int contentY = 52;
        int contentWidth = width - contentX - 12;
        int rowHeight = 20;

        WorkspaceTab tab = model.activeTab();
        boolean isLibrary = (tab == WorkspaceTab.LIBRARY);
        if (hierarchyList != null) hierarchyList.setVisible(!isLibrary);
        if (brushList != null) brushList.setVisible(isLibrary);
        for (UIWidget b : libraryFilterButtons) b.setVisible(isLibrary);

        switch (tab) {
            case HOME -> {
                // Quick-Start Home & City Templates
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 24, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.home.cottage"), b -> {
                    model.setCityMode(false);
                    applyHomeTemplate(9, 9, 1, StylePreset.RUSTIC, WingConfiguration.SINGLE_RECTANGLE);
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 24, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.home.family"), b -> {
                    model.setCityMode(false);
                    applyHomeTemplate(15, 15, 2, StylePreset.MEDIEVAL, WingConfiguration.SINGLE_RECTANGLE);
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 24, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.home.manor"), b -> {
                    model.setCityMode(false);
                    applyHomeTemplate(21, 19, 3, StylePreset.TRADITIONAL, WingConfiguration.L_SHAPED);
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 24, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.home.city"), b -> {
                    model.createNewCity("New Settlement", model.settlementPattern(), 48, 48);
                    refreshHierarchy();
                    setStatus("Generated city (" + model.activeCityProject().totalParcelCount() + " parcels).");
                    tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                }));

                // Quick Navigation & Saved Projects
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 54, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.home.open_editor"), b -> {
                    tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                }));
                List<String> saved = BlueprintStorage.listSavedNames();
                if (!saved.isEmpty()) {
                    UIDropdown<String> recentDropdown = new UIDropdown<>(
                        new UIBounds(contentX + contentWidth / 3, contentY + 54, contentWidth / 3 - 6, rowHeight),
                        "Recent", saved, 0, name -> {}
                    );
                    tabWidgets.add(recentDropdown);
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 3) * 2, contentY + 54, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.open_design"), b -> {
                        String chosen = recentDropdown.selected();
                        if (chosen != null) {
                            BlueprintStorage.load(chosen).ifPresent(bp -> {
                                model.setCityMode(false);
                                model.loadBlueprint(bp);
                                refreshHierarchy();
                                setStatus(TFBLocalization.tr("tfb.status.loaded", chosen));
                                tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                            });
                        }
                    }));
                } else {
                    tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 3, contentY + 54, (contentWidth * 2) / 3 - 6, rowHeight), TFBLocalization.tr("tfb.home.no_saved"), b -> {
                        tabBar.setActiveTab(WorkspaceTab.PROJECTS);
                    }));
                }

                // Shape-First Architectural Studio Actions (100% local procedural grammar)
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 84, contentWidth / 3 - 4, rowHeight), "Shape Sculpt Mode", b -> {
                    model.setCityMode(false);
                    model.setShapeEditorMode(true);
                    refreshHierarchy();
                    setStatus("Shape Sculpt Mode active (" + model.activeArchProject().sections().size() + " sections)");
                    tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 3, contentY + 84, contentWidth / 3 - 4, rowHeight), "Add Wing / Annex", b -> {
                    model.setCityMode(false);
                    model.setShapeEditorMode(true);
                    model.setActiveSculptTool(ArchitectWorkspaceModel.SculptTool.BUILDING);
                    var baseBox = model.activeArchProject().sections().isEmpty()
                        ? model.boundingBox()
                        : model.activeArchProject().sections().get(0).bounds();
                    var wingPos = new com.x64Dynamics.tfb.geometry.Vec3i(baseBox.max().x() + 5, baseBox.min().y(), baseBox.min().z() + 4);
                    String msg = model.sculptAt(wingPos, com.x64Dynamics.tfb.geometry.Direction.EAST, true);
                    refreshHierarchy();
                    setStatus(msg);
                    tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 3) * 2, contentY + 84, contentWidth / 3 - 4, rowHeight), "Add Tower", b -> {
                    model.setCityMode(false);
                    model.setShapeEditorMode(true);
                    model.setActiveSculptTool(ArchitectWorkspaceModel.SculptTool.TOWER);
                    var baseBox = model.activeArchProject().sections().isEmpty()
                        ? model.boundingBox()
                        : model.activeArchProject().sections().get(0).bounds();
                    var towerPos = new com.x64Dynamics.tfb.geometry.Vec3i(baseBox.min().x() - 3, baseBox.min().y(), baseBox.min().z());
                    String msg = model.sculptAt(towerPos, com.x64Dynamics.tfb.geometry.Direction.NORTH, true);
                    refreshHierarchy();
                    setStatus(msg);
                    tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
                }));
            }

            case ARCHITECTURE -> {
                if (model.isShapeEditorMode()) {
                    String sid = model.selectedSectionId();
                    var selOpt = sid != null ? model.activeArchProject().findSection(sid) : java.util.Optional.<com.x64Dynamics.tfb.architectural.editor.BuildingSection>empty();
                    int bw = contentWidth / 4 - 4;
                    tabWidgets.add(new UIButton(new UIBounds(contentX, contentY, bw, rowHeight), "Extend N +2", b -> {
                        if (sid != null && model.activeArchProject().extendSection(sid, com.x64Dynamics.tfb.geometry.Direction.NORTH, 2)) {
                            model.generatePreview(); refreshHierarchy(); setStatus("Extended " + sid + " North");
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + bw + 4, contentY, bw, rowHeight), "Extend S +2", b -> {
                        if (sid != null && model.activeArchProject().extendSection(sid, com.x64Dynamics.tfb.geometry.Direction.SOUTH, 2)) {
                            model.generatePreview(); refreshHierarchy(); setStatus("Extended " + sid + " South");
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 2, contentY, bw, rowHeight), "Extend E +2", b -> {
                        if (sid != null && model.activeArchProject().extendSection(sid, com.x64Dynamics.tfb.geometry.Direction.EAST, 2)) {
                            model.generatePreview(); refreshHierarchy(); setStatus("Extended " + sid + " East");
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 3, contentY, bw, rowHeight), "Extend W +2", b -> {
                        if (sid != null && model.activeArchProject().extendSection(sid, com.x64Dynamics.tfb.geometry.Direction.WEST, 2)) {
                            model.generatePreview(); refreshHierarchy(); setStatus("Extended " + sid + " West");
                        }
                    }));

                    tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 26, bw, rowHeight), "Stack Floor +1", b -> {
                        if (sid != null && model.activeArchProject().stackFloor(sid)) {
                            model.generatePreview(); refreshHierarchy(); setStatus("Stacked floor on " + sid);
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + bw + 4, contentY + 26, bw, rowHeight), "Cycle Roof", b -> {
                        if (selOpt.isPresent() && !selOpt.get().isLocked()) {
                            var types = RoofGenerator.RoofType.values();
                            var next = types[(selOpt.get().roofType().ordinal() + 1) % types.length];
                            selOpt.get().setRoofType(next);
                            model.generatePreview(); refreshHierarchy(); setStatus(sid + " roof: " + next.name());
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 2, contentY + 26, bw, rowHeight), selOpt.map(s -> s.isLocked() ? "Unlock" : "Lock").orElse("Lock"), b -> {
                        if (sid != null && model.activeArchProject().toggleLock(sid)) {
                            refreshHierarchy(); rebuildTabWidgets(); setStatus("Toggled lock on " + sid);
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 3, contentY + 26, bw, rowHeight), "Remove Sec", b -> {
                        if (sid != null && model.activeArchProject().removeSection(sid)) {
                            model.setSelectedSectionId(null);
                            model.generatePreview(); refreshHierarchy(); rebuildTabWidgets(); setStatus("Removed " + sid);
                        }
                    }));

                    tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 52, bw, rowHeight), "Tool: Building", b -> {
                        model.setActiveSculptTool(ArchitectWorkspaceModel.SculptTool.BUILDING);
                        setStatus("Sculpt Tool: Building");
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + bw + 4, contentY + 52, bw, rowHeight), "Tool: Porch", b -> {
                        model.setActiveSculptTool(ArchitectWorkspaceModel.SculptTool.PORCH_TERRACE);
                        setStatus("Sculpt Tool: Porch / Terrace");
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 2, contentY + 52, bw, rowHeight), "Tool: Tower", b -> {
                        model.setActiveSculptTool(ArchitectWorkspaceModel.SculptTool.TOWER);
                        setStatus("Sculpt Tool: Tower / Stack");
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (bw + 4) * 3, contentY + 52, bw, rowHeight), "Exit Sculpt", b -> {
                        model.setShapeEditorMode(false);
                        refreshHierarchy(); rebuildTabWidgets(); setStatus("Returned to Home Builder");
                    }));
                } else if (model.isCityMode() && model.activeCityProject() != null) {
                    // City Designer Controls
                    tabWidgets.add(new UISlider(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "City Width", 24, 96, model.width(), w -> {
                        model.setWidth(w);
                        if (model.activeCityProject() != null) com.x64Dynamics.tfb.city.CityPlannerEngine.planCity(model.activeCityProject(), model.sampleLiveHeightmap(model.boundingBox()));
                        model.generatePreview();
                        refreshHierarchy();
                    }));
                    tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 2 - 6, rowHeight), "City Depth", 24, 96, model.depth(), d -> {
                        model.setDepth(d);
                        if (model.activeCityProject() != null) com.x64Dynamics.tfb.city.CityPlannerEngine.planCity(model.activeCityProject(), model.sampleLiveHeightmap(model.boundingBox()));
                        model.generatePreview();
                        refreshHierarchy();
                    }));

                    List<com.x64Dynamics.tfb.city.SettlementPattern> patterns = Arrays.asList(com.x64Dynamics.tfb.city.SettlementPattern.values());
                    tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY + 26, contentWidth / 2 - 6, rowHeight), TFBLocalization.tr("tfb.city.pattern"), patterns, patterns.indexOf(model.settlementPattern()), p -> {
                        model.setSettlementPattern(p);
                        refreshHierarchy();
                    }));

                    List<StylePreset> styles = Arrays.asList(StylePreset.values());
                    tabWidgets.add(new UIDropdown<>(new UIBounds(contentX + contentWidth / 2, contentY + 26, contentWidth / 2 - 6, rowHeight), TFBLocalization.tr("tfb.ui.style"), styles, styles.indexOf(model.stylePreset()), st -> {
                        model.setStylePreset(st);
                        if (model.activeCityProject() != null) com.x64Dynamics.tfb.city.CityPlannerEngine.planCity(model.activeCityProject(), model.sampleLiveHeightmap(model.boundingBox()));
                        model.generatePreview();
                        refreshHierarchy();
                    }));

                    tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.city.regen_city"), b -> {
                        model.randomizeSeed();
                        com.x64Dynamics.tfb.city.CityPlannerEngine.planCity(model.activeCityProject());
                        model.generatePreview();
                        refreshHierarchy();
                        setStatus("Regenerated city layout.");
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.city.regen_parcel"), b -> {
                        if (model.regenerateSelectedParcel()) {
                            refreshHierarchy();
                            setStatus("Regenerated parcel " + model.selectedParcelId());
                        } else {
                            setStatus("Select an unlocked parcel in the hierarchy first.");
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.city.lock_parcel"), b -> {
                        if (model.toggleSelectedParcelLock()) {
                            refreshHierarchy();
                            setStatus("Toggled lock on parcel " + model.selectedParcelId());
                        } else {
                            setStatus("Select a parcel in the hierarchy first.");
                        }
                    }));
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.city.single_mode"), b -> {
                        model.setCityMode(false);
                        rebuildTabWidgets();
                    }));
                    break;
                }
                // Row 1: Essential Home Dimensions & Floors
                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.width"), 7, 48, model.width(), w -> {
                    model.setWidth(w);
                    refreshHierarchy();
                }));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 3, contentY, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.depth"), 7, 48, model.depth(), d -> {
                    model.setDepth(d);
                    refreshHierarchy();
                }));
                tabWidgets.add(new UISlider(new UIBounds(contentX + (contentWidth / 3) * 2, contentY, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.floors"), 0, 4, model.requestedStories(), s -> {
                    model.setRequestedStories(s);
                    refreshHierarchy();
                }));

                // Row 2: Style, Roof & Footprint Layout
                List<StylePreset> styles = Arrays.asList(StylePreset.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY + 26, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.style"), styles, styles.indexOf(model.stylePreset()), st -> {
                    model.setStylePreset(st);
                    refreshHierarchy();
                }));

                List<RoofGenerator.RoofType> roofShapes = Arrays.asList(RoofGenerator.RoofType.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX + contentWidth / 3, contentY + 26, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.roof"), roofShapes, roofShapes.indexOf(model.roofShape()), rf -> {
                    model.setRoofShape(rf);
                    refreshHierarchy();
                }));

                List<WingConfiguration> wingConfigs = Arrays.asList(WingConfiguration.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX + (contentWidth / 3) * 2, contentY + 26, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.layout"), wingConfigs, wingConfigs.indexOf(model.wingConfiguration()), wc -> {
                    model.setWingConfiguration(wc);
                    refreshHierarchy();
                }));

                // Row 3: Essential Home Features & Variation
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.furnish"), model.furnishRooms(), v -> {
                    model.setFurnishRooms(v);
                    refreshHierarchy();
                }));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + contentWidth / 4, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.porch_garden"), model.addPorchAndGarden(), v -> {
                    model.setAddPorchAndGarden(v);
                    refreshHierarchy();
                }));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.chimney"), model.addChimney(), v -> {
                    model.setAddChimney(v);
                    refreshHierarchy();
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 52, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.new_variation"), b -> {
                    model.randomizeSeed();
                    refreshHierarchy();
                    setStatus("Generated new home variation (seed " + model.proceduralSeed() + ").");
                }));

                // Row 4: Integrated World Selection Actions
                WorldSelectionManager sel = model.selectionManager();
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 78, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.expand"), b -> {
                    model.expandSelection(1);
                    rebuildTabWidgets();
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 78, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.contract"), b -> {
                    model.contractSelection(1);
                    rebuildTabWidgets();
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 78, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.center"), b -> {
                    model.centerSelection();
                    rebuildTabWidgets();
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 78, contentWidth / 4 - 4, rowHeight), TFBLocalization.tr("tfb.ui.clear"), b -> {
                    model.clearSelection();
                    rebuildTabWidgets();
                }));
            }

            case MATERIALS -> {
                List<StylePreset> styles = Arrays.asList(StylePreset.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), TFBLocalization.tr("tfb.ui.style"), styles, styles.indexOf(model.stylePreset()), preset -> {
                    model.setStylePreset(preset);
                    rebuildTabWidgets();
                    setStatus("Applied style: " + preset.displayName());
                }));

                int discoveredCount = BlockTaxonomyRegistry.getInstance().all().size();
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 2 - 6, rowHeight),
                    "Discovered Blocks: " + discoveredCount + " (Harmony: " + Math.round(model.paletteHarmonyScore() * 100) + "%)", b -> {}));

                materialSearchField = new UITextField(new UIBounds(contentX, contentY + 26, contentWidth / 2 - 6, rowHeight), "", "Search vanilla & modded blocks...", null);
                tabWidgets.add(materialSearchField);

                List<BlockTaxonomyEntry> results = SearchableTaxonomyBrowser.search(materialSearchField.text(), null, null, null);
                UIScrollList<BlockTaxonomyEntry> matList = new UIScrollList<>(
                    new UIBounds(contentX, contentY + 50, contentWidth - 6, 56), 14, results,
                    (entry, idx) -> {
                        model.activePalette().set(MaterialRole.WALL_PRIMARY, entry.block());
                        model.generatePreview();
                        setStatus("Overrode primary wall: " + entry.block().asString());
                    }
                );
                matList.setLabelProvider(e -> e.block().asString() + " [" + e.substance().name() + "]");
                tabWidgets.add(matList);

                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 2, contentY + 26, contentWidth / 2 - 6, rowHeight), "Search / Reset", b -> {
                    matList.setItems(SearchableTaxonomyBrowser.search(materialSearchField.text(), null, null, null));
                }));
            }

            case LIBRARY -> {
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY, contentWidth - 6, rowHeight),
                    "Active Tool: " + (model.activeBrush() != null ? model.activeBrush().displayName() : "None") + " — Select a category & tool on the left.", b -> {}));
                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY + 28, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.width"), 3, 64, model.width(), model::setWidth));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 3, contentY + 28, contentWidth / 3 - 6, rowHeight), "Height", 3, 48, model.height(), model::setHeight));
                tabWidgets.add(new UISlider(new UIBounds(contentX + (contentWidth / 3) * 2, contentY + 28, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.depth"), 3, 64, model.depth(), model::setDepth));
            }

            case PROJECTS -> {
                blueprintNameField = new UITextField(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "My Home Design", "Design Name", null);
                blueprintAuthorField = new UITextField(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 4 - 6, rowHeight), "KyroXova", "Author", null);
                tabWidgets.add(blueprintNameField);
                tabWidgets.add(blueprintAuthorField);

                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY, contentWidth / 4 - 6, rowHeight), TFBLocalization.tr("tfb.ui.save_design"), b -> {
                    try {
                        if (model.isShapeEditorMode()) {
                            model.activeArchProject().setName(blueprintNameField.text());
                            com.x64Dynamics.tfb.architectural.editor.ArchitecturalProjectStorage.save(model.activeArchProject());
                            setStatus(TFBLocalization.tr("tfb.status.saved", model.activeArchProject().name()));
                        } else if (model.isCityMode() && model.activeCityProject() != null) {
                            model.activeCityProject().setName(blueprintNameField.text());
                            model.activeCityProject().setAuthor(blueprintAuthorField.text());
                            com.x64Dynamics.tfb.city.WorldProjectStorage.save(model.activeCityProject());
                            setStatus(TFBLocalization.tr("tfb.status.saved", model.activeCityProject().name()));
                        } else {
                            ParametricBlueprint bp = model.toBlueprint(blueprintNameField.text(), blueprintAuthorField.text());
                            BlueprintStorage.save(bp);
                            setStatus(TFBLocalization.tr("tfb.status.saved", bp.metadata().name()));
                        }
                        rebuildTabWidgets();
                    } catch (Exception ex) {
                        setStatus("Save failed: " + ex.getMessage());
                    }
                }));

                List<String> savedCities = com.x64Dynamics.tfb.city.WorldProjectStorage.listSavedProjects();
                if (!savedCities.isEmpty()) {
                    UIDropdown<String> cityDropdown = new UIDropdown<>(
                        new UIBounds(contentX, contentY + 56, (contentWidth * 3) / 4 - 6, rowHeight),
                        "Saved Cities", savedCities, 0, name -> {}
                    );
                    tabWidgets.add(cityDropdown);
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth * 3) / 4, contentY + 56, contentWidth / 4 - 6, rowHeight), TFBLocalization.tr("tfb.city.open_project"), b -> {
                        String chosen = cityDropdown.selected();
                        if (chosen != null) {
                            com.x64Dynamics.tfb.city.WorldProjectStorage.load(chosen).ifPresent(proj -> {
                                model.loadWorldProject(proj);
                                setStatus(TFBLocalization.tr("tfb.status.loaded", chosen));
                                rebuildTabWidgets();
                            });
                        }
                    }));
                }

                List<String> savedNames = BlueprintStorage.listSavedNames();
                if (!savedNames.isEmpty()) {
                    UIDropdown<String> savedDropdown = new UIDropdown<>(
                        new UIBounds(contentX, contentY + 28, (contentWidth * 3) / 4 - 6, rowHeight),
                        "Saved Designs", savedNames, 0, name -> {}
                    );
                    tabWidgets.add(savedDropdown);
                    tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth * 3) / 4, contentY + 28, contentWidth / 4 - 6, rowHeight), TFBLocalization.tr("tfb.ui.open_design"), b -> {
                        String chosen = savedDropdown.selected();
                        if (chosen != null) {
                            BlueprintStorage.load(chosen).ifPresentOrElse(bp -> {
                                model.loadBlueprint(bp);
                                setStatus(TFBLocalization.tr("tfb.status.loaded", chosen));
                                rebuildTabWidgets();
                            }, () -> setStatus("Could not load design: " + chosen));
                        }
                    }));
                } else {
                    tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 28, contentWidth - 6, rowHeight), TFBLocalization.tr("tfb.home.no_saved"), b -> {}));
                }
            }

            case SETTINGS -> {
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY, contentWidth / 3 - 6, rowHeight), "Architect Flight", TFBClientController.getInstance().isFlightActive(), active -> {
                    TFBClientController.getInstance().setFlightActive(active);
                    if (flightButton != null) flightButton.setLabel(TFBLocalization.tr(active ? "tfb.ui.flight_on" : "tfb.ui.flight_off"));
                }));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 3, contentY, contentWidth / 3 - 6, rowHeight), "Flight Speed", 1, 4, Math.round(TFBClientController.getInstance().getFlightSpeed()), speed -> {
                    TFBClientController.getInstance().setFlightSpeed(speed);
                }));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 3) * 2, contentY, contentWidth / 3 - 6, rowHeight), "Noclip Camera", TFBClientController.getInstance().isNoclip(), TFBClientController.getInstance()::setNoclip));

                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY + 28, contentWidth / 3 - 6, rowHeight), TFBLocalization.tr("tfb.ui.grid_snap"), 1, 16, model.selectionManager().gridSnap(), model.selectionManager()::setGridSnap));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 3, contentY + 28, (contentWidth * 2) / 3 - 6, rowHeight), "B=Studio | H=Flight | P=Preview | G=Build | Ctrl+Z/Y=Undo/Redo", b -> {}));
            }
        }
    }

    private void applyHomeTemplate(int w, int d, int stories, StylePreset style, WingConfiguration wing) {
        model.setActiveBrush(new ArchitecturalBrushes.HouseBrush());
        model.setWidth(w);
        model.setDepth(d);
        model.setRequestedStories(stories);
        model.setStylePreset(style);
        model.setWingConfiguration(wing);
        model.generatePreview();
        refreshHierarchy();
        setStatus("Created " + style.displayName() + " home (" + w + "x" + d + ", " + stories + "F).");
        tabBar.setActiveTab(WorkspaceTab.ARCHITECTURE);
    }

    public void setStatus(String message) {
        this.statusMessage = message;
        this.statusMessageExpiry = System.currentTimeMillis() + 4000;
    }

    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        int sidebarWidth = Math.max(132, width / 5);
        int headerHeight = 24;
        int tabBarHeight = 22;
        int tabContentHeight = 114;
        int footerHeight = 18;

        // Header Bar
        graphics.fill(0, 0, width, headerHeight, 0xEE0F172A);
        graphics.drawBorder(0, 0, width, headerHeight, 0xFF1E293B);

        // Tab Bar Area
        graphics.fill(sidebarWidth, headerHeight, width, headerHeight + tabBarHeight, 0xDD0B132B);
        graphics.drawBorder(sidebarWidth, headerHeight, width - sidebarWidth, tabBarHeight, 0xFF1E293B);

        // Tab Content Floating Shelf (translucent dock leaving center world 3D preview visible)
        if (!tabWidgets.isEmpty()) {
            graphics.fill(sidebarWidth + 2, headerHeight + tabBarHeight, width - 4, headerHeight + tabBarHeight + tabContentHeight, 0xD00F172A);
            graphics.drawBorder(sidebarWidth + 2, headerHeight + tabBarHeight, width - sidebarWidth - 6, tabContentHeight, 0xFF25334D);
        }

        // Left Sidebar (Building Hierarchy or Tool Library)
        graphics.fill(0, headerHeight, sidebarWidth, height - footerHeight, 0xEE0B132B);
        graphics.drawBorder(0, headerHeight, sidebarWidth, height - headerHeight - footerHeight, 0xFF1E293B);

        // Footer Bar
        graphics.fill(0, height - footerHeight, width, height, 0xEE0B0F19);
        graphics.drawBorder(0, height - footerHeight, width, footerHeight, 0xFF1E293B);

        // Header Title & Badges
        graphics.drawText(TFBLocalization.tr("tfb.studio.title"), 8, 8, 0xFF38BDF8, true);
        String sizeBadge = model.width() + "x" + model.depth() + " | " + model.generatedStories() + "F | " + model.generatedRoomCount() + "R";
        graphics.drawText("[" + sizeBadge + "]", 78, 8, 0xFF94A3B8, false);
        graphics.drawText(model.totalBlockCount() + " blks", 180, 8, 0xFF34D399, false);

        // Sidebar Header
        String sidebarTitle = model.activeTab() == WorkspaceTab.LIBRARY
            ? TFBLocalization.tr("tfb.nav.library").toUpperCase(Locale.ROOT)
            : TFBLocalization.tr("tfb.hierarchy.title");
        graphics.drawText(sidebarTitle, 8, headerHeight + 6, 0xFF64748B, false);

        // Home Dashboard Welcome Banner
        if (model.activeTab() == WorkspaceTab.HOME) {
            int cx = sidebarWidth + 12;
            graphics.drawText(TFBLocalization.tr("tfb.home.welcome"), cx, 50, 0xFFF8FAFC, true);
            graphics.drawText(TFBLocalization.tr("tfb.home.subtitle"), cx, 62, 0xFF94A3B8, false);
            BuildingPlan plan = model.latestPlan();
            if (plan != null) {
                String validStr = TFBLocalization.tr("tfb.status.valid", plan.validation().reachableRoomCount(), plan.validation().totalRoomCount());
                graphics.drawText(TFBLocalization.tr("tfb.home.active_project") + ": " + validStr, cx, 134, 0xFF34D399, false);
            }
        }

        // Render main widgets & active tab widgets
        for (UIWidget widget : widgets) {
            widget.render(graphics, mouseX, mouseY);
        }
        for (UIWidget widget : tabWidgets) {
            widget.render(graphics, mouseX, mouseY);
        }

        // Footer Status
        String status = (System.currentTimeMillis() < statusMessageExpiry)
            ? statusMessage
            : "Pos: " + model.origin() + " | " + TFBLocalization.tr("tfb.status.ready");
        graphics.drawText(status, 8, height - footerHeight + 5, 0xFFCBD5E1, false);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        for (UIWidget widget : tabWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) return true;
        }
        for (UIWidget widget : widgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        for (UIWidget widget : tabWidgets) {
            if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        for (UIWidget widget : widgets) {
            if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        for (UIWidget widget : tabWidgets) {
            if (widget.mouseReleased(mouseX, mouseY, button)) return true;
        }
        for (UIWidget widget : widgets) {
            if (widget.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (UIWidget widget : tabWidgets) {
            if (widget.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        for (UIWidget widget : widgets) {
            if (widget.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        for (UIWidget widget : tabWidgets) {
            if (widget.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }
}
