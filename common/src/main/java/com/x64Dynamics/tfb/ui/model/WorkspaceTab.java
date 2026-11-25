package com.x64Dynamics.tfb.ui.model;

public enum WorkspaceTab {
    ARCHITECTURE("Architecture", "Brushes, building types, dimensions, roofs, and facades"),
    SELECTION("Selection", "POS1, POS2, region bounds, volume, expand/contract, and presets"),
    MATERIALS("Materials", "Universal taxonomy, palettes, styles, and block substitution"),
    TERRAIN("Terrain", "Foundation strategies, excavation, leveling, and adaptation"),
    INTERIORS("Interiors", "Room layout planning, furnishing presets, and decor"),
    CONSTRUCTION("Construction", "Build progress, scheduling, tick budgets, and checkpoints"),
    EDITING("Editing", "Transforms, rotate, mirror, duplicate, and undo/redo history"),
    BLUEPRINTS("Blueprints", "Save, load, import, export, and browse designs"),
    SETTINGS("Settings", "Snapping, preview visuals, performance, and keybindings");

    private final String title;
    private final String description;

    WorkspaceTab(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() { return title; }
    public String description() { return description; }
}