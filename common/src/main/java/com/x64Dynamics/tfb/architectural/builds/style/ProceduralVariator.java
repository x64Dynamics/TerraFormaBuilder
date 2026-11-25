package com.x64Dynamics.tfb.architectural.builds.style;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.ArrayList;
import java.util.List;

// Context-aware architectural pattern variation engine:
// applies ground-contact plinth courses, alternating corner quoins, and upper-story
// material zoning rather than arbitrary per-block random noise.
public final class ProceduralVariator {
    private ProceduralVariator() {}

    public static List<PlacementTarget> applyFacadeVariation(
        List<PlacementTarget> targets,
        MaterialPalette palette,
        long seed,
        float variationRate
    ) {
        if (variationRate <= 0.0f || targets.isEmpty()) return new ArrayList<>(targets);

        int minY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (PlacementTarget t : targets) {
            if (t.role() == MaterialRole.WALL_PRIMARY || t.role() == MaterialRole.FOUNDATION) {
                minY = Math.min(minY, t.pos().y());
                minX = Math.min(minX, t.pos().x());
                maxX = Math.max(maxX, t.pos().x());
                minZ = Math.min(minZ, t.pos().z());
                maxZ = Math.max(maxZ, t.pos().z());
            }
        }

        List<PlacementTarget> result = new ArrayList<>(targets.size());
        BlockIdentifier wallAccent = palette.get(MaterialRole.WALL_ACCENT);
        BlockIdentifier foundation = palette.get(MaterialRole.FOUNDATION);

        for (PlacementTarget target : targets) {
            if (target.role() == MaterialRole.WALL_PRIMARY) {
                Vec3i p = target.pos();
                int relY = p.y() - minY;
                boolean nearCorner = (Math.abs(p.x() - minX) <= 1 || Math.abs(p.x() - maxX) <= 1)
                    && (Math.abs(p.z() - minZ) <= 1 || Math.abs(p.z() - maxZ) <= 1);

                if (relY <= 1) {
                    // Ground-contact masonry course with natural stone mixing
                    long h = (p.x() * 73856093L) ^ (p.z() * 83492791L) ^ seed;
                    BlockIdentifier baseBlk = (Math.abs(h) % 5 == 0 && foundation.path().equals("cobblestone"))
                        ? BlockIdentifier.of("minecraft:andesite")
                        : (Math.abs(h) % 7 == 0 && foundation.path().equals("stone_bricks")
                            ? BlockIdentifier.of("minecraft:mossy_stone_bricks") : foundation);
                    result.add(new PlacementTarget(p, baseBlk, MaterialRole.FOUNDATION));
                } else if (nearCorner && (relY % 2 == 0)) {
                    // Alternating corner quoins
                    result.add(new PlacementTarget(p, wallAccent, MaterialRole.WALL_ACCENT));
                } else if (relY >= 5 && variationRate >= 0.35f) {
                    // Upper-story material zoning
                    result.add(new PlacementTarget(p, wallAccent, MaterialRole.WALL_ACCENT));
                } else {
                    result.add(target);
                }
            } else if (target.role() == MaterialRole.ROOF_SLOPE) {
                Vec3i p = target.pos();
                long h = (p.x() * 73856093L) ^ (p.y() * 19349663L) ^ (p.z() * 83492791L) ^ seed;
                String rp = target.block().path();
                if (Math.abs(h) % 6 == 0 && rp.equals("cobblestone_stairs")) {
                    BlockIdentifier mix = BlockIdentifier.of("minecraft:andesite_stairs");
                    if (target.block().property("facing") != null) mix = mix.withProperty("facing", target.block().property("facing"));
                    if (target.block().property("half") != null) mix = mix.withProperty("half", target.block().property("half"));
                    result.add(new PlacementTarget(p, mix, MaterialRole.ROOF_SLOPE));
                } else if (Math.abs(h) % 8 == 0 && rp.equals("cobblestone_stairs")) {
                    BlockIdentifier mix = BlockIdentifier.of("minecraft:mossy_cobblestone_stairs");
                    if (target.block().property("facing") != null) mix = mix.withProperty("facing", target.block().property("facing"));
                    if (target.block().property("half") != null) mix = mix.withProperty("half", target.block().property("half"));
                    result.add(new PlacementTarget(p, mix, MaterialRole.ROOF_SLOPE));
                } else {
                    result.add(target);
                }
            } else {
                result.add(target);
            }
        }
        return result;
    }
}
