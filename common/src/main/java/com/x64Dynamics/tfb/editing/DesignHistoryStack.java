package com.x64Dynamics.tfb.editing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class DesignHistoryStack {
    private final int maxDepth;
    private final Deque<DesignHistorySnapshot> undoStack = new ArrayDeque<>();
    private final Deque<DesignHistorySnapshot> redoStack = new ArrayDeque<>();

    public DesignHistoryStack(int maxDepth) {
        this.maxDepth = Math.max(1, maxDepth);
    }

    public synchronized void push(DesignHistorySnapshot state) {
        if (undoStack.size() >= maxDepth) {
            undoStack.removeLast();
        }
        undoStack.push(state);
        redoStack.clear();
    }

    public synchronized boolean canUndo() {
        return undoStack.size() > 1;
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized Optional<DesignHistorySnapshot> undo() {
        if (!canUndo()) return Optional.empty();
        DesignHistorySnapshot current = undoStack.pop();
        redoStack.push(current);
        return Optional.of(undoStack.peek());
    }

    public synchronized Optional<DesignHistorySnapshot> redo() {
        if (!canRedo()) return Optional.empty();
        DesignHistorySnapshot next = redoStack.pop();
        undoStack.push(next);
        return Optional.of(next);
    }
}
