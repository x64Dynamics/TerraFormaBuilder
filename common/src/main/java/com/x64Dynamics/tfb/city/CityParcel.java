package com.x64Dynamics.tfb.city;

import com.x64Dynamics.tfb.architectural.plan.BuildingPlan;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;

// A buildable lot within a CityDistrict, oriented toward an adjacent street.
public final class CityParcel {
    public enum ParcelPurpose {
        COTTAGE,
        TOWNHOUSE,
        MANOR,
        TAVERN_INN,
        WORKSHOP_SMITHY,
        LIBRARY_CIVIC,
        WAREHOUSE,
        MARKET_STALL,
        PARK_LOT
    }

    private final String id;
    private final String districtId;
    private BoundingBox3i bounds;
    private ParcelPurpose purpose;
    private Direction streetFacing;
    private boolean locked;
    private long seed;
    private BuildingPlan buildingPlan;

    public CityParcel(
        String id,
        String districtId,
        BoundingBox3i bounds,
        ParcelPurpose purpose,
        Direction streetFacing,
        long seed
    ) {
        this.id = id;
        this.districtId = districtId;
        this.bounds = bounds;
        this.purpose = purpose;
        this.streetFacing = streetFacing != null ? streetFacing : Direction.SOUTH;
        this.seed = seed;
    }

    public String id() { return id; }
    public String districtId() { return districtId; }
    public BoundingBox3i bounds() { return bounds; }
    public void setBounds(BoundingBox3i bounds) { this.bounds = bounds; }
    public ParcelPurpose purpose() { return purpose; }
    public void setPurpose(ParcelPurpose purpose) { this.purpose = purpose; }
    public Direction streetFacing() { return streetFacing; }
    public void setStreetFacing(Direction streetFacing) { this.streetFacing = streetFacing; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public long seed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public BuildingPlan buildingPlan() { return buildingPlan; }
    public void setBuildingPlan(BuildingPlan buildingPlan) { this.buildingPlan = buildingPlan; }
}
