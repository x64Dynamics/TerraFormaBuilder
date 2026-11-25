package com.x64Dynamics.tfb.material;

import java.util.*;

public record BlockIdentifier(String namespace, String path, Map<String, String> properties) {
    public static final BlockIdentifier AIR = new BlockIdentifier("minecraft", "air", Collections.emptyMap());

    public boolean isAir() {
        return "air".equals(path) || "cave_air".equals(path) || "void_air".equals(path);
    }

    public BlockIdentifier {
        properties = properties == null || properties.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public static BlockIdentifier of(String fullId) {
        String namespace = "minecraft";
        String path = fullId;
        Map<String, String> props = Collections.emptyMap();

        int bracketIndex = fullId.indexOf('[');
        if (bracketIndex >= 0 && fullId.endsWith("]")) {
            String propString = fullId.substring(bracketIndex + 1, fullId.length() - 1);
            path = fullId.substring(0, bracketIndex);
            props = parseProperties(propString);
        }

        int colonIndex = path.indexOf(':');
        if (colonIndex >= 0) {
            namespace = path.substring(0, colonIndex);
            path = path.substring(colonIndex + 1);
        }

        return new BlockIdentifier(namespace, path, props);
    }

    private static Map<String, String> parseProperties(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : raw.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return map;
    }

    public BlockIdentifier withProperty(String key, String value) {
        Map<String, String> updated = new LinkedHashMap<>(properties);
        updated.put(key, value);
        return new BlockIdentifier(namespace, path, updated);
    }

    public String property(String key) {
        return properties.get(key);
    }

    public String namespacedId() {
        return namespace + ":" + path;
    }

    public String asString() {
        if (properties.isEmpty()) {
            return namespace + ":" + path;
        }
        StringBuilder builder = new StringBuilder(namespace).append(':').append(path).append('[');
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) builder.append(',');
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toString() {
        return asString();
    }
}
