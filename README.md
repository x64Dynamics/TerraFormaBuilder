# TerraFormaBuilder (TFB)

**Author:** x64Dynamics | **Mod ID:** `terraformabuilder` | **Platform:** Minecraft Java Edition

An intelligent architectural design and automated construction mod that allows players to design, customise, preview, and generate complex structures directly inside Minecraft worlds without manually placing individual blocks.

**Core philosophy: The player is the architect. TFB is the construction engine.**

---

## Technical Architecture

* **Multi-Version Range:** Minecraft **1.18.2 through 26.3** (all stable releases in-between) across **Fabric**, **Forge**, and **NeoForge**.
* **Language Baseline:** Pure Java 17 core (`common/`), with target-specific toolchains (Java 17, 21, and 25) managed automatically per Minecraft version.
* **Architecture:** Shared unencumbered multi-loader design (no Architectury, no Stonecutter, no local binary mod runtime dependencies).
* **Root Package:** `com.x64Dynamics.tfb`

---

## Project Structure

```
TerraFormaBuilder/
├── common/                               # PURE JAVA 17 CORE (Zero MC / Loader Imports)
│   ├── src/main/java/com/x64Dynamics/tfb/
│   │   ├── TerraFormaBuilder.java        # Core mod entrypoint & constants
│   │   ├── config/                       # Mod configuration logic
│   │   ├── gameplay/                     # Core rules & mathematical models
│   │   └── platform/                     # Abstract platform contracts (SPI)
│   └── src/main/resources/               # Universal assets & language files
├── versions/
│   ├── <mc_version>/common/              # Version-shared Minecraft API bindings
│   └── <mc_version>/<loader>/            # Loader entrypoints, client rendering, mixins
│       └── src/main/java/com/x64Dynamics/tfb/<loader>/
│           └── Entrypoint.java
├── properties/                           # Pinned dependencies per Minecraft release
└── tools/kyro/                           # Build dispatcher & validation tooling
```

---

## Commands & Verification

### Structural & Matrix Validation
Verifies target inventory, source isolation, package consistency, duplicate class/resource checks:
```powershell
python tools/kyro/template.py validate
```

### Core Logic Build (Java 17)
Compiles and tests pure common logic without launching Minecraft:
```powershell
python tools/kyro/template.py core
```

### Building Specific Version Targets
```powershell
.\modw.bat '-Pmc=1.18.2' '-Ploader=fabric' build
.\modw.bat '-Pmc=1.20.1' '-Ploader=forge' build
.\modw.bat '-Pmc=1.21.1' '-Ploader=neoforge' build
.\modw.bat '-Pmc=26.3' '-Ploader=fabric' build
```

### Running Client / Server for Development
```powershell
.\modw.bat '-Pmc=1.21.1' '-Ploader=neoforge' runClient
.\modw.bat '-Pmc=1.20.1' '-Ploader=fabric' runServer
```

To list all 30 releases and 80 loader targets:
```powershell
python tools/kyro/template.py list
```
