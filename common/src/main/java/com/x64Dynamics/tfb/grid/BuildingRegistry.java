package com.x64Dynamics.tfb.grid;

import java.util.*;

/**
 * Tracks all GridBuilding instances and explicit BuildingConnection relationships
 * as a connected architectural graph within the current project.
 */
public final class BuildingRegistry {
    private final Map<String, GridBuilding> buildings = new LinkedHashMap<>();
    private final List<ComplexConnectionPlanner.BuildingConnection> connections = new ArrayList<>();

    public void register(GridBuilding building) {
        buildings.put(building.id(), building);
    }

    public boolean remove(String buildingId) {
        connections.removeIf(c -> c.fromBuildingId().equals(buildingId) || c.toBuildingId().equals(buildingId));
        return buildings.remove(buildingId) != null;
    }

    public Optional<GridBuilding> get(String buildingId) {
        return Optional.ofNullable(buildings.get(buildingId));
    }

    public Collection<GridBuilding> allBuildings() {
        return Collections.unmodifiableCollection(buildings.values());
    }

    public List<ComplexConnectionPlanner.BuildingConnection> connections() {
        return Collections.unmodifiableList(connections);
    }

    public void setConnections(List<ComplexConnectionPlanner.BuildingConnection> updated) {
        connections.clear();
        connections.addAll(updated);
    }

    public List<ComplexConnectionPlanner.BuildingConnection> connectionsFor(String buildingId) {
        List<ComplexConnectionPlanner.BuildingConnection> list = new ArrayList<>();
        for (var c : connections) {
            if (c.fromBuildingId().equals(buildingId) || c.toBuildingId().equals(buildingId)) {
                list.add(c);
            }
        }
        return list;
    }

    public void clear() {
        buildings.clear();
        connections.clear();
    }
}
