package com.x64Dynamics.tfb.gameplay;

import com.x64Dynamics.tfb.config.ModConfig;

public final class RewardPolicy {
    private RewardPolicy() { }

    public static int rewardFor(ModConfig config, boolean eligible) {
        return config.enabled && eligible ? config.rewardAmount : 0;
    }
}
