package io.github.razekteixeira.roofwright;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.block.state.BlockState;

import eu.pb4.common.protection.api.CommonProtection;

/**
 * Whether a roof may put a block at a position: inside the world, inside the world border, allowed by
 * vanilla (spawn protection, adventure mode) and by every claim mod that implements the Common
 * Protection API, and only replacing what the builder agreed to replace.
 */
public final class Protection {
	private Protection() {
	}

	/** Why a block is skipped; {@link #NONE} means it may be placed. */
	public enum Verdict {
		NONE("placed"),
		OUTSIDE_WORLD("outside the world"),
		PROTECTED("protected"),
		OCCUPIED("in the way");

		private final String reason;

		Verdict(String reason) {
			this.reason = reason;
		}

		public String reason() {
			return reason;
		}
	}

	/**
	 * Checks one position.
	 *
	 * @param player the builder, or {@code null} for the server console (no claims to respect)
	 * @param force  whether blocks other than air may be replaced
	 */
	public static Verdict check(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player, boolean force) {
		return check(level, pos, player, player == null ? null : player.nameAndId(), force);
	}

	/**
	 * As above, for a builder who may have logged off since: claims are still checked against their
	 * name and id, so leaving the server never lifts a protection.
	 */
	public static Verdict check(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player, @Nullable NameAndId builder, boolean force) {
		if (level.isOutsideBuildHeight(pos.getY()) || !level.getWorldBorder().isWithinBounds(pos)) {
			return Verdict.OUTSIDE_WORLD;
		}
		if (player != null && (!player.mayBuild() || !level.mayInteract(player, pos))) {
			return Verdict.PROTECTED;
		}
		if (builder != null && !CommonProtection.canPlaceBlock(level, pos, builder, player)) {
			return Verdict.PROTECTED;
		}
		return replaceable(level.getBlockState(pos), level, pos, force) ? Verdict.NONE : Verdict.OCCUPIED;
	}

	/**
	 * Air may always be replaced. With {@code force}, plain blocks may be replaced too, but never a block
	 * with a block entity (chests, signs, spawners) or an unbreakable one (bedrock, barriers).
	 */
	public static boolean replaceable(BlockState state, ServerLevel level, BlockPos pos, boolean force) {
		if (state.isAir()) {
			return true;
		}
		return force && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0;
	}
}
