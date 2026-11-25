package com.x64Dynamics.tfb.ui.model;

import com.x64Dynamics.tfb.i18n.TFBLocalization;

public enum WorkspaceTab {
    HOME("tfb.nav.home", "tfb.nav.home.desc"),
    ARCHITECTURE("tfb.nav.design", "tfb.nav.design.desc"),
    GRID_EDITOR("tfb.nav.grid", "tfb.nav.grid.desc"),
    MATERIALS("tfb.nav.materials", "tfb.nav.materials.desc"),
    LIBRARY("tfb.nav.library", "tfb.nav.library.desc"),
    PROJECTS("tfb.nav.projects", "tfb.nav.projects.desc"),
    SETTINGS("tfb.nav.settings", "tfb.nav.settings.desc");

    private final String titleKey;
    private final String descKey;

    WorkspaceTab(String titleKey, String descKey) {
        this.titleKey = titleKey;
        this.descKey = descKey;
    }

    public String title() { return TFBLocalization.tr(titleKey); }
    public String description() { return TFBLocalization.tr(descKey); }
}
