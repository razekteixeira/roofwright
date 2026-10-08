package io.github.razekteixeira.roofwright.core;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Server owner settings, read from {@code config/roofwright.json}. Missing fields take their defaults and
 * out-of-range values are clamped, so a partial or hand-edited file always works.
 *
 * @param maxSpan          widest outline, in blocks, that detection and roofs accept
 * @param maxBlocks        most blocks one roof may place (permission {@code roofwright.unlimited} skips it)
 * @param blocksPerTick    most blocks placed in one server tick
 * @param millisPerTick    time budget for placing blocks in one server tick
 * @param historySize      roofs each player can undo
 * @param previewLimit     most ghost blocks in one preview; bigger roofs preview their surface only
 * @param previewSeconds   how long a preview stays before it disappears
 */
public record Settings(
		int maxSpan,
		int maxBlocks,
		int blocksPerTick,
		int millisPerTick,
		int historySize,
		int previewLimit,
		int previewSeconds,
		PermissionLevels permissionLevels) {

	/**
	 * Vanilla permission levels used when no permissions mod is installed. With one (e.g. LuckPerms),
	 * the nodes {@code roofwright.use}, {@code roofwright.force}, {@code roofwright.unlimited} and
	 * {@code roofwright.admin} take precedence.
	 */
	public record PermissionLevels(int use, int force, int unlimited, int admin) {
		public static final PermissionLevels DEFAULTS = new PermissionLevels(2, 2, 3, 3);

		static final Codec<PermissionLevels> WRITE_CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("use").forGetter(PermissionLevels::use),
				Codec.INT.fieldOf("force").forGetter(PermissionLevels::force),
				Codec.INT.fieldOf("unlimited").forGetter(PermissionLevels::unlimited),
				Codec.INT.fieldOf("admin").forGetter(PermissionLevels::admin)
		).apply(i, PermissionLevels::new));

		static final Codec<PermissionLevels> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.optionalFieldOf("use", DEFAULTS.use).forGetter(PermissionLevels::use),
				Codec.INT.optionalFieldOf("force", DEFAULTS.force).forGetter(PermissionLevels::force),
				Codec.INT.optionalFieldOf("unlimited", DEFAULTS.unlimited).forGetter(PermissionLevels::unlimited),
				Codec.INT.optionalFieldOf("admin", DEFAULTS.admin).forGetter(PermissionLevels::admin)
		).apply(i, PermissionLevels::new));
	}

	public static final Settings DEFAULTS = new Settings(96, 30_000, 2_000, 5, 10, 3_000, 300, PermissionLevels.DEFAULTS);

	/**
	 * Writes every field, including defaults, so the generated file documents all options.
	 * ({@code optionalFieldOf} would omit values equal to their default.)
	 */
	public static final Codec<Settings> WRITE_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("maxSpan").forGetter(Settings::maxSpan),
			Codec.INT.fieldOf("maxBlocks").forGetter(Settings::maxBlocks),
			Codec.INT.fieldOf("blocksPerTick").forGetter(Settings::blocksPerTick),
			Codec.INT.fieldOf("millisPerTick").forGetter(Settings::millisPerTick),
			Codec.INT.fieldOf("historySize").forGetter(Settings::historySize),
			Codec.INT.fieldOf("previewLimit").forGetter(Settings::previewLimit),
			Codec.INT.fieldOf("previewSeconds").forGetter(Settings::previewSeconds),
			PermissionLevels.WRITE_CODEC.fieldOf("permissionLevels").forGetter(Settings::permissionLevels)
	).apply(i, Settings::new));

	/** Reads leniently: every field is optional. */
	public static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("maxSpan", DEFAULTS.maxSpan).forGetter(Settings::maxSpan),
			Codec.INT.optionalFieldOf("maxBlocks", DEFAULTS.maxBlocks).forGetter(Settings::maxBlocks),
			Codec.INT.optionalFieldOf("blocksPerTick", DEFAULTS.blocksPerTick).forGetter(Settings::blocksPerTick),
			Codec.INT.optionalFieldOf("millisPerTick", DEFAULTS.millisPerTick).forGetter(Settings::millisPerTick),
			Codec.INT.optionalFieldOf("historySize", DEFAULTS.historySize).forGetter(Settings::historySize),
			Codec.INT.optionalFieldOf("previewLimit", DEFAULTS.previewLimit).forGetter(Settings::previewLimit),
			Codec.INT.optionalFieldOf("previewSeconds", DEFAULTS.previewSeconds).forGetter(Settings::previewSeconds),
			PermissionLevels.CODEC.optionalFieldOf("permissionLevels", PermissionLevels.DEFAULTS).forGetter(Settings::permissionLevels)
	).apply(i, Settings::new));

	/** Settings with every value clamped to its allowed range, plus one warning per clamped value. */
	public record Checked(Settings settings, List<String> warnings) {
	}

	public Checked checked() {
		List<String> warnings = new ArrayList<>();
		PermissionLevels p = permissionLevels;
		Settings clamped = new Settings(
				clamp("maxSpan", maxSpan, 4, RoofPlanner.MAX_SPAN, warnings),
				clamp("maxBlocks", maxBlocks, 1, 2_000_000, warnings),
				clamp("blocksPerTick", blocksPerTick, 1, 100_000, warnings),
				clamp("millisPerTick", millisPerTick, 1, 40, warnings),
				clamp("historySize", historySize, 0, 100, warnings),
				clamp("previewLimit", previewLimit, 0, 20_000, warnings),
				clamp("previewSeconds", previewSeconds, 5, 3_600, warnings),
				new PermissionLevels(
						clamp("permissionLevels.use", p.use(), 0, 4, warnings),
						clamp("permissionLevels.force", p.force(), 0, 4, warnings),
						clamp("permissionLevels.unlimited", p.unlimited(), 0, 4, warnings),
						clamp("permissionLevels.admin", p.admin(), 0, 4, warnings)));
		return new Checked(clamped, List.copyOf(warnings));
	}

	private static int clamp(String name, int value, int min, int max, List<String> warnings) {
		int clamped = Math.max(min, Math.min(max, value));
		if (clamped != value) {
			warnings.add(name + " = " + value + " is outside " + min + ".." + max + ", using " + clamped);
		}
		return clamped;
	}

	public Settings withBlocksPerTick(int value) {
		return new Settings(maxSpan, maxBlocks, value, millisPerTick, historySize, previewLimit, previewSeconds, permissionLevels);
	}

	public Settings withMaxBlocks(int value) {
		return new Settings(maxSpan, value, blocksPerTick, millisPerTick, historySize, previewLimit, previewSeconds, permissionLevels);
	}

	public Settings withPreviewLimit(int value) {
		return new Settings(maxSpan, maxBlocks, blocksPerTick, millisPerTick, historySize, value, previewSeconds, permissionLevels);
	}
}
