package com.x64Dynamics.tfb.platform;

import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.UUID;

public interface IPlayerAccess {
    UUID getUuid();
    String getTeamId();
    boolean isCreative();
    boolean hasResource(BlockIdentifier block, int count);
    boolean consumeResource(BlockIdentifier block, int count);
}
