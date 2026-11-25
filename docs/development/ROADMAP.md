# TerraFormaBuilder — Development Roadmap

## Strategic Phasing & Milestones

### Phase 1: In-Game Runtime Foundation (COMPLETED)
- [x] Clear, unblurred Architect Workspace (`B`) with transparent 3D world viewport.
- [x] Functional Architect Mode lifecycle (Enter / Exit / Pause / Resume).
- [x] Architect Flight navigation with vertical ascend/descend and zero-gravity hover.
- [x] Flight speeds: Normal 1.0x, Sprint 2.5x fast transit, Alt 0.25x micro-precision.
- [x] Dedicated 9-slot TFB Builder Hotbar replacing survival hotbar without inventory loss.
- [x] In-World Architectural Studio HUD with tool stats, flight speed, and contextual action hints.
- [x] Hotbar slot number keys 1–9 and mouse wheel tool scrolling.
- [x] Shift + scroll 4-category page cycling (Architecture, Infrastructure, Editing, Decoration).
- [x] NeoForge 1.21.1 and Fabric 1.20.1 implementation & verification.

### Phase 2: Expanded Procedural Architecture & Brushes (COMPLETED)
- [x] Multi-segment circular and polygonal towers with connected battlements.
- [x] Spline-based road intersection networks with automated crosswalks, traffic lights, and curbs.
- [x] Procedural staircase configurations (Spiral, Dog-leg, Bifurcated imperial, Straight).
- [x] Dynamic roof intersection resolver for non-rectangular complex compounds.
- [x] Advanced facade window arrays with rhythmic pilaster fluting and pediment cornices.

### Phase 3: Universal Block Intelligence & Dynamic Mod Discovery (COMPLETED)
- [x] Automated scan of loaded block registries for custom modded stairs, slabs, pillars, and decorations.
- [x] Universal visual harmony calculator taking RGB texture averages and HSV complementary colors into account.
- [x] Modded furniture and light source dynamic taxonomy tagging.

### Phase 4: Full Multi-Version Matrix Rollout
- [ ] Deploy client adapters across all 30 Minecraft releases from 1.18.2 to 26.3.
- [ ] Forge 1.18.2 - 1.20.1 legacy adapter wiring.
- [ ] NeoForge 1.20.2 - 1.21.4 modern component adapters.
- [ ] Next-Gen 26.x Java 25 compatibility validation.

### Phase 5: Multiplayer & Server Authority (COMPLETED)
- [x] Client-to-server C2S construction packet protocol.
- [x] Server permission verification for survival world building.
- [x] Progressive tick-budgeted server block placement with anti-cheat tolerance.
