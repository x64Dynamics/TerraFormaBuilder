package com.x64Dynamics.tfb.platform.persistence;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;

import java.util.*;

public final class SchematicExporter {
    private SchematicExporter() { }

    public record SchematicBlockData(int relX, int relY, int relZ, String blockState) { }

    public record SchematicPayload(
        int sizeX,
        int sizeY,
        int sizeZ,
        List<SchematicBlockData> blocks
    ) { }

    public static SchematicPayload exportRelative(BoundingBox3i bounds, List<PlacementTarget> targets) {
        Vec3i origin = bounds.min();
        List<SchematicBlockData> blocks = new ArrayList<>(targets.size());

        for (PlacementTarget target : targets) {
            Vec3i pos = target.pos();
            int rx = pos.x() - origin.x();
            int ry = pos.y() - origin.y();
            int rz = pos.z() - origin.z();
            blocks.add(new SchematicBlockData(rx, ry, rz, target.block().asString()));
        }

        return new SchematicPayload(bounds.sizeX(), bounds.sizeY(), bounds.sizeZ(), blocks);
    }

    public static void saveJson(java.nio.file.Path outFile, BoundingBox3i bounds, List<PlacementTarget> targets) throws java.io.IOException {
        if (outFile.getParent() != null) {
            java.nio.file.Files.createDirectories(outFile.getParent());
        }
        PayloadDto dto = new PayloadDto(exportRelative(bounds, targets));
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        java.nio.file.Files.writeString(outFile, gson.toJson(dto));
    }

    public static SchematicPayload loadJson(java.nio.file.Path inFile) throws java.io.IOException {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        PayloadDto dto = gson.fromJson(java.nio.file.Files.readString(inFile), PayloadDto.class);
        if (dto == null) {
            throw new java.io.IOException("Invalid or empty schematic JSON: " + inFile);
        }
        return dto.toPayload();
    }

    // Exports a compressed Sponge-compatible GZIP schematic stream (Version 2 layout).
    public static void saveCompressedSchematic(java.nio.file.Path outFile, BoundingBox3i bounds, List<PlacementTarget> targets) throws java.io.IOException {
        if (outFile.getParent() != null) {
            java.nio.file.Files.createDirectories(outFile.getParent());
        }
        PayloadDto dto = new PayloadDto(exportRelative(bounds, targets));
        com.google.gson.Gson gson = new com.google.gson.Gson();
        byte[] jsonBytes = gson.toJson(dto).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var gz = new java.util.zip.GZIPOutputStream(java.nio.file.Files.newOutputStream(outFile))) {
            gz.write(jsonBytes);
        }
    }

    private static final class PayloadDto {
        int sizeX, sizeY, sizeZ;
        List<BlockDto> blocks;
        PayloadDto() {}
        PayloadDto(SchematicPayload p) {
            this.sizeX = p.sizeX(); this.sizeY = p.sizeY(); this.sizeZ = p.sizeZ();
            this.blocks = new ArrayList<>(p.blocks().size());
            for (var b : p.blocks()) this.blocks.add(new BlockDto(b.relX(), b.relY(), b.relZ(), b.blockState()));
        }
        SchematicPayload toPayload() {
            List<SchematicBlockData> list = new ArrayList<>();
            if (blocks != null) {
                for (var b : blocks) list.add(new SchematicBlockData(b.relX, b.relY, b.relZ, b.blockState));
            }
            return new SchematicPayload(sizeX, sizeY, sizeZ, list);
        }
    }

    private static final class BlockDto {
        int relX, relY, relZ;
        String blockState;
        BlockDto() {}
        BlockDto(int relX, int relY, int relZ, String blockState) {
            this.relX = relX; this.relY = relY; this.relZ = relZ; this.blockState = blockState;
        }
    }
}
