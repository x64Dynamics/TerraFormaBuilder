package com.x64Dynamics.tfb.architectural.builds.city;

import com.x64Dynamics.tfb.architectural.builds.HomeGenerator;
import com.x64Dynamics.tfb.architectural.builds.PlacementTarget;
import com.x64Dynamics.tfb.architectural.builds.RoadInfrastructurePlanner;

import com.x64Dynamics.tfb.architectural.builds.*;
import com.x64Dynamics.tfb.architectural.builds.plan.BuildingDefinition;
import com.x64Dynamics.tfb.architectural.builds.plan.BuildingPlan;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.SplinePath;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.*;

import com.x64Dynamics.tfb.architectural.interior.layouts.WingConfiguration;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
// Procedural urban planner that builds road networks, zones districts, subdivides blocks
// into street-facing parcels, creates public spaces, and generates diverse buildings.
public final class CityPlanner {
    private CityPlanner() { }

    public static void planCity(WorldProject project) {
        planCity(project, null);
    }

    public static void planCity(WorldProject project, com.x64Dynamics.tfb.architectural.exterior.terrain.HeightmapGrid heightmap) {
        BoundingBox3i bounds = project.bounds();
        int minX = bounds.min().x(), maxX = bounds.max().x();
        int minZ = bounds.min().z(), maxZ = bounds.max().z();
        int y = bounds.min().y();
        int spanX = bounds.sizeX();
        int spanZ = bounds.sizeZ();

        Random rng = new Random(project.seed());
        StylePreset baseStyle = project.primaryStyle();
        MaterialPalette basePalette = baseStyle.createPalette();

        // 1. Preserve any locked parcels before regenerating
        Map<String, CityParcel> lockedParcels = new LinkedHashMap<>();
        for (CityDistrict d : project.districts()) {
            for (CityParcel p : d.parcels()) {
                if (p.isLocked()) lockedParcels.put(p.id(), p);
            }
        }

        project.roadNetwork().clear();
        project.clearDistricts();
        project.clearPublicSpaces();

        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;

        // 2. Generate Macro Road Network (Central Crossroads + Secondary Streets)
        List<PlacementTarget> roadTargets = new ArrayList<>();
        RoadInfrastructurePlanner.RoadStyle roadStyle = switch (project.pattern()) {
            case PLANNED_GRID, URBAN_DISTRICT, SUBURBAN_NEIGHBORHOOD -> RoadInfrastructurePlanner.RoadStyle.ASPHALT_MODERN;
            case INDUSTRIAL_SECTOR -> RoadInfrastructurePlanner.RoadStyle.PAVED_STONE;
            case ORGANIC_VILLAGE, MEDIEVAL_TOWN -> RoadInfrastructurePlanner.RoadStyle.PAVED_STONE;
        };
        RoadInfrastructurePlanner.RoadProfile arterialProfile = new RoadInfrastructurePlanner.RoadProfile(
            roadStyle, 3, 1, true, project.pattern() == SettlementPattern.PLANNED_GRID || project.pattern() == SettlementPattern.URBAN_DISTRICT,
            true, 14, false, false
        );

        // East-West Arterial
        List<Vec3i> ewPts = List.of(new Vec3i(minX, y, midZ), new Vec3i(maxX, y, midZ));
        project.roadNetwork().addSegment(new RoadNetwork.RoadSegment("arterial_ew", RoadNetwork.RoadTier.ARTERIAL, ewPts, 5));
        roadTargets.addAll(RoadInfrastructurePlanner.generateSplineRoad(
            SplinePath.fromIntegerPoints(SplinePath.SplineType.LINEAR, ewPts, false), arterialProfile, basePalette
        ));

        // North-South Arterial
        List<Vec3i> nsPts = List.of(new Vec3i(midX, y, minZ), new Vec3i(midX, y, maxZ));
        project.roadNetwork().addSegment(new RoadNetwork.RoadSegment("arterial_ns", RoadNetwork.RoadTier.ARTERIAL, nsPts, 5));
        roadTargets.addAll(RoadInfrastructurePlanner.generateSplineRoad(
            SplinePath.fromIntegerPoints(SplinePath.SplineType.LINEAR, nsPts, false), arterialProfile, basePalette
        ));

        Vec3i centerCross = new Vec3i(midX, y, midZ);
        project.roadNetwork().addIntersection(centerCross);
        roadTargets.addAll(RoadInfrastructurePlanner.generateIntersection(
            centerCross,
            project.pattern() == SettlementPattern.ORGANIC_VILLAGE ? RoadInfrastructurePlanner.IntersectionType.ROUNDABOUT : RoadInfrastructurePlanner.IntersectionType.FOUR_WAY,
            arterialProfile, basePalette
        ));
        if (heightmap != null) {
            if (!heightmap.isFlat()) {
                List<TerrainRoadPathfinder.RoadNode> ewNodes = TerrainRoadPathfinder.findPath(
                    new Vec3i(minX, y, midZ), new Vec3i(maxX, y, midZ), heightmap, y - 2, 4096
                );
                List<TerrainRoadPathfinder.RoadNode> nsNodes = TerrainRoadPathfinder.findPath(
                    new Vec3i(midX, y, minZ), new Vec3i(midX, y, maxZ), heightmap, y - 2, 4096
                );
                if (!ewNodes.isEmpty() && !nsNodes.isEmpty()) {
                    List<PlacementTarget> astarRoads = new ArrayList<>();
                    astarRoads.addAll(TerrainRoadPathfinder.buildRoadTargets(ewNodes, 2, basePalette));
                    astarRoads.addAll(TerrainRoadPathfinder.buildRoadTargets(nsNodes, 2, basePalette));
                    roadTargets = astarRoads;
                }
            }
            List<PlacementTarget> gradedRoads = new ArrayList<>(roadTargets.size());
            com.x64Dynamics.tfb.material.BlockIdentifier sub = basePalette.get(com.x64Dynamics.tfb.material.MaterialRole.FOUNDATION);
            for (PlacementTarget rt : roadTargets) {
                int gy = heightmap.getElevation(rt.pos().x(), rt.pos().z());
                int dy = gy - y;
                Vec3i newPos = rt.pos().add(0, dy, 0);
                gradedRoads.add(new PlacementTarget(newPos, rt.block(), rt.role()));
                if (rt.pos().y() == y && dy > 0) {
                    gradedRoads.add(new PlacementTarget(newPos.add(0, -1, 0), sub, com.x64Dynamics.tfb.material.MaterialRole.FOUNDATION));
                }
            }
            roadTargets = gradedRoads;
        }
        project.roadNetwork().setResolvedTargets(roadTargets);

        // 3. Create 4 Districts around the main crossroads with thematic diversity
        int roadClear = 4; // half-width buffer around central streets
        BoundingBox3i qNW = safeBox(minX, y, minZ, midX - roadClear, bounds.max().y(), midZ - roadClear);
        BoundingBox3i qNE = safeBox(midX + roadClear, y, minZ, maxX, bounds.max().y(), midZ - roadClear);
        BoundingBox3i qSW = safeBox(minX, y, midZ + roadClear, midX - roadClear, bounds.max().y(), maxZ);
        BoundingBox3i qSE = safeBox(midX + roadClear, y, midZ + roadClear, maxX, bounds.max().y(), maxZ);

        StylePreset secondaryStyle = pickComplementaryStyle(baseStyle);

        CityDistrict dNW = new CityDistrict("dist_nw", "Civic & Market Quarter", CityDistrict.DistrictRole.CIVIC_CENTER, qNW, baseStyle);
        CityDistrict dNE = new CityDistrict("dist_ne", "North Residential", CityDistrict.DistrictRole.RESIDENTIAL, qNE, baseStyle);
        CityDistrict dSW = new CityDistrict("dist_sw", "Artisan & Trade Ward", CityDistrict.DistrictRole.INDUSTRIAL, qSW, secondaryStyle);
        CityDistrict dSE = new CityDistrict("dist_se", "Garden Suburb", CityDistrict.DistrictRole.GARDEN_PARK, qSE, baseStyle);

        // 4. Add Public Spaces (Market Square in NW if large, Park in SE if large)
        if (qNW.sizeX() >= 11 && qNW.sizeZ() >= 11) {
            int pSize = Math.min(13, Math.min(qNW.sizeX(), qNW.sizeZ()));
            BoundingBox3i plazaBox = BoundingBox3i.fromOriginAndSize(
                new Vec3i(qNW.max().x() - pSize + 1, y, qNW.max().z() - pSize + 1), pSize, 4, pSize
            );
            PublicSpace plaza = new PublicSpace("pub_market", "Town Market Square", PublicSpace.PublicSpaceType.MARKET_SQUARE, plazaBox);
            plaza.setTargets(PublicSpaceGenerator.generate(plaza, basePalette, project.seed() + 11));
            project.addPublicSpace(plaza);
        }

        if (qSE.sizeX() >= 11 && qSE.sizeZ() >= 11) {
            int pSize = Math.min(11, Math.min(qSE.sizeX(), qSE.sizeZ()));
            BoundingBox3i parkBox = BoundingBox3i.fromOriginAndSize(
                new Vec3i(qSE.min().x(), y, qSE.min().z()), pSize, 5, pSize
            );
            PublicSpace park = new PublicSpace("pub_park", "Central Commons Park", PublicSpace.PublicSpaceType.PARK_GARDEN, parkBox);
            park.setTargets(PublicSpaceGenerator.generate(park, basePalette, project.seed() + 29));
            project.addPublicSpace(park);
        }

        // 5. Subdivide each district into street-facing buildable parcels
        subdivideDistrictParcels(dNW, Direction.SOUTH, project.pattern(), rng, lockedParcels);
        subdivideDistrictParcels(dNE, Direction.SOUTH, project.pattern(), rng, lockedParcels);
        subdivideDistrictParcels(dSW, Direction.NORTH, project.pattern(), rng, lockedParcels);
        subdivideDistrictParcels(dSE, Direction.NORTH, project.pattern(), rng, lockedParcels);

        project.addDistrict(dNW);
        project.addDistrict(dNE);
        project.addDistrict(dSW);
        project.addDistrict(dSE);

        // 6. Generate varied BuildingPlans for every unlocked parcel (with terrain adaptation if heightmap provided)
        for (CityDistrict d : project.districts()) {
            for (CityParcel p : d.parcels()) {
                if (!p.isLocked() || p.buildingPlan() == null) {
                    if (heightmap != null) {
                        int cx = (p.bounds().min().x() + p.bounds().max().x()) / 2;
                        int cz = (p.bounds().min().z() + p.bounds().max().z()) / 2;
                        int elev = heightmap.getElevation(cx, cz);
                        BoundingBox3i adapted = BoundingBox3i.fromOriginAndSize(
                            new Vec3i(p.bounds().min().x(), elev, p.bounds().min().z()),
                            p.bounds().sizeX(), p.bounds().sizeY(), p.bounds().sizeZ()
                        );
                        p.setBounds(adapted);
                    }
                    generateParcelBuilding(p, d);
                    if (heightmap != null && p.buildingPlan() != null) {
                        var terrainRes = com.x64Dynamics.tfb.architectural.exterior.terrain.TerrainAdapter.adapt(
                            p.bounds(), heightmap, com.x64Dynamics.tfb.architectural.exterior.terrain.FoundationStrategy.STEPPED_TERRACE, d.style().createPalette()
                        );
                        List<PlacementTarget> combined = new ArrayList<>(terrainRes.foundationPlacements());
                        combined.addAll(p.buildingPlan().targets());
                        var bp = p.buildingPlan();
                        p.setBuildingPlan(new BuildingPlan(bp.definition(), bp.floors(), bp.stairwells(), combined, bp.totalBounds(), bp.validation()));
                    }
                }
            }
        }
    }

    public static boolean regenerateParcel(WorldProject project, String parcelId, long newSeed) {
        for (CityDistrict d : project.districts()) {
            for (CityParcel p : d.parcels()) {
                if (p.id().equals(parcelId)) {
                    if (p.isLocked()) return false;
                    p.setSeed(newSeed);
                    generateParcelBuilding(p, d);
                    project.touch();
                    return true;
                }
            }
        }
        return false;
    }

    public static void generateParcelBuilding(CityParcel parcel, CityDistrict district) {
        Random rng = new Random(parcel.seed());
        StylePreset style = district.style();
        MaterialPalette palette = style.createPalette();

        RoofGenerator.RoofType[] roofs = {
            RoofGenerator.RoofType.GABLE,
            RoofGenerator.RoofType.HIP,
            RoofGenerator.RoofType.MANSARD,
            RoofGenerator.RoofType.SHED
        };
        RoofGenerator.RoofType roof = switch (style) {
            case MODERN, MINIMALIST -> rng.nextBoolean() ? RoofGenerator.RoofType.FLAT : RoofGenerator.RoofType.SHED;
            case INDUSTRIAL -> RoofGenerator.RoofType.MANSARD;
            default -> roofs[rng.nextInt(3)];
        };

        int stories = switch (parcel.purpose()) {
            case COTTAGE, WORKSHOP_SMITHY, WAREHOUSE -> 1;
            case TOWNHOUSE, TAVERN_INN, LIBRARY_CIVIC -> 2;
            case MANOR -> (parcel.bounds().sizeX() >= 14 && parcel.bounds().sizeZ() >= 14) ? 3 : 2;
            default -> 1;
        };

        WingConfiguration wing = (parcel.purpose() == CityParcel.ParcelPurpose.MANOR
            && parcel.bounds().sizeX() >= 15 && parcel.bounds().sizeZ() >= 15)
            ? WingConfiguration.L_SHAPED
            : WingConfiguration.SINGLE_RECTANGLE;

        // Rotate/orient building so entrance and porch face parcel.streetFacing()
        com.x64Dynamics.tfb.geometry.Rotation rot = switch (parcel.streetFacing()) {
            case SOUTH -> com.x64Dynamics.tfb.geometry.Rotation.NONE;
            case WEST -> com.x64Dynamics.tfb.geometry.Rotation.CLOCKWISE_90;
            case NORTH -> com.x64Dynamics.tfb.geometry.Rotation.CLOCKWISE_180;
            case EAST -> com.x64Dynamics.tfb.geometry.Rotation.COUNTERCLOCKWISE_90;
            default -> com.x64Dynamics.tfb.geometry.Rotation.NONE;
        };

        BoundingBox3i genBounds = parcel.bounds();
        if (rot == com.x64Dynamics.tfb.geometry.Rotation.CLOCKWISE_90 || rot == com.x64Dynamics.tfb.geometry.Rotation.COUNTERCLOCKWISE_90) {
            // Swap X/Z dimensions before rotating 90 degrees so the rotated building fits the parcel bounds
            genBounds = BoundingBox3i.fromOriginAndSize(
                parcel.bounds().min(), parcel.bounds().sizeZ(), parcel.bounds().sizeY(), parcel.bounds().sizeX()
            );
        }

        BuildingDefinition def = new BuildingDefinition(
            parcel.id(),
            genBounds,
            wing,
            style,
            palette,
            roof,
            stories,
            parcel.purpose() != CityParcel.ParcelPurpose.WAREHOUSE,
            parcel.purpose() != CityParcel.ParcelPurpose.WAREHOUSE && style != StylePreset.MODERN,
            true,
            0.85f,
            parcel.seed()
        );
        BuildingPlan plan = HomeGenerator.generatePlan(def);
        if (rot != com.x64Dynamics.tfb.geometry.Rotation.NONE) {
            Vec3i pivot = new Vec3i(
                (parcel.bounds().min().x() + parcel.bounds().max().x()) / 2,
                parcel.bounds().min().y(),
                (parcel.bounds().min().z() + parcel.bounds().max().z()) / 2
            );
            List<PlacementTarget> rotated = com.x64Dynamics.tfb.construction.editing.ConstructionTransforms.rotate(plan.targets(), rot, pivot);
            plan = new BuildingPlan(def, plan.floors(), plan.stairwells(), rotated, parcel.bounds(), plan.validation());
        }
        parcel.setBuildingPlan(plan);
    }

    private static void subdivideDistrictParcels(
        CityDistrict district,
        Direction defaultFacing,
        SettlementPattern pattern,
        Random rng,
        Map<String, CityParcel> lockedParcels
    ) {
        BoundingBox3i db = district.bounds();
        if (db.sizeX() < 9 || db.sizeZ() < 9) return;

        int targetParcelW = (pattern == SettlementPattern.URBAN_DISTRICT || pattern == SettlementPattern.PLANNED_GRID) ? 12 : 11;
        int gap = 2; // alley/yard spacing between buildings
        int idx = 0;

        for (int x = db.min().x(); x + 8 <= db.max().x(); x += targetParcelW + gap) {
            int pw = Math.min(targetParcelW, db.max().x() - x + 1);
            if (pw < 9) break;
            int pd = Math.min(13, db.sizeZ());
            int pz = (defaultFacing == Direction.SOUTH) ? (db.max().z() - pd + 1) : db.min().z();
            BoundingBox3i pBox = BoundingBox3i.fromOriginAndSize(new Vec3i(x, db.min().y(), pz), pw, db.sizeY(), pd);

            String pid = district.id() + "_p" + idx++;
            if (lockedParcels.containsKey(pid)) {
                district.addParcel(lockedParcels.get(pid));
                continue;
            }

            CityParcel.ParcelPurpose purpose = pickPurpose(district.role(), rng);
            CityParcel parcel = new CityParcel(pid, district.id(), pBox, purpose, defaultFacing, rng.nextLong());
            district.addParcel(parcel);
        }
    }

    private static CityParcel.ParcelPurpose pickPurpose(CityDistrict.DistrictRole role, Random rng) {
        return switch (role) {
            case CIVIC_CENTER -> rng.nextBoolean() ? CityParcel.ParcelPurpose.LIBRARY_CIVIC : CityParcel.ParcelPurpose.TAVERN_INN;
            case INDUSTRIAL -> rng.nextBoolean() ? CityParcel.ParcelPurpose.WORKSHOP_SMITHY : CityParcel.ParcelPurpose.WAREHOUSE;
            case RESIDENTIAL -> switch (rng.nextInt(3)) {
                case 0 -> CityParcel.ParcelPurpose.COTTAGE;
                case 1 -> CityParcel.ParcelPurpose.TOWNHOUSE;
                default -> CityParcel.ParcelPurpose.MANOR;
            };
            default -> rng.nextBoolean() ? CityParcel.ParcelPurpose.COTTAGE : CityParcel.ParcelPurpose.TOWNHOUSE;
        };
    }

    private static StylePreset pickComplementaryStyle(StylePreset primary) {
        return switch (primary) {
            case MEDIEVAL -> StylePreset.RUSTIC;
            case MODERN -> StylePreset.MINIMALIST;
            case RUSTIC -> StylePreset.TRADITIONAL;
            case NORDIC -> StylePreset.MEDIEVAL;
            default -> primary;
        };
    }

    private static BoundingBox3i safeBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return BoundingBox3i.fromCorners(
            new Vec3i(minX, minY, minZ),
            new Vec3i(Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ))
        );
    }
}
