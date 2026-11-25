"""Synchronize server-authoritative construction execution and selection handlers across targets."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]

SERVER_BUILD_BLOCK = """            if (targets.isEmpty()) {
                mc.player.displayClientMessage(
                    Component.nullToEmpty("§c[TFB] Build failed: No preview blocks available! Set POS1/POS2 or configure a brush in Workspace [B]."),
                    false
                );
                return;
            }

            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    server.execute(() -> {
                        int placed = 0;
                        for (PlacementTarget target : targets) {
                            Vec3i pos = target.pos();
                            BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                            BlockState state = BlockStateParser.parse(target.block());
                            if (serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                            }
                        }
                        final int finalPlaced = placed;
                        serverLevel.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                        mc.execute(() -> {
                            mc.player.displayClientMessage(
                                Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + finalPlaced + " blocks with server lighting."),
                                false
                            );
                        });
                    });
                    return;
                }
            }

            int placed = 0;
            for (PlacementTarget target : targets) {
                Vec3i pos = target.pos();
                BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
                BlockState state = BlockStateParser.parse(target.block());
                if (level.setBlock(bp, state, 3)) {
                    placed++;
                }
            }

            mc.player.displayClientMessage(
                Component.nullToEmpty("§a[TFB] Construction complete! Successfully placed " + placed + " client blocks."),
                false
            );"""

OLD_BODY_PATTERN = re.compile(
    r'(\s*int placed = 0;\s*for \(PlacementTarget target : targets\) \{[\s\S]*?mc\.player\.displayClientMessage\([\s\S]*?\);\s*)'
)

def sync_construction():
    updated = 0
    for file_path in (ROOT / 'versions').rglob('*.java'):
        content = file_path.read_text(encoding='utf-8')
        if 'void executeBuild(Minecraft mc)' in content and 'ServerLevel serverLevel' not in content:
            idx = content.find('void executeBuild(Minecraft mc)')
            m = OLD_BODY_PATTERN.search(content, idx)
            if m:
                new_content = content[:m.start(1)] + '\n' + SERVER_BUILD_BLOCK + '\n        ' + content[m.end(1):]
                file_path.write_text(new_content, encoding='utf-8', newline='\n')
                updated += 1
    print(f'Synchronized executeBuild across {updated} target files.')
    return updated

if __name__ == '__main__':
    sync_construction()
