from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ["26.1", "26.1.1", "26.1.2", "26.2", "26.3"]

SHARED_CLIENT_FILES = [
    "TerraFormaScreen.java",
    "ArchitectFlightController.java",
    "ArchitectKeyMappings.java",
    "ArchitectHudRenderer.java",
    "BlockStateParser.java",
]


def adapt_common_26x(text: str) -> str:
    text = re.sub(r"\bResourceLocation\b", "Identifier", text)
    text = re.sub(r"\bGuiGraphics\b", "GuiGraphicsExtractor", text)
    text = text.replace("g.drawString(mc.font,", "g.text(mc.font,")
    text = text.replace("g.drawCenteredString(mc.font,", "g.centeredText(mc.font,")
    text = text.replace("g.renderItem(", "g.item(")
    text = text.replace("BuiltInRegistries.BLOCK.get(", "BuiltInRegistries.BLOCK.getValue(")
    text = text.replace("BuiltInRegistries.ITEM.get(", "BuiltInRegistries.ITEM.getValue(")
    text = re.sub(
        r"([a-zA-Z0-9_.]+)\.displayClientMessage\(\s*(Component\.[^(]+\([^;]+\))\s*,\s*true\s*\)",
        r"\1.sendOverlayMessage(\2)",
        text,
        flags=re.DOTALL,
    )
    text = re.sub(
        r"([a-zA-Z0-9_.]+)\.displayClientMessage\(\s*(Component\.[^(]+\([^;]+\))\s*,\s*false\s*\)",
        r"\1.sendSystemMessage(\2)",
        text,
        flags=re.DOTALL,
    )
    text = text.replace("net.minecraft.client.gui.screens.Screen.hasShiftDown()", "Minecraft.getInstance().hasShiftDown()")
    text = text.replace("Screen.hasShiftDown()", "Minecraft.getInstance().hasShiftDown()")
    text = text.replace("net.minecraft.client.gui.screens.Screen.hasAltDown()", "Minecraft.getInstance().hasAltDown()")
    text = text.replace("Screen.hasAltDown()", "Minecraft.getInstance().hasAltDown()")
    return text


def main() -> None:
    for ver in VERSIONS:
        nf_base = ROOT / "versions" / ver / "neoforge" / "src/main/java/com/x64Dynamics/tfb"
        if not nf_base.exists():
            continue

        for loader in ("fabric", "forge"):
            base = ROOT / "versions" / ver / loader / "src/main/java/com/x64Dynamics/tfb"
            if not base.exists():
                continue

            for fname in SHARED_CLIENT_FILES:
                src = nf_base / "client" / fname
                if src.exists():
                    shutil.copy2(src, base / "client" / fname)

            nf_scanner = nf_base / "BlockRegistryScanner.java"
            if nf_scanner.exists():
                shutil.copy2(nf_scanner, base / "BlockRegistryScanner.java")

            if loader == "fabric":
                tfs = base / "client/TerraFormaScreen.java"
                if tfs.exists():
                    tfs.write_text(
                        tfs.read_text(encoding="utf-8").replace(
                            "ArchitectClientEvents.ForgeEvents.executeBuild",
                            "ClientEntrypoint.executeBuild",
                        ),
                        encoding="utf-8",
                    )
                km = base / "client/ArchitectKeyMappings.java"
                if km.exists():
                    km_txt = km.read_text(encoding="utf-8")
                    if "registerAll(" not in km_txt:
                        km_txt = km_txt.replace(
                            "    private ArchitectKeyMappings() {}",
                            """    private ArchitectKeyMappings() {}

    public static void registerAll(net.minecraft.client.Minecraft mc) {
        if (mc == null || mc.options == null) return;
        try {
            java.lang.reflect.Field f = mc.options.getClass().getDeclaredField("keyMappings");
            f.setAccessible(true);
            KeyMapping[] current = (KeyMapping[]) f.get(mc.options);
            java.util.List<KeyMapping> list = new java.util.ArrayList<>(java.util.Arrays.asList(current));
            boolean added = false;
            for (KeyMapping km : ALL) {
                if (!list.contains(km)) {
                    list.add(km);
                    added = true;
                }
            }
            if (added) {
                f.set(mc.options, list.toArray(new KeyMapping[0]));
            }
        } catch (Throwable ignored) {
        }
    }""",
                        )
                        km.write_text(km_txt, encoding="utf-8")
                ce = base / "client/ClientEntrypoint.java"
                if ce.exists():
                    ce_txt = adapt_common_26x(ce.read_text(encoding="utf-8"))
                    ce_txt = ce_txt.replace("private static void executeBuild(", "public static void executeBuild(")
                    ce.write_text(ce_txt, encoding="utf-8")
                gm = base / "mixin/GuiMixin.java"
                if gm.exists():
                    gm.write_text(adapt_common_26x(gm.read_text(encoding="utf-8")), encoding="utf-8")
            else:
                tfs = base / "client/TerraFormaScreen.java"
                if tfs.exists():
                    tfs.write_text(
                        tfs.read_text(encoding="utf-8").replace(
                            "ArchitectClientEvents.ForgeEvents.executeBuild",
                            "ArchitectClientEvents.ForgeBusEvents.executeBuild",
                        ),
                        encoding="utf-8",
                    )
                te = base / "client/ArchitectClientEvents.java"
                if te.exists():
                    txt = adapt_common_26x(te.read_text(encoding="utf-8"))
                    txt = txt.replace(
                        "public static void onClientTick(TickEvent.ClientTickEvent event) {\n            if (event.phase != TickEvent.Phase.END) return;",
                        "public static void onClientTick(TickEvent.ClientTickEvent.Post event) {",
                    )
                    txt = txt.replace(
                        "net.minecraftforge.eventbus.api.SubscribeEvent",
                        "net.minecraftforge.eventbus.api.listener.SubscribeEvent",
                    )
                    txt = txt.replace("private static void executeBuild(", "public static void executeBuild(")
                    txt = re.sub(
                        r"public static void onMouseScroll\(InputEvent\.MouseScrollingEvent event\) \{(.*?)\n        \}",
                        lambda m: "public static boolean onMouseScroll(InputEvent.MouseScrollingEvent event) {"
                        + m.group(1)
                            .replace("return;", "return false;")
                            .replace("event.setCanceled(true);", "return true;")
                        + "\n            return false;\n        }",
                        txt,
                        flags=re.DOTALL,
                    )
                    te.write_text(txt, encoding="utf-8")

            print(f"Adapted 26.x {ver}/{loader}")


if __name__ == "__main__":
    main()
