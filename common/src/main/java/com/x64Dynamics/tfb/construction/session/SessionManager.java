package com.x64Dynamics.tfb.construction.session;

import com.x64Dynamics.tfb.geometry.BoundingBox3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<UUID, ArchitectSession> sessions = new ConcurrentHashMap<>();

    private SessionManager() { }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public ArchitectSession createSession(UUID ownerId, String brushId, BoundingBox3i bounds, SessionMode mode, String teamId) {
        UUID sessionId = UUID.randomUUID();
        ArchitectSession session = new ArchitectSession(sessionId, ownerId, brushId, bounds, mode, teamId);
        sessions.put(sessionId, session);
        return session;
    }

    public ArchitectSession getSession(UUID sessionId) {
        return sessions.get(sessionId);
    }

    public Optional<ArchitectSession> getSessionByPlayer(UUID playerId) {
        return sessions.values().stream()
            .filter(s -> s.ownerId().equals(playerId) || s.canEdit(playerId, null))
            .findFirst();
    }

    public void removeSession(UUID sessionId) {
        sessions.remove(sessionId);
    }

    public Collection<ArchitectSession> allSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public void cleanupStaleSessions(long maxIdleDurationMs) {
        long cutoff = System.currentTimeMillis() - maxIdleDurationMs;
        sessions.entrySet().removeIf(entry -> entry.getValue().lastUpdatedEpochMs() < cutoff);
    }
}
