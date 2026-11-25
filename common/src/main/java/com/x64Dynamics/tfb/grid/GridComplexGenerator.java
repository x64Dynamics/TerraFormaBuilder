package com.x64Dynamics.tfb.grid;

import com.x64Dynamics.tfb.architectural.*;
import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.layout.ConnectedBuildingComposer;
import com.x64Dynamics.tfb.architectural.layout.StaircaseGenerator;
import com.x64Dynamics.tfb.architectural.layout.TowerGenerator;
import com.x64Dynamics.tfb.architectural.layout.WingConfiguration;
/**
 * Compiles a complete BuildingRegistry and ComplexConnectionPlanner graph into deterministic
 * PlacementTargets for preview or world construction, without modifying the Minecraft world.
 */
public final class GridComplexGenerator {
    public record ComplexResult(
        List<PlacementTarget> targets,
        List<BuildingPlan> plans,
        List<ComplexConnectionPlanner.BuildingConnection> connections
    ) { }

    private GridComplexGenerator() { }

    public static ComplexResult generate(
        GridManager grid,
        BuildingRegistry registry,
        ComplexConnectionPlanner connectionEngine,
        PlacementValidator.PlacementRules rules
    ) {
        Map<Vec3i, PlacementTarget> merged = new LinkedHashMap<>();
        List<BuildingPlan> plans = new ArrayList<>();

        List<ComplexConnectionPlanner.BuildingConnection> connections = connectionEngine.analyzeConnections(grid, registry, rules);
        List<GridBuilding> buildings = new ArrayList<>(registry.allBuildings());

        for (int i = 0; i < buildings.size(); i++) {
            GridBuilding b = buildings.get(i);
            var spec = ComplexVariationPlanner.deriveSpec(b, i);
            BoundingBox3i box = grid.toWorldBounds(b);
            WingConfiguration wing = switch (b.footprint()) {
                case L_SHAPED -> WingConfiguration.L_SHAPED;
                case U_SHAPED -> WingConfiguration.U_SHAPED;
                default -> WingConfiguration.SINGLE_RECTANGLE;
            };

            if (b.function() == GridBuilding.BuildingFunction.COURTYARD) {
                for (int x = box.min().x(); x <= box.max().x(); x++) {
                    for (int z = box.min().z(); z <= box.max().z(); z++) {
                        merged.put(new Vec3i(x, box.min().y(), z), new PlacementTarget(new Vec3i(x, box.min().y(), z), spec.palette().get(MaterialRole.FOUNDATION), MaterialRole.FOUNDATION));
                    }
                }
                if (box.sizeX() >= 7 && box.sizeZ() >= 7) {
                    for (PlacementTarget ct : ConnectedBuildingComposer.generateCourtyardCloister(box, Math.max(3, spec.storyHeight() - 1), spec.palette())) {
                        merged.put(ct.pos(), ct);
                    }
                }
            } else if (b.function() == GridBuilding.BuildingFunction.TOWER) {
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
            if (conn.status() == ComplexConnectionPlanner.ConnectionStatus.PROHIBITED
                || conn.type() == ComplexConnectionPlanner.ConnectionType.DELIBERATE_GAP) {
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
                    GridBuilding to = registry.get(conn.toBuildingId()).orElse(null);
                    if (from != null && to != null) {
                        BoundingBox3i ba = grid.toWorldBounds(from);
                        BoundingBox3i bb = grid.toWorldBounds(to);
                        int y = Math.max(ba.min().y(), bb.min().y()) + 1;
                        if (ba.max().x() + 1 == bb.min().x() || bb.max().x() + 1 == ba.min().x()) {
                            int xA = (ba.max().x() + 1 == bb.min().x()) ? ba.max().x() : bb.max().x();
                            int xB = xA + 1;
                            int zMid = (Math.max(ba.min().z(), bb.min().z()) + Math.min(ba.max().z(), bb.max().z())) / 2;
                            for (int dy = 0; dy < 2; dy++) {
                                merged.put(new Vec3i(xA, y + dy, zMid), new PlacementTarget(new Vec3i(xA, y + dy, zMid), BlockIdentifier.AIR, MaterialRole.AIR));
                                merged.put(new Vec3i(xB, y + dy, zMid), new PlacementTarget(new Vec3i(xB, y + dy, zMid), BlockIdentifier.AIR, MaterialRole.AIR));
                            }
                        } else if (ba.max().z() + 1 == bb.min().z() || bb.max().z() + 1 == ba.min().z()) {
                            int zA = (ba.max().z() + 1 == bb.min().z()) ? ba.max().z() : bb.max().z();
                            int zB = zA + 1;
                            int xMid = (Math.max(ba.min().x(), bb.min().x()) + Math.min(ba.max().x(), bb.max().x())) / 2;
                            for (int dy = 0; dy < 2; dy++) {
                                merged.put(new Vec3i(xMid, y + dy, zA), new PlacementTarget(new Vec3i(xMid, y + dy, zA), BlockIdentifier.AIR, MaterialRole.AIR));
                                merged.put(new Vec3i(xMid, y + dy, zB), new PlacementTarget(new Vec3i(xMid, y + dy, zB), BlockIdentifier.AIR, MaterialRole.AIR));
                            }
                        }
                    }
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
