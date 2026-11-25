package com.x64Dynamics.tfb.architectural.builds.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.x64Dynamics.tfb.architectural.exterior.roof.RoofGenerator;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Direction;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.StylePreset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Persists editable {@link ArchitecturalProject} hierarchies (sections, bounds,
// lock states, styles, roof forms) to JSON so users can reopen and continue editing.
public final class ArchitecturalProjectStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path storageDir = Path.of("config", "terraformabuilder", "architectural_projects");

    private ArchitecturalProjectStorage() {}

    public static void setStorageDir(Path dir) {
        if (dir != null) storageDir = dir;
    }

    public static void save(ArchitecturalProject project) throws IOException {
        Files.createDirectories(storageDir);
        ProjectDto dto = new ProjectDto();
        dto.id = project.id();
        dto.name = project.name();
        dto.sections = new ArrayList<>();
        for (EditableBuildingSection s : project.sections()) {
            SectionDto sd = new SectionDto();
            sd.id = s.id();
            sd.kind = s.kind().name();
            sd.minX = s.bounds().min().x();
            sd.minY = s.bounds().min().y();
            sd.minZ = s.bounds().min().z();
            sd.maxX = s.bounds().max().x();
            sd.maxY = s.bounds().max().y();
            sd.maxZ = s.bounds().max().z();
            sd.stories = s.stories();
            sd.style = s.style().name();
            sd.roofType = s.roofType().name();
            sd.facing = s.facing().name();
            sd.locked = s.isLocked();
            sd.seed = s.seed();
            sd.parentSectionId = s.parentSectionId();
            dto.sections.add(sd);
        }
        String safe = project.name().replaceAll("[^a-zA-Z0-9_-]", "_");
        Files.writeString(storageDir.resolve(safe + ".json"), GSON.toJson(dto));
    }

    public static Optional<ArchitecturalProject> load(String name) throws IOException {
        String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path file = storageDir.resolve(safe + ".json");
        if (!Files.exists(file)) return Optional.empty();
        ProjectDto dto = GSON.fromJson(Files.readString(file), ProjectDto.class);
        ArchitecturalProject proj = new ArchitecturalProject(dto.id, dto.name);
        if (dto.sections != null) {
            for (SectionDto sd : dto.sections) {
                BoundingBox3i box = BoundingBox3i.fromCorners(
                    new Vec3i(sd.minX, sd.minY, sd.minZ),
                    new Vec3i(sd.maxX, sd.maxY, sd.maxZ)
                );
                EditableBuildingSection sec = proj.addSection(
                    EditableBuildingSection.SectionKind.valueOf(sd.kind),
                    box,
                    sd.stories,
                    StylePreset.valueOf(sd.style),
                    RoofGenerator.RoofType.valueOf(sd.roofType),
                    Direction.valueOf(sd.facing),
                    sd.seed
                );
                sec.setParentSectionId(sd.parentSectionId);
                sec.setLocked(sd.locked);
            }
        }
        return Optional.of(proj);
    }

    public static List<String> listSavedNames() {
        List<String> out = new ArrayList<>();
        if (!Files.exists(storageDir)) return out;
        try (var stream = Files.list(storageDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> out.add(p.getFileName().toString().replace(".json", "")));
        } catch (IOException ignored) {}
        return out;
    }

    private static final class ProjectDto {
        String id;
        String name;
        List<SectionDto> sections;
    }

    private static final class SectionDto {
        String id;
        String kind;
        int minX, minY, minZ, maxX, maxY, maxZ;
        int stories;
        String style;
        String roofType;
        String facing;
        boolean locked;
        long seed;
        String parentSectionId;
    }
}
