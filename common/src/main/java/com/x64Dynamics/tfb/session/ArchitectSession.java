package com.x64Dynamics.tfb.session;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.brush.BrushContext;
import com.x64Dynamics.tfb.brush.BrushRegistry;
import com.x64Dynamics.tfb.brush.IConstructionBrush;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.terrain.FoundationStrategy;
import com.x64Dynamics.tfb.terrain.HeightmapGrid;

import java.util.*;

public final class ArchitectSession {
    private final UUID sessionId;
    private final UUID ownerId;
    private String teamId;
    private SessionMode mode;
    private String activeBrushId;
    private BoundingBox3i bounds;
    private final List<Vec3i> pathPoints = new ArrayList<>();
    private MaterialPalette palette;
    private FoundationStrategy foundationStrategy = FoundationStrategy.PILLARS_STILTS;
    private final Map<String, Object> properties = new HashMap<>();
    private final Set<UUID> collaborators = new HashSet<>();
    private long lastUpdatedEpochMs;

    public ArchitectSession(UUID sessionId, UUID ownerId, String activeBrushId, BoundingBox3i bounds, SessionMode mode, String teamId) {
        this.sessionId = sessionId;
        this.ownerId = ownerId;
        this.activeBrushId = activeBrushId;
        this.bounds = bounds;
        this.mode = mode == null ? SessionMode.PRIVATE_SOLO : mode;
        this.teamId = teamId;
        this.palette = StylePreset.MEDIEVAL.createPalette();
        this.lastUpdatedEpochMs = System.currentTimeMillis();
    }

    public UUID sessionId() { return sessionId; }
    public UUID ownerId() { return ownerId; }
    public String teamId() { return teamId; }
    public SessionMode mode() { return mode; }
    public String activeBrushId() { return activeBrushId; }
    public BoundingBox3i bounds() { return bounds; }
    public List<Vec3i> pathPoints() { return Collections.unmodifiableList(pathPoints); }
    public MaterialPalette palette() { return palette; }
    public FoundationStrategy foundationStrategy() { return foundationStrategy; }
    public long lastUpdatedEpochMs() { return lastUpdatedEpochMs; }

    public void setTeamId(String teamId) { this.teamId = teamId; touch(); }
    public void setMode(SessionMode mode) { this.mode = mode; touch(); }
    public void setActiveBrushId(String brushId) { this.activeBrushId = brushId; touch(); }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; touch(); }
    public void setPalette(MaterialPalette palette) { this.palette = palette; touch(); }
    public void setFoundationStrategy(FoundationStrategy strat) { this.foundationStrategy = strat; touch(); }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
        touch();
    }

    public void setPathPoints(List<Vec3i> points) {
        pathPoints.clear();
        if (points != null) pathPoints.addAll(points);
        touch();
    }

    public void addCollaborator(UUID playerId) {
        collaborators.add(playerId);
        touch();
    }

    public void removeCollaborator(UUID playerId) {
        collaborators.remove(playerId);
        touch();
    }

    public boolean canEdit(UUID playerId, String playerTeamId) {
        if (ownerId.equals(playerId)) return true;
        if (collaborators.contains(playerId)) return true;
        if (mode == SessionMode.TEAM_PARTY_SHARED && teamId != null && teamId.equals(playerTeamId)) {
            return true;
        }
        return false;
    }

    public List<PlacementTarget> generateTargets(HeightmapGrid heightmap) {
        IConstructionBrush brush = BrushRegistry.get(activeBrushId);
        if (brush == null) return Collections.emptyList();
        BrushContext ctx = new BrushContext(bounds, pathPoints, palette, heightmap, foundationStrategy, properties);
        return brush.generate(ctx);
    }

    private void touch() {
        this.lastUpdatedEpochMs = System.currentTimeMillis();
    }
}
