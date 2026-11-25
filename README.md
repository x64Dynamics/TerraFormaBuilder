<div align="center">

# 🏛️ TerraFormaBuilder (TFB)

**Next-Generation Procedural Architecture, Civil Infrastructure & Construction Intelligence Studio for Minecraft.**

[![GitHub Release](https://img.shields.io/github/v/release/x64Dynamics/TerraFormaBuilder?style=for-the-badge&logo=github&color=2ea44f&label=Release)](https://github.com/x64Dynamics/TerraFormaBuilder/releases/latest)
[![CI Build](https://img.shields.io/badge/CI_Build-Passing-2ea44f?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/x64Dynamics/TerraFormaBuilder/actions)
[![Minecraft Versions](https://img.shields.io/badge/Minecraft-1.18.2_to_26.3-3C8527?style=for-the-badge&logo=minecraft&logoColor=white)](RELEASES.md)
<br>
[![Fabric](https://img.shields.io/badge/Fabric-30_Versions-dbb36a?style=flat-square&logo=fabric&logoColor=white)](RELEASES.md#supported-versions--downloads)
[![Forge](https://img.shields.io/badge/Forge-28_Versions-df7126?style=flat-square&logo=curseforge&logoColor=white)](RELEASES.md#supported-versions--downloads)
[![NeoForge](https://img.shields.io/badge/NeoForge-22_Versions-e54b4b?style=flat-square&logo=neoforge&logoColor=white)](RELEASES.md#supported-versions--downloads)
[![Side: Universal](https://img.shields.io/badge/Side-Client_%26_Server-007ec6?style=flat-square)](#features)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Targets: 80 Builds](https://img.shields.io/badge/Total_Targets-80_Builds-blueviolet?style=flat-square)](RELEASES.md)

| [📖<br><nobr>Overview</nobr>](#overview) | [✨<br><nobr>Features</nobr>](#features) | [🎮<br><nobr>Controls</nobr>](#controls--keybindings) | [🎨<br><nobr>Hotbar & HUD</nobr>](#dedicated-architect-hud--builder-hotbar) | [🏗️<br><nobr>Architecture</nobr>](#technical-architecture) | [📦<br><nobr>Releases</nobr>](RELEASES.md) | [🤝<br><nobr>Contributing</nobr>](CONTRIBUTING.md) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |

</div>

---

## Overview

**TerraFormaBuilder (TFB)** is a comprehensive procedural architectural construction system and in-game studio that empowers builders, server creators, and architects to design, customize, preview, and generate complex architectural structures, civil infrastructure, and interiors directly inside Minecraft worlds without manually placing millions of individual blocks.

> **Core Philosophy**: *The player is the architect. TFB is the construction engine.*

Unlike traditional static schematic tools, TerraFormaBuilder is an **intelligent procedural engine**. Structures adapt dynamically to terrain elevations, respect architectural taxonomy, generate contextual facades and roofs, maintain procedural connectivity between rooms and wings, and seamlessly trace smooth Bezier/Catmull-Rom road splines through the landscape.

---

## Features

### 🏛️ Advanced Procedural Architecture & Geometry
- **Building Geometries**: Rectangular, polygonal, circular, elliptical, L-shaped, T-shaped, U-shaped, cross-shaped, octagonal, hexagonal, and irregular foundations.
- **Intelligent Connected Wings**: Seamlessly attach wings, towers, cloisters, courtyards, and expansions to existing buildings with coherent structural transitions.
- **Contextual Facades & Openings**: Procedural window patterns, doorway lintels, pediments, arches, balconies, and decorative columns matching selected architectural styles.
- **Parametric Roof Systems**: Gabled, hipped, mansard, gambrel, flat parapet, domed, conical, and tiered pagoda roofing with automatic overhangs and ridge caps.
- **Interior & Room Intelligence**: Automatic division into functional rooms (entryways, halls, living quarters, libraries, towers) with floorplans, staircases, and procedural furnishing.

### 🛣️ Civil Engineering & Spline Infrastructure
- **Curved & Spline Roads**: Bezier and Catmull-Rom spline curves with adjustable tangent control points for natural, winding roads.
- **Multi-Lane Highways & Streets**: Configurable road widths, road markings, curbs, sidewalks, and street lighting.
- **Intersections & Junctions**: Automatic generation of 3-way T-junctions, 4-way intersections, roundabouts, and branching paths.
- **Terrain Adaptation & Bridges**: Smooth slope leveling, retaining walls, embankment bridges, and tunnel boring through steep elevations.

### 🛠️ In-Game Architect Studio & Builder Hotbar
- **Dedicated 9-Slot Builder Hotbar**: When Architect Mode is toggled, the survival hotbar is replaced with a custom architectural toolset without modifying or risking inventory contents.
- **Categorized Tool Paging**: Instant category cycling (`Shift + Mouse Wheel`) across Architectural Brushes, Infrastructure Tools, Editing Utilities, and Decoration Brushes.
- **Direct Slot Selection**: Number keys `1`–`9` and mouse wheel scrolling with active slot highlighting and floating tooltip indicators.
- **3D WASD Free-Flight Physics**: Smooth velocity-based aerial movement with sprint boost, jump elevation, sneak descent, and fine speed tuning.
- **Persistent In-World Studio HUD**: Live real-time readout of active brush, dimensions, coordinates, flight speed, and estimated block count.

### ⚡ Non-Destructive Workflow
- **Live 3D Preview**: Real-time bounding boxes and voxel preview render in the world before placing a single block.
- **Full Undo / Redo**: Multi-level history stack via `Ctrl + Z` and `Ctrl + Y` (or `Ctrl + Shift + Z`).
- **One-Click Construction (`G`)**: Instant validation and compilation of the active architectural plan directly into level voxel placements with block accounting.
- **Dynamic Material Taxonomy**: Cohesive material palettes (Medieval Timber, Gothic Stone, Nordic Pine, Modern Concrete, Cyberpunk Neon, Terracotta) with automatic structural assignment (frame, wall, foundation, roof, trim).

---

## Controls & Keybindings

All keybindings are fully configurable via the dedicated **TerraFormaBuilder** category in Minecraft's Controls settings.

| Default Key | Action | Description |
| :--- | :--- | :--- |
| **`B`** | **Open Architect Workspace** | Opens the main design interface while preserving the active project state. |
| **`P`** | **Toggle Live Preview** | Enables or disables in-world 3D wireframes, bounding boxes, and voxel preview. |
| **`G`** | **Execute Build** | Validates and compiles the active design, instantly placing blocks into the level. |
| **`Ctrl + Z`** | **Undo** | Reverts the latest design, resize, material, or shape modification in the workspace. |
| **`Ctrl + Y`** | **Redo** | Reapplies the latest undone operation in the design workspace. |
| **`1` – `9`** | **Select Hotbar Slot** | Directly equips the corresponding architectural brush or tool on the builder hotbar. |
| **`Mouse Wheel`** | **Cycle Tools** | Cycles forward and backward through available tools in the active category. |
| **`Shift + Scroll`** | **Switch Tool Page** | Flips between tool categories (Architecture, Infrastructure, Editing, Decoration). |
| **`[`** | **Decrease Flight Speed** | Decreases 3D architect flight velocity by `0.2x`. |
| **`]`** | **Increase Flight Speed** | Increases 3D architect flight velocity by `0.2x`. |
| **`Esc`** | **Close Workspace** | Closes the interface and returns to world view without resetting active plans. |

---

## Dedicated Architect HUD & Builder Hotbar

When Architect Mode is activated (via **`B`** or command):
1. **Hotbar Override**: The standard Minecraft hotbar is cleanly replaced by the **TFB Builder Hotbar**, featuring custom tool categories:
   - **Architectural Brushes**: House, Wing, Room, Wall, Roof, Arch, Pillar, Foundation, Staircase.
   - **Infrastructure Brushes**: Road, Curved Spline Road, Path, Sidewalk, Bridge, Tunnel, Roundabout.
   - **Editing Tools**: Select, Box Select, Move, Resize, Rotate, Mirror, Duplicate, Erase.
   - **Decoration Brushes**: Furnishing, Lighting, Facade Ornament, Landscaping, Fenestration.
2. **In-World Studio Banner HUD**: A non-intrusive top-center banner displays:
   - Active mode status (`ARCHITECT ACTIVE`)
   - Selected brush & active tool category
   - Dimensions (`Width × Height × Depth`)
   - Flight speed multiplier (`1.0x` – `5.0x`)
   - Estimated block count & preview status
3. **Player Safety**:
   - Survival inventory and hotbar items remain completely untouched and restored upon exiting Architect Mode.
   - Flight state and movement abilities revert cleanly upon disconnect or exit.

---

## Technical Architecture

TerraFormaBuilder is engineered with a **pure Java core architecture** that isolates all geometric, mathematical, state, and procedural algorithms from Minecraft engine versions.

```
TerraFormaBuilder/
├── common/                               # PURE JAVA 17 CORE (Zero MC / Loader Imports)
│   ├── src/main/java/com/x64Dynamics/tfb/
│   │   ├── architectural/                # Geometry, facades, roofs, rooms, splines, foundations
│   │   ├── material/                     # Material taxonomy, block identification, palettes
│   │   ├── geometry/                     # 3D vector math, curves, Bezier & Catmull-Rom splines
│   │   ├── ui/                           # Pure UI state machines, models, hotbar controllers
│   │   └── history/                      # Multi-level undo/redo snapshot stack
│   └── src/main/resources/               # Universal assets and localization
├── versions/
│   ├── <mc_version>/common/              # Version-shared Minecraft API bindings
│   └── <mc_version>/<loader>/            # Loader entrypoints, client events, mixins, screens
│       └── src/main/java/com/x64Dynamics/tfb/client/
├── properties/                           # Pinned dependencies across all releases
└── tools/kyro/                           # Matrix validation and build dispatch tooling
```

---

## Building & Development

### Prerequisites
- **Git**
- **JDK 17, 21, and 25** configured in your environment
- **Python 3.8+** (for matrix orchestration and validation tools)

### Central Validation
Verify target inventory, source isolation, package consistency, and duplicate resource checks:
```bash
python tools/kyro/template.py validate
```

### Core Logic Build & Unit Tests
Compile and run tests on the pure common core:
```bash
python tools/kyro/template.py core
```

### Building Specific Version Targets
```bash
# Build Fabric target
.\modw.bat -Pmc=1.20.1 -Ploader=fabric build

# Build Forge target
.\modw.bat -Pmc=1.20.1 -Ploader=forge build

# Build NeoForge target
.\modw.bat -Pmc=1.21.1 -Ploader=neoforge build
```

---

## Releases & Downloads

> [!TIP]
> 📦 **Dedicated Releases Catalog**: All 80 JAR downloads across 30 Minecraft versions (1.18.2 through 26.3) for Fabric, Forge, and NeoForge are hosted on [**RELEASES.md**](RELEASES.md) and [**GitHub Releases**](https://github.com/x64Dynamics/TerraFormaBuilder/releases/latest).

| Release | Status | Minecraft Compatibility | Target Builds | Direct Downloads |
|:---:|:---:|:---:|:---:|:---:|
| [**v1.0.0**](https://github.com/x64Dynamics/TerraFormaBuilder/releases/latest) | **Production (Latest)** | 1.18.2 – 26.3 (30 releases) | 80 Targets | [80 Asset JARs](https://github.com/x64Dynamics/TerraFormaBuilder/releases/latest) · [RELEASES.md](RELEASES.md) |

---

## Authors & Credits

TerraFormaBuilder is developed and published by **x64Dynamics**:

- **Studio**: [**x64Dynamics**](https://github.com/x64Dynamics)
- **Lead Developer**: [**KyroXova**](https://github.com/KyroXova) (`kyroxova@gmail.com`)

---

## License

TerraFormaBuilder is released under the [MIT License](LICENSE).
