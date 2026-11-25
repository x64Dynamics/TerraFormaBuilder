from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS_DIR = ROOT / "versions"

SCULPT_HUD_SNIPPET = """        if (workspaceModel.isShapeEditorMode() && controller.isStudioBannerVisible()) {
            String sid = workspaceModel.selectedSectionId() != null ? workspaceModel.selectedSectionId() : "none";
            String tool = workspaceModel.activeSculptTool().name();
            String secInfo = "§aSculpt: §e" + tool + " §8| §7Section: §b" + sid + " §8| §7Sections: §f" + workspaceModel.activeArchProject().sections().size();
            g.{TEXT_METHOD}(mc.font, secInfo, 14, 66, 0xFFFFFFFF, false);
        }"""

MOUSE_SCULPT_CHECK_EVENT = """                    if (controller.model().isShapeEditorMode()) {
                        com.x64Dynamics.tfb.geometry.Direction tfbDir = switch (bhr.getDirection()) {
                            case NORTH -> com.x64Dynamics.tfb.geometry.Direction.NORTH;
                            case SOUTH -> com.x64Dynamics.tfb.geometry.Direction.SOUTH;
                            case WEST -> com.x64Dynamics.tfb.geometry.Direction.WEST;
                            case EAST -> com.x64Dynamics.tfb.geometry.Direction.EAST;
                            case UP -> com.x64Dynamics.tfb.geometry.Direction.UP;
                            case DOWN -> com.x64Dynamics.tfb.geometry.Direction.DOWN;
                        };
                        String msg = controller.model().sculptAt(pos, tfbDir, event.getButton() == 0);
                        mc.player.{MSG_METHOD}(Component.literal("§b[TFB] §f" + msg){MSG_FLAG});
                        event.setCanceled(true);
                        return;
                    }"""

MOUSE_SCULPT_CHECK_FABRIC = """            if (controller.model().isShapeEditorMode()) {
                com.x64Dynamics.tfb.geometry.Direction tfbDir = switch (bhr.getDirection()) {
                    case NORTH -> com.x64Dynamics.tfb.geometry.Direction.NORTH;
                    case SOUTH -> com.x64Dynamics.tfb.geometry.Direction.SOUTH;
                    case WEST -> com.x64Dynamics.tfb.geometry.Direction.WEST;
                    case EAST -> com.x64Dynamics.tfb.geometry.Direction.EAST;
                    case UP -> com.x64Dynamics.tfb.geometry.Direction.UP;
                    case DOWN -> com.x64Dynamics.tfb.geometry.Direction.DOWN;
                };
                String msg = controller.model().sculptAt(pos, tfbDir, button == 0);
                mc.player.{MSG_METHOD}({COMP_FN}("§b[TFB] §f" + msg){MSG_FLAG});
                return true;
            }"""

SECTION_RENDER_BOX = """        if (controller.model().isShapeEditorMode() && controller.model().selectedSectionId() != null) {
            controller.model().activeArchProject().findSection(controller.model().selectedSectionId()).ifPresent(sec -> {
                BoundingBox3i sb = sec.bounds();
                double sx0 = sb.min().x() - camPos.x - 0.03;
                double sy0 = sb.min().y() - camPos.y - 0.03;
                double sz0 = sb.min().z() - camPos.z - 0.03;
                double sx1 = sb.max().x() + 1.03 - camPos.x;
                double sy1 = sb.min().y() + sec.stories() * 4.0 + 1.03 - camPos.y;
                double sz1 = sb.max().z() + 1.03 - camPos.z;
                if (sec.isLocked()) {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, sx0, sy0, sz0, sx1, sy1, sz1, 1.0f, 0.78f, 0.1f, 1.0f);
                } else {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, sx0, sy0, sz0, sx1, sy1, sz1, 0.18f, 0.95f, 0.45f, 1.0f);
                }
            });
        }"""

def main() -> None:
    hud_count = 0
    mouse_count = 0
    render_count = 0

    for vdir in sorted(VERSIONS_DIR.iterdir()):
        if not vdir.is_dir():
            continue
        is_26 = vdir.name.startswith("26.")
        for loader in ("neoforge", "forge", "fabric"):
            cdir = vdir / loader / "src/main/java/com/x64Dynamics/tfb/client"
            if not cdir.exists():
                continue

            # 1. ArchitectHudRenderer.java
            hud_path = cdir / "ArchitectHudRenderer.java"
            if hud_path.exists():
                txt = hud_path.read_text(encoding="utf-8")
                if "isShapeEditorMode()" not in txt and "// 2. Bottom Dedicated Builder Hotbar" in txt:
                    text_method = "text" if is_26 else "drawString"
                    if "g.drawString(mc.font," in txt or "g.text(mc.font," in txt:
                        snippet = SCULPT_HUD_SNIPPET.replace("{TEXT_METHOD}", text_method)
                        txt = txt.replace("        // 2. Bottom Dedicated Builder Hotbar", snippet + "\n\n        // 2. Bottom Dedicated Builder Hotbar", 1)
                        hud_path.write_text(txt, encoding="utf-8")
                        hud_count += 1

            # 2. Mouse sculpting hook
            if loader in ("neoforge", "forge"):
                ev_path = cdir / "ArchitectClientEvents.java"
                if ev_path.exists():
                    txt = ev_path.read_text(encoding="utf-8")
                    if "sculptAt(" not in txt and "if (event.getButton() == 0)" in txt:
                        msg_method = "sendOverlayMessage" if is_26 else "displayClientMessage"
                        msg_flag = "" if is_26 else ", true"
                        snippet = MOUSE_SCULPT_CHECK_EVENT.replace("{MSG_METHOD}", msg_method).replace("{MSG_FLAG}", msg_flag)
                        txt = txt.replace("                    if (event.getButton() == 0)", snippet + "\n                    if (event.getButton() == 0)", 1)
                        ev_path.write_text(txt, encoding="utf-8")
                        mouse_count += 1
            else:
                ce_path = cdir / "ClientEntrypoint.java"
                if ce_path.exists():
                    txt = ce_path.read_text(encoding="utf-8")
                    if "sculptAt(" not in txt and "if (button == 0) {" in txt:
                        msg_method = "sendOverlayMessage" if is_26 else "displayClientMessage"
                        msg_flag = "" if is_26 else ", true"
                        comp_fn = "Component.literal" if "Component.literal(" in txt else "Component.nullToEmpty"
                        snippet = (
                            MOUSE_SCULPT_CHECK_FABRIC
                            .replace("{MSG_METHOD}", msg_method)
                            .replace("{MSG_FLAG}", msg_flag)
                            .replace("{COMP_FN}", comp_fn)
                        )
                        txt = txt.replace("            if (button == 0) {", snippet + "\n            if (button == 0) {", 1)
                        ce_path.write_text(txt, encoding="utf-8")
                        mouse_count += 1

            # 3. ArchitectWorldRenderer.java (1.18.2 - 1.21.11)
            if not is_26:
                ren_path = cdir / "ArchitectWorldRenderer.java"
                if ren_path.exists():
                    txt = ren_path.read_text(encoding="utf-8")
                    if "isShapeEditorMode()" not in txt and "WorldPreviewBuffer previewBuffer" in txt:
                        txt = txt.replace("        WorldPreviewBuffer previewBuffer", SECTION_RENDER_BOX + "\n\n        WorldPreviewBuffer previewBuffer", 1)
                        ren_path.write_text(txt, encoding="utf-8")
                        render_count += 1

    print(f"Updated HUD: {hud_count}, Mouse sculpt: {mouse_count}, Renderer: {render_count}")

if __name__ == "__main__":
    main()
