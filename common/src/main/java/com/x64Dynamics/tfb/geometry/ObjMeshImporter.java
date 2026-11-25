package com.x64Dynamics.tfb.geometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Pure-Java Wavefront OBJ (+ MTL material colors) and ASCII STL mesh importer.
 */
public final class ObjMeshImporter {
    public record MaterialColor(int r, int g, int b, boolean translucent, boolean emissive) {
        public static final MaterialColor DEFAULT = new MaterialColor(160, 160, 160, false, false);
    }

    public record Triangle(Vec3d a, Vec3d b, Vec3d c, MaterialColor material) {}

    public record MeshData(List<Triangle> triangles, Vec3d minBounds, Vec3d maxBounds) {}

    private ObjMeshImporter() {}

    public static MeshData loadObj(Path objPath) throws IOException {
        List<String> lines = Files.readAllLines(objPath);
        Map<String, MaterialColor> materials = new HashMap<>();
        Path parent = objPath.getParent();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("mtllib ") && parent != null) {
                Path mtlPath = parent.resolve(t.substring(7).trim());
                if (Files.exists(mtlPath)) {
                    materials.putAll(parseMtl(mtlPath));
                }
            }
        }
        return parseObjLines(lines, materials);
    }

    public static MeshData parseObjString(String objText) {
        return parseObjLines(Arrays.asList(objText.split("\\R")), Collections.emptyMap());
    }

    private static MeshData parseObjLines(List<String> lines, Map<String, MaterialColor> materials) {
        List<Vec3d> vertices = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();
        MaterialColor currentMat = MaterialColor.DEFAULT;

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "v" -> {
                    if (parts.length >= 4) {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        vertices.add(new Vec3d(x, y, z));
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (z < minZ) minZ = z;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        if (z > maxZ) maxZ = z;
                    }
                }
                case "usemtl" -> {
                    if (parts.length >= 2) {
                        currentMat = materials.getOrDefault(parts[1], MaterialColor.DEFAULT);
                    }
                }
                case "f" -> {
                    if (parts.length >= 4) {
                        int[] idx = new int[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            String token = parts[i].split("/")[0];
                            int vi = Integer.parseInt(token);
                            idx[i - 1] = vi > 0 ? vi - 1 : vertices.size() + vi;
                        }
                        Vec3d v0 = vertices.get(idx[0]);
                        for (int i = 1; i + 1 < idx.length; i++) {
                            triangles.add(new Triangle(v0, vertices.get(idx[i]), vertices.get(idx[i + 1]), currentMat));
                        }
                    }
                }
                default -> {}
            }
        }

        if (vertices.isEmpty()) {
            return new MeshData(triangles, new Vec3d(0, 0, 0), new Vec3d(0, 0, 0));
        }
        return new MeshData(triangles, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }

    private static Map<String, MaterialColor> parseMtl(Path mtlPath) throws IOException {
        Map<String, MaterialColor> map = new HashMap<>();
        String current = null;
        int r = 180, g = 180, b = 180;
        boolean translucent = false;
        boolean emissive = false;

        for (String raw : Files.readAllLines(mtlPath)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "newmtl" -> {
                    if (current != null) {
                        map.put(current, new MaterialColor(r, g, b, translucent, emissive));
                    }
                    current = parts.length > 1 ? parts[1] : "default";
                    r = 180; g = 180; b = 180; translucent = false; emissive = false;
                }
                case "Kd" -> {
                    if (parts.length >= 4) {
                        r = (int) Math.round(Math.max(0, Math.min(1, Double.parseDouble(parts[1]))) * 255);
                        g = (int) Math.round(Math.max(0, Math.min(1, Double.parseDouble(parts[2]))) * 255);
                        b = (int) Math.round(Math.max(0, Math.min(1, Double.parseDouble(parts[3]))) * 255);
                    }
                }
                case "d" -> {
                    if (parts.length >= 2 && Double.parseDouble(parts[1]) < 0.95) {
                        translucent = true;
                    }
                }
                case "Ke" -> {
                    if (parts.length >= 4) {
                        double e = Double.parseDouble(parts[1]) + Double.parseDouble(parts[2]) + Double.parseDouble(parts[3]);
                        if (e > 0.2) emissive = true;
                    }
                }
                default -> {}
            }
        }
        if (current != null) {
            map.put(current, new MaterialColor(r, g, b, translucent, emissive));
        }
        return map;
    }
}
