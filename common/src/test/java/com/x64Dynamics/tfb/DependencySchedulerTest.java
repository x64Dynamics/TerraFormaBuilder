package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.construction.ChunkCoordinate;
import com.x64Dynamics.tfb.construction.ConstructionCheckpoint;
import com.x64Dynamics.tfb.construction.DependencyAwareScheduler;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public final class DependencySchedulerTest {
    @Test
    public void testDoorSupportDependency() {
        PlacementTarget floor = new PlacementTarget(new Vec3i(0, 0, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.FLOOR);
        PlacementTarget lowerDoor = new PlacementTarget(new Vec3i(0, 1, 0), BlockIdentifier.of("minecraft:oak_door[half=lower]"), MaterialRole.DOOR);
        PlacementTarget upperDoor = new PlacementTarget(new Vec3i(0, 2, 0), BlockIdentifier.of("minecraft:oak_door[half=upper]"), MaterialRole.DOOR);

        // Feed in reverse order (upper door -> lower door -> floor)
        List<ScheduledBlockPlacement> scheduled = DependencyAwareScheduler.scheduleWithDependencies(
            List.of(upperDoor, lowerDoor, floor)
        );

        assertEquals(3, scheduled.size());

        // Floor must be placed before lower door, and lower door before upper door
        int floorIdx = -1, lowerIdx = -1, upperIdx = -1;
        for (int i = 0; i < scheduled.size(); i++) {
            if (scheduled.get(i).pos().equals(new Vec3i(0, 0, 0))) floorIdx = i;
            if (scheduled.get(i).pos().equals(new Vec3i(0, 1, 0))) lowerIdx = i;
            if (scheduled.get(i).pos().equals(new Vec3i(0, 2, 0))) upperIdx = i;
        }

        assertTrue(floorIdx < lowerIdx);
        assertTrue(lowerIdx < upperIdx);
    }

    @Test
    public void testChunkGrouping() {
        ScheduledBlockPlacement p1 = new ScheduledBlockPlacement(new Vec3i(5, 64, 5), BlockIdentifier.of("minecraft:stone"), null, 64);
        ScheduledBlockPlacement p2 = new ScheduledBlockPlacement(new Vec3i(10, 64, 10), BlockIdentifier.of("minecraft:stone"), null, 64);
        ScheduledBlockPlacement p3 = new ScheduledBlockPlacement(new Vec3i(32, 64, 5), BlockIdentifier.of("minecraft:stone"), null, 64);

        Map<ChunkCoordinate, List<ScheduledBlockPlacement>> chunks = DependencyAwareScheduler.groupByChunk(List.of(p1, p2, p3));
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(new ChunkCoordinate(0, 0)).size());
        assertEquals(1, chunks.get(new ChunkCoordinate(2, 0)).size());
    }

    @Test
    public void testCheckpointJson() {
        UUID id = UUID.randomUUID();
        ConstructionCheckpoint cp = new ConstructionCheckpoint(id, 1000, 250, System.currentTimeMillis(), false);
        String json = cp.toJson();
        assertNotNull(json);

        ConstructionCheckpoint loaded = ConstructionCheckpoint.fromJson(json);
        assertEquals(id, loaded.sessionId());
        assertEquals(1000, loaded.totalBlocks());
        assertEquals(250, loaded.placedBlocks());
    }
}
