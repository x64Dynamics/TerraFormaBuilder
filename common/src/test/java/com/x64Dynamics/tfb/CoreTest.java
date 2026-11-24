package com.x64Dynamics.tfb;

import com.x64Dynamics.tfb.config.ModConfig;
import com.x64Dynamics.tfb.gameplay.RewardPolicy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class CoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rewardRequiresEligibilityAndEnabledConfig() {
        ModConfig config = new ModConfig();
        assertEquals(5, RewardPolicy.rewardFor(config, true));
        assertEquals(0, RewardPolicy.rewardFor(config, false));
        config.enabled = false;
        assertEquals(0, RewardPolicy.rewardFor(config, true));
    }

    @Test public void configPersistsAndReadsUserValues() throws Exception {
        Path directory = temporary.newFolder().toPath();
        assertEquals(5, ModConfig.load(directory).rewardAmount);
        Files.write(directory.resolve("terraformabuilder.json"), "{\"rewardAmount\":17}".getBytes(StandardCharsets.UTF_8));
        assertEquals(17, ModConfig.load(directory).rewardAmount);
    }

    @Test public void invalidConfigIsPreserved() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path config = directory.resolve("terraformabuilder.json");
        byte[] content = "{\"rewardAmount\":-1}".getBytes(StandardCharsets.UTF_8);
        Files.write(config, content);
        try { ModConfig.load(directory); fail("Expected invalid configuration to fail"); }
        catch (IllegalArgumentException expected) { assertArrayEquals(content, Files.readAllBytes(config)); }
    }
}
