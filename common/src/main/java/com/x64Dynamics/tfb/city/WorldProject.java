package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.*;

/**
 * Persistent world-scale architectural design project containing districts,
 * parcels, building plans, street networks, and public spaces.
 */
public final class WorldProject {
    private final String id;
    private String name;
    private String author;
    private BoundingBox3i bounds;
    private SettlementPattern pattern;
    private StylePreset primaryStyle;
    private long seed;
    private long lastModifiedEpochMs;

    private final List<CityDistrict> districts = new ArrayList<>();
    private final RoadNetwork roadNetwork = new RoadNetwork();
    private final List<PublicSpace> publicSpaces = new ArrayList<>();

    public WorldProject(
        String id,
        String name,
        String author,
        BoundingBox3i bounds,
        SettlementPattern pattern,
        StylePreset primaryStyle,
        long seed
    ) {
        this.id = id != null ? id : "city-" + Long.toHexString(seed);
        this.name = name != null && !name.isBlank() ? name : "New Settlement";
        this.author = author != null ? author : "KyroXova";
        this.bounds = bounds;
        this.pattern = pattern != null ? pattern : SettlementPattern.MEDIEVAL_TOWN;
        this.primaryStyle = primaryStyle != null ? primaryStyle : StylePreset.MEDIEVAL;
        this.seed = seed;
        this.lastModifiedEpochMs = System.currentTimeMillis();
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; touch(); }
    public String author() { return author; }
    public void setAuthor(String author) { this.author = author; touch(); }
    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; touch(); }
    public SettlementPattern pattern() { return pattern; }
    public void setPattern(SettlementPattern pattern) { this.pattern = pattern; touch(); }
    public StylePreset primaryStyle() { return primaryStyle; }
    public void setPrimaryStyle(StylePreset style) { this.primaryStyle = style; touch(); }
    public long seed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; touch(); }
    public long lastModifiedEpochMs() { return lastModifiedEpochMs; }
    public void touch() { this.lastModifiedEpochMs = System.currentTimeMillis(); }

    public List<CityDistrict> districts() { return Collections.unmodifiableList(districts); }
    public void addDistrict(CityDistrict district) { districts.add(district); touch(); }
    public void clearDistricts() { districts.clear(); touch(); }

    public RoadNetwork roadNetwork() { return roadNetwork; }

    public List<PublicSpace> publicSpaces() { return Collections.unmodifiableList(publicSpaces); }
    public void addPublicSpace(PublicSpace space) { publicSpaces.add(space); touch(); }
    public void clearPublicSpaces() { publicSpaces.clear(); touch(); }

    public int totalParcelCount() {
        int count = 0;
        for (CityDistrict d : districts) count += d.parcels().size();
        return count;
    }

    public Optional<CityParcel> findParcel(String parcelId) {
        for (CityDistrict d : districts) {
            for (CityParcel p : d.parcels()) {
                if (p.id().equals(parcelId)) return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<CityDistrict> findDistrict(String districtId) {
        for (CityDistrict d : districts) {
            if (d.id().equals(districtId)) return Optional.of(d);
        }
        return Optional.empty();
    }

    /**
     * Resolves deduplicated placements across the entire city (roads -> public spaces -> parcel buildings).
     */
    public List<PlacementTarget> resolveAllTargets() {
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        for (PlacementTarget rt : roadNetwork.resolvedTargets()) {
            map.put(rt.pos(), rt);
        }
        for (PublicSpace ps : publicSpaces) {
            for (PlacementTarget pt : ps.targets()) {
                map.put(pt.pos(), pt);
            }
        }
        for (CityDistrict d : districts) {
            for (CityParcel p : d.parcels()) {
                if (p.buildingPlan() != null) {
                    for (PlacementTarget bt : p.buildingPlan().targets()) {
                        map.put(bt.pos(), bt);
                    }
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<PlacementTarget> resolveDistrictTargets(String districtId) {
        Optional<CityDistrict> opt = findDistrict(districtId);
        if (opt.isEmpty()) return Collections.emptyList();
        Map<Vec3i, PlacementTarget> map = new LinkedHashMap<>();
        for (CityParcel p : opt.get().parcels()) {
            if (p.buildingPlan() != null) {
                for (PlacementTarget bt : p.buildingPlan().targets()) {
                    map.put(bt.pos(), bt);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<PlacementTarget> resolveParcelTargets(String parcelId) {
        return findParcel(parcelId)
            .filter(p -> p.buildingPlan() != null)
            .map(p -> p.buildingPlan().targets())
            .orElse(Collections.emptyList());
    }
}
