# TerraFormaBuilder — Architectural & Procedural City Research Report

## 1. Scope & Creator Licensing Policy
This research synthesizes public Minecraft architectural guides, community build references (Planet Minecraft, Minecraft-Schematics, Litematica community packs), and procedural urbanism literature (Parish & Müller *CityEngine* pipeline, BSP parcel subdivision, L-system/tensor road graphs, and stochastic shape grammars).
- **Licensing & Attribution:** Community maps and schematics remain the intellectual property of their respective creators. TerraFormaBuilder does **not** bundle or redistribute third-party `.litematic` or `.schem` files; all architectural rules below are clean-room procedural algorithms implemented in TFB.

## 2. Core Minecraft Architectural Composition Rules
1. **60-30-10 Material Balance Rule:**
   - **60% Dominant Wall/Surface Material** (e.g. `stone_bricks`, `white_concrete`, `oak_planks`, `calcite`).
   - **30% Structural Framing & Roof** (e.g. `spruce_log` pillars, horizontal beams, `deepslate_tile_stairs` roof).
   - **10% Accent & Detail** (e.g. `trapdoor` shutters, `lantern`s, `cobblestone` plinths, flower boxes).
2. **Depth & Wall Layering (Avoiding Flat Wall Syndrome):**
   - **Foundation Anchoring:** Bottom 1–2 courses use heavier masonry (`cobblestone`, `cobbled_deepslate`, `polished_andesite`) protruding or framing the base.
   - **Structural Framing:** Vertical corner pillars (`axis=y` logs/pillars) and horizontal floor-divider beams (`axis=x`/`z`) break up multi-story facades.
   - **Recessed & Framed Openings:** Windows use sills (`stair` half=top), glass panes, and lintel headers (`stair` half=bottom); doors use real 2-block `DoorBlock`s with lintels and covered porches.
3. **Roof Silhouettes & Overhangs:**
   - Roofs extend at least **1 block past exterior walls** (overhangs), with filled gable pediments, ridge slabs, and integrated chimneys rising from ground-floor hearths through the roof peak.
4. **Style & District Palette Relationships:**
   - **Medieval / Rustic:** Cobblestone plinth + stone brick/plank walls + spruce/oak log framing + deepslate tile or cobble roofs.
   - **Modern / Minimalist:** Polished andesite/blackstone base + white concrete/smooth quartz walls + tinted glass + flat/shed roofs.
   - **Japanese / East Asian:** Stone brick base + calcite/cherry walls + dark oak log framing + deepslate brick roofs + bamboo fences.
   - **Industrial / Victorian:** Brick/tuff walls + basalt pillars + deepslate brick roofs + iron bar accents.

## 3. Procedural City Generation Pipeline (CityEngine-Adapted for Voxels)
1. **Macro Road Network (The Skeleton):**
   - *Planned Grid / Urban:* Orthogonal arterial avenues + secondary streets + pedestrian alleys.
   - *Organic Village / Medieval Town:* Central plaza/crossroads with branching main roads, curved secondary lanes, and radial zoning (civic/market core -> dense townhouses -> outer cottages/workshops).
2. **Block & Parcel Subdivision (The Zoning):**
   - City blocks enclosed by roads are recursively subdivided along their longest axis into buildable `CityParcel`s (`9x9` to `21x19`+), ensuring **every parcel touches a street edge** and records its `Direction streetFacing`.
3. **Public Spaces & Infrastructure:**
   - Dedicated `PublicSpace` parcels become Market Plazas (stone paving, central fountain monument, colored-awning stalls, benches, streetlights) and Parks (meandering gravel/dirt paths, trees, flower beds, lanterns).
4. **Stochastic Shape-Grammar Buildings:**
   - Each parcel invokes `BuildingGrammarEngine` / `IntelligentHomeGenerator` parameterized by `ParcelPurpose`, `DistrictRole`, `StylePreset`, `streetFacing`, and deterministic `seed`, producing distinct footprints, story counts, roof types, and interior room graphs.

## 4. References
- Parish, Y. I. H., & Müller, P. (2001). *Procedural Modeling of Cities*. SIGGRAPH.
- Community build guides: MinecraftGradient, MCHost, Planet Minecraft modular medieval town references, and Litematica community workflows.
