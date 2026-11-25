package com.x64Dynamics.tfb.architectural.exterior.wall;

import com.x64Dynamics.tfb.architectural.exterior.facades.FacadeOrnamentGenerator;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.util.*;

import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
public final class WallSegmenter {
    public enum BayType {
        PLAIN,
        WINDOW_SINGLE,
        WINDOW_DOUBLE,
        ARCHED_WINDOW,
        ENTRANCE_DOOR,
        BALCONY
    }

    public record WallBay(
        int startOffset,
        int width,
        BayType type,
        boolean isCenterBay
    ) { }

    public record FacadeSegmentation(
        int wallLength,
        int wallHeight,
        List<Integer> pillarOffsets,
        List<WallBay> bays
    ) { }

    private WallSegmenter() { }

    // Calculates the optimal architectural rhythm (pillars and bays) for a given wall length.
    // Ensures strict bilateral symmetry and proper proportion.
    public static FacadeSegmentation calculateRhythm(int wallLength, int wallHeight, boolean isEntranceFacade) {
        return calculateRhythm(wallLength, wallHeight, isEntranceFacade, 4, false, false);
    }

    public static FacadeSegmentation calculateRhythm(
        int wallLength,
        int wallHeight,
        boolean isEntranceFacade,
        int optimalBayWidth,
        boolean allowBalcony,
        boolean allowArch
    ) {
        List<Integer> pillarOffsets = new ArrayList<>();
        List<WallBay> bays = new ArrayList<>();

        if (wallLength < 5) {
            // Very short wall: single bay without intermediate pillars
            bays.add(new WallBay(0, wallLength, BayType.PLAIN, true));
            return new FacadeSegmentation(wallLength, wallHeight, pillarOffsets, bays);
        }

        // Always place pillars at the two outer corners (0 and wallLength - 1)
        pillarOffsets.add(0);
        pillarOffsets.add(wallLength - 1);

        int internalSpan = wallLength - 2; // Span between corner pillars

        // Adaptive bay width clamped to [2..6]
        int targetBayWidth = Math.max(2, Math.min(6, optimalBayWidth));
        int numBays = Math.max(1, (int) Math.round((double) internalSpan / (targetBayWidth + 1)));

        // Adjust numBays so that if isEntranceFacade, numBays is preferably odd (so there is a central entrance bay)
        if (isEntranceFacade && numBays % 2 == 0 && numBays > 1) {
            numBays = (internalSpan / 3 >= numBays + 1) ? numBays + 1 : numBays - 1;
        }

        int totalPillarsInside = numBays - 1;
        int spaceForBays = internalSpan - totalPillarsInside;
        int baseBayWidth = spaceForBays / numBays;
        int remainder = spaceForBays % numBays;

        int currentOffset = 1;
        int centerBayIndex = numBays / 2;

        for (int b = 0; b < numBays; b++) {
            // Distribute remainder symmetrically to center bays
            int extra = (b >= (numBays - remainder) / 2 && b < (numBays + remainder) / 2) ? 1 : 0;
            int currentBayWidth = baseBayWidth + extra;

            boolean isCenter = (b == centerBayIndex);
            BayType bayType;

            if (isEntranceFacade && isCenter) {
                bayType = BayType.ENTRANCE_DOOR;
            } else if (!isEntranceFacade && isCenter && allowBalcony && currentBayWidth >= 3 && wallHeight >= 5) {
                bayType = BayType.BALCONY;
            } else if (allowArch && currentBayWidth >= 3 && wallHeight >= 5) {
                bayType = BayType.ARCHED_WINDOW;
            } else if (currentBayWidth >= 4 && wallHeight >= 6) {
                bayType = BayType.WINDOW_DOUBLE;
            } else if (currentBayWidth >= 3 && wallHeight >= 4) {
                bayType = BayType.WINDOW_SINGLE;
            } else {
                bayType = BayType.PLAIN;
            }

            bays.add(new WallBay(currentOffset, currentBayWidth, bayType, isCenter));
            currentOffset += currentBayWidth;

            if (b < numBays - 1) {
                pillarOffsets.add(currentOffset);
                currentOffset += 1; // 1-block pillar
            }
        }

        Collections.sort(pillarOffsets);
        return new FacadeSegmentation(wallLength, wallHeight, pillarOffsets, bays);
    }

    // Composes a detailed 3D facade along a line with plinth base, segmented bays,
    // decorative string courses, window trims, and cornice header.
    public static List<PlacementTarget> composeFacade(
        Vec3i wallStart,
        Direction wallFacing,
        Direction outwardFacing,
        int length,
        int height,
        boolean isEntranceFacade,
        MaterialPalette palette
    ) {
        return composeFacade(wallStart, wallFacing, outwardFacing, length, height, isEntranceFacade, palette, null);
    }

    public static List<PlacementTarget> composeFacade(
        Vec3i wallStart,
        Direction wallFacing,
        Direction outwardFacing,
        int length,
        int height,
        boolean isEntranceFacade,
        MaterialPalette palette,
        com.x64Dynamics.tfb.architectural.builds.style.ArchitecturalStyleRuleSet styleRules
    ) {
        List<PlacementTarget> targets = new ArrayList<>();
        Map<Vec3i, PlacementTarget> map = new HashMap<>();

        int optBay = styleRules != null ? Math.max(2, styleRules.windowSpacing() + 1) : 4;
        boolean allowArch = styleRules != null && styleRules.allowPilasters();
        boolean allowBalcony = styleRules != null && styleRules.allowBalconies() && !isEntranceFacade;
        boolean allowDepth = styleRules == null || styleRules.allowCornice() || styleRules.allowPilasters();
        FacadeSegmentation segmentation = calculateRhythm(length, height, isEntranceFacade, optBay, allowBalcony, allowArch);

        BlockIdentifier wallBlock = palette.get(MaterialRole.WALL_PRIMARY);
        BlockIdentifier pillarBlock = palette.get(MaterialRole.PILLAR);
        BlockIdentifier plinthBlock = palette.get(MaterialRole.FOUNDATION);
        BlockIdentifier stairBlock = palette.get(MaterialRole.STAIR);
        BlockIdentifier glassBlock = palette.get(MaterialRole.WINDOW);
        BlockIdentifier slabBlock = palette.get(MaterialRole.SLAB);

        int plinthHeight = Math.min(1, height / 4);
        int stringCourseY = (height >= 8) ? wallStart.y() + height / 2 : -1;
        int corniceY = wallStart.y() + height - 1;

        // 1. Compose Structural Pillars with 3D base plinth & top corbel relief
        for (int pOffset : segmentation.pillarOffsets()) {
            for (int y = 0; y < height; y++) {
                Vec3i pillarPos = wallStart.offset(wallFacing, pOffset).add(0, y, 0);
                BlockIdentifier blk = (y < plinthHeight) ? plinthBlock : pillarBlock;
                map.put(pillarPos, new PlacementTarget(pillarPos, blk, MaterialRole.PILLAR));
                if (allowDepth && y == 0) {
                    Vec3i outBase = pillarPos.offset(outwardFacing, 1);
                    map.put(outBase, new PlacementTarget(outBase, stairBlock.withProperty("facing", outwardFacing.opposite().name().toLowerCase()).withProperty("half", "bottom"), MaterialRole.SUB_FOUNDATION));
                } else if (allowDepth && y == corniceY) {
                    Vec3i outCap = pillarPos.offset(outwardFacing, 1);
                    map.put(outCap, new PlacementTarget(outCap, stairBlock.withProperty("facing", outwardFacing.opposite().name().toLowerCase()).withProperty("half", "top"), MaterialRole.BEAM));
                }
            }
        }

        // 2. Compose Each Architectural Bay
        for (WallBay bay : segmentation.bays()) {
            int bayStart = bay.startOffset();
            int bayWidth = bay.width();

            for (int dx = 0; dx < bayWidth; dx++) {
                int currentOffset = bayStart + dx;
                Vec3i columnBase = wallStart.offset(wallFacing, currentOffset);

                for (int y = 0; y < height; y++) {
                    Vec3i pos = columnBase.add(0, y, 0);

                    if (y < plinthHeight) {
                        // Plinth foundation base
                        map.put(pos, new PlacementTarget(pos, plinthBlock, MaterialRole.FOUNDATION));
                    } else if (y == corniceY) {
                        // Upper cornice frieze band
                        map.put(pos, new PlacementTarget(pos, stairBlock.withProperty("facing", outwardFacing.opposite().name().toLowerCase()).withProperty("half", "top"), MaterialRole.ROOF_OVERHANG));
                    } else if (y == stringCourseY) {
                        // Mid-level horizontal string course band
                        map.put(pos, new PlacementTarget(pos, slabBlock.withProperty("type", "bottom"), MaterialRole.SLAB));
                    } else {
                        // Plain wall baseline for bay
                        map.put(pos, new PlacementTarget(pos, wallBlock, MaterialRole.WALL_PRIMARY));
                    }
                }
            }

            // Apply bay-specific opening compositions (windows/doors)
            int midBayOffset = bayStart + bayWidth / 2;
            Vec3i bayCenterBase = wallStart.offset(wallFacing, midBayOffset);

            switch (bay.type()) {
                case ENTRANCE_DOOR -> {
                    // Carve 1x2 entrance doorway with protruding step, lintel & porch lantern
                    Vec3i doorBase = bayCenterBase.add(0, plinthHeight, 0);
                    map.put(doorBase, new PlacementTarget(doorBase, BlockIdentifier.of("minecraft:air"), MaterialRole.AIR));
                    map.put(doorBase.add(0, 1, 0), new PlacementTarget(doorBase.add(0, 1, 0), BlockIdentifier.of("minecraft:air"), MaterialRole.AIR));
                    if (allowDepth) {
                        Vec3i stepOut = bayCenterBase.add(0, Math.max(0, plinthHeight - 1), 0).offset(outwardFacing, 1);
                        map.put(stepOut, new PlacementTarget(stepOut, slabBlock.withProperty("type", "bottom"), MaterialRole.SLAB));
                        Vec3i lampOut = doorBase.add(0, 2, 0).offset(outwardFacing, 1);
                        map.put(lampOut, new PlacementTarget(lampOut, palette.get(MaterialRole.LIGHT_SOURCE).withProperty("hanging", "false"), MaterialRole.LIGHT_SOURCE));
                    }

                    // Lintel stair above door
                    map.put(doorBase.add(0, 2, 0), new PlacementTarget(doorBase.add(0, 2, 0), stairBlock.withProperty("facing", outwardFacing.opposite().name().toLowerCase()).withProperty("half", "top"), MaterialRole.STAIR));
                }
                case WINDOW_SINGLE -> {
                    int relWinY = plinthHeight + 1;
                    if (wallStart.y() + relWinY + 1 < corniceY) {
                        Vec3i sillPos = bayCenterBase.add(0, relWinY - 1, 0);
                        Vec3i glass1 = bayCenterBase.add(0, relWinY, 0);
                        Vec3i glass2 = bayCenterBase.add(0, relWinY + 1, 0);
                        Vec3i lintelPos = bayCenterBase.add(0, relWinY + 2, 0);

                        map.put(sillPos, new PlacementTarget(sillPos, stairBlock.withProperty("facing", outwardFacing.name().toLowerCase()).withProperty("half", "top"), MaterialRole.STAIR));
                        map.put(glass1, new PlacementTarget(glass1, glassBlock, MaterialRole.WINDOW));
                        map.put(glass2, new PlacementTarget(glass2, glassBlock, MaterialRole.WINDOW));
                        if (allowDepth) {
                            Vec3i outSill = glass1.add(0, -1, 0).offset(outwardFacing, 1);
                            map.put(outSill, new PlacementTarget(outSill, slabBlock.withProperty("type", "top"), MaterialRole.SLAB));
                            if (bayWidth >= 3) {
                                BlockIdentifier shutter = BlockIdentifier.of("minecraft:spruce_trapdoor[facing=" + outwardFacing.name().toLowerCase() + ",half=bottom,open=true]");
                                Vec3i leftShutter = glass1.offset(wallFacing, -1).offset(outwardFacing, 1);
                                Vec3i rightShutter = glass1.offset(wallFacing, 1).offset(outwardFacing, 1);
                                map.put(leftShutter, new PlacementTarget(leftShutter, shutter, MaterialRole.DECORATION));
                                map.put(rightShutter, new PlacementTarget(rightShutter, shutter, MaterialRole.DECORATION));
                            }
                        }
                        if (wallStart.y() + relWinY + 2 < corniceY) {
                            map.put(lintelPos, new PlacementTarget(lintelPos, stairBlock.withProperty("facing", outwardFacing.opposite().name().toLowerCase()).withProperty("half", "top"), MaterialRole.STAIR));
                        }
                    }
                }
                case WINDOW_DOUBLE -> {
                    int relWinY = plinthHeight + 1;
                    if (wallStart.y() + relWinY + 1 < corniceY) {
                        Vec3i leftBase = wallStart.offset(wallFacing, midBayOffset - 1);
                        Vec3i rightBase = wallStart.offset(wallFacing, midBayOffset);

                        if (allowDepth) {
                            map.put(leftBase.add(0, relWinY - 1, 0).offset(outwardFacing, 1), new PlacementTarget(leftBase.add(0, relWinY - 1, 0).offset(outwardFacing, 1), slabBlock.withProperty("type", "top"), MaterialRole.SLAB));
                            map.put(rightBase.add(0, relWinY - 1, 0).offset(outwardFacing, 1), new PlacementTarget(rightBase.add(0, relWinY - 1, 0).offset(outwardFacing, 1), slabBlock.withProperty("type", "top"), MaterialRole.SLAB));
                        }
                        map.put(leftBase.add(0, relWinY, 0), new PlacementTarget(leftBase.add(0, relWinY, 0), glassBlock, MaterialRole.WINDOW));
                        map.put(leftBase.add(0, relWinY + 1, 0), new PlacementTarget(leftBase.add(0, relWinY + 1, 0), glassBlock, MaterialRole.WINDOW));
                        map.put(rightBase.add(0, relWinY, 0), new PlacementTarget(rightBase.add(0, relWinY, 0), glassBlock, MaterialRole.WINDOW));
                        map.put(rightBase.add(0, relWinY + 1, 0), new PlacementTarget(rightBase.add(0, relWinY + 1, 0), glassBlock, MaterialRole.WINDOW));
                    }
                }
                case ARCHED_WINDOW -> {
                    int relWinY = plinthHeight + 1;
                    if (wallStart.y() + relWinY + 2 < corniceY && bayWidth >= 3) {
                        Vec3i left = wallStart.offset(wallFacing, midBayOffset - 1);
                        Vec3i right = wallStart.offset(wallFacing, midBayOffset + 1);
                        map.put(bayCenterBase.add(0, relWinY, 0), new PlacementTarget(bayCenterBase.add(0, relWinY, 0), glassBlock, MaterialRole.WINDOW));
                        map.put(bayCenterBase.add(0, relWinY + 1, 0), new PlacementTarget(bayCenterBase.add(0, relWinY + 1, 0), glassBlock, MaterialRole.WINDOW));
                        map.put(left.add(0, relWinY + 2, 0), new PlacementTarget(left.add(0, relWinY + 2, 0),
                            stairBlock.withProperty("facing", wallFacing.opposite().name().toLowerCase()).withProperty("half", "top"), MaterialRole.ARCH));
                        map.put(right.add(0, relWinY + 2, 0), new PlacementTarget(right.add(0, relWinY + 2, 0),
                            stairBlock.withProperty("facing", wallFacing.name().toLowerCase()).withProperty("half", "top"), MaterialRole.ARCH));
                        map.put(bayCenterBase.add(0, relWinY + 2, 0), new PlacementTarget(bayCenterBase.add(0, relWinY + 2, 0),
                            slabBlock.withProperty("type", "top"), MaterialRole.ARCH));
                    }
                }
                case BALCONY -> {
                    int relDoorY = plinthHeight;
                    Vec3i doorBase = bayCenterBase.add(0, relDoorY, 0);
                    map.put(doorBase, new PlacementTarget(doorBase, BlockIdentifier.AIR, MaterialRole.AIR));
                    map.put(doorBase.add(0, 1, 0), new PlacementTarget(doorBase.add(0, 1, 0), BlockIdentifier.AIR, MaterialRole.AIR));
                    for (PlacementTarget bt : FacadeOrnamentGenerator.generateBalcony(doorBase, outwardFacing, Math.min(3, bayWidth), 2, palette)) {
                        map.put(bt.pos(), bt);
                    }
                }
                case PLAIN -> {
                    // Solid wall, optionally add decorative niche or wall sconce
                    if (height >= 5) {
                        Vec3i sconcePos = bayCenterBase.add(0, plinthHeight + 2, 0).offset(outwardFacing, 1);
                        map.put(sconcePos, new PlacementTarget(sconcePos, BlockIdentifier.of("minecraft:wall_torch[facing=" + outwardFacing.name().toLowerCase() + "]"), MaterialRole.LIGHT_SOURCE));
                    }
                }
                default -> { }
            }
        }

        targets.addAll(map.values());
        return targets;
    }
}
