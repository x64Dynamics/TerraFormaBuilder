package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

/**
 * Compiles a complete BuildingRegistry and ConnectionEngine graph into deterministic
 * PlacementTargets for preview or world construction, without modifying the Minecraft world.
 */
public final class GridComplexGenerator {
    public record ComplexResult(
        List<PlacementTarget> targets,
        List<BuildingPlan> plans,
        List<ConnectionEngine.BuildingConnection> connections
    ) { }

    private GridComplexGenerator() { }

    public static ComplexResult generate(
        GridManager grid,
        BuildingRegistry registry,
        ConnectionEngine connectionEngine,
        PlacementRuleEngine.PlacementRules rules
    ) {
        Map<Vec3i, PlacementTarget> merged = new LinkedHashMap<>();
        List<BuildingPlan> plans = new ArrayList<>();

        List<ConnectionEngine.BuildingConnection> connections = connectionEngine.analyzeConnections(grid, registry, rules);
        List<GridBuilding> buildings = new ArrayList<>(registry.allBuildings());

        for (int i = 0; i < buildings.size(); i++) {
            GridBuilding b = buildings.get(i);
            var spec = ArchitecturalVariationEngine.deriveSpec(b, i);
            BoundingBox3i box = grid.toWorldBounds(b);
            WingConfiguration wing = switch (b.footprint()) {
                case L_SHAPED -> WingConfiguration.L_SHAPED;
                case U_SHAPED -> WingConfiguration.U_SHAPED;
                default -> WingConfiguration.SINGLE_RECTANGLE;
            };

            if (b.function() == GridBuilding.BuildingFunction.TOWER) {
                int radius = Math.max(3, Math.min(box.sizeX(), box.sizeZ()) / 2);
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, box.min().y(), (box.min().z() + box.max().z()) / 2);
                var towerCfg = new TowerGenerator.TowerConfig(TowerGenerator.TowerShape.OCTAGONAL, radius, b.stories() * spec.storyHeight(), Math.max(1, b.stories()), true, true, true);
                for (PlacementTarget t : TowerGenerator.generateTower(center, towerCfg, spec.palette())) {
                    merged.put(t.pos(), t);
                }
            } else {
                var cfg = new IntelligentHomeGenerator.HomeConfig(
                    box, spec.palette(), spec.roofType(), b.stories(), spec.storyHeight(),
                    b.style(), b.facing(), wing, spec.addPorch(), spec.addChimney(), true, 0.85f, spec.buildingSeed()
                );
                var res = IntelligentHomeGenerator.generateHome(cfg);
                plans.add(res.plan());
                for (PlacementTarget t : res.targets()) {
                    // Enforce exact footprint unless allowExpansion is explicitly enabled
                    if (!b.allowExpansion()) {
                        if (t.pos().x() < box.min().x() || t.pos().x() > box.max().x()
                            || t.pos().z() < box.min().z() || t.pos().z() > box.max().z()) {
                            continue;
                        }
                    }
                    merged.put(t.pos(), t);
                }
            }
        }

        // Realize explicit architectural connections between buildings
        for (var conn : connections) {
            if (conn.status() == ConnectionEngine.ConnectionStatus.PROHIBITED
                || conn.type() == ConnectionEngine.ConnectionType.DELIBERATE_GAP) {
                continue;
            }
            GridBuilding from = registry.get(conn.fromBuildingId()).orElse(null);
            MaterialPalette palette = from != null ? from.palette() : com.x64Dynamics.tfb.material.StylePreset.MEDIEVAL.createPalette();

            switch (conn.type()) {
                case CORRIDOR, COVERED_PASSAGE, ELEVATED_WALKWAY -> {
                    for (PlacementTarget t : ConnectedBuildingComposer.generateCoveredWalkway(conn.anchorA(), conn.anchorB(), rules.minCorridorWidth(), 3, palette)) {
                        merged.put(t.pos(), t);
                    }
                }
                case ARCHWAY -> {
                    for (PlacementTarget t : TowerGenerator.generateConnectedBattlementWall(conn.anchorA(), conn.anchorB(), 3, palette)) {
                        merged.put(t.pos(), t);
                    }
                }
                case DOORWAY -> {
                    Vec3i mid = new Vec3i((conn.anchorA().x() + conn.anchorB().x()) / 2, conn.anchorA().y() + 1, (conn.anchorA().z() + conn.anchorB().z()) / 2);
                    merged.put(mid, new PlacementTarget(mid, BlockIdentifier.AIR, MaterialRole.AIR));
                    merged.put(mid.add(0, 1, 0), new PlacementTarget(mid.add(0, 1, 0), BlockIdentifier.AIR, MaterialRole.AIR));
                }
                case VERTICAL_STACK -> {
                    int h = Math.max(3, Math.abs(conn.anchorB().y() - conn.anchorA().y()) + 1);
                    for (PlacementTarget t : StaircaseGenerator.generateSpiral(conn.anchorA(), h, palette)) {
                        merged.put(t.pos(), t);
                    }
                }
                default -> {}
            }
        }

        return new ComplexResult(new ArrayList<>(merged.values()), plans, connections);
    }
}
