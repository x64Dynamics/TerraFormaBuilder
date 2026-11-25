package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.construction.editing.WorldConstructionHistory;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.platform.network.*;
import com.x64Dynamics.tfb.platform.ILevelAccess;
import com.x64Dynamics.tfb.platform.IPlayerAccess;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class Phase5ServerAuthorityTest {

    private static final class MockSurvivalPlayer implements IPlayerAccess {
        private final UUID id = UUID.randomUUID();
        private final boolean creative;
        private final Map<String, Integer> inventory = new HashMap<>();

        MockSurvivalPlayer(boolean creative, int stoneCount) {
            this.creative = creative;
            this.inventory.put("minecraft:stone", stoneCount);
        }

        @Override public UUID getUuid() { return id; }
        @Override public String getTeamId() { return "default"; }
        @Override public boolean isCreative() { return creative; }
        @Override public boolean hasResource(BlockIdentifier block, int count) {
            return inventory.getOrDefault(block.namespacedId(), 0) >= count;
        }
        @Override public boolean consumeResource(BlockIdentifier block, int count) {
            String key = block.namespacedId();
            int cur = inventory.getOrDefault(key, 0);
            if (cur < count) return false;
            inventory.put(key, cur - count);
            return true;
        }
    }

    @Test
    public void testC2SPacketRoundtripAndServerPermissionChecks() throws Exception {
        var player = new MockSurvivalPlayer(false, 2);
        List<PlacementTarget> targets = List.of(
            new PlacementTarget(new Vec3i(0, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY),
            new PlacementTarget(new Vec3i(1, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY),
            new PlacementTarget(new Vec3i(2, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY)
        );
        var packet = new C2SConstructionPacket(UUID.randomUUID(), player.getUuid(), Vec3i.ZERO, targets, true);
        byte[] bytes = packet.encode();
        var decoded = C2SConstructionPacket.decode(bytes);
        assertEquals(3, decoded.targets().size());

        var validator = ServerBuildPermissionValidator.defaultOverworld();
        assertFalse("Survival player with 2 stone cannot build 3 stone blocks", validator.validate(player, decoded).allowed());

        var creative = new MockSurvivalPlayer(true, 0);
        var creativePacket = new C2SConstructionPacket(UUID.randomUUID(), creative.getUuid(), Vec3i.ZERO, targets, true);
        assertTrue(validator.validate(creative, creativePacket).allowed());
    }

    @Test
    public void testTickBudgetedServerPlacement() {
        var player = new MockSurvivalPlayer(false, 10);
        List<PlacementTarget> targets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            targets.add(new PlacementTarget(new Vec3i(i, 64, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY));
        }
        var executor = new ServerTickBuildExecutor(ServerBuildPermissionValidator.defaultOverworld(), 2);
        var ack = executor.submit(player, new C2SConstructionPacket(UUID.randomUUID(), player.getUuid(), Vec3i.ZERO, targets, true));
        assertTrue(ack.accepted());

        Map<Vec3i, BlockIdentifier> world = new HashMap<>();
        ILevelAccess level = new ILevelAccess() {
            @Override public int getSurfaceHeight(int x, int z) { return 64; }
            @Override public BlockIdentifier getBlockState(Vec3i pos) { return world.getOrDefault(pos, BlockIdentifier.AIR); }
            @Override public boolean setBlockState(Vec3i pos, BlockIdentifier state) { world.put(pos, state); return true; }
            @Override public boolean isAir(Vec3i pos) { return !world.containsKey(pos); }
        };
        WorldConstructionHistory history = WorldConstructionHistory.getInstance();

        assertEquals(2, executor.tick(level, null, history));
        assertEquals(2, executor.tick(level, null, history));
        assertEquals(1, executor.tick(level, null, history));
        assertEquals(0, executor.activeJobCount());
        assertEquals(5, world.size());
    }
}
