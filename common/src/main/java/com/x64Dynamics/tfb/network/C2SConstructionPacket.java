package com.x64Dynamics.tfb.network;

import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;
import com.x64Dynamics.tfb.material.MaterialRole;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Pure Java client-to-server construction packet with binary codec for cross-loader networking.
public record C2SConstructionPacket(
    UUID sessionId,
    UUID playerId,
    Vec3i origin,
    List<PlacementTarget> targets,
    boolean validateResources
) {
    public static final String CHANNEL_ID = "tfb:c2s_construct";

    public byte[] encode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(sessionId.toString());
            out.writeUTF(playerId.toString());
            out.writeInt(origin.x());
            out.writeInt(origin.y());
            out.writeInt(origin.z());
            out.writeBoolean(validateResources);
            out.writeInt(targets.size());
            for (PlacementTarget t : targets) {
                out.writeInt(t.pos().x());
                out.writeInt(t.pos().y());
                out.writeInt(t.pos().z());
                out.writeUTF(t.block().asString());
                out.writeUTF(t.role().name());
            }
        }
        return baos.toByteArray();
    }

    public static C2SConstructionPacket decode(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            UUID sessionId = UUID.fromString(in.readUTF());
            UUID playerId = UUID.fromString(in.readUTF());
            Vec3i origin = new Vec3i(in.readInt(), in.readInt(), in.readInt());
            boolean validate = in.readBoolean();
            int count = in.readInt();
            if (count < 0 || count > 250_000) {
                throw new IOException("Invalid target count in C2SConstructionPacket: " + count);
            }
            List<PlacementTarget> targets = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Vec3i pos = new Vec3i(in.readInt(), in.readInt(), in.readInt());
                BlockIdentifier block = BlockIdentifier.of(in.readUTF());
                MaterialRole role = MaterialRole.valueOf(in.readUTF());
                targets.add(new PlacementTarget(pos, block, role));
            }
            return new C2SConstructionPacket(sessionId, playerId, origin, targets, validate);
        }
    }
}
