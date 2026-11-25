package com.x64Dynamics.tfb.architectural.builds.plan;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.platform.ILevelAccess;

import java.util.*;

// Deterministic structural-integrity solver and enforcer.
// Evaluates load paths, column continuity, floor/ceiling spans, cantilevered roof overhangs,
// door threshold support, and terrain grounding, automatically reinforcing any unsupported
// geometry using the active MaterialPalette so builds look human-engineered and never float.
public final class StructuralIntegrityEngine {
    public record IntegrityReport(
        int groundedPilings,
        int repairedColumns,
        int addedSpanBeams,
        int addedCorbels,
        int doorLandingsAdded,
        int totalTargets,
        int integrityScore
    ) { }

    public record EnforcementResult(
        List<PlacementTarget> targets,
        IntegrityReport report
    ) { }

    private StructuralIntegrityEngine() { }

    public static EnforcementResult enforce(List<PlacementTarget> input, MaterialPalette palette, ILevelAccess level) {
        if (input == null || input.isEmpty()) {
            return new EnforcementResult(Collections.emptyList(), new IntegrityReport(0, 0, 0, 0, 0, 0, 100));
        }

        MaterialPalette pal = palette != null ? palette : com.x64Dynamics.tfb.material.StylePreset.MEDIEVAL.createPalette();
        Map<Vec3i, PlacementTarget> byPos = new LinkedHashMap<>(input.size() * 2);
        int minY = Integer.MAX_VALUE;
        for (PlacementTarget t : input) {
            if (t == null || t.pos() == null || t.block() == null) continue;
            byPos.put(t.pos(), t);
            if (!t.block().isAir() && t.role() != MaterialRole.SUB_FOUNDATION) {
                minY = Math.min(minY, t.pos().y());
            }
        }
        if (minY == Integer.MAX_VALUE) {
            return new EnforcementResult(new ArrayList<>(byPos.values()), new IntegrityReport(0, 0, 0, 0, 0, byPos.size(), 100));
        }

        int groundedPilings = 0;
        int repairedColumns = 0;
        int addedSpanBeams = 0;
        int addedCorbels = 0;
        int doorLandings = 0;

        BlockIdentifier subFoundation = pal.get(MaterialRole.SUB_FOUNDATION);
        BlockIdentifier pillarBlock = pal.get(MaterialRole.PILLAR);
        BlockIdentifier beamBlock = pal.get(MaterialRole.BEAM);
        BlockIdentifier slabBlock = pal.get(MaterialRole.SLAB);
        BlockIdentifier stairBlock = pal.get(MaterialRole.STAIR);

        List<PlacementTarget> snapshot = new ArrayList<>(byPos.values());

        // 1. Ensure every DOOR lower half has a solid threshold underneath
        for (PlacementTarget t : snapshot) {
            if (t.role() == MaterialRole.DOOR && !"upper".equalsIgnoreCase(t.block().property("half"))) {
                Vec3i below = t.pos().add(0, -1, 0);
                PlacementTarget b = byPos.get(below);
                if (b == null || b.block().isAir()) {
                    byPos.put(below, new PlacementTarget(below, pal.get(MaterialRole.FOUNDATION), MaterialRole.FOUNDATION));
                    doorLandings++;
                }
            }
        }

        // 2. Trace load-bearing PILLARs & corner WALL_CORNERs downward to foundation level
        for (PlacementTarget t : snapshot) {
            if ((t.role() == MaterialRole.PILLAR || t.role() == MaterialRole.WALL_CORNER) && t.pos().y() > minY) {
                Vec3i cur = t.pos().add(0, -1, 0);
                while (cur.y() >= minY) {
                    PlacementTarget below = byPos.get(cur);
                    if (below != null && !below.block().isAir()) {
                        break;
                    }
                    byPos.put(cur, new PlacementTarget(cur, cur.y() == minY ? subFoundation : pillarBlock,
                        cur.y() == minY ? MaterialRole.SUB_FOUNDATION : MaterialRole.PILLAR));
                    repairedColumns++;
                    cur = cur.add(0, -1, 0);
                }
            }
        }

        // 3. Ground perimeter FOUNDATION / PILLAR columns down onto solid terrain when ILevelAccess is available
        if (level != null) {
            List<PlacementTarget> baseColumns = new ArrayList<>();
            for (PlacementTarget t : byPos.values()) {
                if (t.pos().y() == minY && (t.role() == MaterialRole.FOUNDATION || t.role() == MaterialRole.PILLAR || t.role() == MaterialRole.SUB_FOUNDATION)) {
                    baseColumns.add(t);
                }
            }
            for (PlacementTarget col : baseColumns) {
                int x = col.pos().x();
                int z = col.pos().z();
                int surfaceY = level.getSurfaceHeight(x, z);
                int maxDepth = Math.max(surfaceY, minY - 10);
                for (int y = minY - 1; y >= maxDepth; y--) {
                    Vec3i p = new Vec3i(x, y, z);
                    if (byPos.containsKey(p)) break;
                    if (!level.isAir(p)) {
                        String path = level.getBlockState(p).path().toLowerCase(Locale.ROOT);
                        if (!path.contains("water") && !path.contains("leaves") && !path.contains("grass")) {
                            break;
                        }
                    }
                    byPos.put(p, new PlacementTarget(p, subFoundation, MaterialRole.SUB_FOUNDATION));
                    groundedPilings++;
                }
            }
        }

        // 4. Reinforce wide interior floor/ceiling spans (> 4 blocks from nearest wall/pillar) with structural BEAMs
        for (PlacementTarget t : snapshot) {
            if (t.role() == MaterialRole.FLOOR || t.role() == MaterialRole.CEILING) {
                Vec3i p = t.pos();
                if (p.x() % 4 == 0 && p.z() % 4 == 0) {
                    Vec3i below = p.add(0, -1, 0);
                    PlacementTarget b = byPos.get(below);
                    if ((b == null || b.block().isAir()) && !hasHorizontalSupportWithin(byPos, below, 3)) {
                        // Add ceiling beam rib underneath unsupported long span
                        byPos.put(below, new PlacementTarget(below, beamBlock, MaterialRole.BEAM));
                        addedSpanBeams++;
                    }
                }
            }
        }

        // 5. Add human-style corbel/bracket supports under cantilevered roof overhangs
        for (PlacementTarget t : snapshot) {
            if (t.role() == MaterialRole.ROOF_OVERHANG || t.role() == MaterialRole.ROOF_SLOPE) {
                Vec3i p = t.pos();
                Vec3i below = p.add(0, -1, 0);
                if (byPos.containsKey(below) && !byPos.get(below).block().isAir()) continue;

                // Only place corbels on periodic structural intervals adjacent to an exterior wall
                if ((Math.abs(p.x()) + Math.abs(p.z())) % 3 != 0) continue;
                for (Direction dir : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                    Vec3i adjWall = below.offset(dir);
                    PlacementTarget wallTarget = byPos.get(adjWall);
                    if (wallTarget != null && !wallTarget.block().isAir()
                        && (wallTarget.role() == MaterialRole.WALL_PRIMARY || wallTarget.role() == MaterialRole.WALL_ACCENT
                        || wallTarget.role() == MaterialRole.PILLAR || wallTarget.role() == MaterialRole.BEAM)) {
                        BlockIdentifier corbel = stairBlock.path().contains("stair")
                            ? stairBlock.withProperty("facing", dir.name().toLowerCase(Locale.ROOT)).withProperty("half", "top")
                            : slabBlock.withProperty("type", "top");
                        byPos.put(below, new PlacementTarget(below, corbel, MaterialRole.BEAM));
                        addedCorbels++;
                        break;
                    }
                }
            }
        }

        int total = byPos.size();
        int score = 100;
        if (repairedColumns > 0 || doorLandings > 0) {
            score = Math.max(85, 100 - Math.min(15, repairedColumns));
        }
        return new EnforcementResult(
            new ArrayList<>(byPos.values()),
            new IntegrityReport(groundedPilings, repairedColumns, addedSpanBeams, addedCorbels, doorLandings, total, score)
        );
    }

    private static boolean hasHorizontalSupportWithin(Map<Vec3i, PlacementTarget> byPos, Vec3i center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                PlacementTarget t = byPos.get(center.add(dx, 0, dz));
                if (t != null && !t.block().isAir()
                    && (t.role() == MaterialRole.WALL_PRIMARY || t.role() == MaterialRole.PILLAR || t.role() == MaterialRole.WALL_ACCENT)) {
                    return true;
                }
            }
        }
        return false;
    }
}
