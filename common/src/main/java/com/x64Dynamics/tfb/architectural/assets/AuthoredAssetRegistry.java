package com.x64Dynamics.tfb.architectural.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Persistent registry of authored architectural assets usable by the city planner
// and building composer.
public final class AuthoredAssetRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AuthoredAssetRegistry INSTANCE = new AuthoredAssetRegistry();
    private final Map<String, AuthoredAsset> assets = new LinkedHashMap<>();
    private Path storageDir = Path.of("config", "terraformabuilder", "assets");

    private AuthoredAssetRegistry() {}

    public static AuthoredAssetRegistry getInstance() { return INSTANCE; }

    public void setStorageDir(Path dir) {
        if (dir != null) this.storageDir = dir;
    }

    public void register(AuthoredAsset asset) {
        if (asset != null) assets.put(asset.id(), asset);
    }

    public Optional<AuthoredAsset> get(String id) {
        return Optional.ofNullable(assets.get(id));
    }

    public List<AuthoredAsset> byCategory(AuthoredAsset.AssetCategory category) {
        List<AuthoredAsset> out = new ArrayList<>();
        for (AuthoredAsset a : assets.values()) {
            if (a.category() == category) out.add(a);
        }
        return out;
    }

    public Collection<AuthoredAsset> all() {
        return Collections.unmodifiableCollection(assets.values());
    }

    public void save(AuthoredAsset asset) throws IOException {
        register(asset);
        Files.createDirectories(storageDir);
        String safe = asset.id().replaceAll("[^a-zA-Z0-9_-]", "_");
        AssetDto dto = new AssetDto(asset);
        Files.writeString(storageDir.resolve(safe + ".json"), GSON.toJson(dto));
    }

    public Optional<AuthoredAsset> load(String id) throws IOException {
        String safe = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path file = storageDir.resolve(safe + ".json");
        if (!Files.exists(file)) return Optional.empty();
        AssetDto dto = GSON.fromJson(Files.readString(file), AssetDto.class);
        if (dto == null) return Optional.empty();
        AuthoredAsset asset = dto.toAsset();
        register(asset);
        return Optional.of(asset);
    }

    private static final class AssetDto {
        String id, name, category, frontFacing;
        int sizeX, sizeY, sizeZ, groundOffsetY;
        boolean rotatable;
        List<BlockDto> blocks;

        AssetDto() {}
        AssetDto(AuthoredAsset a) {
            this.id = a.id(); this.name = a.name(); this.category = a.category().name();
            this.sizeX = a.sizeX(); this.sizeY = a.sizeY(); this.sizeZ = a.sizeZ();
            this.groundOffsetY = a.groundOffsetY();
            this.frontFacing = a.frontFacing().name();
            this.rotatable = a.rotatable();
            this.blocks = new ArrayList<>(a.blocks().size());
            for (var b : a.blocks()) this.blocks.add(new BlockDto(b.rx(), b.ry(), b.rz(), b.blockState(), b.roleName()));
        }
        AuthoredAsset toAsset() {
            List<AuthoredAsset.RelativeBlock> list = new ArrayList<>();
            if (blocks != null) {
                for (var b : blocks) list.add(new AuthoredAsset.RelativeBlock(b.rx, b.ry, b.rz, b.blockState, b.roleName));
            }
            return new AuthoredAsset(
                id, name, AuthoredAsset.AssetCategory.valueOf(category),
                sizeX, sizeY, sizeZ, groundOffsetY,
                com.x64Dynamics.tfb.geometry.Direction.valueOf(frontFacing),
                rotatable, list
            );
        }
    }

    private static final class BlockDto {
        int rx, ry, rz;
        String blockState, roleName;
        BlockDto() {}
        BlockDto(int rx, int ry, int rz, String blockState, String roleName) {
            this.rx = rx; this.ry = ry; this.rz = rz; this.blockState = blockState; this.roleName = roleName;
        }
    }
}
