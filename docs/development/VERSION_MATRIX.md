# TerraFormaBuilder — Version & Loader Target Matrix

## Authoritative Target Registry (1.18.2 – 26.3)

Target status lifecycle:
`Missing → In Progress → Implemented → Built → Runtime Tested → Verified`

| Minecraft Release | Java | Loader | Implementation State | Build Status | Runtime Status | Notes |
|---|---|---|---|---|---|---|
| **1.18.2** | 17 | Fabric | In Progress | Pending | Pending | Common-first adapter |
| **1.18.2** | 17 | Forge | In Progress | Pending | Pending | Forge event lifecycle |
| **1.19.0** | 17 | Fabric | In Progress | Pending | Pending | Chat / Component API changes |
| **1.19.0** | 17 | Forge | In Progress | Pending | Pending | Forge 41.1.0 |
| **1.19.1** | 17 | Fabric | In Progress | Pending | Pending | Signature / Chat system |
| **1.19.1** | 17 | Forge | In Progress | Pending | Pending | Forge 42.0.9 |
| **1.19.2** | 17 | Fabric | In Progress | Pending | Pending | LTS Modpack target |
| **1.19.2** | 17 | Forge | In Progress | Pending | Pending | LTS Modpack target |
| **1.19.3** | 17 | Fabric | In Progress | Pending | Pending | Experimental features |
| **1.19.3** | 17 | Forge | In Progress | Pending | Pending | Forge 44.1.0 |
| **1.19.4** | 17 | Fabric | In Progress | Pending | Pending | Display entities introduced |
| **1.19.4** | 17 | Forge | In Progress | Pending | Pending | Forge 45.4.0 |
| **1.20.0** | 17 | Fabric | In Progress | Pending | Pending | Trails & Tales |
| **1.20.0** | 17 | Forge | In Progress | Pending | Pending | Trails & Tales |
| **1.20.1** | 17 | Fabric | **Implemented** | **Built** | **Verified** | Full hotbar, HUD, screen, flight |
| **1.20.1** | 17 | Forge | **Implemented** | **Built** | **Verified** | Forge 47.4.28, full HUD, events, flight, blur fixed |
| **1.20.2** | 17 | Fabric | In Progress | Pending | Pending | Network configuration phase |
| **1.20.2** | 17 | Forge | In Progress | Pending | Pending | GuiGraphics migration |
| **1.20.2** | 17 | NeoForge | In Progress | Pending | Pending | Early NeoForge fork |
| **1.20.3** | 17 | Fabric | In Progress | Pending | Pending | Minor release |
| **1.20.3** | 17 | NeoForge | In Progress | Pending | Pending | Minor release |
| **1.20.4** | 17 | Fabric | In Progress | Pending | Pending | Stable 1.20.4 release |
| **1.20.4** | 17 | NeoForge | **Implemented** | **Built** | **Verified** | NeoForge 20.4.x, full HUD, events, flight, blur fixed |
| **1.20.5** | 21 | Fabric | In Progress | Pending | Pending | Java 21 migration + Components |
| **1.20.5** | 21 | NeoForge | In Progress | Pending | Pending | Java 21 migration |
| **1.20.6** | 21 | Fabric | In Progress | Pending | Pending | Java 21 + Component Data Fix |
| **1.20.6** | 21 | NeoForge | In Progress | Pending | Pending | Java 21 LTS |
| **1.21.0** | 21 | Fabric | In Progress | Pending | Pending | Tricky Trials initial |
| **1.21.0** | 21 | NeoForge | In Progress | Pending | Pending | Tricky Trials initial |
| **1.21.1** | 21 | Fabric | In Progress | Pending | Pending | Tricky Trials LTS |
| **1.21.1** | 21 | NeoForge | **Implemented** | **Built** | **Verified** | Primary reference target, flight, hotbar, blur fixed |
| **1.21.2** | 21 | Fabric | In Progress | Pending | Pending | Bundles update |
| **1.21.2** | 21 | NeoForge | In Progress | Pending | Pending | Bundles update |
| **1.21.3** | 21 | Fabric | In Progress | Pending | Pending | Pale Garden / Eyeblossom |
| **1.21.3** | 21 | NeoForge | In Progress | Pending | Pending | Pale Garden / Eyeblossom |
| **1.21.4** | 21 | Fabric | In Progress | Pending | Pending | Current Vanilla release |
| **1.21.4** | 21 | NeoForge | In Progress | Pending | Pending | Current NeoForge release |
| **26.1** | 25 | Fabric | In Progress | Pending | Pending | Next-Gen target |
| **26.2** | 25 | Fabric | In Progress | Pending | Pending | Next-Gen target |
| **26.3** | 25 | Fabric | In Progress | Pending | Pending | Next-Gen target |

---

## Verification & Compilation Commands
- **Core Common Library**: `python tools/kyro/template.py core`
- **NeoForge 1.21.1**: `python tools/kyro/template.py gradle build -Pmc=1.21.1 -Ploader=neoforge`
- **Fabric 1.20.1**: `python tools/kyro/template.py gradle build -Pmc=1.20.1 -Ploader=fabric`
- **Matrix Validation**: `python tools/kyro/template.py validate`
