package io.github.razekteixeira.roofwright;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import io.github.razekteixeira.roofwright.core.Footprint;
import io.github.razekteixeira.roofwright.core.History;
import io.github.razekteixeira.roofwright.core.RoofSpec;

/**
 * One builder's state: chosen settings, materials, the selected outline, the preview on their screen and
 * their undo history. Lives in memory only; a server restart starts everyone fresh.
 */
public final class RoofSession {
	RoofSpec spec = RoofSpec.DEFAULTS;
	Materials materials = Materials.DEFAULT;
	/** The block for gable walls, or {@code null} to match the walls the outline was detected from. */
	@Nullable BlockState gableBlock;
	@Nullable Footprint footprint;
	@Nullable ResourceKey<Level> level;
	/** The clicked wall-top block when the outline was detected, used for gable walls. */
	@Nullable BlockState wallSample;
	Preview.@Nullable Shown preview;
	final History<Journal> history = new History<>();

	public RoofSpec spec() {
		return spec;
	}

	public void setSpec(RoofSpec value) {
		spec = value;
	}

	public void setMaterials(Materials value) {
		materials = value;
	}

	public Materials materials() {
		return materials;
	}

	public @Nullable Footprint footprint() {
		return footprint;
	}

	public History<Journal> history() {
		return history;
	}

	public Preview.@Nullable Shown preview() {
		return preview;
	}
}
