package com.x64Dynamics.tfb.architectural;

import com.x64Dynamics.tfb.geometry.Axis;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.ShapeRasterizer;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

public final class ConnectedBuildingComposer {
    private ConnectedBuildingComposer() { }

    public record ConnectionPortal(
        Vec3i centerFloor,
        Direction orientation,
        int width,
        int height
    ) { }

    public record CompositionResult(
        List<PlacementTarget> targets,
        List<ConnectionPortal> portals,
        BoundingBox3i totalBounds
    ) { }

    public static CompositionResult composeCompound(
        List<BuildingSection> sections,
        MaterialPalette palette
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        Map<Vec3i, PlacementTarget> blockMap = new HashMap<>();
        List<ConnectionPortal> portals = new ArrayList<>();

        if (sections.isEmpty()) {
            return new CompositionResult(targets, portals, BoundingBox3i.fromOriginAndSize(Vec3i.ZERO, 1, 1, 1));
        }

        // 1. Compute total bounds
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BuildingSection s : sections) {
            minX = Math.min(minX, s.bounds().min().x());
            minY = Math.min(minY, s.bounds().min().y());
            minZ = Math.min(minZ, s.bounds().min().z());
            maxX = Math.max(maxX, s.bounds().max().x());
            maxY = Math.max(maxY, s.bounds().max().y());
            maxZ = Math.max(maxZ, s.bounds().max().z());
        }
        BoundingBox3i totalBounds = new BoundingBox3i(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));

        // 2. Identify shared interfaces & create connection portals between adjacent/overlapping sections
        for (int i = 0; i < sections.size(); i++) {
            for (int j = i + 1; j < sections.size(); j++) {
                BuildingSection a = sections.get(i);
                BuildingSection b = sections.get(j);
                findAndCreatePortal(a, b, portals);
            }
        }

        // 3. Generate Floors across all sections
        BlockIdentifier floorBlock = palette.get(MaterialRole.FLOOR);
        for (BuildingSection section : sections) {
            BoundingBox3i box = section.bounds();
            int floorY = box.min().y();

            List<Vec3i> floorPositions = getSectionFloorPositions(section, floorY);
            for (Vec3i pos : floorPositions) {
                blockMap.put(pos, new PlacementTarget(pos, floorBlock, MaterialRole.FLOOR));
            }
        }

        // 4. Generate Walls with shared interface resolution & carved connection portals
        for (BuildingSection section : sections) {
            generateSectionWalls(section, sections, portals, palette, blockMap);
        }

        // 5. Generate Roofs for each section without penetrating taller neighboring sections
        for (BuildingSection section : sections) {
            generateSectionRoof(section, sections, palette, blockMap);
        }

        // 6. Frame Connection Portals with interior arch headers
        BlockIdentifier archBlock = palette.get(MaterialRole.STAIR);
        for (ConnectionPortal portal : portals) {
            Vec3i center = portal.centerFloor();
            int h = portal.height();
            int w = portal.width();
            Direction dir = portal.orientation();
            Direction perp = dir.rotateY();

            // Clear doorway air space
            int halfW = w / 2;
            for (int dx = -halfW; dx <= halfW; dx++) {
                for (int y = 1; y <= h; y++) {
                    Vec3i p = center.offset(perp, dx).add(0, y, 0);
                    blockMap.put(p, new PlacementTarget(p, BlockIdentifier.of("minecraft:air"), MaterialRole.AIR));
                }
                // Arch header top
                Vec3i archPos = center.offset(perp, dx).add(0, h + 1, 0);
                blockMap.put(archPos, new PlacementTarget(archPos, archBlock, MaterialRole.STAIR));
            }
        }

        targets.addAll(blockMap.values());
        return new CompositionResult(targets, portals, totalBounds);
    }

    private static void findAndCreatePortal(BuildingSection a, BuildingSection b, List<ConnectionPortal> portals) {
        BoundingBox3i ba = a.bounds();
        BoundingBox3i bb = b.bounds();

        // Check if adjacent or overlapping in X and Z
        int overlapMinX = Math.max(ba.min().x(), bb.min().x());
        int overlapMaxX = Math.min(ba.max().x(), bb.max().x());
        int overlapMinZ = Math.max(ba.min().z(), bb.min().z());
        int overlapMaxZ = Math.min(ba.max().z(), bb.max().z());
        int floorY = Math.max(ba.min().y(), bb.min().y());

        // Touching along X (face contact at Z)
        boolean touchingZ = (ba.max().z() + 1 == bb.min().z()) || (bb.max().z() + 1 == ba.min().z()) ||
                            (ba.max().z() == bb.min().z()) || (bb.max().z() == ba.min().z());
        // Touching along Z (face contact at X)
        boolean touchingX = (ba.max().x() + 1 == bb.min().x()) || (bb.max().x() + 1 == ba.min().x()) ||
                            (ba.max().x() == bb.min().x()) || (bb.max().x() == ba.min().x());

        if (touchingZ && (overlapMaxX - overlapMinX >= 2)) {
            int midX = (overlapMinX + overlapMaxX) / 2;
            int contactZ = (ba.max().z() + 1 == bb.min().z() || ba.max().z() == bb.min().z()) ? ba.max().z() : ba.min().z();
            int portalWidth = Math.min(3, Math.max(2, (overlapMaxX - overlapMinX) / 2));
            portals.add(new ConnectionPortal(new Vec3i(midX, floorY, contactZ), Direction.SOUTH, portalWidth, 2));
        } else if (touchingX && (overlapMaxZ - overlapMinZ >= 2)) {
            int midZ = (overlapMinZ + overlapMaxZ) / 2;
            int contactX = (ba.max().x() + 1 == bb.min().x() || ba.max().x() == bb.min().x()) ? ba.max().x() : ba.min().x();
            int portalWidth = Math.min(3, Math.max(2, (overlapMaxZ - overlapMinZ) / 2));
            portals.add(new ConnectionPortal(new Vec3i(contactX, floorY, midZ), Direction.EAST, portalWidth, 2));
        } else if (overlapMaxX >= overlapMinX && overlapMaxZ >= overlapMinZ) {
            // Overlapping boxes (shared volume)
            int midX = (overlapMinX + overlapMaxX) / 2;
            int midZ = (overlapMinZ + overlapMaxZ) / 2;
            portals.add(new ConnectionPortal(new Vec3i(midX, floorY, midZ), Direction.SOUTH, 2, 2));
        }
    }

    private static List<Vec3i> getSectionFloorPositions(BuildingSection section, int floorY) {
        BoundingBox3i box = section.bounds();
        return switch (section.shape()) {
            case RECTANGLE, ROTATED_45 -> ShapeRasterizer.box(
                BoundingBox3i.fromCorners(new Vec3i(box.min().x(), floorY, box.min().z()), new Vec3i(box.max().x(), floorY, box.max().z())),
                false
            );
            case CYLINDER -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, floorY, (box.min().z() + box.max().z()) / 2);
                int radX = box.sizeX() / 2;
                int radZ = box.sizeZ() / 2;
                yield ShapeRasterizer.cylinder(center, radX, radZ, 1, false);
            }
            case OCTAGON -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, floorY, (box.min().z() + box.max().z()) / 2);
                int rad = Math.min(box.sizeX(), box.sizeZ()) / 2;
                yield ShapeRasterizer.regularPolygon(center, rad, 8, 1, Math.PI / 8.0, false);
            }
            case HEXAGON -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, floorY, (box.min().z() + box.max().z()) / 2);
                int rad = Math.min(box.sizeX(), box.sizeZ()) / 2;
                yield ShapeRasterizer.regularPolygon(center, rad, 6, 1, 0, false);
            }
            case ROUNDED_RECTANGLE -> ShapeRasterizer.roundedBox(
                BoundingBox3i.fromCorners(new Vec3i(box.min().x(), floorY, box.min().z()), new Vec3i(box.max().x(), floorY, box.max().z())),
                2,
                false
            );
        };
    }

    private static void generateSectionWalls(
        BuildingSection current,
        List<BuildingSection> allSections,
        List<ConnectionPortal> portals,
        MaterialPalette palette,
        Map<Vec3i, PlacementTarget> blockMap
    ) {
        BoundingBox3i box = current.bounds();
        int roofHeight = Math.max(3, box.sizeY() / 3);
        int wallTopY = current.isPrimary() ? box.max().y() - roofHeight : box.max().y();
        if (current.roofType() == RoofGenerator.RoofType.FLAT) {
            wallTopY = box.max().y();
        }

        BoundingBox3i wallBounds = BoundingBox3i.fromCorners(box.min(), new Vec3i(box.max().x(), wallTopY, box.max().z()));

        List<Vec3i> rawWallBlocks;
        switch (current.shape()) {
            case RECTANGLE, ROTATED_45 -> rawWallBlocks = ShapeRasterizer.box(wallBounds, true);
            case CYLINDER -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, box.min().y(), (box.min().z() + box.max().z()) / 2);
                int radX = box.sizeX() / 2;
                int radZ = box.sizeZ() / 2;
                rawWallBlocks = ShapeRasterizer.cylinder(center, radX, radZ, wallTopY - box.min().y() + 1, true);
            }
            case OCTAGON -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, box.min().y(), (box.min().z() + box.max().z()) / 2);
                int rad = Math.min(box.sizeX(), box.sizeZ()) / 2;
                rawWallBlocks = ShapeRasterizer.regularPolygon(center, rad, 8, wallTopY - box.min().y() + 1, Math.PI / 8.0, true);
            }
            case HEXAGON -> {
                Vec3i center = new Vec3i((box.min().x() + box.max().x()) / 2, box.min().y(), (box.min().z() + box.max().z()) / 2);
                int rad = Math.min(box.sizeX(), box.sizeZ()) / 2;
                rawWallBlocks = ShapeRasterizer.regularPolygon(center, rad, 6, wallTopY - box.min().y() + 1, 0, true);
            }
            case ROUNDED_RECTANGLE -> rawWallBlocks = ShapeRasterizer.roundedBox(wallBounds, 2, true);
            default -> rawWallBlocks = ShapeRasterizer.box(wallBounds, true);
        }

        BlockIdentifier primaryWall = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier cornerWall = palette.get(MaterialRole.WALL_CORNER);

        for (Vec3i pos : rawWallBlocks) {
            // Exclude bottom floor (already paved) and top ceiling
            if (pos.y() == box.min().y() || pos.y() == wallTopY) continue;

            // Check if pos falls inside an active connection portal (clearway)
            boolean inPortal = false;
            for (ConnectionPortal portal : portals) {
                if (pos.y() <= portal.centerFloor().y() + portal.height()) {
                    int distPortal = (int) Math.round(pos.distance(portal.centerFloor().add(0, pos.y() - portal.centerFloor().y(), 0)));
                    if (distPortal <= portal.width() / 2) {
                        inPortal = true;
                        break;
                    }
                }
            }
            if (inPortal) continue; // Keep open for walking through!

            // Check if block is inside another section's interior
            boolean insideAnother = false;
            for (BuildingSection other : allSections) {
                if (other == current) continue;
                BoundingBox3i ob = other.bounds();
                // Interior check: strictly inside without outer edge
                if (pos.x() > ob.min().x() && pos.x() < ob.max().x() &&
                    pos.z() > ob.min().z() && pos.z() < ob.max().z() &&
                    pos.y() >= ob.min().y() && pos.y() <= ob.max().y()) {
                    insideAnother = true;
                    break;
                }
            }

            if (insideAnother) continue; // Don't block other section's room!

            boolean isCorner = (pos.x() == box.min().x() || pos.x() == box.max().x()) &&
                               (pos.z() == box.min().z() || pos.z() == box.max().z());
            BlockIdentifier block = isCorner ? cornerWall : primaryWall;
            blockMap.put(pos, new PlacementTarget(pos, block, isCorner ? MaterialRole.WALL_CORNER : MaterialRole.WALL_PRIMARY));
        }
    }

    private static void generateSectionRoof(
        BuildingSection current,
        List<BuildingSection> allSections,
        MaterialPalette palette,
        Map<Vec3i, PlacementTarget> blockMap
    ) {
        BoundingBox3i box = current.bounds();
        int roofHeight = Math.max(3, box.sizeY() / 3);
        int roofStartY = box.max().y() - roofHeight;

        BoundingBox3i roofBounds = BoundingBox3i.fromCorners(
            new Vec3i(box.min().x(), roofStartY, box.min().z()),
            new Vec3i(box.max().x(), box.max().y(), box.max().z())
        );

        Axis ridge = (box.sizeX() >= box.sizeZ()) ? Axis.X : Axis.Z;
        List<PlacementTarget> roofTargets = RoofGenerator.generateRoof(roofBounds, current.roofType(), ridge, 1, palette);

        for (PlacementTarget target : roofTargets) {
            Vec3i pos = target.pos();

            // Prevent roof blocks from clipping inside taller or intersecting attached sections
            boolean clipsInsideOther = false;
            for (BuildingSection other : allSections) {
                if (other == current) continue;
                BoundingBox3i ob = other.bounds();
                boolean insideFootprint = pos.x() >= ob.min().x() && pos.x() <= ob.max().x() &&
                                          pos.z() >= ob.min().z() && pos.z() <= ob.max().z();
                if (!insideFootprint) continue;

                // Taller section: never clip inside its volume
                if (ob.max().y() > current.bounds().max().y() &&
                    pos.y() >= ob.min().y() && pos.y() <= ob.max().y()) {
                    clipsInsideOther = true;
                    break;
                }
                // Equal-height intersecting wing: keep higher roof surface (valley resolution)
                if (ob.max().y() == current.bounds().max().y()) {
                    PlacementTarget existing = blockMap.get(pos);
                    if (existing != null && existing.role() == MaterialRole.ROOF_RIDGE) {
                        clipsInsideOther = true;
                        break;
                    }
                    // Clear lower overhang if inside adjacent wing interior
                    if (pos.x() > ob.min().x() && pos.x() < ob.max().x() &&
                        pos.z() > ob.min().z() && pos.z() < ob.max().z() &&
                        pos.y() < ob.max().y() - Math.max(3, ob.sizeY() / 3)) {
                        clipsInsideOther = true;
                        break;
                    }
                }
            }

            if (!clipsInsideOther) {
                blockMap.put(pos, target);
            }
        }
    }
}
