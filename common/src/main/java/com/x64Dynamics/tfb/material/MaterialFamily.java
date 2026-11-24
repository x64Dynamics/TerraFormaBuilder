package com.x64Dynamics.tfb.material;

public record MaterialFamily(
    BlockIdentifier base,
    BlockIdentifier stair,
    BlockIdentifier slab,
    BlockIdentifier wall,
    BlockIdentifier fence,
    BlockIdentifier gate,
    BlockIdentifier door,
    BlockIdentifier trapdoor
) {
    public static MaterialFamily simple(BlockIdentifier base) {
        return new MaterialFamily(base, base, base, base, base, base, base, base);
    }
}
