package com.x64Dynamics.tfb.material;

import com.x64Dynamics.tfb.geometry.Direction;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Pure Java BlockState property normalizer and placement-order/attachment helper
// ensuring directional, stateful, and multi-part blocks (doors, beds, stairs, slabs,
// chests, furnaces, trapdoors, logs) orient and place cleanly across all MC versions.
public final class BlockStateNormalizer {
    private BlockStateNormalizer() { }

    public static BlockIdentifier normalize(BlockIdentifier raw, Direction defaultFacing) {
        if (raw == null || raw.isAir()) return BlockIdentifier.AIR;
        String path = raw.path().toLowerCase(Locale.ROOT);
        Map<String, String> props = new LinkedHashMap<>(raw.properties());
        Direction dir = defaultFacing != null ? defaultFacing : Direction.SOUTH;
        String facingStr = dir.name().toLowerCase(Locale.ROOT);

        // Normalize property keys/values to lowercase
        Map<String, String> clean = new LinkedHashMap<>();
        for (var e : props.entrySet()) {
            clean.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().toLowerCase(Locale.ROOT));
        }

        if (path.contains("stairs")) {
            clean.putIfAbsent("facing", facingStr);
            clean.putIfAbsent("half", "bottom");
            clean.putIfAbsent("shape", "straight");
            clean.putIfAbsent("waterlogged", "false");
        } else if (path.contains("slab")) {
            if (clean.containsKey("half")) {
                String half = clean.remove("half");
                clean.putIfAbsent("type", "top".equals(half) ? "top" : "bottom");
            }
            clean.putIfAbsent("type", "bottom");
            clean.putIfAbsent("waterlogged", "false");
        } else if (path.contains("trapdoor")) {
            clean.putIfAbsent("facing", facingStr);
            clean.putIfAbsent("half", "bottom");
            clean.putIfAbsent("open", "false");
            clean.putIfAbsent("waterlogged", "false");
        } else if (path.contains("door")) {
            clean.putIfAbsent("facing", facingStr);
            clean.putIfAbsent("half", "lower");
            clean.putIfAbsent("hinge", "left");
            clean.putIfAbsent("open", "false");
        } else if (path.contains("_bed")) {
            clean.putIfAbsent("facing", facingStr);
            clean.putIfAbsent("part", "head");
        } else if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_pillar") || path.endsWith("_stem") || path.endsWith("_hyphae")) {
            clean.putIfAbsent("axis", (dir == Direction.EAST || dir == Direction.WEST) ? "x" : (dir == Direction.NORTH || dir == Direction.SOUTH) ? "z" : "y");
        } else if (path.contains("chest") || path.contains("furnace") || path.contains("smoker") || path.contains("lectern") || path.contains("anvil")) {
            clean.putIfAbsent("facing", facingStr);
        } else if (path.contains("barrel")) {
            clean.putIfAbsent("facing", "up");
        } else if (path.contains("lantern")) {
            clean.putIfAbsent("hanging", "false");
            clean.putIfAbsent("waterlogged", "false");
        } else if (path.contains("pane") || path.contains("fence") || path.contains("iron_bars")) {
            clean.putIfAbsent("waterlogged", "false");
        }

        return new BlockIdentifier(raw.namespace(), raw.path(), clean);
    }

    public static BlockIdentifier rotateClockwise(BlockIdentifier id) {
        if (id == null || id.properties().isEmpty()) return id;
        String facing = id.property("facing");
        if (facing != null) {
            String next = switch (facing.toLowerCase(Locale.ROOT)) {
                case "north" -> "east";
                case "east" -> "south";
                case "south" -> "west";
                case "west" -> "north";
                default -> facing;
            };
            return id.withProperty("facing", next);
        }
        String axis = id.property("axis");
        if ("x".equalsIgnoreCase(axis)) return id.withProperty("axis", "z");
        if ("z".equalsIgnoreCase(axis)) return id.withProperty("axis", "x");
        return id;
    }

    public static boolean requiresLowerSupport(BlockIdentifier id) {
        if (id == null) return false;
        String p = id.path().toLowerCase(Locale.ROOT);
        return p.contains("door") || p.contains("carpet") || p.contains("lantern")
            || p.contains("torch") || p.contains("flower") || p.contains("bed")
            || p.contains("pressure_plate") || p.contains("rail");
    }
}
