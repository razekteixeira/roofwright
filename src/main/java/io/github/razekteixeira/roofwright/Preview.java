package io.github.razekteixeira.roofwright;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.math.Transformation;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import io.github.razekteixeira.roofwright.core.Settings;
import io.github.razekteixeira.roofwright.mixin.BlockDisplayAccessor;
import io.github.razekteixeira.roofwright.mixin.DisplayAccessor;

/**
 * The ghost roof: block display entities that exist only on the builder's screen. They are sent as
 * packets to that one player and never added to the world, so nobody else sees them, nothing is saved,
 * and a crash cannot leave them behind. Vanilla clients render them as they would any display entity.
 */
public final class Preview {
	/** Ghost outline colour, and the colour (and red glass) for blocks that will be skipped. */
	static final int GHOST_GLOW = 0x9FE7FF;
	static final int BLOCKED_GLOW = 0xFF4A4A;
	/** The protocol refuses bundles of more than 4096 packets; each ghost takes two. */
	static final int PACKETS_PER_BUNDLE = 4000;

	private Preview() {
	}

	/** What a player currently sees. */
	public record Shown(int[] ids, ResourceKey<Level> level, long expiresAtTick, int shown, int total, boolean surfaceOnly) {
	}

	/** Packets for a preview, and the entity ids they create. */
	public record Built(List<Packet<? super ClientGamePacketListener>> packets, int[] ids, int total, boolean surfaceOnly) {
	}

	public static Shown show(ServerPlayer player, RoofSession session, RoofService.Prepared prepared, Settings settings) {
		clear(player, session);
		Built built = build(prepared.level(), prepared.targets(), settings.previewLimit());
		send(player, built.packets());
		long expires = prepared.level().getServer().getTickCount() + settings.previewSeconds() * 20L;
		Shown shown = new Shown(built.ids(), prepared.level().dimension(), expires, built.ids().length, built.total(), built.surfaceOnly());
		session.preview = shown;
		return shown;
	}

	public static void clear(ServerPlayer player, RoofSession session) {
		Shown shown = session.preview;
		session.preview = null;
		if (shown != null && shown.ids().length > 0 && player.level().dimension().equals(shown.level())) {
			player.connection.send(new ClientboundRemoveEntitiesPacket(new IntArrayList(shown.ids())));
		}
	}

	/** Removes previews that have been up longer than {@code previewSeconds}. */
	static void expire(net.minecraft.server.MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RoofSession session = RoofService.session(player.getUUID());
			if (session.preview != null && server.getTickCount() >= session.preview.expiresAtTick()) {
				clear(player, session);
			}
		}
	}

	/**
	 * Builds the ghosts. Up to {@code limit} blocks are shown one by one; bigger roofs show only the top
	 * block of each column, thinned out evenly if even that is too many.
	 */
	public static Built build(ServerLevel level, List<RoofService.Target> targets, int limit) {
		List<RoofService.Target> chosen = targets;
		boolean surfaceOnly = false;
		if (targets.size() > limit) {
			surfaceOnly = true;
			chosen = targets.stream().filter(RoofService.Target::surface).toList();
			if (chosen.size() > limit) {
				List<RoofService.Target> thinned = new ArrayList<>(limit);
				double stride = (double) chosen.size() / Math.max(1, limit);
				for (int i = 0; i < limit; i++) {
					thinned.add(chosen.get((int) (i * stride)));
				}
				chosen = thinned;
			}
		}
		List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(chosen.size() * 2);
		int[] ids = new int[chosen.size()];
		for (int i = 0; i < chosen.size(); i++) {
			RoofService.Target target = chosen.get(i);
			boolean blocked = target.verdict() != Protection.Verdict.NONE;
			Display.BlockDisplay ghost = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
			ghost.setPos(target.pos().getX(), target.pos().getY(), target.pos().getZ());
			((BlockDisplayAccessor) ghost).roofwright$setBlockState(blocked ? Blocks.STAINED_GLASS.pick(DyeColor.RED).defaultBlockState() : target.state());
			DisplayAccessor display = (DisplayAccessor) ghost;
			display.roofwright$setGlowColorOverride(blocked ? BLOCKED_GLOW : GHOST_GLOW);
			display.roofwright$setBrightnessOverride(Brightness.FULL_BRIGHT);
			if (blocked) {
				// A touch larger than the block in the way, so the red glass shows around it.
				display.roofwright$setTransformation(new Transformation(new Vector3f(-0.02f), null, new Vector3f(1.04f), null));
			}
			ghost.setGlowingTag(true);
			ids[i] = ghost.getId();
			packets.add(new ClientboundAddEntityPacket(ghost.getId(), ghost.getUUID(), ghost.getX(), ghost.getY(), ghost.getZ(),
					0, 0, EntityTypes.BLOCK_DISPLAY, 0, Vec3.ZERO, 0));
			packets.add(new ClientboundSetEntityDataPacket(ghost.getId(), ghost.getEntityData().getNonDefaultValues()));
		}
		return new Built(packets, ids, targets.size(), surfaceOnly);
	}

	private static void send(ServerPlayer player, List<Packet<? super ClientGamePacketListener>> packets) {
		for (List<Packet<? super ClientGamePacketListener>> part : bundles(packets)) {
			player.connection.send(new ClientboundBundlePacket(part));
		}
	}

	/** Splits the packets into bundles the protocol accepts, keeping each ghost's two packets together. */
	public static List<List<Packet<? super ClientGamePacketListener>>> bundles(List<Packet<? super ClientGamePacketListener>> packets) {
		List<List<Packet<? super ClientGamePacketListener>>> bundles = new ArrayList<>();
		for (int start = 0; start < packets.size(); start += PACKETS_PER_BUNDLE) {
			bundles.add(new ArrayList<>(packets.subList(start, Math.min(packets.size(), start + PACKETS_PER_BUNDLE))));
		}
		return bundles;
	}
}
