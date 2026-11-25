package com.x64Dynamics.tfb.architectural.grid;

import com.x64Dynamics.tfb.architectural.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.x64Dynamics.tfb.TerraFormaBuilder;
// Saves and restores complete GridManager + BuildingRegistry projects to disk.
public final class GridProjectManager {
    private static Path storageDir = Path.of("config", "terraformabuilder", "grid_projects");

    private GridProjectManager() { }

    public static void setStorageDir(Path dir) { storageDir = dir; }

    public static void save(String projectName, GridManager grid, BuildingRegistry registry) throws IOException {
        Files.createDirectories(storageDir);
        String safe = projectName.replaceAll("[^a-zA-Z0-9_-]", "_");
        Properties props = new Properties();
        props.setProperty("name", projectName);
        props.setProperty("origin", grid.origin().x() + "," + grid.origin().y() + "," + grid.origin().z());
        props.setProperty("cellSize", String.valueOf(grid.cellSize()));
        props.setProperty("gridWidth", String.valueOf(grid.gridWidth()));
        props.setProperty("gridDepth", String.valueOf(grid.gridDepth()));

        List<GridBuilding> list = new ArrayList<>(registry.allBuildings());
        props.setProperty("buildingCount", String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            GridBuilding b = list.get(i);
            String p = "b." + i + ".";
            props.setProperty(p + "id", b.id());
            props.setProperty(p + "pos", b.gridX() + "," + b.gridZ() + "," + b.baseY());
            props.setProperty(p + "size", b.width() + "," + b.depth() + "," + b.stories() + "," + b.storyHeight());
            props.setProperty(p + "footprint", b.footprint().name());
            props.setProperty(p + "facing", b.facing().name());
            props.setProperty(p + "style", b.style().name());
            props.setProperty(p + "roofType", b.roofType().name());
            props.setProperty(p + "function", b.function().name());
            props.setProperty(p + "allowExpansion", String.valueOf(b.allowExpansion()));
            props.setProperty(p + "seed", String.valueOf(b.seed()));
        }
        StringWriter sw = new StringWriter();
        props.store(sw, "TerraFormaBuilder Grid Project");
        Files.writeString(storageDir.resolve(safe + ".tfbgrid"), sw.toString(), StandardCharsets.UTF_8);
    }

    public static Optional<StructurePlacementController> load(String projectName) {
        String safe = projectName.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path file = storageDir.resolve(safe + ".tfbgrid");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            Properties props = new Properties();
            props.load(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
            String[] org = props.getProperty("origin", "0,64,0").split(",");
            Vec3i origin = new Vec3i(Integer.parseInt(org[0]), Integer.parseInt(org[1]), Integer.parseInt(org[2]));
            GridManager grid = new GridManager(
                origin,
                Integer.parseInt(props.getProperty("cellSize", "1")),
                Integer.parseInt(props.getProperty("gridWidth", "64")),
                Integer.parseInt(props.getProperty("gridDepth", "64")),
                -64, 319
            );
            BuildingRegistry registry = new BuildingRegistry();
            PlacementValidator rules = new PlacementValidator(PlacementValidator.PlacementRules.defaults());
            GridConnectionPlanner connEngine = new GridConnectionPlanner();
            StructurePlacementController ctrl = new StructurePlacementController(grid, registry, rules, connEngine);

            int count = Integer.parseInt(props.getProperty("buildingCount", "0"));
            for (int i = 0; i < count; i++) {
                String p = "b." + i + ".";
                String[] pos = props.getProperty(p + "pos").split(",");
                String[] sz = props.getProperty(p + "size").split(",");
                StylePreset style = StylePreset.valueOf(props.getProperty(p + "style"));
                GridBuilding b = new GridBuilding(
                    props.getProperty(p + "id"),
                    Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]),
                    Integer.parseInt(sz[0]), Integer.parseInt(sz[1]), Integer.parseInt(sz[2]), Integer.parseInt(sz[3]),
                    GridBuilding.FootprintShape.valueOf(props.getProperty(p + "footprint")),
                    Direction.valueOf(props.getProperty(p + "facing")),
                    style,
                    style.createPalette(),
                    RoofGenerator.RoofType.valueOf(props.getProperty(p + "roofType")),
                    GridBuilding.BuildingFunction.valueOf(props.getProperty(p + "function")),
                    Boolean.parseBoolean(props.getProperty(p + "allowExpansion", "false")),
                    Long.parseLong(props.getProperty(p + "seed", "1"))
                );
                ctrl.placeBuilding(b);
            }
            return Optional.of(ctrl);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
