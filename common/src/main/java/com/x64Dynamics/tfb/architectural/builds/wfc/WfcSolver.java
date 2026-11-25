package com.x64Dynamics.tfb.architectural.builds.wfc;

import com.x64Dynamics.tfb.geometry.Direction;

import java.util.*;

// 3D Wave Function Collapse solver (inspired by MGAIA-Minecraft and GDMC2024)
// with directional socket adjacency, 90-degree Y-rotations, locked cells,
// weighted entropy collapse, and contradiction retry.
public final class WfcSolver {
    public record ModuleRule(
        String id,
        double weight,
        String northSocket,
        String eastSocket,
        String southSocket,
        String westSocket,
        String upSocket,
        String downSocket,
        boolean allowRotation
    ) {
        public String socketFor(Direction dir, int rotSteps) {
            if (dir == Direction.UP) return upSocket;
            if (dir == Direction.DOWN) return downSocket;
            int idx = switch (dir) {
                case NORTH -> 0;
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            int shifted = Math.floorMod(idx - rotSteps, 4);
            return switch (shifted) {
                case 0 -> northSocket;
                case 1 -> eastSocket;
                case 2 -> southSocket;
                default -> westSocket;
            };
        }
    }

    public record ModuleVariant(String moduleId, int rotation) {}

    public record WfcResult(boolean success, ModuleVariant[][][] grid, int attempts) {}

    private WfcSolver() {}

    public static WfcResult solve(
            int sizeX,
            int sizeY,
            int sizeZ,
            List<ModuleRule> rules,
            Map<String, ModuleVariant> lockedCells,
            long seed,
            int maxAttempts) {
        Map<String, ModuleRule> ruleMap = new LinkedHashMap<>();
        List<ModuleVariant> universe = new ArrayList<>();
        for (ModuleRule r : rules) {
            ruleMap.put(r.id(), r);
            int rots = r.allowRotation() ? 4 : 1;
            for (int rot = 0; rot < rots; rot++) {
                universe.add(new ModuleVariant(r.id(), rot));
            }
        }

        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            Random rng = new Random(seed + attempt * 31337L);
            @SuppressWarnings("unchecked")
            Set<ModuleVariant>[][][] space = new LinkedHashSet[sizeX][sizeY][sizeZ];
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        space[x][y][z] = new LinkedHashSet<>(universe);
                    }
                }
            }

            ArrayDeque<int[]> queue = new ArrayDeque<>();
            if (lockedCells != null) {
                for (Map.Entry<String, ModuleVariant> e : lockedCells.entrySet()) {
                    String[] parts = e.getKey().split(",");
                    if (parts.length == 3) {
                        int lx = Integer.parseInt(parts[0]);
                        int ly = Integer.parseInt(parts[1]);
                        int lz = Integer.parseInt(parts[2]);
                        if (inBounds(lx, ly, lz, sizeX, sizeY, sizeZ)) {
                            space[lx][ly][lz].clear();
                            space[lx][ly][lz].add(e.getValue());
                            queue.add(new int[]{lx, ly, lz});
                        }
                    }
                }
            }

            boolean ok = propagate(space, queue, ruleMap, sizeX, sizeY, sizeZ);
            while (ok) {
                int[] next = findMinEntropyCell(space, sizeX, sizeY, sizeZ, rng);
                if (next == null) {
                    ModuleVariant[][][] out = new ModuleVariant[sizeX][sizeY][sizeZ];
                    for (int x = 0; x < sizeX; x++) {
                        for (int y = 0; y < sizeY; y++) {
                            for (int z = 0; z < sizeZ; z++) {
                                out[x][y][z] = space[x][y][z].iterator().next();
                            }
                        }
                    }
                    return new WfcResult(true, out, attempt);
                }
                ModuleVariant chosen = pickWeighted(space[next[0]][next[1]][next[2]], ruleMap, rng);
                space[next[0]][next[1]][next[2]].clear();
                space[next[0]][next[1]][next[2]].add(chosen);
                queue.add(next);
                ok = propagate(space, queue, ruleMap, sizeX, sizeY, sizeZ);
            }
        }
        return new WfcResult(false, new ModuleVariant[0][0][0], maxAttempts);
    }

    private static boolean propagate(
            Set<ModuleVariant>[][][] space,
            ArrayDeque<int[]> queue,
            Map<String, ModuleRule> rules,
            int sx, int sy, int sz) {
        Direction[] dirs = Direction.values();
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1], z = cur[2];
            Set<ModuleVariant> curSet = space[x][y][z];
            if (curSet.isEmpty()) return false;

            for (Direction d : dirs) {
                int nx = x + d.stepX(), ny = y + d.stepY(), nz = z + d.stepZ();
                if (!inBounds(nx, ny, nz, sx, sy, sz)) continue;

                Set<String> validSockets = new HashSet<>();
                for (ModuleVariant mv : curSet) {
                    ModuleRule r = rules.get(mv.moduleId());
                    if (r != null) validSockets.add(r.socketFor(d, mv.rotation()));
                }

                Direction opp = d.opposite();
                Set<ModuleVariant> neighbor = space[nx][ny][nz];
                int before = neighbor.size();
                neighbor.removeIf(nmv -> {
                    ModuleRule nr = rules.get(nmv.moduleId());
                    return nr == null || !validSockets.contains(nr.socketFor(opp, nmv.rotation()));
                });
                if (neighbor.isEmpty()) return false;
                if (neighbor.size() < before) {
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }
        return true;
    }

    private static int[] findMinEntropyCell(Set<ModuleVariant>[][][] space, int sx, int sy, int sz, Random rng) {
        int bestSize = Integer.MAX_VALUE;
        List<int[]> ties = new ArrayList<>();
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    int n = space[x][y][z].size();
                    if (n > 1) {
                        if (n < bestSize) {
                            bestSize = n;
                            ties.clear();
                            ties.add(new int[]{x, y, z});
                        } else if (n == bestSize) {
                            ties.add(new int[]{x, y, z});
                        }
                    }
                }
            }
        }
        return ties.isEmpty() ? null : ties.get(rng.nextInt(ties.size()));
    }

    private static ModuleVariant pickWeighted(Set<ModuleVariant> options, Map<String, ModuleRule> rules, Random rng) {
        double total = 0.0;
        for (ModuleVariant mv : options) {
            ModuleRule r = rules.get(mv.moduleId());
            total += r != null ? Math.max(0.01, r.weight()) : 1.0;
        }
        double target = rng.nextDouble() * total;
        double acc = 0.0;
        ModuleVariant last = null;
        for (ModuleVariant mv : options) {
            last = mv;
            ModuleRule r = rules.get(mv.moduleId());
            acc += r != null ? Math.max(0.01, r.weight()) : 1.0;
            if (acc >= target) return mv;
        }
        return last;
    }

    private static boolean inBounds(int x, int y, int z, int sx, int sy, int sz) {
        return x >= 0 && x < sx && y >= 0 && y < sy && z >= 0 && z < sz;
    }

    public static List<ModuleRule> defaultArchitecturalGrammar() {
        return List.of(
            new ModuleRule("air", 1.2, "ext", "ext", "ext", "ext", "ext", "ext", false),
            new ModuleRule("wall_solid", 2.0, "ext", "wall", "int", "wall", "wall_v", "wall_v", true),
            new ModuleRule("wall_window", 1.8, "ext", "wall", "int", "wall", "wall_v", "wall_v", true),
            new ModuleRule("corner_pillar", 1.5, "ext", "wall", "wall", "ext", "wall_v", "wall_v", true),
            new ModuleRule("interior_hall", 2.5, "int", "int", "int", "int", "int_v", "int_v", false),
            new ModuleRule("courtyard", 1.0, "ext", "ext", "ext", "ext", "ext", "ext", false)
        );
    }
}
