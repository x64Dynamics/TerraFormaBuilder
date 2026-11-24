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
| **Interior Furnishing** | Multi-quadrant room subdivision, transit clearance, room presets | `InteriorFloorPlanner` | Integrated in Room brushes | Integrated in Room brushes | **Verified** |
| **Roofs** | Gable, Hip, Mansard, Flat, Conical, Dome, Shed, Gambrel | `RoofGenerator` | Integrated in Roof brushes | Integrated in Roof brushes | **Verified** |
| **Universal Materials** | Taxonomy discovery, 5 architectural presets, harmony scoring | `BlockTaxonomyRegistry` | Dynamic block resolution | Dynamic block resolution | **Verified** |
| **Live 3D Preview** | In-world bounding boxes, transformation handles, ghost voxels | `WorldPreviewBuffer` | `TFBClientRenderer` | `FabricLevelAccess` | **Verified** |
| **Construction Engine** | Direct level placement, blockstate parsing, progress tracking | `ProgressiveBuildPlan` | `BlockStateParser` + `Level.setBlock` | `BlockStateParser` + `Level.setBlock` | **Verified** |
| **History & Undo/Redo** | Memento pattern design history stack (`Ctrl+Z`, `Ctrl+Y`) | `DesignHistoryStack` | Keymapped & hooked | Keymapped & hooked | **Verified** |
| **Blueprints** | Parametric serialization, JSON export/import, metadata | `ParametricBlueprint` | Workspace Blueprints tab | Workspace Blueprints tab | **Verified** |

---

## Parity Verification Standard
- Features must be implemented in `common/` first.
- Every loader platform must provide real, usable gameplay adapters, not stubs or no-op classes.
- Both Fabric and NeoForge must compile, link, and function with identical behavior.
