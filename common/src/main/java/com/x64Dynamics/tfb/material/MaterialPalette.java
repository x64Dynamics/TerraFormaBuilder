package com.x64Dynamics.tfb.material;

import java.util.*;

public final class MaterialPalette {
    private final String id;
    private final Map<MaterialRole, BlockIdentifier> mappings = new EnumMap<>(MaterialRole.class);

    public MaterialPalette(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public MaterialPalette set(MaterialRole role, BlockIdentifier block) {
        mappings.put(role, block);
        return this;
    }

    public MaterialPalette set(MaterialRole role, String blockId) {
        return set(role, BlockIdentifier.of(blockId));
    }

    public BlockIdentifier get(MaterialRole role) {
        MaterialRole cur = role;
        for (int depth = 0; depth < 6 && cur != null; depth++) {
            BlockIdentifier found = mappings.get(cur);
            if (found != null) return found;
            MaterialRole next = cur.fallback();
            if (next == cur) break;
            cur = next;
        }

        BlockIdentifier primary = mappings.get(MaterialRole.WALL_PRIMARY);
        return primary != null ? primary : BlockIdentifier.AIR;
    }

    public Map<MaterialRole, BlockIdentifier> allMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public MaterialPalette copy(String newId) {
        MaterialPalette copy = new MaterialPalette(newId);
        copy.mappings.putAll(this.mappings);
        return copy;
    }
}
