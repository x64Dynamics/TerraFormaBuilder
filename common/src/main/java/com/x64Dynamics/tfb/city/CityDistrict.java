package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A thematic neighborhood or zone within a WorldProject.
public final class CityDistrict {
    public enum DistrictRole {
        CIVIC_CENTER,
        MARKET_PLAZA,
        RESIDENTIAL,
        INDUSTRIAL,
        GARDEN_PARK,
        HARBOR_WATERFRONT
    }

    private final String id;
    private String name;
    private DistrictRole role;
    private BoundingBox3i bounds;
    private StylePreset style;
    private final List<CityParcel> parcels = new ArrayList<>();

    public CityDistrict(String id, String name, DistrictRole role, BoundingBox3i bounds, StylePreset style) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.bounds = bounds;
        this.style = style != null ? style : StylePreset.MEDIEVAL;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public DistrictRole role() { return role; }
    public void setRole(DistrictRole role) { this.role = role; }
    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; }
    public StylePreset style() { return style; }
    public void setStyle(StylePreset style) { this.style = style; }
    public List<CityParcel> parcels() { return Collections.unmodifiableList(parcels); }
    public void addParcel(CityParcel parcel) { parcels.add(parcel); }
    public void clearParcels() { parcels.clear(); }
}
