"""Modular codebase cleanup, class renaming, and package reorganization tool."""
from pathlib import Path
import re
import time

ROOT = Path(__file__).resolve().parents[2]
COMMON_MAIN = ROOT / "common/src/main/java"
COMMON_TEST = ROOT / "common/src/test/java"
VERSIONS_DIR = ROOT / "versions"
TOOLS_DIR = ROOT / "tools/kyro"

# Exact FQN -> New FQN mapping for common/src/main/java classes
COMMON_MOVES = {
    # Remove TFB prefix & organize packages
    "com.x64Dynamics.tfb.i18n.TFBLocalization": "com.x64Dynamics.tfb.i18n.Localization",
    "com.x64Dynamics.tfb.platform.TFBPlatform": "com.x64Dynamics.tfb.platform.PlatformBridgeRegistry",
    "com.x64Dynamics.tfb.ui.TFBClientController": "com.x64Dynamics.tfb.ui.controller.ClientController",

    # Remove Engine / System / duplicate names
    "com.x64Dynamics.tfb.architectural.AutoArchitectEngine": "com.x64Dynamics.tfb.architectural.AutonomousArchitect",
    "com.x64Dynamics.tfb.architectural.RoadInfrastructureEngine": "com.x64Dynamics.tfb.architectural.RoadInfrastructurePlanner",
    "com.x64Dynamics.tfb.architectural.WallSegmentationEngine": "com.x64Dynamics.tfb.architectural.facade.WallSegmenter",
    "com.x64Dynamics.tfb.architectural.style.ProceduralVariationEngine": "com.x64Dynamics.tfb.architectural.style.ProceduralVariator",
    "com.x64Dynamics.tfb.city.CityPlannerEngine": "com.x64Dynamics.tfb.city.CityPlanner",
    "com.x64Dynamics.tfb.grid.ArchitecturalVariationEngine": "com.x64Dynamics.tfb.grid.ComplexVariationPlanner",
    "com.x64Dynamics.tfb.grid.ConnectionEngine": "com.x64Dynamics.tfb.grid.ComplexConnectionPlanner",
    "com.x64Dynamics.tfb.grid.PlacementRuleEngine": "com.x64Dynamics.tfb.grid.PlacementValidator",
    "com.x64Dynamics.tfb.taxonomy.BlockTaxonomyEngine": "com.x64Dynamics.tfb.taxonomy.BlockTaxonomyClassifier",
    "com.x64Dynamics.tfb.terrain.ExteriorLandscapingEngine": "com.x64Dynamics.tfb.terrain.ExteriorLandscaper",
    "com.x64Dynamics.tfb.architectural.editor.BuildingSection": "com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection",

    # Organize architectural subpackages
    "com.x64Dynamics.tfb.architectural.InteriorFloorPlanner": "com.x64Dynamics.tfb.architectural.interior.InteriorFloorPlanner",
    "com.x64Dynamics.tfb.architectural.InteriorFurnishingGenerator": "com.x64Dynamics.tfb.architectural.interior.InteriorFurnishingGenerator",
    "com.x64Dynamics.tfb.architectural.InteriorRoom": "com.x64Dynamics.tfb.architectural.interior.InteriorRoom",
    "com.x64Dynamics.tfb.architectural.RoomType": "com.x64Dynamics.tfb.architectural.interior.RoomType",

    "com.x64Dynamics.tfb.architectural.RoofGenerator": "com.x64Dynamics.tfb.architectural.roof.RoofGenerator",
    "com.x64Dynamics.tfb.architectural.AdvancedRoofFeatures": "com.x64Dynamics.tfb.architectural.roof.AdvancedRoofFeatures",

    "com.x64Dynamics.tfb.architectural.WallGenerator": "com.x64Dynamics.tfb.architectural.facade.WallGenerator",
    "com.x64Dynamics.tfb.architectural.OpeningGenerator": "com.x64Dynamics.tfb.architectural.facade.OpeningGenerator",
    "com.x64Dynamics.tfb.architectural.PillarGenerator": "com.x64Dynamics.tfb.architectural.facade.PillarGenerator",
    "com.x64Dynamics.tfb.architectural.FacadeOrnamentGenerator": "com.x64Dynamics.tfb.architectural.facade.FacadeOrnamentGenerator",
    "com.x64Dynamics.tfb.architectural.LayeredFacadeComposer": "com.x64Dynamics.tfb.architectural.facade.LayeredFacadeComposer",

    "com.x64Dynamics.tfb.architectural.FloorGenerator": "com.x64Dynamics.tfb.architectural.layout.FloorGenerator",
    "com.x64Dynamics.tfb.architectural.StaircaseGenerator": "com.x64Dynamics.tfb.architectural.layout.StaircaseGenerator",
    "com.x64Dynamics.tfb.architectural.TowerGenerator": "com.x64Dynamics.tfb.architectural.layout.TowerGenerator",
    "com.x64Dynamics.tfb.architectural.WingConfiguration": "com.x64Dynamics.tfb.architectural.layout.WingConfiguration",
    "com.x64Dynamics.tfb.architectural.WingLayoutGenerator": "com.x64Dynamics.tfb.architectural.layout.WingLayoutGenerator",
    "com.x64Dynamics.tfb.architectural.ParametricFootprint": "com.x64Dynamics.tfb.architectural.layout.ParametricFootprint",
    "com.x64Dynamics.tfb.architectural.BuildingSection": "com.x64Dynamics.tfb.architectural.layout.BuildingSection",
    "com.x64Dynamics.tfb.architectural.ConnectedBuildingComposer": "com.x64Dynamics.tfb.architectural.layout.ConnectedBuildingComposer",
}

VERSION_SIMPLE_RENAMES = {
    "TFBClientRenderer": "ArchitectWorldRenderer",
    "TFBInWorldHud": "ArchitectHudRenderer",
    "TFBKeyMappings": "ArchitectKeyMappings",
    "TFBClientEvents": "ArchitectClientEvents",
}

TEST_RENAMES = {
    "AutoArchitectEngineTest": "AutonomousArchitectTest",
    "GridComplexSystemTest": "GridComplexTest",
}


def fqn_to_path(root: Path, fqn: str) -> Path:
    return root / (fqn.replace(".", "/") + ".java")


def split_fqn(fqn: str) -> tuple[str, str]:
    pkg, _, cls = fqn.rpartition(".")
    return pkg, cls


def discover_all_common_classes() -> dict[str, str]:
    """Returns old_fqn -> old_pkg for all classes in common/src/main/java (idempotent if partially moved)."""
    reverse_moves = {v: k for k, v in COMMON_MOVES.items()}
    out = {}
    for p in COMMON_MAIN.rglob("*.java"):
        rel = p.relative_to(COMMON_MAIN).as_posix()[:-5]
        fqn = rel.replace("/", ".")
        old_fqn = reverse_moves.get(fqn, fqn)
        out[old_fqn] = old_fqn.rpartition(".")[0]
    return out


def safe_write(path: Path, text: str) -> None:
    for attempt in range(3):
        try:
            path.write_text(text, encoding="utf-8")
            return
        except OSError:
            if attempt == 2:
                raise
            time.sleep(0.15)


def update_java_file(path: Path, old_fqn: str | None, new_fqn: str | None, all_old_classes: dict[str, str]) -> None:
    orig_text = path.read_text(encoding="utf-8")
    text = orig_text
    m_pkg = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.MULTILINE)
    orig_pkg = m_pkg.group(1) if m_pkg else ""
    target_pkg = split_fqn(new_fqn)[0] if new_fqn else orig_pkg

    # 1. Update package declaration if moved
    if new_fqn and target_pkg != orig_pkg:
        text = re.sub(
            r"(^\s*package\s+)[\w.]+(\s*;)",
            rf"\g<1>{target_pkg}\2",
            text,
            count=1,
            flags=re.MULTILINE,
        )

    # 2. Track if file had wildcard import of any source package whose classes moved
    wildcard_pkgs = set(re.findall(r"^\s*import\s+([\w.]+)\.\*\s*;", text, re.MULTILINE))
    had_implicit_access = set()
    if orig_pkg:
        had_implicit_access.add(orig_pkg)
    had_implicit_access.update(wildcard_pkgs)

    # 3. Replace explicit FQN references (longest first to avoid partial matches)
    # Handle com.x64Dynamics.tfb.architectural.editor.BuildingSection first!
    for o_fqn, n_fqn in sorted(COMMON_MOVES.items(), key=lambda kv: -len(kv[0])):
        text = re.sub(r"\b" + re.escape(o_fqn) + r"\b", n_fqn, text)

    # 4. Replace simple names for renamed classes (skip editor.BuildingSection except inside editor or when unambiguous)
    simple_renames = {}
    for o_fqn, n_fqn in COMMON_MOVES.items():
        o_pkg, o_cls = split_fqn(o_fqn)
        n_pkg, n_cls = split_fqn(n_fqn)
        if o_cls != n_cls and o_cls != "BuildingSection":
            simple_renames[o_cls] = n_cls

    # Special handling for editor.BuildingSection -> EditableBuildingSection
    if (
        orig_pkg == "com.x64Dynamics.tfb.architectural.editor"
        or "com.x64Dynamics.tfb.architectural.editor.EditableBuildingSection" in text
    ):
        text = re.sub(r"\bBuildingSection\b", "EditableBuildingSection", text)

    for o_cls, n_cls in simple_renames.items():
        text = re.sub(r"\b" + re.escape(o_cls) + r"\b", n_cls, text)

    # Also rename own class name if moved/renamed
    if old_fqn and new_fqn:
        _, o_cls = split_fqn(old_fqn)
        _, n_cls = split_fqn(new_fqn)
        if o_cls != n_cls:
            text = re.sub(r"\b" + re.escape(o_cls) + r"\b", n_cls, text)

    # 5. Add missing imports for:
    #    (a) classes that moved OUT of orig_pkg (or wildcard_pkgs) and are referenced in this file
    #    (b) classes that stayed in orig_pkg when THIS file moved OUT of orig_pkg
    needed_imports = set()
    for o_fqn, o_pkg in all_old_classes.items():
        n_fqn = COMMON_MOVES.get(o_fqn, o_fqn)
        n_pkg, n_cls = split_fqn(n_fqn)
        if n_pkg == target_pkg:
            continue  # same target package, no import needed
        if o_pkg in had_implicit_access or orig_pkg in had_implicit_access:
            # Check if n_cls is referenced as an identifier in text and not already imported
            if re.search(r"\b" + re.escape(n_cls) + r"\b", text):
                if f"import {n_fqn};" not in text and f"import {n_pkg}.*;" not in text:
                    # Avoid importing if it's only a local method/variable or already FQN
                    # Only import if not preceded by '.' (like foo.BuildingSection) or if static/class usage
                    if re.search(r"(?<![.\w])" + re.escape(n_cls) + r"\b", text):
                        needed_imports.add(n_fqn)

    if needed_imports:
        import_block = "\n".join(f"import {fqn};" for fqn in sorted(needed_imports))
        # Insert after package line (and after existing imports if any)
        m_last_imp = None
        for m in re.finditer(r"^\s*import\s+[^;]+;\s*$", text, re.MULTILINE):
            m_last_imp = m
        if m_last_imp:
            idx = m_last_imp.end()
            text = text[:idx] + "\n" + import_block + text[idx:]
        elif m_pkg:
            m_pkg_cur = re.search(r"^\s*package\s+[\w.]+;\s*$", text, re.MULTILINE)
            if m_pkg_cur:
                idx = m_pkg_cur.end()
                text = text[:idx] + "\n\n" + import_block + text[idx:]

    # 6. Version client class renames if in versions/
    if path.is_relative_to(VERSIONS_DIR):
        for o_cls, n_cls in VERSION_SIMPLE_RENAMES.items():
            text = re.sub(r"\b" + re.escape(o_cls) + r"\b", n_cls, text)

    if text != orig_text:
        safe_write(path, text)


def main() -> None:
    all_old_classes = discover_all_common_classes()

    # 1. Update all common/src/main/java files and move renamed ones
    main_files = list(COMMON_MAIN.rglob("*.java"))
    for p in main_files:
        rel = p.relative_to(COMMON_MAIN).as_posix()[:-5]
        old_fqn = rel.replace("/", ".")
        new_fqn = COMMON_MOVES.get(old_fqn)
        update_java_file(p, old_fqn, new_fqn, all_old_classes)
        if new_fqn and new_fqn != old_fqn:
            dest = fqn_to_path(COMMON_MAIN, new_fqn)
            dest.parent.mkdir(parents=True, exist_ok=True)
            p.replace(dest)

    # 2. Update all common/src/test/java files and rename test classes
    for p in list(COMMON_TEST.rglob("*.java")):
        update_java_file(p, None, None, all_old_classes)
        stem = p.stem
        if stem in TEST_RENAMES:
            new_stem = TEST_RENAMES[stem]
            txt = p.read_text(encoding="utf-8")
            txt = re.sub(r"\b" + re.escape(stem) + r"\b", new_stem, txt)
            dest = p.with_name(new_stem + ".java")
            p.write_text(txt, encoding="utf-8")
            p.replace(dest)

    # 3. Update all versions/ Java files and rename TFB* client files
    for p in list(VERSIONS_DIR.rglob("*.java")):
        update_java_file(p, None, None, all_old_classes)
        if p.stem in VERSION_SIMPLE_RENAMES:
            dest = p.with_name(VERSION_SIMPLE_RENAMES[p.stem] + ".java")
            p.replace(dest)

    # 4. Update tools/kyro/*.py references (excluding this script itself)
    for py in TOOLS_DIR.glob("*.py"):
        if py.name == Path(__file__).name:
            continue
        txt = py.read_text(encoding="utf-8")
        orig = txt
        for o_fqn, n_fqn in sorted(COMMON_MOVES.items(), key=lambda kv: -len(kv[0])):
            txt = re.sub(r"\b" + re.escape(o_fqn) + r"\b", n_fqn, txt)
        for o_fqn, n_fqn in COMMON_MOVES.items():
            o_cls = split_fqn(o_fqn)[1]
            n_cls = split_fqn(n_fqn)[1]
            if o_cls != n_cls and o_cls != "BuildingSection":
                txt = re.sub(r"\b" + re.escape(o_cls) + r"\b", n_cls, txt)
        for o_cls, n_cls in VERSION_SIMPLE_RENAMES.items():
            txt = re.sub(r"\b" + re.escape(o_cls) + r"\b", n_cls, txt)
        if txt != orig:
            py.write_text(txt, encoding="utf-8")

    # Clean up empty directories under common/src/main/java
    for d in sorted(COMMON_MAIN.rglob("*"), reverse=True):
        if d.is_dir() and not any(d.iterdir()):
            d.rmdir()

    print("Refactor cleanup completed.")


if __name__ == "__main__":
    main()
