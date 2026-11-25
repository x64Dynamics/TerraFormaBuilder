package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.MaterialPalette;
import com.x64Dynamics.tfb.material.StylePreset;
import com.x64Dynamics.tfb.architectural.settlement.SettlementGenerator;
import com.x64Dynamics.tfb.architectural.settlement.SettlementPlan;
import org.junit.Test;

import static org.junit.Assert.*;

public final class SettlementTest {
    private final MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

    @Test
    public void testSettlementGeneration() {
        Vec3i center = new Vec3i(0, 64, 0);
        SettlementPlan plan = SettlementGenerator.generateSettlement(center, 30, 4, palette);

        assertEquals(4, plan.buildingPlots().size());
        assertFalse(plan.mainRoadPath().isEmpty());
        assertFalse(plan.streetlightsAndDecorations().isEmpty());
    }
}
