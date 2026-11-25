"""Propagate 3D block preview renderer (ArchitectWorldRenderer) across NeoForge and Forge targets."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC_NEO = ROOT / "versions/1.21.1/neoforge/src/main/java/com/x64Dynamics/tfb/client/ArchitectWorldRenderer.java"

def sync():
    neo_src = SRC_NEO.read_text(encoding="utf-8")

    # 1. NeoForge 1.20.2 - 1.21.11
    neo_early = neo_src.replace(
        '@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)\n',
        ''
    ).replace(
        'import net.neoforged.fml.common.EventBusSubscriber;\n',
        ''
    )

    neo_count = 0
    for d in sorted((ROOT / "versions").glob("*/neoforge")):
        mc = d.parts[-2]
        if mc.startswith("26."):
            continue
        dest = d / "src/main/java/com/x64Dynamics/tfb/client/ArchitectWorldRenderer.java"
        if mc in ("1.20.2", "1.20.3", "1.20.4"):
            dest.write_text(neo_early, encoding="utf-8", newline="\n")
            ep = d / "src/main/java/com/x64Dynamics/tfb/Entrypoint.java"
            if ep.exists():
                ep_txt = ep.read_text(encoding="utf-8")
                if "ArchitectWorldRenderer.class" not in ep_txt:
                    ep_txt = ep_txt.replace(
                        "NeoForge.EVENT_BUS.register(ArchitectClientEvents.ForgeBusEvents.class);",
                        "NeoForge.EVENT_BUS.register(ArchitectClientEvents.ForgeBusEvents.class);\n            NeoForge.EVENT_BUS.register(com.x64Dynamics.tfb.client.ArchitectWorldRenderer.class);"
                    )
                    ep.write_text(ep_txt, encoding="utf-8", newline="\n")
        else:
            dest.write_text(neo_src, encoding="utf-8", newline="\n")
        neo_count += 1

    # 2. Forge 1.19 - 1.21.11
    forge_src = (
        neo_src
        .replace("import net.neoforged.api.distmarker.Dist;", "import net.minecraftforge.api.distmarker.Dist;")
        .replace("import net.neoforged.bus.api.SubscribeEvent;", "import net.minecraftforge.eventbus.api.SubscribeEvent;")
        .replace("import net.neoforged.fml.common.EventBusSubscriber;", "import net.minecraftforge.fml.common.Mod;")
        .replace("import net.neoforged.neoforge.client.event.RenderLevelStageEvent;", "import net.minecraftforge.client.event.RenderLevelStageEvent;")
        .replace(
            '@EventBusSubscriber(modid = "terraformabuilder", value = Dist.CLIENT)',
            '@Mod.EventBusSubscriber(modid = "terraformabuilder", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)'
        )
    )

    forge_count = 0
    for d in sorted((ROOT / "versions").glob("*/forge")):
        mc = d.parts[-2]
        if mc == "1.18.2" or mc.startswith("26."):
            continue
        dest = d / "src/main/java/com/x64Dynamics/tfb/client/ArchitectWorldRenderer.java"
        dest.write_text(forge_src, encoding="utf-8", newline="\n")
        forge_count += 1

    print(f"Synced ArchitectWorldRenderer to {neo_count} NeoForge targets and {forge_count} Forge targets.")

if __name__ == "__main__":
    sync()
