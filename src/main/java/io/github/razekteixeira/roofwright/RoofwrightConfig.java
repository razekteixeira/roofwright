package io.github.razekteixeira.roofwright;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.fabricmc.loader.api.FabricLoader;

import io.github.razekteixeira.roofwright.core.Settings;

/** Loads {@code config/roofwright.json}. A broken file is reported and left untouched, never overwritten. */
public final class RoofwrightConfig {
	private static volatile Settings current = Settings.DEFAULTS;

	private RoofwrightConfig() {
	}

	public static Settings get() {
		return current;
	}

	/** Replaces the settings in memory only (GameTests and captures). */
	public static void set(Settings settings) {
		current = settings.checked().settings();
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(Roofwright.MOD_ID + ".json");
	}

	/** Loads the file, writing the defaults if it does not exist yet. Returns a short status message. */
	public static String load() {
		return load(path());
	}

	static String load(Path file) {
		try {
			if (Files.notExists(file)) {
				JsonElement defaults = Settings.WRITE_CODEC.encodeStart(JsonOps.INSTANCE, Settings.DEFAULTS).getOrThrow();
				Files.createDirectories(file.getParent());
				Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(defaults) + "\n");
				current = Settings.DEFAULTS;
				return "Wrote default settings to " + file;
			}
			JsonElement json = JsonParser.parseString(Files.readString(file));
			Settings parsed = Settings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
			Settings.Checked checked = parsed.checked();
			checked.warnings().forEach(warning -> Roofwright.LOGGER.warn("{}: {}", file.getFileName(), warning));
			current = checked.settings();
			return "Loaded settings" + (checked.warnings().isEmpty() ? "" : " with " + checked.warnings().size() + " corrected value(s), see the log");
		} catch (IOException | RuntimeException e) {
			Roofwright.LOGGER.error("Could not read {}, keeping the previous settings: {}", file, e.getMessage());
			return "Could not read " + file.getFileName() + ", kept the previous settings: " + e.getMessage();
		}
	}
}
