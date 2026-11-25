"""Enhance ArchitectWorkspaceScreen with manual coordinate input fields for POS1 and POS2."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN_FILE = ROOT / 'common/src/main/java/com/x64Dynamics/tfb/ui/screen/ArchitectWorkspaceScreen.java'

NEW_BLOCK = """                // Row 2: Manual Coordinate Inputs for POS1 and POS2
                String p1Coord = sel.pos1() != null ? sel.pos1().x() + " " + sel.pos1().y() + " " + sel.pos1().z() : "";
                UITextField p1Field = new UITextField(new UIBounds(contentX, contentY + 28, contentWidth / 4 - 4, rowHeight), p1Coord, "POS1 (X Y Z)", null);
                tabWidgets.add(p1Field);

                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 28, contentWidth / 4 - 4, rowHeight), "Apply POS1", b -> {
                    com.x64Dynamics.tfb.geometry.Vec3i p = parseVec3i(p1Field.text());
                    if (p != null) {
                        model.setPos1(p);
                        rebuildTabWidgets();
                        setStatus("Applied POS1: " + p);
                    } else {
                        setStatus("Invalid format: use 'X Y Z'");
                    }
                }));

                String p2Coord = sel.pos2() != null ? sel.pos2().x() + " " + sel.pos2().y() + " " + sel.pos2().z() : "";
                UITextField p2Field = new UITextField(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 28, contentWidth / 4 - 4, rowHeight), p2Coord, "POS2 (X Y Z)", null);
                tabWidgets.add(p2Field);

                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 28, contentWidth / 4 - 4, rowHeight), "Apply POS2", b -> {
                    com.x64Dynamics.tfb.geometry.Vec3i p = parseVec3i(p2Field.text());
                    if (p != null) {
                        model.setPos2(p);
                        rebuildTabWidgets();
                        setStatus("Applied POS2: " + p);
                    } else {
                        setStatus("Invalid format: use 'X Y Z'");
                    }
                }));

                // Row 3: Region operations (Expand, Contract, Center, Clear)
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 56, contentWidth / 4 - 4, rowHeight), "Expand (+1)", b -> {
                    model.expandSelection(1);
                    rebuildTabWidgets();
                    setStatus("Expanded selection by 1 block.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + contentWidth / 4, contentY + 56, contentWidth / 4 - 4, rowHeight), "Contract (-1)", b -> {
                    model.contractSelection(1);
                    rebuildTabWidgets();
                    setStatus("Contracted selection by 1 block.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 2, contentY + 56, contentWidth / 4 - 4, rowHeight), "Center Origin", b -> {
                    model.centerSelection();
                    rebuildTabWidgets();
                    setStatus("Centered origin inside selection.");
                }));
                tabWidgets.add(new UIButton(new UIBounds(contentX + (contentWidth / 4) * 3, contentY + 56, contentWidth / 4 - 4, rowHeight), "Clear All", b -> {
                    model.clearSelection();
                    rebuildTabWidgets();
                    setStatus("Cleared entire selection.");
                }));

                // Row 4: Selection Stats & Grid Snap
                String info = "Size: " + model.width() + "x" + model.height() + "x" + model.depth() + " | Vol: " + sel.volume() + " blocks";
                tabWidgets.add(new UIButton(new UIBounds(contentX, contentY + 84, contentWidth / 2 - 4, rowHeight), info, b -> {}));
                tabWidgets.add(new UISlider(new UIBounds(contentX + contentWidth / 2, contentY + 84, contentWidth / 2 - 4, rowHeight), "Grid Snap", 1, 16, sel.gridSnap(), sel::setGridSnap));""".replace('\r\n', '\n')

HELPER = """    private com.x64Dynamics.tfb.geometry.Vec3i parseVec3i(String str) {
        if (str == null) return null;
        String[] parts = str.replaceAll("[(),]", " ").trim().split("\\\\s+");
        if (parts.length >= 3) {
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                return new com.x64Dynamics.tfb.geometry.Vec3i(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
""".replace('\r\n', '\n')

def update_screen():
    txt = SCREEN_FILE.read_text(encoding='utf-8').replace('\r\n', '\n')
    start_tag = '// Row 2: Region operations (Expand, Contract, Center, Clear)'
    end_tag = 'sel::setGridSnap));'
    start_idx = txt.find(start_tag)
    end_idx = txt.find(end_tag, start_idx) + len(end_tag)
    assert start_idx != -1 and end_idx != -1, 'Tags not found'

    txt = txt[:start_idx] + NEW_BLOCK.strip() + txt[end_idx:]
    txt = txt.replace('    public void setStatus(String message) {', HELPER + '\n    public void setStatus(String message) {')
    txt = txt.replace('int tabContentHeight = 88;', 'int tabContentHeight = 114;')
    SCREEN_FILE.write_text(txt, encoding='utf-8', newline='\n')
    print('PASS: Updated ArchitectWorkspaceScreen with manual coordinate input fields.')

if __name__ == '__main__':
    update_screen()
