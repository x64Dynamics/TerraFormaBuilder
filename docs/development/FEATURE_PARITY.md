# TerraFormaBuilder — Feature Parity Matrix

## Architectural Systems & Feature Coverage

This matrix tracks core architectural features, gameplay integration, and in-game controls across the codebase and loader platforms.

| Feature Area | Subsystem / Capability | Common Core | NeoForge (1.21.1) | Fabric (1.20.1) | Parity Status |
|---|---|---|---|---|---|
| **Architect Mode** | Seamless lifecycle toggle (Enter / Exit / Pause / Resume) | `TFBClientController` | `ArchitectFlightController` | `ArchitectFlightController` | **Verified** |
| **Architect Flight** | 3D flight navigation, vertical ascend/descend, zero-drift hover | N/A (Client physics) | `ArchitectFlightController` | `ArchitectFlightController` | **Verified** |
| **Speed Multipliers** | Normal 1.0x, Sprint 2.5x fast transit, Alt 0.25x precision alignment | Configurable via controller | `ArchitectFlightController` | `ArchitectFlightController` | **Verified** |
| **Workspace Screen** | Interactive CAD/BIM studio UI (`B` key) | `ArchitectWorkspaceScreen` | `TerraFormaScreen` | `TerraFormaScreen` | **Verified** |
| **Unblurred Viewport** | 100% transparent central viewport showing sharp Minecraft world | Transparent studio panels | Overridden `renderBackground` | Overridden `renderBackground` | **Verified** |
| **In-World Studio HUD** | Top-left live status (tool, dimensions, blocks, style, flight, hint) | Model data feeds | `TFBInWorldHud` | `TFBInWorldHud` | **Verified** |
| **Builder Hotbar** | Dedicated 9-slot architectural tool dock | `BuilderHotbarModel` | `TFBInWorldHud` | `TFBInWorldHud` | **Verified** |
| **Hotbar Categories** | 4 Pages: Architecture, Infrastructure, Editing, Decoration | `BuilderHotbarModel` | 4 tab pages + cycling | 4 tab pages + cycling | **Verified** |
| **Hotbar Controls** | Number keys 1–9, mouse wheel slot scroll, Shift+scroll page cycle | `BuilderHotbarModel` | `TFBClientEvents` | `ClientEntrypoint` | **Verified** |
| **Vanilla HUD Supression** | Clean replacement of vanilla survival hotbar/exp bar in Architect Mode | N/A | `RenderGuiLayerEvent.Pre` | Vanilla replacement | **Verified** |
| **Procedural Geometry** | Rectangular, Polygons, Circles, Curves, L/T/Cross Wings | `ShapeRasterizer`, `ParametricFootprint` | Full access via Brushes | Full access via Brushes | **Verified** |
| **Roads & Curves** | Straight, Splines, Curbs, Sidewalks, Streetlights, Intersections | `RoadInfrastructureEngine` | Infrastructure Brushes | Infrastructure Brushes | **Verified** |
| **Facades & Wall Rhythm** | Tri-partite rhythmic segmentation (Plinth, Nobile, Cornice) | `WallSegmentationEngine` | Integrated in Building brushes | Integrated in Building brushes | **Verified** |
| **Home Builder Pipeline** | Deterministic `BuildingDefinition`, validated `BuildingPlan`, multi-wing homes | `IntelligentHomeGenerator`, `BuildingPlanValidator` | Integrated Home Builder (`HouseBrush`) | Integrated Home Builder (`HouseBrush`) | **Verified** |
| **Interior Furnishing** | Seed-driven BSP floor plans, room graph, stairwells, room presets | `InteriorFloorPlanner`, `InteriorFurnishingGenerator` | Integrated in Home Builder | Integrated in Home Builder | **Verified** |
| **Roofs** | Gable (filled pediments), Hip, Mansard, Flat, Conical, Dome, Shed | `RoofGenerator` | Integrated in Home Builder | Integrated in Home Builder | **Verified** |
| **Universal Materials** | Runtime block registry scan, 12 architectural styles, family resolver | `BlockTaxonomyRegistry`, `MaterialFamilyResolver` | `BlockRegistryScanner` | `BlockTaxonomyRegistry` | **Verified** |
| **Live 3D Preview** | Textured 3D block models, `endBatch()` flush, 8192 budget, POS1/POS2 markers | `WorldPreviewBuffer` | `TFBClientRenderer` | `TFBClientRenderer` + `LevelRendererMixin` | **Verified** |
| **In-World Selection** | Left-click POS1, Right-click POS2, HUD dimension readout | `SelectionManager` | `TFBClientEvents` | `MouseHandlerMixin` + `ClientEntrypoint` | **Verified** |
| **City Designer** | Multi-district planning, street-facing parcel orientation, terrain heightmap grading | `CityPlannerEngine`, `WorldProjectStorage` | Workspace City Mode | Workspace City Mode | **Verified** |
| **Public Spaces & Docks** | Market plazas, parks, waterfront docks (`WATERFRONT_DOCK`), selective `BuildScope` | `PublicSpaceGenerator`, `BuildScope` | Integrated in City Mode | Integrated in City Mode | **Verified** |
| **Facade & Roof Detail** | Window trapdoor shutters, 2nd-floor balconies, roof dormers | `IntelligentHomeGenerator` | Integrated in Home Builder | Integrated in Home Builder | **Verified** |
| **Construction Engine** | Dependency-ordered placement, world block undo/redo, verified counts | `DependencyAwareScheduler`, `WorldConstructionHistory` | `TFBClientEvents.executeBuild` | `ClientEntrypoint` | **Verified** |
| **History & Undo/Redo** | World block transaction undo/redo + design history (`Ctrl+Z`, `Ctrl+Y`) | `WorldConstructionHistory`, `DesignHistoryStack` | `executeWorldUndo` / `executeWorldRedo` | Keymapped & hooked | **Verified** |
| **Blueprints** | JSON disk persistence (`config/terraformabuilder/blueprints/`) + load/save UI | `ParametricBlueprint`, `BlueprintStorage` | Workspace Blueprints tab | Workspace Blueprints tab | **Verified** |

---

## Parity Verification Standard
- Features must be implemented in `common/` first.
- Every loader platform must provide real, usable gameplay adapters, not stubs or no-op classes.
- Both Fabric and NeoForge must compile, link, and function with identical behavior.
