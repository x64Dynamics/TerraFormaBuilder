package com.x64Dynamics.tfb.ui.model;

import com.x64Dynamics.tfb.architectural.AdvancedRoofFeatures;
import com.x64Dynamics.tfb.architectural.FacadeOrnamentGenerator;
import com.x64Dynamics.tfb.architectural.FloorGenerator;
import com.x64Dynamics.tfb.architectural.InteriorFloorPlanner;
import com.x64Dynamics.tfb.architectural.InteriorFurnishingGenerator;
import com.x64Dynamics.tfb.architectural.InteriorRoom;
import com.x64Dynamics.tfb.architectural.LayeredFacadeComposer;
import com.x64Dynamics.tfb.architectural.ParametricFootprint;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.architectural.WingLayoutGenerator;
import com.x64Dynamics.tfb.architectural.style.ArchitecturalStyleRuleSet;
import com.x64Dynamics.tfb.architectural.style.ProceduralVariationEngine;
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

    // Interior
    private boolean enableInteriors = true;
    private boolean furnishRooms = true;
    private float furnishingDensity = 1.0f;

    // Construction state
    private boolean instantBuild = false;
    private int blocksPerTick = 64;
    private int tickBudgetMs = 15;

    // History & Preview Cache
    private final DesignHistoryStack historyStack = new DesignHistoryStack(32);
    private final WorldPreviewBuffer previewBuffer = new WorldPreviewBuffer();
    private List<PlacementTarget> cachedPreviewTargets = new ArrayList<>();
    private ProgressiveBuildPlan cachedBuildPlan = null;

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

    public void setPos1(Vec3i pos) {
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

    public List<PlacementTarget> cachedPreviewTargets() { return cachedPreviewTargets; }
    public ProgressiveBuildPlan cachedBuildPlan() { return cachedBuildPlan; }
    public int totalBlockCount() { return cachedPreviewTargets.size(); }

    public synchronized void generatePreview() {
        List<PlacementTarget> targets = new ArrayList<>();
        BoundingBox3i box = boundingBox();

        // 1. Primary generation: Wing Compound or standard brush
        if (activeCategory == BrushCategory.ARCHITECTURAL && wingConfiguration != WingConfiguration.SINGLE_RECTANGLE) {
            ParametricFootprint footprint = ParametricFootprint.create(box, wingConfiguration);
            targets.addAll(WingLayoutGenerator.generateWingCompound(footprint, roofShape, activePalette));
        } else {
            List<Vec3i> pathPts = Collections.emptyList();
            if (selectionManager.pos1() != null && selectionManager.pos2() != null) {
                pathPts = List.of(selectionManager.pos1(), selectionManager.pos2());
            }
            BrushContext ctx = new BrushContext(box, pathPts, activePalette, null, foundationStrategy, Collections.emptyMap());
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
            int storyHeight = 4;
            int numStories = Math.max(1, (height - 2) / storyHeight);

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

                if (furnishRooms) {
                    for (InteriorRoom room : floorPlan.rooms()) {
                        targets.addAll(InteriorFurnishingGenerator.furnishRoom(room, activePalette, floorPlan.transitClearanceZones(), furnishingDensity));
                    }
                }
            }
        }

        // 4. Procedural texture variation
        if (proceduralVariation > 0.0f) {
            targets = ProceduralVariationEngine.applyFacadeVariation(targets, activePalette, proceduralSeed, proceduralVariation);
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
        this.previewBuffer.updateFromTargets(targets, null);
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
        Optional<DesignHistorySnapshot> snapshot = historyStack.undo();
        if (snapshot.isPresent()) {
            restoreSnapshot(snapshot.get());
            return true;
        }
        return false;
    }

    public boolean redo() {
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