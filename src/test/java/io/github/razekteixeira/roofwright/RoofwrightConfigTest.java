package io.github.razekteixeira.roofwright;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.razekteixeira.roofwright.core.Settings;

class RoofwrightConfigTest {
	@Test
	void missingFileIsCreatedWithDefaults(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config/roofwright.json");
		String status = RoofwrightConfig.load(file);
		assertTrue(status.startsWith("Wrote default settings"), status);
		String written = Files.readString(file);
		for (String option : new String[] {"maxSpan", "maxBlocks", "blocksPerTick", "millisPerTick", "historySize", "previewLimit",
				"previewSeconds", "permissionLevels", "\"use\"", "\"force\"", "\"unlimited\"", "\"admin\""}) {
			assertTrue(written.contains(option), "generated file documents " + option + ":\n" + written);
		}
		assertEquals(Settings.DEFAULTS, RoofwrightConfig.get());
	}

	@Test
	void brokenFileKeepsPreviousSettingsAndIsNotOverwritten(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("roofwright.json");
		Files.writeString(file, "{\"maxBlocks\": 500}");
		RoofwrightConfig.load(file);
		assertEquals(500, RoofwrightConfig.get().maxBlocks());

		String broken = "{\"maxBlocks\": 5,, oops";
		Files.writeString(file, broken);
		String status = RoofwrightConfig.load(file);

		assertTrue(status.startsWith("Could not read"), status);
		assertEquals(500, RoofwrightConfig.get().maxBlocks(), "previous settings stay active");
		assertEquals(broken, Files.readString(file), "the owner's file is never overwritten");
	}

	@Test
	void wrongTypeIsReportedNotIgnored(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("roofwright.json");
		Files.writeString(file, "{\"blocksPerTick\": \"lots\"}");
		assertTrue(RoofwrightConfig.load(file).startsWith("Could not read"));
	}

	@Test
	void outOfRangeValuesAreClampedWithAWarning(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("roofwright.json");
		Files.writeString(file, "{\"blocksPerTick\": 0, \"millisPerTick\": 500, \"permissionLevels\": {\"force\": 9}}");
		String status = RoofwrightConfig.load(file);
		assertTrue(status.contains("3 corrected"), status);
		assertEquals(1, RoofwrightConfig.get().blocksPerTick());
		assertEquals(40, RoofwrightConfig.get().millisPerTick());
		assertEquals(4, RoofwrightConfig.get().permissionLevels().force());
		assertEquals(Settings.PermissionLevels.DEFAULTS.use(), RoofwrightConfig.get().permissionLevels().use(), "missing nested field defaults");
	}
}
