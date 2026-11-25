package com.x64Dynamics.tfb.ui.hotbar;

import com.x64Dynamics.tfb.architectural.brush.BrushRegistry;
import com.x64Dynamics.tfb.architectural.brush.IConstructionBrush;
import com.x64Dynamics.tfb.ui.controller.ClientController;

import java.util.ArrayList;
import java.util.List;

import com.x64Dynamics.tfb.geometry.Axis;
public final class BuilderHotbarModel {
    private static final BuilderHotbarModel INSTANCE = new BuilderHotbarModel();

    public static final String[] PAGE_NAMES = {
        "Architecture",
        "Infrastructure",
        "Editing & Transforms",
        "Interior & Details"
    };

    private final List<List<BuilderHotbarSlot>> pages = new ArrayList<>();
    private int activePage = 0;
    private int activeSlot = 0;

    private BuilderHotbarModel() {
        initPages();
    }

    public static BuilderHotbarModel getInstance() {
        return INSTANCE;
    }

    private void initPages() {
        // --- Page 0: Architecture ---
        List<BuilderHotbarSlot> archPage = new ArrayList<>();
        archPage.add(new BuilderHotbarSlot("HSE", "House", "Architecture", "minecraft:oak_door", 0xFF38BDF8, BrushRegistry.get("house"), "[L-Click] Set Origin | [R-Click] Drag Bounds | [R] Rotate 90° | [G] Build"));
        archPage.add(new BuilderHotbarSlot("GRD", "Grid Painter", "Architecture", "minecraft:scaffolding", 0xFF22C55E, BrushRegistry.get("building"), "[L-Click] Paste Grid Building Preview | [R-Click] Remove | [G] Build & Next"));
        archPage.add(new BuilderHotbarSlot("ROM", "Room", "Architecture", "minecraft:oak_planks", 0xFF818CF8, BrushRegistry.get("room"), "[L-Click] Footprint | Floorplan Subdivide | [G] Build"));
        archPage.add(new BuilderHotbarSlot("WAL", "Wall", "Architecture", "minecraft:cobblestone_wall", 0xFFA78BFA, BrushRegistry.get("wall"), "[L-Click] Point A | [R-Click] Point B | Height 3-12 | [G] Build"));
        archPage.add(new BuilderHotbarSlot("ROF", "Roof", "Architecture", "minecraft:brick_stairs", 0xFFF472B6, BrushRegistry.get("roof"), "Gable/Hip/Mansard/Dome/Conical | [G] Build"));
        archPage.add(new BuilderHotbarSlot("PIL", "Pillar", "Architecture", "minecraft:quartz_pillar", 0xFF34D399, BrushRegistry.get("pillar"), "Colonnade / Pilasters | Height 3-32 | [G] Build"));
        archPage.add(new BuilderHotbarSlot("STR", "Stairs", "Architecture", "minecraft:stone_stairs", 0xFFFBBF24, BrushRegistry.get("stair"), "Rise / Run Step Progression | [G] Build"));
        archPage.add(new BuilderHotbarSlot("ARC", "Archway", "Architecture", "minecraft:mud_brick_wall", 0xFFFB923C, BrushRegistry.get("arch"), "Catenary / Semicircular Arch | [G] Build"));
        archPage.add(new BuilderHotbarSlot("AUT", "Auto-Build", "Architecture", "minecraft:nether_star", 0xFFFACC15, BrushRegistry.get("house"), "[L-Click] Auto-Detect Biome & Preview | [G] Build"));
        pages.add(archPage);

        // --- Page 1: Infrastructure ---
        List<BuilderHotbarSlot> infraPage = new ArrayList<>();
        infraPage.add(new BuilderHotbarSlot("ROD", "Straight Road", "Infrastructure", "minecraft:dirt_path", 0xFFF59E0B, BrushRegistry.get("road"), "[L-Click] Start | [R-Click] End | Width: 3-15"));
        infraPage.add(new BuilderHotbarSlot("PTH", "Path", "Infrastructure", "minecraft:gravel", 0xFFEAB308, BrushRegistry.get("path"), "[L-Click] Add Path Node | Dirt / Gravel Path"));
        infraPage.add(new BuilderHotbarSlot("STR", "Street", "Infrastructure", "minecraft:gray_concrete", 0xFFD97706, BrushRegistry.get("street"), "Urban Street + Center Lines + Curbs"));
        infraPage.add(new BuilderHotbarSlot("SDW", "Sidewalk", "Infrastructure", "minecraft:smooth_stone_slab", 0xFF78716C, BrushRegistry.get("sidewalk"), "Pavement & Stone Curbs | Width: 1-4"));
        infraPage.add(new BuilderHotbarSlot("BRG", "Bridge", "Infrastructure", "minecraft:spruce_fence", 0xFF0284C7, BrushRegistry.get("bridge"), "Arched / Pier Bridge Span"));
        infraPage.add(new BuilderHotbarSlot("TUN", "Tunnel", "Infrastructure", "minecraft:deepslate", 0xFF475569, BrushRegistry.get("tunnel"), "Cylindrical Bore Excavation + Vault Lining"));
        infraPage.add(new BuilderHotbarSlot("RLW", "Railway", "Infrastructure", "minecraft:rail", 0xFF0D9488, BrushRegistry.get("railway"), "Ballast Track + Iron Rails + Sleepers"));
        infraPage.add(new BuilderHotbarSlot("DRN", "Drainage", "Infrastructure", "minecraft:water_bucket", 0xFF10B981, BrushRegistry.get("drainage"), "Stone Canal / Waterway Runoff Culvert"));
        infraPage.add(new BuilderHotbarSlot("FNC", "Fence", "Infrastructure", "minecraft:oak_fence", 0xFFA3E635, BrushRegistry.get("fence"), "Enclosure Perimeter Fence & Gateposts"));
        pages.add(infraPage);

        // --- Page 2: Editing & Transforms ---
        List<BuilderHotbarSlot> editPage = new ArrayList<>();
        editPage.add(new BuilderHotbarSlot("SEL", "Select Box", "Editing", "minecraft:target", 0xFF38BDF8, null, "[L-Click] Corner A | [R-Click] Corner B"));
        editPage.add(new BuilderHotbarSlot("MOV", "Move Handle", "Editing", "minecraft:piston", 0xFF60A5FA, null, "[Drag Handle] Translate selection along axis"));
        editPage.add(new BuilderHotbarSlot("RSZ", "Resize Handle", "Editing", "minecraft:sticky_piston", 0xFF818CF8, null, "[Drag Face] Expand or contract dimensions"));
        editPage.add(new BuilderHotbarSlot("R90", "Rotate 90°", "Editing", "minecraft:compass", 0xFFA78BFA, null, "[L-Click] Rotate active footprint 90° CW"));
        editPage.add(new BuilderHotbarSlot("R45", "Rotate 45°", "Editing", "minecraft:recovery_compass", 0xFFF472B6, null, "[L-Click] Rotate active footprint 45°"));
        editPage.add(new BuilderHotbarSlot("MIR", "Mirror X/Z", "Editing", "minecraft:glass", 0xFF34D399, null, "[L-Click] Toggle Axis Symmetry Reflection"));
        editPage.add(new BuilderHotbarSlot("DUP", "Duplicate", "Editing", "minecraft:writable_book", 0xFFFBBF24, null, "[L-Click] Clone selection to offset target"));
        editPage.add(new BuilderHotbarSlot("ERS", "Erase", "Editing", "minecraft:barrier", 0xFFEF4444, null, "[L-Click] Clear blocks inside selection volume"));
        editPage.add(new BuilderHotbarSlot("CLR", "Deselect", "Editing", "minecraft:shears", 0xFF94A3B8, null, "[L-Click] Clear active selection bounding box"));
        pages.add(editPage);

        // --- Page 3: Interior & Details ---
        List<BuilderHotbarSlot> decorPage = new ArrayList<>();
        decorPage.add(new BuilderHotbarSlot("FUR", "Furniture", "Decoration", "minecraft:crafting_table", 0xFFA855F7, null, "Tables, Chairs, Desks, Bookshelves, Beds"));
        decorPage.add(new BuilderHotbarSlot("LGT", "Lighting", "Decoration", "minecraft:lantern", 0xFFFACC15, null, "Chandeliers, Lanterns, Sconces, Streetlights"));
        decorPage.add(new BuilderHotbarSlot("FAC", "Pilasters", "Decoration", "minecraft:chiseled_stone_bricks", 0xFF2DD4BF, null, "Classical Facade Columns & Fluting"));
        decorPage.add(new BuilderHotbarSlot("SHT", "Shutters", "Decoration", "minecraft:oak_trapdoor", 0xFF38BDF8, null, "Window Louver Shutters & Flower Boxes"));
        decorPage.add(new BuilderHotbarSlot("BAL", "Balconies", "Decoration", "minecraft:iron_bars", 0xFF6366F1, null, "Stone & Iron Railing Overhangs"));
        decorPage.add(new BuilderHotbarSlot("DOR", "Dormers", "Decoration", "minecraft:glass_pane", 0xFFEC4899, null, "Gabled Attic Dormer Windows"));
        decorPage.add(new BuilderHotbarSlot("CHM", "Chimney", "Decoration", "minecraft:campfire", 0xFFF97316, null, "Brick Flue + Campfire Smoke Vent"));
        decorPage.add(new BuilderHotbarSlot("CRN", "Cornice", "Decoration", "minecraft:smooth_quartz_stairs", 0xFF14B8A6, null, "Dentil & Crown Molding Roof Borders"));
        decorPage.add(new BuilderHotbarSlot("LND", "Landscape", "Decoration", "minecraft:flowering_azalea", 0xFF22C55E, null, "Flora Adaptation, Planters & Terracing"));
        pages.add(decorPage);
    }

    public int getActivePage() { return activePage; }
    public void setPage(int page) {
        if (page >= 0 && page < pages.size()) {
            this.activePage = page;
            syncActiveSlot();
        }
    }
    public void nextPage() {
        setPage((activePage + 1) % pages.size());
    }
    public void prevPage() {
        setPage((activePage - 1 + pages.size()) % pages.size());
    }

    public int getActiveSlot() { return activeSlot; }
    public void setSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            this.activeSlot = slot;
            syncActiveSlot();
        }
    }
    public void nextSlot() {
        setSlot((activeSlot + 1) % 9);
    }
    public void prevSlot() {
        setSlot((activeSlot - 1 + 9) % 9);
    }

    public BuilderHotbarSlot getSlot(int pageIndex, int slotIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            List<BuilderHotbarSlot> p = pages.get(pageIndex);
            if (slotIndex >= 0 && slotIndex < p.size()) {
                return p.get(slotIndex);
            }
        }
        return null;
    }

    public BuilderHotbarSlot getActiveSlotData() {
        return getSlot(activePage, activeSlot);
    }

    public String getContextualHint() {
        BuilderHotbarSlot slot = getActiveSlotData();
        return slot != null ? slot.contextualHint() : "Configure parameters and preview in-world";
    }

    private void syncActiveSlot() {
        BuilderHotbarSlot slot = getActiveSlotData();
        if (slot == null) return;
        var model = ClientController.getInstance().model();
        if ("GRD".equals(slot.code())) {
            model.setInWorldGridMode(true);
        } else {
            model.setInWorldGridMode(false);
            if ("AUT".equals(slot.code())) {
                model.runAutoModeDesign();
            }
        }
        if (slot.brush() != null) {
            model.setActiveBrush(slot.brush());
        }
    }

    public void syncFromBrush(IConstructionBrush brush) {
        if (brush == null) return;
        for (int p = 0; p < pages.size(); p++) {
            List<BuilderHotbarSlot> page = pages.get(p);
            for (int s = 0; s < page.size(); s++) {
                BuilderHotbarSlot slot = page.get(s);
                if (slot.brush() != null && slot.brush().id().equals(brush.id())) {
                    this.activePage = p;
                    this.activeSlot = s;
                    return;
                }
            }
        }
    }
}
