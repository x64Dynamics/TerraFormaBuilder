from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS_DIR = ROOT / "versions"

HOOK_SNIPPET = """        if (mc.player == null) return;
        if (mc.level != null) {
            if (liveLevelAccess == null) {
                liveLevelAccess = new {ACCESS_CLASS}(mc.level);
                TFBClientController.getInstance().model().setLiveLevelAccess(liveLevelAccess);
            } else {
                liveLevelAccess.setLevel(mc.level);
            }
        }"""

def main() -> None:
    updated = 0
    for vdir in sorted(VERSIONS_DIR.iterdir()):
        if not vdir.is_dir():
            continue
        for loader, fname, cls in [
            ("neoforge", "TFBClientEvents.java", "NeoForgeLevelAccess"),
            ("forge", "TFBClientEvents.java", "ForgeLevelAccess"),
            ("fabric", "ClientEntrypoint.java", "FabricLevelAccess"),
        ]:
            fpath = vdir / loader / "src/main/java/com/x64Dynamics/tfb/client" / fname
            if not fpath.exists():
                continue
            txt = fpath.read_text(encoding="utf-8")
            if "liveLevelAccess" in txt:
                continue
            target = None
            for cand in (
                "        if (mc.player == null || mc.level == null) return;",
                "            if (mc.player == null) return;",
                "        if (mc.player == null) return;",
            ):
                if cand in txt:
                    target = cand
                    break
            if target is None:
                continue

            indent = "    " if target.startswith("            ") else ""
            field_decl = f"    {indent}private static {cls} liveLevelAccess;\n"
            if "public static class ForgeEvents" in txt:
                txt = txt.replace("    public static class ForgeEvents {\n", f"    public static class ForgeEvents {{\n{field_decl}", 1)
            elif "public static class ForgeBusEvents" in txt:
                txt = txt.replace("    public static class ForgeBusEvents {\n", f"    public static class ForgeBusEvents {{\n{field_decl}", 1)
            elif "public static void onClientTick(" in txt:
                txt = txt.replace("    public static void onClientTick(", f"{field_decl}    public static void onClientTick(", 1)

            snippet = "\n".join((indent + line if line else line) for line in HOOK_SNIPPET.splitlines())
            txt = txt.replace(target, snippet.replace("{ACCESS_CLASS}", cls), 1)
            fpath.write_text(txt, encoding="utf-8")
            updated += 1
            print(f"Wired {cls} in {vdir.name}/{loader}")
    print(f"Done: {updated} targets updated.")

if __name__ == "__main__":
    main()
