package com.x64Dynamics.tfb.editing;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.BlockIdentifier;

import java.util.*;

// Tracks actual world block modifications for construction Undo / Redo.
public final class WorldConstructionHistory {
    public record BlockChange(Vec3i pos, BlockIdentifier oldBlock, BlockIdentifier newBlock) { }

    private static final WorldConstructionHistory INSTANCE = new WorldConstructionHistory();
    private static final int MAX_TRANSACTIONS = 16;

    private final Deque<List<BlockChange>> undoStack = new ArrayDeque<>();
    private final Deque<List<BlockChange>> redoStack = new ArrayDeque<>();

    private WorldConstructionHistory() { }

    public static WorldConstructionHistory getInstance() {
        return INSTANCE;
    }

    public synchronized void recordTransaction(List<BlockChange> changes) {
        if (changes == null || changes.isEmpty()) return;
        undoStack.push(Collections.unmodifiableList(new ArrayList<>(changes)));
        redoStack.clear();
        while (undoStack.size() > MAX_TRANSACTIONS) {
            undoStack.removeLast();
        }
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized List<BlockChange> popUndo() {
        if (undoStack.isEmpty()) return Collections.emptyList();
        List<BlockChange> tx = undoStack.pop();
        redoStack.push(tx);
        return tx;
    }

    public synchronized List<BlockChange> popRedo() {
        if (redoStack.isEmpty()) return Collections.emptyList();
        List<BlockChange> tx = redoStack.pop();
        undoStack.push(tx);
        return tx;
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
