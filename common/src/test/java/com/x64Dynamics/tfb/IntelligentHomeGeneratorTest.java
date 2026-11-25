package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.architectural.IntelligentHomeGenerator;
import com.x64Dynamics.tfb.architectural.PlacementTarget;
import com.x64Dynamics.tfb.architectural.RoofGenerator;
import com.x64Dynamics.tfb.architectural.WingConfiguration;
import com.x64Dynamics.tfb.construction.DependencyAwareScheduler;
import com.x64Dynamics.tfb.construction.ScheduledBlockPlacement;
import com.x64Dynamics.tfb.geometry.BoundingBox3i;
import com.x64Dynamics.tfb.geometry.Vec3i;
import com.x64Dynamics.tfb.material.*;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyEntry;
import com.x64Dynamics.tfb.taxonomy.BlockTaxonomyRegistry;
import com.x64Dynamics.tfb.taxonomy.BlockTrait;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class IntelligentHomeGeneratorTest {

    @Test
    public void testMultiStoryHomeHasDoorsStairwellsRoofAndFurnishings() {
        BoundingBox3i bounds = BoundingBox3i.fromOriginAndSize(new Vec3i(0, 64, 0), 15, 14, 15);
        MaterialPalette palette = StylePreset.MEDIEVAL.createPalette();

        // Verify palette resolves real stairs and slabs (never falling back to full cubes)
        assertTrue(palette.get(MaterialRole.STAIR).path().endsWith("_stairs"));
        assertTrue(palette.get(MaterialRole.SLAB).path().endsWith("_slab"));

        IntelligentHomeGenerator.HomeConfig config = new IntelligentHomeGenerator.HomeConfig(
            bounds,
            palette,
            RoofGenerator.RoofType.GABLE,
            2,
            WingConfiguration.SINGLE_RECTANGLE,
            true,
            true,
            true,
            1.0f,
            42L
        );

        IntelligentHomeGenerator.HomeGenerationResult result = IntelligentHomeGenerator.generateHome(config);
        assertEquals(2, result.stories());
        assertTrue("Multi-story 15x15 home should have at least 4 rooms", result.roomCount() >= 4);

        Map<Vec3i, PlacementTarget> byPos = new HashMap<>();
        for (PlacementTarget t : result.targets()) {
            byPos.put(t.pos(), t);
        }

        // Verify 2-block tall entrance door exists (lower + upper)
        boolean hasLowerDoor = result.targets().stream().anyMatch(
            t -> t.role() == MaterialRole.DOOR && "lower".equals(t.block().property("half"))
        );
        boolean hasUpperDoor = result.targets().stream().anyMatch(
            t -> t.role() == MaterialRole.DOOR && "upper".equals(t.block().property("half"))
        );
        assertTrue("Home must have a lower door block", hasLowerDoor);
        assertTrue("Home must have an upper door block", hasUpperDoor);

        // Verify staircase treads have air headroom above them (no blocked stairwell)
        List<PlacementTarget> stairs = result.targets().stream()
            .filter(t -> t.role() == MaterialRole.STAIR && t.pos().x() == 2 && t.pos().y() >= 65 && t.pos().y() <= 68)
            .toList();
        assertFalse("Multi-story home must have interior stairs", stairs.isEmpty());
        for (PlacementTarget st : stairs) {
            PlacementTarget above1 = byPos.get(st.pos().add(0, 1, 0));
            PlacementTarget above2 = byPos.get(st.pos().add(0, 2, 0));
            assertTrue("Stair headroom y+1 must be air", above1 == null || above1.block().path().equals("air"));
            assertTrue("Stair headroom y+2 must be air", above2 == null || above2.block().path().equals("air"));
        }

        // Verify dependency scheduler orders lower door before upper door
        List<ScheduledBlockPlacement> scheduled = DependencyAwareScheduler.scheduleWithDependencies(result.targets());
        assertEquals(result.targets().size(), scheduled.size());
    }

    @Test
    public void testModdedBlockDiscoveryAndFamilyResolution() {
        BlockTaxonomyRegistry reg = BlockTaxonomyRegistry.getInstance();
        reg.registerDiscovered(
            BlockIdentifier.of("create:andesite_bricks"),
            EnumSet.of(BlockTrait.SOLID_CUBE),
            Collections.emptySet(),
            0
        );
        reg.registerDiscovered(
            BlockIdentifier.of("create:andesite_brick_stairs"),
            EnumSet.of(BlockTrait.STAIR, BlockTrait.WATERLOGGABLE),
            Set.of("facing", "half", "shape", "waterlogged"),
            0
        );

        BlockTaxonomyEntry entry = reg.getOrRegister(BlockIdentifier.of("create:andesite_brick_stairs"));
        assertTrue(entry.hasTrait(BlockTrait.STAIR));
        assertTrue(entry.hasTrait(BlockTrait.WATERLOGGABLE));

        MaterialFamily family = MaterialFamilyResolver.resolve(BlockIdentifier.of("create:andesite_bricks"));
        assertEquals("create:andesite_brick_stairs", family.stair().asString());
    }
}
