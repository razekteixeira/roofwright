package io.github.razekteixeira.roofwright.core;

/**
 * Everything the planner needs besides the footprint.
 *
 * @param overhang  columns the roof reaches past the walls, 0 to {@link #MAX_OVERHANG}
 * @param ridge     ridge direction for gable, dutch gable and gambrel roofs
 * @param shedRise  the side a shed roof rises towards
 * @param slabRidge cap single-column ridges and peaks with a slab instead of a full block
 */
public record RoofSpec(RoofStyle style, Pitch pitch, int overhang, RidgeAxis ridge, Dir shedRise, boolean slabRidge) {
	public static final int MAX_OVERHANG = 3;
	public static final RoofSpec DEFAULTS = new RoofSpec(RoofStyle.GABLE, Pitch.NORMAL, 1, RidgeAxis.AUTO, Dir.NORTH, true);

	public RoofSpec {
		if (style == null || pitch == null || ridge == null || shedRise == null) {
			throw new IllegalArgumentException("roof settings cannot be null");
		}
		if (overhang < 0 || overhang > MAX_OVERHANG) {
			throw new IllegalArgumentException("overhang must be 0 to " + MAX_OVERHANG + ", got " + overhang);
		}
	}

	public RoofSpec withStyle(RoofStyle value) {
		return new RoofSpec(value, pitch, overhang, ridge, shedRise, slabRidge);
	}

	public RoofSpec withPitch(Pitch value) {
		return new RoofSpec(style, value, overhang, ridge, shedRise, slabRidge);
	}

	public RoofSpec withOverhang(int value) {
		return new RoofSpec(style, pitch, value, ridge, shedRise, slabRidge);
	}

	public RoofSpec withRidge(RidgeAxis value) {
		return new RoofSpec(style, pitch, overhang, value, shedRise, slabRidge);
	}

	public RoofSpec withShedRise(Dir value) {
		return new RoofSpec(style, pitch, overhang, ridge, value, slabRidge);
	}

	public RoofSpec withSlabRidge(boolean value) {
		return new RoofSpec(style, pitch, overhang, ridge, shedRise, value);
	}

	/** One line for chat, e.g. {@code hip, 1:1, overhang 1}. */
	public String describe() {
		StringBuilder text = new StringBuilder(style.id().replace('_', ' '));
		if (style.usesPitch()) {
			text.append(", ").append(pitch.ratio());
		}
		if (style != RoofStyle.FLAT) {
			text.append(", overhang ").append(overhang);
		}
		if (style == RoofStyle.SHED) {
			text.append(", rising ").append(shedRise.id());
		} else if (ridge != RidgeAxis.AUTO && style.usesRidgeAxis()) {
			text.append(", ridge ").append(ridge.id());
		}
		return text.toString();
	}
}
