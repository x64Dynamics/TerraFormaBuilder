package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.construction.ConstructionQueue;
import com.x64Dynamics.tfb.construction.ProgressiveBuildPlan;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.construction.TopologicalPhase;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.construction.session.ArchitectSession;
import com.x64Dynamics.tfb.construction.session.SessionManager;
import com.x64Dynamics.tfb.construction.session.SessionMode;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public final class SessionAndConstructionTest {
    @Test
    public void testSessionCreationAndPermissions() {
        UUID owner = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        SessionManager manager = SessionManager.getInstance();
        BoundingBox3i box = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 0, 0), 10, 5, 10);
        ArchitectSession session = manager.createSession(owner, "room", box, SessionMode.TEAM_PARTY_SHARED, "builders_team");

        assertTrue(session.canEdit(owner, null));
        assertTrue(session.canEdit(teammate, "builders_team"));
        assertFalse(session.canEdit(stranger, "other_team"));
    }

    @Test
    public void testTopologicalBuildOrdering() {
        PlacementTarget roofTarget = new PlacementTarget(new Vec3i(0, 10, 0), BlockIdentifier.of("minecraft:oak_stairs"), MaterialRole.ROOF_SLOPE);
        PlacementTarget foundationTarget = new PlacementTarget(new Vec3i(0, 0, 0), BlockIdentifier.of("minecraft:cobblestone"), MaterialRole.FOUNDATION);
        PlacementTarget wallTarget = new PlacementTarget(new Vec3i(0, 5, 0), BlockIdentifier.of("minecraft:stone_bricks"), MaterialRole.WALL_PRIMARY);

        ProgressiveBuildPlan plan = new ProgressiveBuildPlan(List.of(roofTarget, foundationTarget, wallTarget));
        List<ScheduledBlockPlacement> placements = plan.scheduledPlacements();

        assertEquals(3, placements.size());
        assertEquals(TopologicalPhase.FOUNDATION, placements.get(0).phase());
        assertEquals(TopologicalPhase.WALLS_OPENINGS, placements.get(1).phase());
        assertEquals(TopologicalPhase.ROOF, placements.get(2).phase());
    }

    @Test
    public void testConstructionQueuePolling() {
        PlacementTarget t1 = new PlacementTarget(new Vec3i(0, 0, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.FOUNDATION);
        PlacementTarget t2 = new PlacementTarget(new Vec3i(0, 1, 0), BlockIdentifier.of("minecraft:stone"), MaterialRole.WALL_PRIMARY);

        ProgressiveBuildPlan plan = new ProgressiveBuildPlan(List.of(t1, t2));
        ConstructionQueue queue = new ConstructionQueue(UUID.randomUUID(), plan, 1);

        assertFalse(queue.isComplete());
        List<ScheduledBlockPlacement> b1 = queue.pollNextBatch();
        assertEquals(1, b1.size());
        assertEquals(1, queue.placedCount());

        List<ScheduledBlockPlacement> b2 = queue.pollNextBatch();
        assertEquals(1, b2.size());
        assertTrue(queue.isComplete());
    }
}
