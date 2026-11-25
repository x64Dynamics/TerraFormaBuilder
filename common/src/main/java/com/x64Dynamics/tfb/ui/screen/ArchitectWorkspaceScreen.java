package com.x64Dynamics.tfb.ui.screen;

import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.brush.BrushRegistry;
import com.x64Dynamics.tfb.brush.IConstructionBrush;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.persistence.ParametricBlueprint;
import com.x64Dynamics.tfb.preview.PreviewFilterMode;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyEntry;
import com.x64Dynamics.tfb.taxonomy.SearchableTaxonomyBrowser;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.ui.model.ArchitectWorkspaceModel;
import com.x64Dynamics.tfb.ui.model.WorkspaceTab;
import com.x64Dynamics.tfb.ui.rendering.IGuiGraphicsContext;
import com.x64Dynamics.tfb.ui.widget.UIButton;
import com.x64Dynamics.tfb.ui.widget.UIDropdown;
import com.x64Dynamics.tfb.ui.widget.UIProgressBar;
import com.x64Dynamics.tfb.ui.widget.UIScrollList;
import com.x64Dynamics.tfb.ui.widget.UISlider;
import com.x64Dynamics.tfb.ui.widget.UITabBar;
import com.x64Dynamics.tfb.ui.widget.UITextField;
import com.x64Dynamics.tfb.ui.widget.UIToggleButton;
import com.x64Dynamics.tfb.ui.widget.UIWidget;
import com.x64Dynamics.tfb.ui.widget.UIBounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private UIScrollList<IConstructionBrush> brushList;

    // Construction progress
    private UIProgressBar constructionProgressBar;

    // Search & Blueprint fields
    private UITextField blueprintNameField;
    private UITextField blueprintAuthorField;
    private UITextField materialSearchField;
    private UITextField selectionNameField;

    // Callbacks
    private Runnable closeCallback;
    private Runnable buildCallback;

    // Status message
    private String statusMessage = "Ready. Configure architectural parameters and preview in-world.";
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

        int headerHeight = 24;
        int tabBarHeight = 20;
        int sidebarWidth = Math.max(120, width / 5);
        int footerHeight = 18;

        // 1. Top Header Actions
        int actionBtnWidth = 52;
        int actionBtnHeight = 16;
        int actionY = 4;

        boolean flightOn = com.x64Dynamics.tfb.ui.TFBClientController.getInstance().isFlightActive();
        flightButton = new UIButton(new UIBounds(width - (actionBtnWidth * 6 + 28), actionY, actionBtnWidth, actionBtnHeight), flightOn ? "Flight: ON" : "Flight: OFF", b -> {
            com.x64Dynamics.tfb.ui.TFBClientController.getInstance().toggleFlight();
            boolean state = com.x64Dynamics.tfb.ui.TFBClientController.getInstance().isFlightActive();
            b.setLabel(state ? "Flight: ON" : "Flight: OFF");
            setStatus(state ? "Architect Flight enabled." : "Architect Flight disabled.");
        });

        previewButton = new UIButton(new UIBounds(width - (actionBtnWidth * 5 + 24), actionY, actionBtnWidth, actionBtnHeight), "Preview", b -> {
            model.generatePreview();
            setStatus("Generated preview with " + model.totalBlockCount() + " blocks.");
        });
        instantBuildButton = new UIButton(new UIBounds(width - (actionBtnWidth * 4 + 20), actionY, actionBtnWidth, actionBtnHeight), "Build", b -> {
            if (buildCallback != null) {
                buildCallback.run();
            } else {
                setStatus("Started construction (" + model.totalBlockCount() + " blocks).");
            }
        });
        undoButton = new UIButton(new UIBounds(width - (actionBtnWidth * 3 + 16), actionY, actionBtnWidth, actionBtnHeight), "Undo", b -> {
            if (model.undo()) setStatus("Reverted last edit.");
            else setStatus("Nothing to undo.");
        });
        redoButton = new UIButton(new UIBounds(width - (actionBtnWidth * 2 + 12), actionY, actionBtnWidth, actionBtnHeight), "Redo", b -> {
            if (model.redo()) setStatus("Redone edit.");
            else setStatus("Nothing to redo.");
        });
        closeButton = new UIButton(new UIBounds(width - (actionBtnWidth + 8), actionY, actionBtnWidth, actionBtnHeight), "Close", b -> {
            if (closeCallback != null) {
                closeCallback.run();
            }
        });

        widgets.add(flightButton);
        widgets.add(previewButton);
        widgets.add(instantBuildButton);
        widgets.add(undoButton);
        widgets.add(redoButton);
        widgets.add(closeButton);

        // 2. Tab Bar
        tabBar = new UITabBar(new UIBounds(sidebarWidth + 4, headerHeight + 2, width - sidebarWidth - 8, tabBarHeight), tab -> {
            model.setActiveTab(tab);
            rebuildTabWidgets();
        });
        tabBar.setActiveTab(model.activeTab());
        widgets.add(tabBar);

        // 3. Left Sidebar (Category Filter & Brush List)
        int catBtnY = headerHeight + 2;
        int catBtnW = (sidebarWidth - 14) / 4;
        int catBtnH = 18;

        widgets.add(new UIButton(new UIBounds(6, catBtnY, catBtnW, catBtnH), "ALL", b -> {
            List<IConstructionBrush> list = new ArrayList<>(BrushRegistry.all());
            brushList.setItems(list);
            if (!list.isEmpty()) {
                model.setActiveBrush(list.get(0));
                brushList.setSelectedIndex(0);
            }
        }));
        widgets.add(new UIButton(new UIBounds(6 + catBtnW + 2, catBtnY, catBtnW, catBtnH), "ARC", b -> {
            List<IConstructionBrush> list = BrushRegistry.byCategory(com.x64Dynamics.tfb.brush.BrushCategory.ARCHITECTURAL);
            brushList.setItems(list);
            if (!list.isEmpty()) {
                model.setActiveBrush(list.get(0));
                brushList.setSelectedIndex(0);
            }
        }));
        widgets.add(new UIButton(new UIBounds(6 + (catBtnW + 2) * 2, catBtnY, catBtnW, catBtnH), "STR", b -> {
            List<IConstructionBrush> list = BrushRegistry.byCategory(com.x64Dynamics.tfb.brush.BrushCategory.STRUCTURAL);
            brushList.setItems(list);
            if (!list.isEmpty()) {
                model.setActiveBrush(list.get(0));
                brushList.setSelectedIndex(0);
            }
        }));
        widgets.add(new UIButton(new UIBounds(6 + (catBtnW + 2) * 3, catBtnY, catBtnW, catBtnH), "INF", b -> {
            List<IConstructionBrush> list = BrushRegistry.byCategory(com.x64Dynamics.tfb.brush.BrushCategory.INFRASTRUCTURE);
            brushList.setItems(list);
            if (!list.isEmpty()) {
                model.setActiveBrush(list.get(0));
                brushList.setSelectedIndex(0);
            }
        }));

        int brushListY = headerHeight + 24;
        int brushListHeight = height - brushListY - footerHeight - 4;

        List<IConstructionBrush> allBrushes = new ArrayList<>(BrushRegistry.all());
        brushList = new UIScrollList<>(new UIBounds(6, brushListY, sidebarWidth - 8, brushListHeight), 16, allBrushes, (brush, idx) -> {
            model.setActiveBrush(brush);
            setStatus("Selected brush: " + brush.displayName());
        });
        brushList.setLabelProvider(brush -> "[" + brush.category().name().substring(0, Math.min(3, brush.category().name().length())) + "] " + brush.displayName());
        brushList.setSelectedIndex(allBrushes.indexOf(model.activeBrush()));
        widgets.add(brushList);

        // 4. Construction progress bar in footer
        constructionProgressBar = new UIProgressBar(new UIBounds(width - 180, height - footerHeight + 2, 172, footerHeight - 4));
        constructionProgressBar.setProgress(1.0f);
        constructionProgressBar.setStatusText("Preview Ready");
        widgets.add(constructionProgressBar);

        // Build active tab widgets
        rebuildTabWidgets();
    }

    private void rebuildTabWidgets() {
        tabWidgets.clear();
        int contentX = Math.max(120, width / 5) + 12;
        int contentY = 52;
        int contentWidth = width - contentX - 12;
        int rowHeight = 20;

        WorkspaceTab tab = model.activeTab();

        switch (tab) {
            case ARCHITECTURE -> {
                // Dimensions
                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY, contentWidth / 3 - 6, rowHeight), "Width", 3, 64, model.width(), model::setWidth));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 3, contentY, contentWidth / 3 - 6, rowHeight), "Height", 3, 48, model.height(), model::setHeight));
                tabWidgets.add(new UISlider(new UIBounds(contentX + (contentWidth / 3) * 2, contentY, contentWidth / 3 - 6, rowHeight), "Depth", 3, 64, model.depth(), model::setDepth));

                // Wings & Roof
                List<WingConfiguration> wingConfigs = Arrays.asList(WingConfiguration.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY + 28, contentWidth / 2 - 6, rowHeight), "Wing Layout", wingConfigs, wingConfigs.indexOf(model.wingConfiguration()), model::setWingConfiguration));

                List<RoofGenerator.RoofType> roofShapes = Arrays.asList(RoofGenerator.RoofType.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX + contentWidth / 2, contentY + 28, contentWidth / 2 - 6, rowHeight), "Roof Style", roofShapes, roofShapes.indexOf(model.roofShape()), model::setRoofShape));

                // Features toggles
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY + 56, contentWidth / 5 - 4, rowHeight), "Rhythm", model.useFacadeRhythm(), model::setUseFacadeRhythm));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + contentWidth / 5, contentY + 56, contentWidth / 5 - 4, rowHeight), "Pilasters", model.addPilasters(), model::setAddPilasters));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 5) * 2, contentY + 56, contentWidth / 5 - 4, rowHeight), "Shutters", model.addWindowShutters(), model::setAddWindowShutters));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 5) * 3, contentY + 56, contentWidth / 5 - 4, rowHeight), "Balconies", model.addBalconies(), model::setAddBalconies));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 5) * 4, contentY + 56, contentWidth / 5 - 4, rowHeight), "Dormers", model.addDormers(), model::setAddDormers));
            }

            case SELECTION -> {
                com.x64Dynamics.tfb.editing.WorldSelectionManager sel = model.selectionManager();
                String p1Str = sel.pos1() != null ? "(" + sel.pos1().x() + "," + sel.pos1().y() + "," + sel.pos1().z() + ")" : "None";
                String p2Str = sel.pos2() != null ? "(" + sel.pos2().x() + "," + sel.pos2().y() + "," + sel.pos2().z() + ")" : "None";

                // Row 1: POS1 and POS2 indicators & setters
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY, contentWidth / 4 - 4, rowHeight), "POS1: " + p1Str, b -> {
                    model.setPos1(model.origin());
                    rebuildTabWidgets();
                    setStatus("POS1 set to origin: " + model.origin());
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY, contentWidth / 4 - 4, rowHeight), "Clear POS1", b -> {
                    sel.clearPositions();
                    rebuildTabWidgets();
                    setStatus("Cleared POS1.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY, contentWidth / 4 - 4, rowHeight), "POS2: " + p2Str, b -> {
                    model.setPos2(model.boundingBox().max());
                    rebuildTabWidgets();
                    setStatus("POS2 set to max bounds: " + model.boundingBox().max());
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY, contentWidth / 4 - 4, rowHeight), "Clear POS2", b -> {
                    sel.clearPositions();
                    rebuildTabWidgets();
                    setStatus("Cleared POS2.");
                }));

                // Row 2: Manual Coordinate Inputs for POS1 and POS2
                String p1Coord = sel.pos1() != null ? sel.pos1().x() + " " + sel.pos1().y() + " " + sel.pos1().z() : "";
                UITextField p1Field = new UITextField(new UIBounds(contentX, contentY + 28, contentWidth / 4 - 4, rowHeight), p1Coord, "POS1 (X Y Z)", null);
                tabWidgets.add(p1Field);

                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 28, contentWidth / 4 - 4, rowHeight), "Apply POS1", b -> {
                    com.x64Dynamics.tfb.geometry.Vec3i p = parseVec3i(p1Field.text());
                    if (p != null) {
                        model.setPos1(p);
                        rebuildTabWidgets();
                        setStatus("Applied POS1: " + p);
                    } else {
                        setStatus("Invalid format: use 'X Y Z'");
                    }
                }));

                String p2Coord = sel.pos2() != null ? sel.pos2().x() + " " + sel.pos2().y() + " " + sel.pos2().z() : "";
                UITextField p2Field = new UITextField(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 28, contentWidth / 4 - 4, rowHeight), p2Coord, "POS2 (X Y Z)", null);
                tabWidgets.add(p2Field);

                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 28, contentWidth / 4 - 4, rowHeight), "Apply POS2", b -> {
                    com.x64Dynamics.tfb.geometry.Vec3i p = parseVec3i(p2Field.text());
                    if (p != null) {
                        model.setPos2(p);
                        rebuildTabWidgets();
                        setStatus("Applied POS2: " + p);
                    } else {
                        setStatus("Invalid format: use 'X Y Z'");
                    }
                }));

                // Row 3: Region operations (Expand, Contract, Center, Clear)
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 56, contentWidth / 4 - 4, rowHeight), "Expand (+1)", b -> {
                    model.expandSelection(1);
                    rebuildTabWidgets();
                    setStatus("Expanded selection by 1 block.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 56, contentWidth / 4 - 4, rowHeight), "Contract (-1)", b -> {
                    model.contractSelection(1);
                    rebuildTabWidgets();
                    setStatus("Contracted selection by 1 block.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 56, contentWidth / 4 - 4, rowHeight), "Center Origin", b -> {
                    model.centerSelection();
                    rebuildTabWidgets();
                    setStatus("Centered origin inside selection.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 56, contentWidth / 4 - 4, rowHeight), "Clear All", b -> {
                    model.clearSelection();
                    rebuildTabWidgets();
                    setStatus("Cleared entire selection.");
                }));

                // Row 4: Selection Stats & Grid Snap
                String info = "Size: " + model.width() + "x" + model.height() + "x" + model.depth() + " | Vol: " + sel.volume() + " blocks";
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 84, contentWidth / 2 - 4, rowHeight), info, b -> {}));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 2, contentY + 84, contentWidth / 2 - 4, rowHeight), "Grid Snap", 1, 16, sel.gridSnap(), sel::setGridSnap));
            }

            case MATERIALS -> {
                List<StylePreset> presets = Arrays.asList(StylePreset.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "Style Preset", presets, presets.indexOf(model.stylePreset()), model::setStylePreset));

                materialSearchField = new UITextField(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 2 - 6, rowHeight), "", "Search blocks...", q -> {
                    List<BlockTaxonomyEntry> results = SearchableTaxonomyBrowser.search(q, null, null, null);
                    setStatus("Found " + results.size() + " taxonomy blocks matching '" + q + "'.");
                });
                tabWidgets.add(materialSearchField);

                int roleY = contentY + 28;
                MaterialRole[] displayRoles = { MaterialRole.FOUNDATION, MaterialRole.WALL_PRIMARY, MaterialRole.ROOF_SLOPE, MaterialRole.PILLAR };
                for (int i = 0; i < displayRoles.length; i++) {
                    MaterialRole role = displayRoles[i];
                    String blockName = model.activePalette().get(role).toString();
                    tabWidgets.add(new UIButton(new UIBounds(contentX, roleY + i * 24, contentWidth - 12, rowHeight), role.name() + ": " + blockName, b -> {
                        setStatus("Role: " + role.name() + " | Harmony: " + Math.round(model.paletteHarmonyScore() * 100) + "%");
                    }));
                }
            }

            case TERRAIN -> {
                List<FoundationStrategy> strategies = Arrays.asList(FoundationStrategy.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY, contentWidth - 12, rowHeight), "Foundation Strategy", strategies, strategies.indexOf(model.foundationStrategy()), model::setFoundationStrategy));

                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY + 28, contentWidth / 2 - 6, rowHeight), "Preserve Natural Terrain", model.preserveTerrain(), model::setPreserveTerrain));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + contentWidth / 2, contentY + 28, contentWidth / 2 - 6, rowHeight), "Clear Vegetation", model.clearVegetation(), model::setClearVegetation));
            }

            case INTERIORS -> {
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "Generate Room Floorplans", model.enableInteriors(), model::setEnableInteriors));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 2 - 6, rowHeight), "Procedural Furnishing", model.furnishRooms(), model::setFurnishRooms));
                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY + 28, contentWidth - 12, rowHeight), "Furnishing Density (%)", 10, 100, (int) (model.furnishingDensity() * 100), val -> model.setFurnishingDensity(val / 100.0f)));
            }

            case CONSTRUCTION -> {
                List<PreviewFilterMode> filterModes = Arrays.asList(PreviewFilterMode.values());
                tabWidgets.add(new UIDropdown<>(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "Preview Layer", filterModes, filterModes.indexOf(model.previewBuffer().filterMode()), model.previewBuffer()::setFilterMode));

                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 2 - 6, rowHeight), "Instant Build", model.instantBuild(), model::setInstantBuild));

                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY + 28, contentWidth / 2 - 6, rowHeight), "Rate (blocks/t)", 1, 512, model.blocksPerTick(), model::setBlocksPerTick));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 2, contentY + 28, contentWidth / 2 - 6, rowHeight), "Tick Budget (ms)", 1, 50, model.tickBudgetMs(), model::setTickBudgetMs));

                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 56, contentWidth / 3 - 6, rowHeight), "Pause Build", b -> setStatus("Build paused.")));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 3, contentY + 56, contentWidth / 3 - 6, rowHeight), "Resume Build", b -> setStatus("Build resumed.")));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 3) * 2, contentY + 56, contentWidth / 3 - 6, rowHeight), "Cancel Build", b -> setStatus("Build cancelled.")));
            }

            case EDITING -> {
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY, contentWidth / 4 - 6, rowHeight), "Rotate 90° CW", b -> {
                    model.setRotation(model.rotation() == Rotation.NONE ? Rotation.CLOCKWISE_90 : (model.rotation() == Rotation.CLOCKWISE_90 ? Rotation.CLOCKWISE_180 : (model.rotation() == Rotation.CLOCKWISE_180 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.NONE)));
                    setStatus("Rotated design.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY, contentWidth / 4 - 6, rowHeight), "Mirror X", b -> {
                    model.setMirrorAxis(model.mirrorAxis() == Axis.X ? null : Axis.X);
                    setStatus("Toggled X mirror.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY, contentWidth / 4 - 6, rowHeight), "Mirror Z", b -> {
                    model.setMirrorAxis(model.mirrorAxis() == Axis.Z ? null : Axis.Z);
                    setStatus("Toggled Z mirror.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY, contentWidth / 4 - 6, rowHeight), "Reset Transform", b -> {
                    model.setRotation(Rotation.NONE);
                    model.setMirrorAxis(null);
                    setStatus("Reset transforms.");
                }));

                tabWidgets.add(new UISlider(new UIBounds(contentX, contentY + 28, contentWidth / 2 - 6, rowHeight), "Grid Snap", 1, 16, model.selectionManager().gridSnap(), model.selectionManager()::setGridSnap));
            }

            case BLUEPRINTS -> {
                blueprintNameField = new UITextField(new UIBounds(contentX, contentY, contentWidth / 2 - 6, rowHeight), "My Architectural Design", "Design Name", null);
                blueprintAuthorField = new UITextField(new UIBounds(contentX + contentWidth / 2, contentY, contentWidth / 4 - 6, rowHeight), "KyroXova", "Author", null);
                tabWidgets.add(blueprintNameField);
                tabWidgets.add(blueprintAuthorField);

                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY, contentWidth / 4 - 6, rowHeight), "Save Design", b -> {
                    ParametricBlueprint bp = model.toBlueprint(blueprintNameField.text(), blueprintAuthorField.text());
                    setStatus("Saved blueprint: " + bp.metadata().name());
                }));
            }

            case SETTINGS -> {
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX, contentY, contentWidth / 3 - 6, rowHeight), "Architect Flight", com.x64Dynamics.tfb.ui.TFBClientController.getInstance().isFlightActive(), active -> {
                    com.x64Dynamics.tfb.ui.TFBClientController.getInstance().setFlightActive(active);
                    if (flightButton != null) flightButton.setLabel(active ? "Flight: ON" : "Flight: OFF");
                    setStatus(active ? "Architect Flight activated." : "Architect Flight paused.");
                }));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 3, contentY, contentWidth / 3 - 6, rowHeight), "Flight Speed", 1, 4, Math.round(com.x64Dynamics.tfb.ui.TFBClientController.getInstance().getFlightSpeed()), speed -> {
                    com.x64Dynamics.tfb.ui.TFBClientController.getInstance().setFlightSpeed(speed);
                    setStatus("Flight speed set to " + speed + "x.");
                }));
                tabWidgets.add(new UIToggleButton(new UIBounds(contentX + (contentWidth / 3) * 2, contentY, contentWidth / 3 - 6, rowHeight), "Noclip Camera", com.x64Dynamics.tfb.ui.TFBClientController.getInstance().isNoclip(), noclip -> {
                    com.x64Dynamics.tfb.ui.TFBClientController.getInstance().setNoclip(noclip);
                    setStatus(noclip ? "Noclip free-camera activated." : "Normal collision restored.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 28, contentWidth - 6, rowHeight), "Hotkeys: B=Workspace | H=Flight | P=Preview | G=Build | [/]=Speed | Ctrl+Z/Y=Undo/Redo", b -> {}));
            }
        }
    }

    private com.x64Dynamics.tfb.geometry.Vec3i parseVec3i(String str) {
        if (str == null) return null;
        String[] parts = str.replaceAll("[(),]", " ").trim().split("\\s+");
        if (parts.length >= 3) {
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                return new com.x64Dynamics.tfb.geometry.Vec3i(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public void setStatus(String message) {
        this.statusMessage = message;
        this.statusMessageExpiry = System.currentTimeMillis() + 4000;
    }

    public void render(IGuiGraphicsContext graphics, int mouseX, int mouseY) {
        int sidebarWidth = Math.max(120, width / 5);
        int headerHeight = 24;
        int tabBarHeight = 22;
        int tabContentHeight = 114;
        int footerHeight = 18;

        // 1. Transparent Architectural Viewport Architecture
        // Notice: The central viewport (from sidebarWidth to width, and below tab drawer to footer)
        // is deliberately left completely transparent so the Minecraft world and live 3D preview remain 100% visible!

        // Header Bar
        graphics.fill(0, 0, width, headerHeight, 0xEE0F172A);
        graphics.drawBorder(0, 0, width, headerHeight, 0xFF1E293B);

        // Tab Bar Area
        graphics.fill(sidebarWidth, headerHeight, width, headerHeight + tabBarHeight, 0xDD0B132B);
        graphics.drawBorder(sidebarWidth, headerHeight, width - sidebarWidth, tabBarHeight, 0xFF1E293B);

        // Tab Content Floating Shelf (translucent dock over the upper screen)
        if (!tabWidgets.isEmpty()) {
            graphics.fill(sidebarWidth + 2, headerHeight + tabBarHeight, width - 4, headerHeight + tabBarHeight + tabContentHeight, 0xD00F172A);
            graphics.drawBorder(sidebarWidth + 2, headerHeight + tabBarHeight, width - sidebarWidth - 6, tabContentHeight, 0xFF25334D);
        }

        // Left Sidebar (Brushes & Categories)
        graphics.fill(0, headerHeight, sidebarWidth, height - footerHeight, 0xEE0B132B);
        graphics.drawBorder(0, headerHeight, sidebarWidth, height - headerHeight - footerHeight, 0xFF1E293B);

        // Footer Bar
        graphics.fill(0, height - footerHeight, width, height, 0xEE0B0F19);
        graphics.drawBorder(0, height - footerHeight, width, footerHeight, 0xFF1E293B);

        // 2. Header Title & Badges
        graphics.drawText("TerraFormaBuilder", 8, 8, 0xFF38BDF8, true);
        String sizeBadge = model.width() + "x" + model.height() + "x" + model.depth();
        graphics.drawText("[" + sizeBadge + "]", 120, 8, 0xFF94A3B8, false);
        graphics.drawText(model.totalBlockCount() + " blocks", 185, 8, 0xFF34D399, false);

        int harmony = Math.round(model.paletteHarmonyScore() * 100);
        graphics.drawText("Harmony: " + harmony + "%", 255, 8, 0xFFF59E0B, false);

        // 3. Sidebar Category Header
        graphics.drawText("BRUSHES", 8, headerHeight + 8, 0xFF64748B, false);

        // 4. Render main widgets
        for (UIWidget widget : widgets) {
            widget.render(graphics, mouseX, mouseY);
        }

        // 5. Render active tab widgets
        for (UIWidget widget : tabWidgets) {
            widget.render(graphics, mouseX, mouseY);
        }

        // 6. Footer Status
        String status = (System.currentTimeMillis() < statusMessageExpiry) ? statusMessage : "Pos: " + model.origin() + " | Ready";
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