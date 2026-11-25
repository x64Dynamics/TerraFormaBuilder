package com.x64Dynamics.tfb.ui.model;

import com.x64Dynamics.tfb.architectural.roof.AdvancedRoofFeatures;
import com.x64Dynamics.tfb.architectural.facade.FacadeOrnamentGenerator;
import com.x64Dynamics.tfb.architectural.layout.FloorGenerator;
import com.x64Dynamics.tfb.architectural.interior.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.interior.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.interior.InteriorRoom;
import com.x64Dynamics.tfb.architectural.facade.LayeredFacadeComposer;
import com.x64Dynamics.tfb.architectural.layout.ParametricFootprint;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.architectural.layout.WingConfiguration;
import com.x64Dynamics.tfb.architectural.layout.WingLayoutGenerator;
import com.x64Dynamics.tfb.architectural.style.ArchitecturalStyleRuleSet;
import com.x64Dynamics.tfb.architectural.style.ProceduralVariator;
import com.x64Dynamics.tfb.architectural.style.StyleRuleSetRegistry;
import com.x64Dynamics.tfb.brush.ArchitecturalBrushes;
import com.x64Dynamics.tfb.brush.BrushCategory;
import com.x64Dynamics.tfb.brush.BrushContext;
import com.x64Dynamics.tfb.brush.BrushRegistry;
import com.x64Dynamics.tfb.brush.IConstructionBrush;
import com.x64Dynamics.tfb.construction.ProgressiveBuildPlan;
import com.x64Dynamics.tfb.editing.ConstructionTransforms;
import com.x64Dynamics.tfb.editing.DesignHistorySnapshot;
import com.x64Dynamics.tfb.editing.DesignHistoryStack;
import com.x64Dynamics.tfb.editing.WorldSelectionManager;
import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Rotation;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.persistence.BlueprintMetadata;
import com.x64Dynamics.tfb.persistence.ParametricBlueprint;
import com.x64Dynamics.tfb.preview.PreviewFilterMode;
import com.x64Dynamics.tfb.preview.WorldPreviewBuffer;
import com.x64Dynamics.tfb.taxonomy.PaletteHarmonizer;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.terrain.LandscapeAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArchitectWorkspaceModel {
    private final UUID sessionId = UUID.randomUUID();
    private WorkspaceTab activeTab = WorkspaceTab.ARCHITECTURE;

    // Geometry & Selection
    private Vec3i origin = new Vec3i(0, 64, 0);
    private int width = 15;
    private int height = 9;
    private int depth = 15;
    private Rotation rotation = Rotation.NONE;
    private Axis mirrorAxis = null;
    private final WorldSelectionManager selectionManager;

    // Brush & Category
    private BrushCategory activeCategory = BrushCategory.ARCHITECTURAL;
    private IConstructionBrush activeBrush = new ArchitecturalBrushes.HouseBrush();
    private WingConfiguration wingConfiguration = WingConfiguration.SINGLE_RECTANGLE;

    // Materials & Style
    private StylePreset stylePreset = StylePreset.MEDIEVAL;
    private MaterialPalette activePalette = StylePreset.MEDIEVAL.createPalette();
    private final Map<String, String> paletteOverrides = new HashMap<>();

    // Terrain & Foundation
    private FoundationStrategy foundationStrategy = FoundationStrategy.STEPPED_TERRACE;
    private boolean preserveTerrain = true;
    private boolean clearVegetation = true;

    // Architecture details
    private RoofGenerator.RoofType roofShape = RoofGenerator.RoofType.GABLE;
    private boolean useFacadeRhythm = true;
    private boolean addDormers = true;
    private boolean addChimney = true;
    private boolean addPilasters = true;
    private boolean addWindowShutters = true;
    private boolean addBalconies = true;
    private boolean addCornice = true;
    private float proceduralVariation = 0.15f;
    private long proceduralSeed = 42L;

    // Interior & Home Builder summary
    private boolean enableInteriors = true;
    private boolean furnishRooms = true;
    private float furnishingDensity = 1.0f;
    private int requestedStories = 0; // 0 = auto
    private boolean addPorchAndGarden = true;
    private int generatedStories = 1;
    private int generatedRoomCount = 1;

    // Construction state
    private boolean instantBuild = false;
    private int blocksPerTick = 64;
    private int tickBudgetMs = 15;

    // History & Preview Cache
    private final DesignHistoryStack historyStack = new DesignHistoryStack(32);
    private final WorldPreviewBuffer previewBuffer = new WorldPreviewBuffer();
    private List<PlacementTarget> cachedPreviewTargets = new ArrayList<>();
    private ProgressiveBuildPlan cachedBuildPlan = null;
    private com.x64Dynamics.tfb.architectural.plan.BuildingPlan latestPlan = null;
    private transient com.x64Dynamics.tfb.platform.ILevelAccess liveLevelAccess = null;

    public enum BuildScope { ENTIRE_PROJECT, SELECTED_DISTRICT, SELECTED_PARCEL }
    public enum SculptTool { BUILDING, PORCH_TERRACE, TOWER, STAIRS_APPROACH, ERASE_SECTION }

    // Shape-First Architectural Editor state
    private boolean shapeEditorMode = false;
    private SculptTool activeSculptTool = SculptTool.BUILDING;
    private final com.x64Dynamics.tfb.architectural.editor.ArchitecturalProject activeArchProject =
        new com.x64Dynamics.tfb.architectural.editor.ArchitecturalProject("arch_main", "Studio Design");
    private String selectedSectionId = null;

    // World-Scale City Designer state
    private boolean cityMode = false;
    private com.x64Dynamics.tfb.city.WorldProject activeCityProject = null;
    private com.x64Dynamics.tfb.city.SettlementPattern settlementPattern = com.x64Dynamics.tfb.city.SettlementPattern.MEDIEVAL_TOWN;
    private String selectedDistrictId = null;
    private String selectedParcelId = null;
    private BuildScope buildScope = BuildScope.ENTIRE_PROJECT;

    public ArchitectWorkspaceModel() {
        this.selectionManager = new WorldSelectionManager(boundingBox());
        this.selectionManager.setChangeListener(mgr -> {
            if (mgr.primaryBox() != null) {
                this.origin = mgr.primaryBox().min();
                this.width = mgr.primaryBox().sizeX();
                this.height = mgr.primaryBox().sizeY();
                this.depth = mgr.primaryBox().sizeZ();
            }
        });
        pushHistorySnapshot();
        generatePreview();
    }

    public UUID sessionId() { return sessionId; }
    public WorkspaceTab activeTab() { return activeTab; }
    public void setActiveTab(WorkspaceTab tab) { this.activeTab = tab; }

    public Vec3i origin() { return origin; }
    public void setOrigin(Vec3i origin) {
        this.origin = origin;
        selectionManager.setPrimaryBox(boundingBox());
        generatePreview();
    }

    public int width() { return width; }
    public void setWidth(int width) {
        this.width = Math.max(3, width);
        selectionManager.setPrimaryBox(boundingBox());
        generatePreview();
    }

    public int height() { return height; }
    public void setHeight(int height) {
        this.height = Math.max(3, height);
        selectionManager.setPrimaryBox(boundingBox());
        generatePreview();
    }

    public int depth() { return depth; }
    public void setDepth(int depth) {
        this.depth = Math.max(3, depth);
        selectionManager.setPrimaryBox(boundingBox());
        generatePreview();
    }

    public BoundingBox3i boundingBox() {
        return new BoundingBox3i(origin, new Vec3i(origin.x() + width - 1, origin.y() + height - 1, origin.z() + depth - 1));
    }

    public WorldSelectionManager selectionManager() { return selectionManager; }
    public WorldPreviewBuffer previewBuffer() { return previewBuffer; }

    public void setLiveLevelAccess(com.x64Dynamics.tfb.platform.ILevelAccess level) {
        this.liveLevelAccess = level;
    }

    public com.x64Dynamics.tfb.platform.ILevelAccess liveLevelAccess() {
        return liveLevelAccess;
    }

    public boolean isShapeEditorMode() { return shapeEditorMode; }
    public void setShapeEditorMode(boolean enabled) {
        this.shapeEditorMode = enabled;
        if (enabled && activeArchProject.sections().isEmpty()) {
            BoundingBox3i initial = BoundingBox3i.fromOriginAndSize(origin, Math.max(9, width), 1, Math.max(9, depth));
            var sec = activeArchProject.addSection(
                com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.MAIN_WING,
                initial, 2, stylePreset, roofShape, Direction.SOUTH, proceduralSeed
            );
            selectedSectionId = sec.id();
        }
        generatePreview();
    }

    public SculptTool activeSculptTool() { return activeSculptTool; }
    public void setActiveSculptTool(SculptTool tool) {
        if (tool != null) this.activeSculptTool = tool;
    }

    public com.x64Dynamics.tfb.architectural.editor.ArchitecturalProject activeArchProject() {
        return activeArchProject;
    }

    public String selectedSectionId() { return selectedSectionId; }
    public void setSelectedSectionId(String id) { this.selectedSectionId = id; }

    private final VoxelGridEditorModel gridEditor = new VoxelGridEditorModel(16, 16, 16);
    public VoxelGridEditorModel gridEditor() { return gridEditor; }

    private final com.x64Dynamics.tfb.grid.ModularGridSession modularGrid =
        new com.x64Dynamics.tfb.grid.ModularGridSession(new Vec3i(0, 64, 0), 5, 5);
    private boolean modularChunkGridMode = true;
    private boolean inWorldGridMode = false;
    public com.x64Dynamics.tfb.grid.ModularGridSession modularGrid() { return modularGrid; }
    public boolean isModularChunkGridMode() { return modularChunkGridMode; }
    public void setModularChunkGridMode(boolean enabled) { this.modularChunkGridMode = enabled; }
    public boolean isInWorldGridMode() { return inWorldGridMode; }
    public void setInWorldGridMode(boolean enabled) {
        this.inWorldGridMode = enabled;
        if (enabled) {
            this.modularChunkGridMode = true;
            syncModularGridToPreview();
        }
    }

    public synchronized int syncModularGridToPreview() {
        modularGrid.setOrigin(origin);
        gridPlacement.grid().setOrigin(origin);
        gridPlacement.grid().setCellSize(modularGrid.cellSize());
        gridPlacement.grid().resizeGrid(16, 16);
        gridPlacement.registry().clear();
        int idx = 0;
        for (var mcCell : modularGrid.modules().values()) {
            var c = mcCell.coord();
            var bld = new com.x64Dynamics.tfb.grid.GridBuilding(
                "mod_" + (++idx), c.gx(), c.gz(), origin.y() + c.gy() * modularGrid.cellHeight(),
                modularGrid.cellSize(), modularGrid.cellSize(), 1, modularGrid.cellHeight(),
                com.x64Dynamics.tfb.grid.GridBuilding.FootprintShape.SQUARE,
                com.x64Dynamics.tfb.geometry.Direction.SOUTH,
                mcCell.style(), mcCell.palette(), mcCell.roofType(),
                com.x64Dynamics.tfb.grid.GridBuilding.BuildingFunction.RESIDENTIAL, false, proceduralSeed + idx
            );
            gridPlacement.registry().register(bld);
            if (c.equals(modularGrid.selectedCell())) {
                gridPlacement.selectBuilding(bld.id());
            }
        }
        List<PlacementTarget> targets = modularGrid.buildPlacementTargets();
        this.cachedPreviewTargets = targets;
        this.cachedBuildPlan = new ProgressiveBuildPlan(targets);
        this.previewBuffer.updateFromTargets(targets, liveLevelAccess);
        return targets.size();
    }

    public synchronized com.x64Dynamics.tfb.architectural.LocalAIBuilder.AIBuildResult runAIDesign(String prompt) {
        this.cityMode = false;
        this.shapeEditorMode = false;
        var res = com.x64Dynamics.tfb.architectural.LocalAIBuilder.designAndBuild(
            origin, prompt, liveLevelAccess, proceduralSeed
        );
        this.stylePreset = res.intent().style();
        this.roofShape = res.intent().roofType();
        this.wingConfiguration = res.intent().wings();
        this.requestedStories = res.intent().stories();
        this.width = res.intent().width();
        this.depth = res.intent().depth();
        this.addPorchAndGarden = res.intent().addPorch();
        this.addChimney = res.intent().addChimney();
        this.cachedPreviewTargets = res.targets();
        this.cachedBuildPlan = new ProgressiveBuildPlan(res.targets());
        this.previewBuffer.updateFromTargets(res.targets(), liveLevelAccess);
        return res;
    }

    private final com.x64Dynamics.tfb.grid.StructurePlacementController gridPlacement =
        new com.x64Dynamics.tfb.grid.StructurePlacementController(
            com.x64Dynamics.tfb.grid.GridManager.defaultProjectGrid(new Vec3i(0, 64, 0)),
            new com.x64Dynamics.tfb.grid.BuildingRegistry(),
            new com.x64Dynamics.tfb.grid.PlacementValidator(com.x64Dynamics.tfb.grid.PlacementValidator.PlacementRules.defaults()),
            new com.x64Dynamics.tfb.grid.ComplexConnectionPlanner()
        );
    private final com.x64Dynamics.tfb.ui.camera.ArchitectCameraController cameraController =
        new com.x64Dynamics.tfb.ui.camera.ArchitectCameraController();

    public com.x64Dynamics.tfb.grid.StructurePlacementController gridPlacement() { return gridPlacement; }
    public com.x64Dynamics.tfb.ui.camera.ArchitectCameraController cameraController() { return cameraController; }

    public synchronized int syncGridComplexToPreview() {
        var result = gridPlacement.generateComplex();
        this.cachedPreviewTargets = result.targets();
        this.cachedBuildPlan = new ProgressiveBuildPlan(result.targets());
        this.previewBuffer.updateFromTargets(result.targets(), liveLevelAccess);
        return result.targets().size();
    }

    public synchronized com.x64Dynamics.tfb.architectural.AutonomousArchitect.AutoDesignReport runAutoModeDesign() {
        this.cityMode = false;
        this.shapeEditorMode = false;
        var report = com.x64Dynamics.tfb.architectural.AutonomousArchitect.autoDesign(
            origin, boundingBox(), liveLevelAccess, null, proceduralSeed
        );
        this.stylePreset = report.chosenStyle();
        this.roofShape = report.chosenRoof();
        this.wingConfiguration = report.chosenWings();
        this.latestPlan = report.plan();
        this.cachedPreviewTargets = report.plan().targets();
        this.cachedBuildPlan = new ProgressiveBuildPlan(report.plan().targets());
        this.previewBuffer.updateFromTargets(report.plan().targets(), liveLevelAccess);
        return report;
    }

    public synchronized com.x64Dynamics.tfb.architectural.AutonomousArchitect.AutoDesignReport runAutoModeComplex() {
        this.cityMode = false;
        this.shapeEditorMode = false;
        var report = com.x64Dynamics.tfb.architectural.AutonomousArchitect.autoPopulateComplex(
            origin, gridPlacement, liveLevelAccess, null, proceduralSeed
        );
        this.stylePreset = report.chosenStyle();
        this.roofShape = report.chosenRoof();
        this.wingConfiguration = report.chosenWings();
        syncGridComplexToPreview();
        return report;
    }

    public synchronized int syncGridEditorToPreview() {
        List<PlacementTarget> targets = gridEditor.toPlacementTargets(origin, activePalette());
        this.cachedPreviewTargets = targets;
        this.cachedBuildPlan = new ProgressiveBuildPlan(targets);
        this.previewBuffer.updateFromTargets(targets, liveLevelAccess);
        return targets.size();
    }

    public synchronized com.x64Dynamics.tfb.architectural.assets.AuthoredAsset saveGridAsAsset(String name) throws java.io.IOException {
        String assetName = (name != null && !name.isBlank()) ? name.trim() : "grid_structure";
        String id = assetName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        var asset = gridEditor.toAuthoredAsset(id, assetName, activePalette());
        com.x64Dynamics.tfb.architectural.assets.AuthoredAssetRegistry.getInstance().save(asset);
        return asset;
    }

    public synchronized int generateConnectedEstate() {
        this.cityMode = false;
        this.shapeEditorMode = false;
        List<PlacementTarget> estate = com.x64Dynamics.tfb.architectural.layout.ConnectedBuildingComposer.generateConnectedEstate(origin, activePalette());
        this.cachedPreviewTargets = estate;
        this.cachedBuildPlan = new ProgressiveBuildPlan(estate);
        this.previewBuffer.updateFromTargets(estate, liveLevelAccess);
        return estate.size();
    }

    public synchronized com.x64Dynamics.tfb.architectural.assets.AuthoredAsset captureSelectionAsAsset(String name) throws java.io.IOException {
        String assetName = (name != null && !name.isBlank()) ? name.trim() : "captured_asset";
        String id = assetName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        BoundingBox3i box = boundingBox();
        var asset = com.x64Dynamics.tfb.architectural.assets.AuthoredAsset.captureFromWorld(
            id, assetName, box, box.min().y(),
            com.x64Dynamics.tfb.architectural.assets.AuthoredAsset.AssetCategory.BUILDING,
            Direction.SOUTH, liveLevelAccess
        );
        com.x64Dynamics.tfb.architectural.assets.AuthoredAssetRegistry.getInstance().save(asset);
        return asset;
    }

    public synchronized java.nio.file.Path exportCurrentSchematic(String name) throws java.io.IOException {
        String safe = ((name != null && !name.isBlank()) ? name.trim() : "tfb_export").replaceAll("[^a-zA-Z0-9_-]", "_");
        java.nio.file.Path dir = java.nio.file.Path.of("config", "terraformabuilder", "schematics");
        java.nio.file.Path gz = dir.resolve(safe + ".schem.gz");
        com.x64Dynamics.tfb.persistence.SchematicExporter.saveCompressedSchematic(gz, boundingBox(), cachedPreviewTargets);
        com.x64Dynamics.tfb.persistence.SchematicExporter.saveJson(dir.resolve(safe + ".json"), boundingBox(), cachedPreviewTargets);
        return gz;
    }

    public synchronized int importObjToPreview(java.nio.file.Path objPath, int targetHeight, boolean solidFill) throws java.io.IOException {
        var mesh = com.x64Dynamics.tfb.geometry.ObjMeshImporter.loadObj(objPath);
        var vol = com.x64Dynamics.tfb.geometry.MeshVoxelizer.voxelize(mesh, targetHeight, solidFill, 12.0f, origin);
        List<PlacementTarget> imported = vol.toPlacementTargets(MaterialRole.WALL_PRIMARY);
        this.cachedPreviewTargets = imported;
        this.cachedBuildPlan = new ProgressiveBuildPlan(imported);
        this.previewBuffer.updateFromTargets(imported, liveLevelAccess);
        return imported.size();
    }

    public synchronized void generateWfcCompound() {
        this.cityMode = false;
        this.shapeEditorMode = true;
        var res = com.x64Dynamics.tfb.architectural.wfc.WfcSolver.solve(
            3, 1, 3,
            com.x64Dynamics.tfb.architectural.wfc.WfcSolver.defaultArchitecturalGrammar(),
            java.util.Map.of("1,0,1", new com.x64Dynamics.tfb.architectural.wfc.WfcSolver.ModuleVariant("interior_hall", 0)),
            proceduralSeed, 8
        );
        for (var s : new ArrayList<>(activeArchProject.sections())) {
            if (!s.isLocked()) activeArchProject.removeSection(s.id());
        }
        if (res.success()) {
            for (int gx = 0; gx < 3; gx++) {
                for (int gz = 0; gz < 3; gz++) {
                    String mod = res.grid()[gx][0][gz].moduleId();
                    if ("air".equals(mod) || "courtyard".equals(mod)) continue;
                    BoundingBox3i cellBox = BoundingBox3i.fromOriginAndSize(
                        origin.add(gx * 8, 0, gz * 8), 8, 1, 8
                    );
                    var kind = "corner_pillar".equals(mod)
                        ? com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.TOWER
                        : (gx == 1 && gz == 1)
                            ? com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.MAIN_WING
                            : com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.SIDE_WING;
                    int stories = kind == com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.TOWER ? 3 : (kind == com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.MAIN_WING ? 2 : 1);
                    var sec = activeArchProject.addSection(kind, cellBox, stories, stylePreset, roofShape, Direction.SOUTH, proceduralSeed + gx * 31L + gz);
                    selectedSectionId = sec.id();
                }
            }
        }
        generatePreview();
    }

    public synchronized String sculptAt(Vec3i hitPos, Direction face, boolean primaryClick) {
        if (!shapeEditorMode) setShapeEditorMode(true);
        Direction outFace = face != null ? face : Direction.SOUTH;
        var hitSecOpt = activeArchProject.findSectionAt(hitPos);

        if (!primaryClick) {
            if (hitSecOpt.isPresent()) {
                selectedSectionId = hitSecOpt.get().id();
                return "Selected " + selectedSectionId + (hitSecOpt.get().isLocked() ? " [LOCKED]" : "");
            }
            return "No section at cursor";
        }

        String msg;
        switch (activeSculptTool) {
            case BUILDING -> {
                if (hitSecOpt.isPresent()) {
                    var sec = hitSecOpt.get();
                    selectedSectionId = sec.id();
                    if (activeArchProject.extendSection(sec.id(), outFace, 2)) {
                        msg = "Extended " + sec.id() + " " + outFace.name() + " +2";
                    } else {
                        msg = sec.id() + " is locked";
                    }
                } else {
                    BoundingBox3i footprint = BoundingBox3i.fromOriginAndSize(
                        new Vec3i(hitPos.x() - 4, hitPos.y(), hitPos.z() - 4), 9, 1, 9
                    );
                    var kind = activeArchProject.sections().isEmpty()
                        ? com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.MAIN_WING
                        : com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.SIDE_WING;
                    var sec = activeArchProject.addSection(kind, footprint, kind == com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.MAIN_WING ? 2 : 1, stylePreset, roofShape, outFace, proceduralSeed + activeArchProject.sections().size());
                    selectedSectionId = sec.id();
                    msg = "Added " + kind.name() + " (" + sec.id() + ")";
                }
            }
            case TOWER -> {
                if (hitSecOpt.isPresent()) {
                    var sec = hitSecOpt.get();
                    selectedSectionId = sec.id();
                    msg = activeArchProject.stackFloor(sec.id())
                        ? "Stacked floor on " + sec.id() + " (" + sec.stories() + " stories)"
                        : sec.id() + " is locked";
                } else {
                    BoundingBox3i footprint = BoundingBox3i.fromOriginAndSize(
                        new Vec3i(hitPos.x() - 3, hitPos.y(), hitPos.z() - 3), 7, 1, 7
                    );
                    var sec = activeArchProject.addSection(
                        com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.TOWER,
                        footprint, 3, stylePreset, roofShape, outFace, proceduralSeed + 77
                    );
                    selectedSectionId = sec.id();
                    msg = "Added 3-story Tower (" + sec.id() + ")";
                }
            }
            case PORCH_TERRACE -> {
                BoundingBox3i footprint = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(hitPos.x() - 2, hitPos.y(), hitPos.z() - 2), 5, 1, 5
                );
                var sec = activeArchProject.addSection(
                    com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.PORCH,
                    footprint, 1, stylePreset, RoofGenerator.RoofType.FLAT, outFace, proceduralSeed + 33
                );
                if (hitSecOpt.isPresent()) sec.setParentSectionId(hitSecOpt.get().id());
                selectedSectionId = sec.id();
                msg = "Attached Porch (" + sec.id() + ")";
            }
            case STAIRS_APPROACH -> {
                BoundingBox3i footprint = BoundingBox3i.fromOriginAndSize(
                    new Vec3i(hitPos.x() - 1, hitPos.y(), hitPos.z()), 3, 1, 4
                );
                var sec = activeArchProject.addSection(
                    com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection.SectionKind.STAIR_APPROACH,
                    footprint, 1, stylePreset, RoofGenerator.RoofType.FLAT, outFace, proceduralSeed + 55
                );
                selectedSectionId = sec.id();
                msg = "Added Stair Approach (" + sec.id() + ")";
            }
            case ERASE_SECTION -> {
                if (hitSecOpt.isPresent()) {
                    String sid = hitSecOpt.get().id();
                    msg = activeArchProject.removeSection(sid) ? "Removed " + sid : sid + " is locked";
                    if (sid.equals(selectedSectionId)) selectedSectionId = null;
                } else {
                    msg = "No section at cursor";
                }
            }
            default -> msg = "Ready";
        }
        generatePreview();
        return msg;
    }

    public com.x64Dynamics.tfb.terrain.HeightmapGrid sampleLiveHeightmap(BoundingBox3i box) {
        return com.x64Dynamics.tfb.terrain.HeightmapGrid.samplePadded(box, 8, liveLevelAccess);
    }

    public void setPos1(Vec3i pos) {
        if (inWorldGridMode && pos != null) {
            if (modularGrid.modules().isEmpty()) {
                this.origin = new Vec3i(pos.x() - modularGrid.cellSize() / 2, pos.y(), pos.z() - modularGrid.cellSize() / 2);
                modularGrid.setOrigin(this.origin);
            }
            var cell = modularGrid.worldToCell(pos);
            modularGrid.stackOrPlace(cell.gx(), cell.gz(), stylePreset, activePalette(), roofShape);
            syncModularGridToPreview();
            return;
        }
        selectionManager.setPos1(pos);
        if (selectionManager.primaryBox() != null) {
            this.origin = selectionManager.primaryBox().min();
            this.width = selectionManager.primaryBox().sizeX();
            this.height = selectionManager.primaryBox().sizeY();
            this.depth = selectionManager.primaryBox().sizeZ();
        }
        generatePreview();
    }

    public void setPos2(Vec3i pos) {
        if (inWorldGridMode && pos != null) {
            var cell = modularGrid.worldToCell(pos);
            modularGrid.removeTopModule(cell.gx(), cell.gz());
            syncModularGridToPreview();
            return;
        }
        selectionManager.setPos2(pos);
        if (selectionManager.primaryBox() != null) {
            this.origin = selectionManager.primaryBox().min();
            this.width = selectionManager.primaryBox().sizeX();
            this.height = selectionManager.primaryBox().sizeY();
            this.depth = selectionManager.primaryBox().sizeZ();
        }
        generatePreview();
    }

    public void clearSelection() {
        selectionManager.clearPositions();
        generatePreview();
    }

    public void expandSelection(int amount) {
        selectionManager.expandSelection(amount);
        if (selectionManager.primaryBox() != null) {
            this.origin = selectionManager.primaryBox().min();
            this.width = selectionManager.primaryBox().sizeX();
            this.height = selectionManager.primaryBox().sizeY();
            this.depth = selectionManager.primaryBox().sizeZ();
        }
        generatePreview();
    }

    public void contractSelection(int amount) {
        selectionManager.contractSelection(amount);
        if (selectionManager.primaryBox() != null) {
            this.origin = selectionManager.primaryBox().min();
            this.width = selectionManager.primaryBox().sizeX();
            this.height = selectionManager.primaryBox().sizeY();
            this.depth = selectionManager.primaryBox().sizeZ();
        }
        generatePreview();
    }

    public void centerSelection() {
        Vec3i c = selectionManager.center();
        setOrigin(c);
    }

    public Rotation rotation() { return rotation; }
    public void setRotation(Rotation rotation) { this.rotation = rotation; generatePreview(); }

    public Axis mirrorAxis() { return mirrorAxis; }
    public void setMirrorAxis(Axis axis) { this.mirrorAxis = axis; generatePreview(); }

    public WingConfiguration wingConfiguration() { return wingConfiguration; }
    public void setWingConfiguration(WingConfiguration config) {
        this.wingConfiguration = config;
        generatePreview();
    }

    public BrushCategory activeCategory() { return activeCategory; }
    public void setActiveCategory(BrushCategory category) {
        this.activeCategory = category;
        List<IConstructionBrush> brushes = BrushRegistry.byCategory(category);
        if (!brushes.isEmpty()) {
            this.activeBrush = brushes.get(0);
        }
        generatePreview();
    }

    public IConstructionBrush activeBrush() { return activeBrush; }
    public void setActiveBrush(IConstructionBrush brush) {
        if (brush != null) {
            this.activeBrush = brush;
            this.activeCategory = brush.category();
            generatePreview();
            try {
                com.x64Dynamics.tfb.ui.hotbar.BuilderHotbarModel.getInstance().syncFromBrush(brush);
            } catch (Throwable ignored) {}
        }
    }

    public void adjustWidth(int delta) {
        setWidth(this.width + delta);
    }

    public void adjustHeight(int delta) {
        setHeight(this.height + delta);
    }

    public void adjustDepth(int delta) {
        setDepth(this.depth + delta);
    }

    public void cycleStylePreset() {
        StylePreset[] presets = StylePreset.values();
        int next = (stylePreset.ordinal() + 1) % presets.length;
        setStylePreset(presets[next]);
    }

    public void cycleRoofShape() {
        RoofGenerator.RoofType[] types = RoofGenerator.RoofType.values();
        int next = (roofShape.ordinal() + 1) % types.length;
        setRoofShape(types[next]);
    }

    public void cycleGridSnap() {
        int current = selectionManager.gridSnap();
        int next = current == 1 ? 2 : current == 2 ? 4 : current == 4 ? 8 : current == 8 ? 16 : 1;
        selectionManager.setGridSnap(next);
    }

    public StylePreset stylePreset() { return stylePreset; }
    public void setStylePreset(StylePreset preset) {
        if (preset != null) {
            this.stylePreset = preset;
            this.activePalette = preset.createPalette();
            ArchitecturalStyleRuleSet rule = StyleRuleSetRegistry.forPreset(preset);
            this.roofShape = rule.preferredRoofType();
            this.addPilasters = rule.allowPilasters();
            this.addWindowShutters = rule.allowShutters();
            this.addBalconies = rule.allowBalconies();
            this.addDormers = rule.allowDormers();
            this.addCornice = rule.allowCornice();
            generatePreview();
        }
    }

    public MaterialPalette activePalette() { return activePalette; }

    public FoundationStrategy foundationStrategy() { return foundationStrategy; }
    public void setFoundationStrategy(FoundationStrategy strategy) {
        this.foundationStrategy = strategy;
        generatePreview();
    }

    public boolean preserveTerrain() { return preserveTerrain; }
    public void setPreserveTerrain(boolean preserve) { this.preserveTerrain = preserve; }

    public boolean clearVegetation() { return clearVegetation; }
    public void setClearVegetation(boolean clear) { this.clearVegetation = clear; }

    public RoofGenerator.RoofType roofShape() { return roofShape; }
    public void setRoofShape(RoofGenerator.RoofType shape) { this.roofShape = shape; generatePreview(); }

    public boolean addDormers() { return addDormers; }
    public void setAddDormers(boolean dormers) { this.addDormers = dormers; generatePreview(); }

    public boolean addChimney() { return addChimney; }
    public void setAddChimney(boolean chimney) { this.addChimney = chimney; generatePreview(); }

    public boolean addPilasters() { return addPilasters; }
    public void setAddPilasters(boolean pilasters) { this.addPilasters = pilasters; generatePreview(); }

    public boolean addWindowShutters() { return addWindowShutters; }
    public void setAddWindowShutters(boolean shutters) { this.addWindowShutters = shutters; generatePreview(); }

    public boolean addBalconies() { return addBalconies; }
    public void setAddBalconies(boolean balconies) { this.addBalconies = balconies; generatePreview(); }

    public boolean useFacadeRhythm() { return useFacadeRhythm; }
    public void setUseFacadeRhythm(boolean rhythm) { this.useFacadeRhythm = rhythm; generatePreview(); }

    public boolean addCornice() { return addCornice; }
    public void setAddCornice(boolean cornice) { this.addCornice = cornice; generatePreview(); }

    public float proceduralVariation() { return proceduralVariation; }
    public void setProceduralVariation(float rate) { this.proceduralVariation = Math.max(0.0f, Math.min(0.5f, rate)); generatePreview(); }

    public boolean enableInteriors() { return enableInteriors; }
    public void setEnableInteriors(boolean interiors) { this.enableInteriors = interiors; generatePreview(); }

    public boolean furnishRooms() { return furnishRooms; }
    public void setFurnishRooms(boolean furnish) { this.furnishRooms = furnish; generatePreview(); }

    public float furnishingDensity() { return furnishingDensity; }
    public void setFurnishingDensity(float density) { this.furnishingDensity = Math.max(0.1f, Math.min(1.0f, density)); generatePreview(); }

    public boolean instantBuild() { return instantBuild; }
    public void setInstantBuild(boolean instant) { this.instantBuild = instant; }

    public int blocksPerTick() { return blocksPerTick; }
    public void setBlocksPerTick(int rate) { this.blocksPerTick = Math.max(1, rate); }

    public int tickBudgetMs() { return tickBudgetMs; }
    public void setTickBudgetMs(int budget) { this.tickBudgetMs = Math.max(1, budget); }

    public float paletteHarmonyScore() {
        return PaletteHarmonizer.scoreHarmony(activePalette);
    }

    public int requestedStories() { return requestedStories; }
    public void setRequestedStories(int stories) { this.requestedStories = Math.max(0, Math.min(4, stories)); generatePreview(); }

    public boolean addPorchAndGarden() { return addPorchAndGarden; }
    public void setAddPorchAndGarden(boolean porch) { this.addPorchAndGarden = porch; generatePreview(); }

    public int generatedStories() { return generatedStories; }
    public int generatedRoomCount() { return generatedRoomCount; }

    public long proceduralSeed() { return proceduralSeed; }
    public void setProceduralSeed(long seed) { this.proceduralSeed = seed; generatePreview(); }
    public void randomizeSeed() { this.proceduralSeed = (this.proceduralSeed * 6364136223846793005L + 1442695040888963407L) & 0x7FFFFFFFL; generatePreview(); }

    public boolean isCityMode() { return cityMode; }
    public void setCityMode(boolean cityMode) { this.cityMode = cityMode; generatePreview(); }
    public com.x64Dynamics.tfb.city.WorldProject activeCityProject() { return activeCityProject; }
    public com.x64Dynamics.tfb.city.SettlementPattern settlementPattern() { return settlementPattern; }
    public void setSettlementPattern(com.x64Dynamics.tfb.city.SettlementPattern pattern) {
        if (pattern != null) {
            this.settlementPattern = pattern;
            if (cityMode && activeCityProject != null) {
                activeCityProject.setPattern(pattern);
                com.x64Dynamics.tfb.city.CityPlanner.planCity(activeCityProject);
                generatePreview();
            }
        }
    }
    public String selectedDistrictId() { return selectedDistrictId; }
    public void setSelectedDistrictId(String id) { this.selectedDistrictId = id; }
    public String selectedParcelId() { return selectedParcelId; }
    public void setSelectedParcelId(String id) { this.selectedParcelId = id; }
    public BuildScope buildScope() { return buildScope; }
    public void setBuildScope(BuildScope scope) { this.buildScope = scope != null ? scope : BuildScope.ENTIRE_PROJECT; generatePreview(); }
    public void cycleBuildScope() {
        BuildScope[] vals = BuildScope.values();
        setBuildScope(vals[(buildScope.ordinal() + 1) % vals.length]);
    }

    public synchronized void createNewCity(String name, com.x64Dynamics.tfb.city.SettlementPattern pattern, int sizeX, int sizeZ) {
        this.width = Math.max(24, sizeX);
        this.depth = Math.max(24, sizeZ);
        this.height = Math.max(16, this.height);
        this.settlementPattern = pattern != null ? pattern : com.x64Dynamics.tfb.city.SettlementPattern.MEDIEVAL_TOWN;
        this.cityMode = true;
        this.activeCityProject = new com.x64Dynamics.tfb.city.WorldProject(
            "city-" + Long.toHexString(proceduralSeed),
            name,
            "KyroXova",
            boundingBox(),
            this.settlementPattern,
            this.stylePreset,
            this.proceduralSeed
        );
        com.x64Dynamics.tfb.city.CityPlanner.planCity(this.activeCityProject, sampleLiveHeightmap(boundingBox()));
        generatePreview();
    }

    public synchronized boolean regenerateSelectedParcel() {
        if (!cityMode || activeCityProject == null || selectedParcelId == null) return false;
        long nextSeed = (proceduralSeed * 6364136223846793005L + System.nanoTime()) & 0x7FFFFFFFL;
        boolean ok = com.x64Dynamics.tfb.city.CityPlanner.regenerateParcel(activeCityProject, selectedParcelId, nextSeed);
        if (ok) generatePreview();
        return ok;
    }

    public synchronized boolean toggleSelectedParcelLock() {
        if (!cityMode || activeCityProject == null || selectedParcelId == null) return false;
        var opt = activeCityProject.findParcel(selectedParcelId);
        if (opt.isPresent()) {
            opt.get().setLocked(!opt.get().isLocked());
            return true;
        }
        return false;
    }

    public synchronized void loadWorldProject(com.x64Dynamics.tfb.city.WorldProject project) {
        if (project == null) return;
        this.activeCityProject = project;
        this.cityMode = true;
        this.settlementPattern = project.pattern();
        this.stylePreset = project.primaryStyle();
        this.activePalette = project.primaryStyle().createPalette();
        this.origin = project.bounds().min();
        this.width = project.bounds().sizeX();
        this.height = project.bounds().sizeY();
        this.depth = project.bounds().sizeZ();
        this.proceduralSeed = project.seed();
        com.x64Dynamics.tfb.city.CityPlanner.planCity(project);
        generatePreview();
    }

    public List<PlacementTarget> cachedPreviewTargets() { return cachedPreviewTargets; }
    public ProgressiveBuildPlan cachedBuildPlan() { return cachedBuildPlan; }
    public com.x64Dynamics.tfb.architectural.plan.BuildingPlan latestPlan() { return latestPlan; }
    public int totalBlockCount() { return cachedPreviewTargets.size(); }

    public synchronized void generatePreview() {
        List<PlacementTarget> targets = new ArrayList<>();
        BoundingBox3i box = boundingBox();
        com.x64Dynamics.tfb.terrain.HeightmapGrid liveHeightmap = sampleLiveHeightmap(box);

        if (shapeEditorMode) {
            BoundingBox3i sampleBox = activeArchProject.sections().isEmpty()
                ? box
                : activeArchProject.sections().get(0).bounds();
            com.x64Dynamics.tfb.terrain.HeightmapGrid secHeight = sampleLiveHeightmap(sampleBox);
            activeArchProject.regenerateDirtySections(secHeight, foundationStrategy);
            targets.addAll(activeArchProject.resolveAllTargets());
            this.generatedStories = activeArchProject.sections().stream().mapToInt(com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection::stories).max().orElse(1);
            this.generatedRoomCount = activeArchProject.sections().size();
        } else if (cityMode && activeCityProject != null) {
            activeCityProject.setBounds(box);
            activeCityProject.setPrimaryStyle(stylePreset);
            activeCityProject.setSeed(proceduralSeed);
            if (activeCityProject.districts().isEmpty()) {
                com.x64Dynamics.tfb.city.CityPlanner.planCity(activeCityProject, liveHeightmap);
            }
            if (buildScope == BuildScope.SELECTED_PARCEL && selectedParcelId != null) {
                targets.addAll(activeCityProject.resolveParcelTargets(selectedParcelId));
            } else if (buildScope == BuildScope.SELECTED_DISTRICT && selectedDistrictId != null) {
                targets.addAll(activeCityProject.resolveDistrictTargets(selectedDistrictId));
            } else {
                targets.addAll(activeCityProject.resolveAllTargets());
            }
            this.generatedStories = activeCityProject.districts().size();
            this.generatedRoomCount = activeCityProject.totalParcelCount();
        } else if (activeBrush instanceof ArchitecturalBrushes.HouseBrush) {
            com.x64Dynamics.tfb.architectural.plan.BuildingDefinition def =
                new com.x64Dynamics.tfb.architectural.plan.BuildingDefinition(
                    "home-" + Long.toHexString(proceduralSeed),
                    box,
                    wingConfiguration,
                    stylePreset,
                    activePalette,
                    roofShape,
                    requestedStories,
                    addPorchAndGarden,
                    addChimney,
                    enableInteriors && furnishRooms,
                    furnishingDensity,
                    proceduralSeed
                );
            com.x64Dynamics.tfb.architectural.plan.BuildingPlan plan =
                com.x64Dynamics.tfb.architectural.IntelligentHomeGenerator.generatePlan(def, liveHeightmap, foundationStrategy);
            this.latestPlan = plan;
            targets.addAll(plan.targets());
            this.generatedStories = plan.floors().size();
            this.generatedRoomCount = plan.totalRoomCount();
        } else {
            // 1. Primary generation: Wing Compound or standard brush
            if (activeCategory == BrushCategory.ARCHITECTURAL && wingConfiguration != WingConfiguration.SINGLE_RECTANGLE) {
                ParametricFootprint footprint = ParametricFootprint.create(box, wingConfiguration);
                targets.addAll(WingLayoutGenerator.generateWingCompound(footprint, roofShape, activePalette));
            } else {
                List<Vec3i> pathPts = Collections.emptyList();
                if (selectionManager.pos1() != null && selectionManager.pos2() != null) {
                    pathPts = List.of(selectionManager.pos1(), selectionManager.pos2());
                }
                BrushContext ctx = new BrushContext(box, pathPts, activePalette, liveHeightmap, foundationStrategy, Collections.emptyMap());
                if (activeBrush != null) {
                    targets.addAll(activeBrush.generate(ctx));
                }
            }

            // 2. Facade detailing & ornaments
            if (activeCategory == BrushCategory.ARCHITECTURAL && width >= 7 && depth >= 7) {
                if (addPilasters) {
                    targets.addAll(FacadeOrnamentGenerator.generatePilasters(box, Direction.SOUTH, 4, activePalette));
                }
                if (addWindowShutters) {
                    Vec3i winCenter = new Vec3i(box.min().x() + width / 2, box.min().y() + 2, box.max().z());
                    targets.addAll(FacadeOrnamentGenerator.addWindowShuttersAndFlowerBox(winCenter, Direction.SOUTH, 1, 2, activePalette));
                }
                if (addBalconies && height >= 8) {
                    Vec3i doorCenter = new Vec3i(box.min().x() + width / 2, box.min().y() + 4, box.max().z());
                    targets.addAll(FacadeOrnamentGenerator.generateBalcony(doorCenter, Direction.SOUTH, 3, 2, activePalette));
                }
                if (addDormers && height >= 7) {
                    Vec3i dormerPos = new Vec3i(box.min().x() + width / 2, box.max().y() - 2, box.max().z());
                    targets.addAll(AdvancedRoofFeatures.generateDormer(dormerPos, Direction.SOUTH, 3, 3, activePalette));
                }
                if (addChimney) {
                    Vec3i chimneyPos = new Vec3i(box.max().x() - 2, box.max().y() - 1, box.max().z() - 2);
                    targets.addAll(AdvancedRoofFeatures.generateChimney(chimneyPos, 4, activePalette));
                }
                if (addCornice) {
                    targets.addAll(LayeredFacadeComposer.generateCorniceTrim(box, activePalette));
                }
            }

            // 3. Multi-Story Interior rooms & furnishings
            if (enableInteriors && activeCategory == BrushCategory.ARCHITECTURAL && width >= 8 && depth >= 8 && height >= 5) {
                int storyHeight = 5;
                int numStories = requestedStories > 0 ? Math.min(4, requestedStories) : Math.max(1, (height - 2) / storyHeight);
                this.generatedStories = numStories;
                int roomTotal = 0;

                for (int s = 0; s < numStories; s++) {
                    int floorY = box.min().y() + 1 + (s * storyHeight);
                    int ceilingY = Math.min(box.max().y() - 1, floorY + storyHeight - 1);
                    if (ceilingY <= floorY) continue;

                    if (s > 0) {
                        BoundingBox3i floorBounds = BoundingBox3i.fromCorners(
                            new Vec3i(box.min().x() + 1, floorY - 1, box.min().z() + 1),
                            new Vec3i(box.max().x() - 1, floorY - 1, box.max().z() - 1)
                        );
                        targets.addAll(FloorGenerator.generateFloor(floorBounds, floorY - 1, true, activePalette));
                    }

                    BoundingBox3i storyBox = BoundingBox3i.fromCorners(
                        new Vec3i(box.min().x() + 1, floorY, box.min().z() + 1),
                        new Vec3i(box.max().x() - 1, ceilingY, box.max().z() - 1)
                    );

                    InteriorFloorPlanner.FloorPlanResult floorPlan = InteriorFloorPlanner.planStory(storyBox, s, numStories, activePalette);
                    targets.addAll(floorPlan.partitionWalls());
                    roomTotal += floorPlan.rooms().size();

                    if (furnishRooms) {
                        for (InteriorRoom room : floorPlan.rooms()) {
                            targets.addAll(InteriorFurnishingGenerator.furnishRoom(room, activePalette, floorPlan.transitClearanceZones(), furnishingDensity));
                        }
                    }
                }
                this.generatedRoomCount = Math.max(1, roomTotal);
            }
        }

        // 4. Procedural texture variation
        if (proceduralVariation > 0.0f) {
            targets = ProceduralVariator.applyFacadeVariation(targets, activePalette, proceduralSeed, proceduralVariation);
        }

        // 5. Transforms (rotation & mirror)
        if (rotation != Rotation.NONE) {
            targets = ConstructionTransforms.rotate(targets, rotation, origin);
        }
        if (mirrorAxis != null) {
            int coord = mirrorAxis == Axis.X ? origin.x() : (mirrorAxis == Axis.Y ? origin.y() : origin.z());
            targets = ConstructionTransforms.mirror(targets, mirrorAxis, coord);
        }

        this.cachedPreviewTargets = targets;
        this.cachedBuildPlan = new ProgressiveBuildPlan(targets);
        this.previewBuffer.updateFromTargets(targets, liveLevelAccess);
    }

    public void pushHistorySnapshot() {
        historyStack.push(new DesignHistorySnapshot(
            activeBrush.id(),
            boundingBox(),
            stylePreset.name(),
            foundationStrategy,
            new HashMap<>(paletteOverrides)
        ));
    }

    public boolean undo() {
        if (shapeEditorMode && activeArchProject.undo()) {
            generatePreview();
            return true;
        }
        Optional<DesignHistorySnapshot> snapshot = historyStack.undo();
        if (snapshot.isPresent()) {
            restoreSnapshot(snapshot.get());
            return true;
        }
        return false;
    }

    public boolean redo() {
        if (shapeEditorMode && activeArchProject.redo()) {
            generatePreview();
            return true;
        }
        Optional<DesignHistorySnapshot> snapshot = historyStack.redo();
        if (snapshot.isPresent()) {
            restoreSnapshot(snapshot.get());
            return true;
        }
        return false;
    }

    private void restoreSnapshot(DesignHistorySnapshot snap) {
        this.width = snap.bounds().sizeX();
        this.height = snap.bounds().sizeY();
        this.depth = snap.bounds().sizeZ();
        this.stylePreset = StylePreset.valueOf(snap.stylePreset());
        this.activePalette = stylePreset.createPalette();
        this.foundationStrategy = snap.foundationStrategy();
        generatePreview();
    }

    public ParametricBlueprint toBlueprint(String name, String author) {
        BlueprintMetadata meta = new BlueprintMetadata(
            name,
            author,
            System.currentTimeMillis(),
            activeBrush.id(),
            boundingBox(),
            stylePreset.name()
        );
        Map<String, Object> params = new HashMap<>();
        params.put("category", activeCategory.name());
        params.put("roofShape", roofShape.name());
        params.put("wingConfig", wingConfiguration.name());
        return new ParametricBlueprint(meta, boundingBox(), activeBrush.id(), stylePreset.name(), foundationStrategy, paletteOverrides, params, Collections.emptyList());
    }

    public void loadBlueprint(ParametricBlueprint bp) {
        if (bp != null) {
            pushHistorySnapshot();
            this.width = bp.bounds().sizeX();
            this.height = bp.bounds().sizeY();
            this.depth = bp.bounds().sizeZ();
            this.stylePreset = StylePreset.valueOf(bp.stylePreset());
            this.activePalette = stylePreset.createPalette();
            this.foundationStrategy = bp.foundationStrategy();
            generatePreview();
        }
    }
}