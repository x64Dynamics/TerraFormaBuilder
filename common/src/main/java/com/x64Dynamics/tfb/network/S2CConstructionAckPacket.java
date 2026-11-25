package com.x64Dynamics.tfb.network;

import java.util.UUID;

public record S2CConstructionAckPacket(
    UUID sessionId,
    boolean accepted,
    int placedCount,
    int remainingCount,
    String message
) {
    public static final String CHANNEL_ID = "tfb:s2c_construct_ack";
}
