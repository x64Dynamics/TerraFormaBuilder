"""Propagate BlockRegistryScanner, storage paths, and dependency-scheduled executeBuild across all 80 targets."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

SCANNER_BUILT_IN = """package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyRegistry;
import com.x64Dynamics.tfb.taxonomy.BlockTrait;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class BlockRegistryScanner {
    private static boolean scanned = false;

    private BlockRegistryScanner() { }

    public static synchronized void scanIfNeeded() {
        if (scanned) return;
        scanned = true;

        BlockTaxonomyRegistry registry = BlockTaxonomyRegistry.getInstance();
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                continue;
            }

            EnumSet<BlockTrait> traits = EnumSet.noneOf(BlockTrait.class);
            if (block instanceof StairBlock) traits.add(BlockTrait.STAIR);
            else if (block instanceof SlabBlock) traits.add(BlockTrait.SLAB);
            else if (block instanceof WallBlock) traits.add(BlockTrait.WALL);
            else if (block instanceof FenceGateBlock) traits.add(BlockTrait.FENCE_GATE);
            else if (block instanceof FenceBlock) traits.add(BlockTrait.FENCE);
            else if (block instanceof DoorBlock) traits.add(BlockTrait.DOOR);
            else if (block instanceof TrapDoorBlock) traits.add(BlockTrait.TRAPDOOR);
            else if (block instanceof RotatedPillarBlock) traits.add(BlockTrait.PILLAR);
            else if (block instanceof IronBarsBlock || block instanceof StainedGlassPaneBlock) traits.add(BlockTrait.GLASS_PANE);
            else if (block instanceof LanternBlock) traits.add(BlockTrait.LANTERN);
            else if (block instanceof TorchBlock) traits.add(BlockTrait.TORCH);
            else if (block instanceof CarpetBlock) traits.add(BlockTrait.CARPET);
            else if (block instanceof LiquidBlock || block instanceof BaseFireBlock) continue;
            else traits.add(BlockTrait.SOLID_CUBE);

            Set<String> propNames = new HashSet<>();
            for (Property<?> prop : block.getStateDefinition().getProperties()) {
                propNames.add(prop.getName());
            }

            BlockState defaultState = block.defaultBlockState();
            int luminance = defaultState.getLightEmission();
            if (luminance > 0) {
                traits.add(BlockTrait.LIGHT_EMITTING);
            }

            registry.registerDiscovered(BlockIdentifier.of(key.toString()), traits, propNames, luminance);
        }
    }
}
"""

SCANNER_LEGACY = SCANNER_BUILT_IN.replace(
    "import net.minecraft.core.registries.BuiltInRegistries;",
    "import net.minecraft.core.Registry;"
).replace("BuiltInRegistries.BLOCK", "Registry.BLOCK")

LEGACY_REG_VERSIONS = {"1.18.2", "1.19", "1.19.1", "1.19.2"}

EXEC_STUB_OLD = """            while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                mc.player.displayClientMessage(
                        Component.literal("§a[TFB] Build execution started..."),
                        true
                );
            }"""

EXEC_STUB_NEW = """            while (TFBKeyMappings.EXECUTE_BUILD.consumeClick()) {
                executeBuild(mc);
            }"""

EXEC_METHODS = """
        public static void executeBuild(Minecraft mc) {
            net.minecraft.world.level.Level level = mc.level;
            if (level == null || mc.player == null) return;
            ArchitectWorkspaceModel model = TFBClientController.getInstance().model();
            java.util.List<com.x64Dynamics.tfb.architectural.PlacementTarget> targets = model.cachedPreviewTargets();
            if (targets.isEmpty()) {
                mc.player.displayClientMessage(Component.literal("§c[TFB] Build failed: No preview blocks available!"), false);
                return;
            }
            java.util.List<com.x64Dynamics.tfb.construction.ScheduledBlockPlacement> scheduled =
                com.x64Dynamics.tfb.construction.DependencyAwareScheduler.scheduleWithDependencies(targets);
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(level.dimension());
                if (serverLevel != null) {
                    server.execute(() -> {
                        int placed = 0;
                        java.util.List<com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange> changes = new java.util.ArrayList<>();
                        for (com.x64Dynamics.tfb.construction.ScheduledBlockPlacement target : scheduled) {
                            com.x64Dynamics.tfb.geometry.Vec3i pos = target.pos();
                            net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
                            net.minecraft.world.level.block.state.BlockState prevState = serverLevel.getBlockState(bp);
                            net.minecraft.world.level.block.state.BlockState state = BlockStateParser.parse(target.block());
                            if (!prevState.equals(state) && serverLevel.setBlock(bp, state, 3)) {
                                placed++;
                                changes.add(new com.x64Dynamics.tfb.editing.WorldConstructionHistory.BlockChange(
                                    pos, BlockStateParser.fromBlockState(prevState), target.block()
                                ));
                            }
                        }
                        if (!changes.isEmpty()) {
                            com.x64Dynamics.tfb.editing.WorldConstructionHistory.getInstance().recordTransaction(changes);
                        }
                        final int finalPlaced = placed;
                        mc.execute(() -> mc.player.displayClientMessage(
                            Component.literal("§a[TFB] Construction complete! Placed " + finalPlaced + " blocks."), false
                        ));
                    });
                }
            }
        }
    }
}"""

def sync_all():
    scanner_count = 0
    entry_count = 0
    exec_count = 0

    for target_dir in sorted((ROOT / "versions").glob("*/*")):
        if not target_dir.is_dir():
            continue
        mc = target_dir.parts[-2]
        loader = target_dir.parts[-1]
        if loader not in ("fabric", "forge", "neoforge"):
            continue

        pkg_dir = target_dir / "src/main/java/com/x64Dynamics/tfb"
        if not pkg_dir.exists():
            continue

        # 1. BlockRegistryScanner.java
        scanner_file = pkg_dir / "BlockRegistryScanner.java"
        scanner_src = SCANNER_LEGACY if mc in LEGACY_REG_VERSIONS else SCANNER_BUILT_IN
        scanner_file.write_text(scanner_src, encoding="utf-8", newline="\n")
        scanner_count += 1

        # 2. Entrypoint.java
        entry_file = pkg_dir / "Entrypoint.java"
        if entry_file.exists():
            txt = entry_file.read_text(encoding="utf-8")
            if "WorldProjectStorage.setProjectsDir" not in txt and "TerraFormaBuilder.initializeWithBridge(bridge);" in txt:
                inject = (
                    "TerraFormaBuilder.initializeWithBridge(bridge);\n"
                    "        com.x64Dynamics.tfb.persistence.BlueprintStorage.setStorageDir(configDir.resolve(\"terraformabuilder\").resolve(\"blueprints\"));\n"
                    "        com.x64Dynamics.tfb.city.WorldProjectStorage.setProjectsDir(configDir.resolve(\"terraformabuilder\").resolve(\"projects\"));\n"
                    "        BlockRegistryScanner.scanIfNeeded();"
                )
                txt = txt.replace("TerraFormaBuilder.initializeWithBridge(bridge);", inject, 1)
                entry_file.write_text(txt, encoding="utf-8", newline="\n")
                entry_count += 1

        # 3. executeBuild for stubbed 1.20-1.20.4 targets
        events_file = pkg_dir / "client/TFBClientEvents.java"
        if events_file.exists():
            etxt = events_file.read_text(encoding="utf-8")
            if EXEC_STUB_OLD in etxt and "void executeBuild(" not in etxt:
                etxt = etxt.replace(EXEC_STUB_OLD, EXEC_STUB_NEW, 1)
                idx = etxt.rfind("    }\n}")
                if idx != -1:
                    etxt = etxt[:idx] + EXEC_METHODS
                    events_file.write_text(etxt, encoding="utf-8", newline="\n")
                    exec_count += 1

    print(f"Synced BlockRegistryScanner={scanner_count}, Entrypoint={entry_count}, stubbedExecuteBuild={exec_count}")

if __name__ == "__main__":
    sync_all()
