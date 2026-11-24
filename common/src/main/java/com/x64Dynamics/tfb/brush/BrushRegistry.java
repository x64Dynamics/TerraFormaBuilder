package com.x64Dynamics.tfb.brush;

import java.util.*;

public final class BrushRegistry {
    private static final Map<String, IConstructionBrush> BRUSHES = new LinkedHashMap<>();

    static {
        // Architectural
        register(new ArchitecturalBrushes.RoomBrush());
        register(new ArchitecturalBrushes.HouseBrush());
        register(new ArchitecturalBrushes.BuildingBrush());
        register(new ArchitecturalBrushes.HallwayBrush());
        register(new ArchitecturalBrushes.TowerBrush());
        register(new ArchitecturalBrushes.CastleBrush());
        register(new ArchitecturalBrushes.StableBrush());
        register(new ArchitecturalBrushes.BarnBrush());
        register(new ArchitecturalBrushes.CourtyardBrush());

        // Structural
        register(new StructuralBrushes.WallBrush());
        register(new StructuralBrushes.FloorBrush());
        register(new StructuralBrushes.RoofBrush());
        register(new StructuralBrushes.CeilingBrush());
        register(new StructuralBrushes.PillarBrush());
        register(new StructuralBrushes.StairBrush());
        register(new StructuralBrushes.ArchBrush());
        register(new StructuralBrushes.BridgeBrush());
        register(new StructuralBrushes.FoundationBrush());
        register(new StructuralBrushes.FenceBrush());

        // Infrastructure
        register(new InfrastructureBrushes.RoadBrush());
        register(new InfrastructureBrushes.PathBrush());
        register(new InfrastructureBrushes.StreetBrush());
        register(new InfrastructureBrushes.SidewalkBrush());
        register(new InfrastructureBrushes.TunnelBrush());
        register(new InfrastructureBrushes.RailwayBrush());
        register(new InfrastructureBrushes.DrainageBrush());
    }

    private BrushRegistry() { }

    public static void register(IConstructionBrush brush) {
        BRUSHES.put(brush.id().toLowerCase(Locale.ROOT), brush);
    }

    public static IConstructionBrush get(String id) {
        return BRUSHES.get(id.toLowerCase(Locale.ROOT));
    }

    public static Collection<IConstructionBrush> all() {
        return Collections.unmodifiableCollection(BRUSHES.values());
    }

    public static List<IConstructionBrush> byCategory(BrushCategory category) {
        List<IConstructionBrush> list = new ArrayList<>();
        for (IConstructionBrush brush : BRUSHES.values()) {
            if (brush.category() == category) {
                list.add(brush);
            }
        }
        return list;
    }
}
