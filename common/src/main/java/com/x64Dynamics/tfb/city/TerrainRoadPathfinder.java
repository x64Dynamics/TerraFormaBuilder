package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;
import com.x64Dynamics.tfb.terrain.HeightmapGrid;

import java.util.*;

// Terrain-aware A* road pathfinder (adapted from RoadArchitect's PathFinder concepts),
// minimizing steep slopes, tagging bridge spans across depressions/water, and generating
// paved road surfaces, curbs, bridge piers, and roadside lanterns.
public final class TerrainRoadPathfinder {
    public enum SpanType { GROUND, BRIDGE, RETAINING }

    public record RoadNode(int x, int y, int z, SpanType spanType) {}

    private TerrainRoadPathfinder() {}

    public static List<RoadNode> findPath(
            Vec3i start,
            Vec3i goal,
            HeightmapGrid heightmap,
            int waterLevel,
            int maxSteps) {
        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, RoadNode> nodeMap = new HashMap<>();

        PriorityQueue<double[]> open = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        int sy = heightmap != null ? heightmap.getElevation(start.x(), start.z()) : start.y();
        int gy = heightmap != null ? heightmap.getElevation(goal.x(), goal.z()) : goal.y();

        long startKey = pack(start.x(), start.z());
        long goalKey = pack(goal.x(), goal.z());
        gScore.put(startKey, 0.0);
        nodeMap.put(startKey, new RoadNode(start.x(), Math.max(sy, waterLevel), start.z(), sy < waterLevel ? SpanType.BRIDGE : SpanType.GROUND));
        open.add(new double[]{heuristic(start.x(), start.z(), goal.x(), goal.z()), start.x(), start.z()});

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        int steps = 0;

        while (!open.isEmpty() && steps < maxSteps) {
            steps++;
            double[] cur = open.poll();
            int cx = (int) cur[1];
            int cz = (int) cur[2];
            long ck = pack(cx, cz);

            if (ck == goalKey) {
                return reconstruct(cameFrom, nodeMap, ck);
            }

            RoadNode curNode = nodeMap.get(ck);
            double curG = gScore.getOrDefault(ck, Double.POSITIVE_INFINITY);

            for (int[] d : dirs) {
                int nx = cx + d[0];
                int nz = cz + d[1];
                if (heightmap != null && !inGrid(nx, nz, heightmap)) continue;

                int rawY = heightmap != null ? heightmap.getElevation(nx, nz) : sy;
                SpanType span = rawY < waterLevel ? SpanType.BRIDGE : SpanType.GROUND;
                int ny = Math.max(rawY, waterLevel);

                double baseStep = (d[0] != 0 && d[1] != 0) ? 1.414 : 1.0;
                int dy = Math.abs(ny - curNode.y());
                double slopePenalty = dy > 2 ? dy * dy * 12.0 : dy * dy * 3.5;
                double waterPenalty = span == SpanType.BRIDGE ? 3.0 : 0.0;
                double tentativeG = curG + baseStep + slopePenalty + waterPenalty;

                long nk = pack(nx, nz);
                if (tentativeG < gScore.getOrDefault(nk, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(nk, ck);
                    gScore.put(nk, tentativeG);
                    nodeMap.put(nk, new RoadNode(nx, ny, nz, span));
                    double f = tentativeG + heuristic(nx, nz, goal.x(), goal.z());
                    open.add(new double[]{f, nx, nz});
                }
            }
        }
        return Collections.emptyList();
    }

    public static List<PlacementTarget> buildRoadTargets(
            List<RoadNode> path,
            int halfWidth,
            MaterialPalette palette) {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        BlockIdentifier roadMat = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier curbMat = palette.get(MaterialRole.SLAB);
        BlockIdentifier fenceMat = palette.get(MaterialRole.FENCE);
        BlockIdentifier pillarMat = palette.get(MaterialRole.PILLAR);
        BlockIdentifier lantern = palette.get(MaterialRole.LIGHT_SOURCE);

        for (int i = 0; i < path.size(); i++) {
            RoadNode n = path.get(i);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > halfWidth + 1) continue;
                    boolean edge = Math.abs(dx) == halfWidth || Math.abs(dz) == halfWidth;
                    Vec3i p = new Vec3i(n.x() + dx, n.y(), n.z() + dz);
                    map.put(p, new PlacementTarget(p, edge ? curbMat : roadMat, edge ? MaterialRole.SLAB : MaterialRole.FLOOR));
                    if (n.spanType() == SpanType.BRIDGE && edge) {
                        map.put(p.add(0, 1, 0), new PlacementTarget(p.add(0, 1, 0), fenceMat, MaterialRole.FENCE));
                        if (i % 6 == 0) {
                            for (int py = 1; py <= 4; py++) {
                                map.put(p.add(0, -py, 0), new PlacementTarget(p.add(0, -py, 0), pillarMat, MaterialRole.PILLAR));
                            }
                        }
                    }
                }
            }
            if (i % 10 == 0) {
                Vec3i lp = new Vec3i(n.x() + halfWidth + 1, n.y() + 1, n.z());
                map.put(lp, new PlacementTarget(lp, fenceMat, MaterialRole.FENCE));
                map.put(lp.add(0, 1, 0), new PlacementTarget(lp.add(0, 1, 0), fenceMat, MaterialRole.FENCE));
                map.put(lp.add(0, 2, 0), new PlacementTarget(lp.add(0, 2, 0), lantern, MaterialRole.LIGHT_SOURCE));
            }
        }
        return new ArrayList<>(map.values());
    }

    private static List<RoadNode> reconstruct(Map<Long, Long> cameFrom, Map<Long, RoadNode> nodes, long cur) {
        List<RoadNode> path = new ArrayList<>();
        Long k = cur;
        while (k != null) {
            RoadNode n = nodes.get(k);
            if (n != null) path.add(n);
            k = cameFrom.get(k);
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean inGrid(int x, int z, HeightmapGrid g) {
        var b = g.bounds();
        return x >= b.min().x() && x <= b.max().x() && z >= b.min().z() && z <= b.max().z();
    }

    private static double heuristic(int x0, int z0, int x1, int z1) {
        return Math.hypot(x1 - x0, z1 - z0);
    }

    private static long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }
}
