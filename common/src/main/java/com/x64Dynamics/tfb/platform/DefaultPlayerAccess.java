package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPlayerAccess implements IPlayerAccess {
    private final UUID uuid;
    private final String teamId;
    private boolean creative;
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    public DefaultPlayerAccess(UUID uuid, String teamId, boolean creative) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.teamId = teamId != null ? teamId : "default";
        this.creative = creative;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    @Override
    public boolean isCreative() {
        return creative;
    }

    public void setCreative(boolean creative) {
        this.creative = creative;
    }

    public void addResource(BlockIdentifier block, int count) {
        inventory.merge(block.namespacedId(), count, Integer::sum);
    }

    @Override
    public boolean hasResource(BlockIdentifier block, int count) {
        if (creative) return true;
        int current = inventory.getOrDefault(block.namespacedId(), 0);
        return current >= count;
    }

    @Override
    public boolean consumeResource(BlockIdentifier block, int count) {
        if (creative) return true;
        int current = inventory.getOrDefault(block.namespacedId(), 0);
        if (current < count) return false;
        if (current == count) {
            inventory.remove(block.namespacedId());
        } else {
            inventory.put(block.namespacedId(), current - count);
        }
        return true;
    }
}
