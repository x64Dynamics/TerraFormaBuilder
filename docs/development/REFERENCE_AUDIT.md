# TerraFormaBuilder — Reference Repository Audit & Architecture Synthesis

## 1. Reference Repositories & License Review

| Repository | License | Language | Adopted Concepts (Reimplemented in Pure Java) |
|---|---|---|---|
| `vengi-voxel/vengi` | MIT | C++ | Sparse/chunked `VoxelVolume`, boolean ops (union/subtract/intersect), connected-component flood fill, transforms |
| `LucasDower/ObjToSchematic` | BSD 3-Clause | TypeScript | `.obj`/`.mtl` parsing, triangle/ray voxelization, 3D 4×4×4 ordered dithering, face-visibility-aware block color matching |
| `chengmarc/minecraft-citygen` | Apache 2.0 | Python | In-world authored asset capture, ground-level offset, stackable sections, landmark/repeatable asset registry |
| `ScholliYT/MGAIA-Minecraft` | MIT | Python | 3D Wave Function Collapse (`WfcSolver`) with `StructureRotation`, directional adjacency, entropy collapse |
| `Niels-NTG/GDMC2024` | MIT | Python | WFC with locked cells, validation/retry on contradiction, multi-floor circulation |
| `Shadscure/RoadArchitect` | Apache 2.0 | Java | Terrain-aware A* road pathfinding, slope/water penalties, bridge/tunnel detection, biome-aware road styles |

All six licenses (MIT, BSD 3-Clause, Apache 2.0) are permissive and compatible with independent Java reimplementation in `common/`. No C++, Python, Node, or external AI dependencies are introduced.

## 2. Existing TFB System Audit

- **Reused & Extended:** `BoundingBox3i`, `Vec3i`, `HeightmapGrid`, `TerrainAdapter`, `ExteriorLandscapingEngine`, `IntelligentHomeGenerator`, `BuildingSection`, `ArchitecturalProject`, `CityPlannerEngine`, `WorldPreviewBuffer`, `DependencyAwareScheduler`, `WorldConstructionHistory`, `ILevelAccess` across 80 targets.
- **Replaced / Upgraded:**
  - `geometry/`: Added `VoxelVolume` (chunked sparse volume, boolean ops, connected components), `ObjMeshImporter` (`.obj`/`.mtl`/`.stl`), `MeshVoxelizer`, and `BlockColorMatcher` (3D ordered dithering + face-visibility color matching).
  - `persistence/SchematicExporter`: Extended beyond in-memory DTOs to read/write GZIP NBT Sponge `.schem` / `.nbt` payloads.
  - `architectural/`: Added `WfcSolver` + `AuthoredAsset` / `AuthoredAssetRegistry` / `WorldAssetScanner`.
  - `city/`: Added `TerrainRoadPathfinder` (A* over `HeightmapGrid` with bridge/tunnel tagging) integrated into `CityPlannerEngine`.
