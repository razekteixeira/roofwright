package io.github.razekteixeira.roofwright.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;

import io.github.razekteixeira.roofwright.Journal;
import io.github.razekteixeira.roofwright.Materials;
import io.github.razekteixeira.roofwright.PlacementJob;
import io.github.razekteixeira.roofwright.Placements;
import io.github.razekteixeira.roofwright.Preview;
import io.github.razekteixeira.roofwright.Protection;
import io.github.razekteixeira.roofwright.RoofService;
import io.github.razekteixeira.roofwright.RoofSession;
import io.github.razekteixeira.roofwright.RoofwrightConfig;
import io.github.razekteixeira.roofwright.Wand;
import io.github.razekteixeira.roofwright.core.Footprint;
import io.github.razekteixeira.roofwright.core.Pitch;
import io.github.razekteixeira.roofwright.core.PlanException;
import io.github.razekteixeira.roofwright.core.RoofSpec;
import io.github.razekteixeira.roofwright.core.RoofStyle;
import io.github.razekteixeira.roofwright.core.Settings;

/**
 * Runs inside a real dedicated server: real walls, real block placement, vanilla's own stair logic. Every
 * builder is a mock player with its own session, so tests running side by side never share state.
 */
public class RoofwrightGameTests {
	/** Wall top used by every house: walls from y 1 to 2 (relative to the test). */
	private static final int TOP = 2;

	// --- helpers -----------------------------------------------------------------------------------

	private static ServerPlayer builder(GameTestHelper helper, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		return player;
	}

	/** Makes a mock player a level 2 operator (the GameTest server's default operator level is 0). */
	private static void op(MinecraftServer server, ServerPlayer player) {
		server.getPlayerList().op(player.nameAndId(), java.util.Optional.of(LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.of(false));
	}

	private static CommandSourceStack source(ServerPlayer player) {
		return player.createCommandSourceStack().withSuppressedOutput();
	}

	/** Walls of stone bricks on the edge of a footprint given in test-relative columns. */
	private static void walls(GameTestHelper helper, Footprint relative) {
		for (int x = relative.minX(); x <= relative.maxX(); x++) {
			for (int z = relative.minZ(); z <= relative.maxZ(); z++) {
				if (relative.isEdge(x, z)) {
					for (int y = 1; y <= TOP; y++) {
						helper.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());
					}
				}
			}
		}
	}

	private static Footprint square(int from, int to) {
		return Footprint.rectangle(from, from, to, to, TOP);
	}

	/** An L: x and z 1..6 without the north-east 3 x 3. */
	private static Footprint ell() {
		Footprint.Builder builder = new Footprint.Builder(TOP);
		for (int x = 1; x <= 6; x++) {
			for (int z = 1; z <= 6; z++) {
				if (!(x >= 4 && z <= 3)) {
					builder.add(x, z);
				}
			}
		}
		return builder.build();
	}

	/** Detects the house from its wall top at (1, TOP, 1) and builds the roof right away. */
	private static PlacementJob roof(GameTestHelper helper, ServerPlayer player, RoofSpec spec, boolean force) throws PlanException {
		CommandSourceStack source = source(player);
		RoofService.session(source).setSpec(spec);
		RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));
		PlacementJob job = RoofService.place(source, force);
		Placements.runNow(job);
		return job;
	}

	private static void expectFailure(GameTestHelper helper, CommandDispatcher<CommandSourceStack> dispatcher, String command,
			CommandSourceStack source, String what) {
		try {
			dispatcher.execute(command, source);
			helper.fail("expected failure: " + what);
		} catch (CommandSyntaxException expected) {
			// refused as it should be
		}
	}

	// --- AC7: shapes survive vanilla's own rule ----------------------------------------------------

	@GameTest(maxTicks = 100)
	public void placedStairsKeepTheirShapes(GameTestHelper helper) throws PlanException {
		walls(helper, ell());
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		int corners = 0;
		int checked = 0;
		for (RoofStyle style : RoofStyle.values()) {
			PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(style).withOverhang(1), false);
			helper.assertTrue(job.placed() > 0, style + " placed nothing");
			for (Journal.Change change : job.journal().changes()) {
				BlockState placed = level.getBlockState(change.pos());
				if (placed.hasProperty(StairBlock.SHAPE)) {
					BlockState vanilla = Block.updateFromNeighbourShapes(placed, level, change.pos());
					helper.assertValueEqual(vanilla, placed, style + " stair at " + change.pos() + " would change");
					checked++;
					if (placed.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT) {
						corners++;
					}
				}
			}
			// Take it off again before the next style; undo must leave nothing behind.
			PlacementJob undo = RoofService.undo(source(player));
			Placements.runNow(undo);
			helper.assertValueEqual(undo.conflicts(), 0, style + " undo conflicts");
			for (Journal.Change change : job.journal().changes()) {
				helper.assertValueEqual(level.getBlockState(change.pos()), change.before(), style + " undo restores " + change.pos());
			}
		}
		helper.assertTrue(checked > 100, "only " + checked + " stairs checked");
		helper.assertTrue(corners > 10, "expected hips and valleys, got " + corners + " corner stairs");
		helper.succeed();
	}

	// --- AC8: consent before replacing anything ----------------------------------------------------

	@GameTest
	public void airOnlyByDefault(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		BlockPos eave = new BlockPos(3, TOP + 1, 1);
		helper.setBlock(eave, Blocks.STONE);
		// Replaceable blocks and fluids count as "not air" too: tall grass and water stay.
		helper.setBlock(new BlockPos(1, TOP + 1, 1), Blocks.WATER);
		helper.setBlock(new BlockPos(5, TOP + 1, 1), Blocks.SHORT_GRASS);
		PlacementJob job = roof(helper, builder(helper, GameType.CREATIVE), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), false);
		helper.assertBlockPresent(Blocks.STONE, eave);
		helper.assertBlockPresent(Blocks.WATER, new BlockPos(1, TOP + 1, 1));
		helper.assertBlockPresent(Blocks.SHORT_GRASS, new BlockPos(5, TOP + 1, 1));
		helper.assertValueEqual(job.skipped().get(Protection.Verdict.OCCUPIED), 3, "blocks in the way");
		helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, eave.east());
		helper.succeed();
	}

	@GameTest
	public void forceReplacesOnlyPlainBlocks(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		helper.setBlock(new BlockPos(2, TOP + 1, 1), Blocks.STONE);
		helper.setBlock(new BlockPos(3, TOP + 1, 1), Blocks.CHEST);
		helper.setBlock(new BlockPos(4, TOP + 1, 1), Blocks.BEDROCK);
		PlacementJob job = roof(helper, builder(helper, GameType.CREATIVE), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), true);
		helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, new BlockPos(2, TOP + 1, 1));
		helper.assertBlockPresent(Blocks.CHEST, new BlockPos(3, TOP + 1, 1));
		helper.assertBlockPresent(Blocks.BEDROCK, new BlockPos(4, TOP + 1, 1));
		helper.assertValueEqual(job.skipped().get(Protection.Verdict.OCCUPIED), 2, "chest and bedrock refused");
		helper.succeed();
	}

	@GameTest
	public void gableWallsMatchTheWalls(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		roof(helper, builder(helper, GameType.CREATIVE), RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), false);
		// Ridge runs east-west, so the west wall (x = 1) carries a gable wall above the wall top.
		helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(1, TOP + 1, 3));
		helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(1, TOP + 2, 3));
		helper.succeed();
	}

	// --- AC9: undo and redo never clobber later edits ----------------------------------------------

	@GameTest
	public void undoRestoresAndSkipsEditedBlocks(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP), false);
		BlockPos edited = new BlockPos(3, TOP, 0);
		helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, edited);
		helper.setBlock(edited, Blocks.GLOWSTONE);
		PlacementJob undo = RoofService.undo(source(player));
		Placements.runNow(undo);
		helper.assertValueEqual(undo.conflicts(), 1, "the edited block is left alone");
		helper.assertBlockPresent(Blocks.GLOWSTONE, edited);
		BlockPos absoluteEdited = helper.absolutePos(edited);
		for (Journal.Change change : job.journal().changes()) {
			if (!change.pos().equals(absoluteEdited)) {
				helper.assertValueEqual(helper.getLevel().getBlockState(change.pos()), change.before(), "restored " + change.pos());
			}
		}
		helper.succeed();
	}

	@GameTest
	public void redoReappliesAndNewRoofClearsRedo(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.PYRAMID), false);
		Placements.runNow(RoofService.undo(source));
		Placements.runNow(RoofService.redo(source));
		for (Journal.Change change : job.journal().changes()) {
			helper.assertValueEqual(helper.getLevel().getBlockState(change.pos()), change.after(), "redone " + change.pos());
		}
		Placements.runNow(RoofService.undo(source));
		RoofSession session = RoofService.session(source);
		helper.assertValueEqual(session.history().redoCount(), 1, "one roof to redo");
		roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withPitch(Pitch.LOW), false);
		helper.assertValueEqual(session.history().redoCount(), 0, "a new roof clears redo");
		helper.assertValueEqual(session.history().undoCount(), 1, "and can be undone");
		helper.succeed();
	}

	// --- AC10: limits and spreading over ticks -----------------------------------------------------

	@GameTest(maxTicks = 200)
	public void placementSpreadsAcrossTicks(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		RoofService.session(source).setSpec(RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE));
		RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));

		Settings original = RoofwrightConfig.get();
		RoofwrightConfig.set(original.withMaxBlocks(10));
		try {
			RoofService.place(source, false);
			helper.fail("a roof over maxBlocks must be refused");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("limit is 10"), expected.getMessage());
		} finally {
			RoofwrightConfig.set(original);
		}
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, TOP, 0));

		Settings slow = original.withBlocksPerTick(20);
		RoofwrightConfig.set(slow);
		PlacementJob job = RoofService.place(source, false);
		List<Integer> perTick = new ArrayList<>();
		int[] last = {0};
		helper.onEachTick(() -> {
			if (!job.isDone()) {
				perTick.add(job.progress() - last[0]);
				last[0] = job.progress();
				// Another test may run /roof reload meanwhile; keep this test's budget in place.
				RoofwrightConfig.set(slow);
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(job.isDone(), "still placing");
			RoofwrightConfig.set(original);
			helper.assertTrue(Collections.max(perTick) <= 20, "more than 20 blocks in one tick: " + perTick);
			helper.assertTrue(job.ticks() >= job.total() / 20, job.total() + " blocks in only " + job.ticks() + " ticks");
			helper.assertValueEqual(job.maxTickBlocks(), 20, "full budget used");
		});
	}

	// --- AC11: protection ---------------------------------------------------------------------------

	@GameTest
	public void claimedBlocksAreSkipped(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		// A claim over the east half of this test's house (x >= 4).
		AABB claim = new AABB(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 0, -2))), Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8))));
		Identifier id = Identifier.fromNamespaceAndPath("roofwright-gametest", "claim-" + helper.absolutePos(BlockPos.ZERO).asLong());
		CommonProtection.register(id, new ProtectionProvider() {
			@Override
			public boolean isProtected(Level level, BlockPos pos) {
				return claim.contains(Vec3.atCenterOf(pos));
			}

			@Override
			public boolean isAreaProtected(Level level, AABB area) {
				return claim.intersects(area);
			}

			@Override
			public boolean canPlaceBlock(Level level, BlockPos pos, net.minecraft.server.players.NameAndId profile,
					net.minecraft.world.entity.player.Player player) {
				return !isProtected(level, pos);
			}
		});
		try {
			PlacementJob job = roof(helper, builder(helper, GameType.CREATIVE), RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP), false);
			helper.assertTrue(job.placed() > 0, "the unclaimed half is roofed");
			for (Journal.Change change : job.journal().changes()) {
				helper.assertFalse(claim.contains(Vec3.atCenterOf(change.pos())), "placed inside the claim at " + change.pos());
			}
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, TOP, 0));
			helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, new BlockPos(2, TOP, 0));
		} finally {
			CommonProtection.remove(id);
		}
		helper.succeed();
	}

	@GameTest
	public void adventureModeCannotBuild(GameTestHelper helper) {
		walls(helper, square(1, 5));
		try {
			roof(helper, builder(helper, GameType.ADVENTURE), RoofSpec.DEFAULTS, false);
			helper.fail("adventure players must not build roofs");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("protected"), expected.getMessage());
		}
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, TOP, 0));
		helper.succeed();
	}

	// --- AC12: permissions --------------------------------------------------------------------------

	@GameTest
	public void commandsNeedPermission(GameTestHelper helper) throws CommandSyntaxException {
		MinecraftServer server = helper.getLevel().getServer();
		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		CommandSourceStack op = server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput();
		CommandSourceStack everyone = op.withPermission(LevelBasedPermissionSet.ALL);
		CommandSourceStack gamemaster = op.withPermission(LevelBasedPermissionSet.GAMEMASTER);
		expectFailure(helper, dispatcher, "roof info", everyone, "level 0 using /roof");
		expectFailure(helper, dispatcher, "roofwright info", everyone, "level 0 using the alias");
		helper.assertValueEqual(dispatcher.execute("roof info", gamemaster), 1, "level 2 uses /roof");
		helper.assertValueEqual(dispatcher.execute("roof style hip", gamemaster), 1, "level 2 changes the style");
		expectFailure(helper, dispatcher, "roof style tent", gamemaster, "unknown style");
		expectFailure(helper, dispatcher, "roof reload", gamemaster, "level 2 reloading (admin = 3)");
		// Parsed, not run: a reload would reset settings other tests in this batch rely on.
		helper.assertFalse(dispatcher.parse("roof reload", op).getReader().canRead(), "op may reload");
		helper.succeed();
	}

	@GameTest
	public void forceNeedsItsOwnPermission(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		CommandSourceStack gamemaster = server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput()
				.withPermission(LevelBasedPermissionSet.GAMEMASTER);
		Settings original = RoofwrightConfig.get();
		RoofwrightConfig.set(original.withPermissionLevels(new Settings.PermissionLevels(2, 4, 3, 3)));
		try {
			helper.assertTrue(dispatcher.parse("roof place", gamemaster).getExceptions().isEmpty()
					&& !dispatcher.parse("roof place", gamemaster).getReader().canRead(), "level 2 may place");
			helper.assertTrue(dispatcher.parse("roof place force", gamemaster).getReader().canRead(), "level 2 may not force when force = 4");
		} finally {
			RoofwrightConfig.set(original);
		}
		helper.assertFalse(dispatcher.parse("roof place force", gamemaster).getReader().canRead(), "default force level 2 allows it");
		helper.succeed();
	}

	// --- AC13: materials ----------------------------------------------------------------------------

	@GameTest
	public void materialsResolveFromAnyFamilyMember(GameTestHelper helper) {
		Materials oak = Materials.resolve(Blocks.OAK_PLANKS).materials();
		helper.assertTrue(oak != null && oak.stairs() == Blocks.OAK_STAIRS && oak.slab() == Blocks.OAK_SLAB, "oak from planks");
		Materials bricks = Materials.resolve(Blocks.STONE_BRICK_WALL).materials();
		helper.assertTrue(bricks != null && bricks.full() == Blocks.STONE_BRICKS && bricks.wall() == Blocks.STONE_BRICK_WALL
				&& bricks.stairs() == Blocks.STONE_BRICK_STAIRS, "stone bricks from the wall");
		// Stone bricks are also a variant of the stone family; their own family must win every time.
		Materials stoneBricks = Materials.resolve(Blocks.STONE_BRICKS).materials();
		helper.assertTrue(stoneBricks != null && stoneBricks.full() == Blocks.STONE_BRICKS && stoneBricks.stairs() == Blocks.STONE_BRICK_STAIRS,
				"stone bricks from stone bricks: " + stoneBricks);
		Materials deepslate = Materials.resolve(Blocks.DEEPSLATE_TILE_SLAB).materials();
		helper.assertTrue(deepslate != null && deepslate.full() == Blocks.DEEPSLATE_TILES, "deepslate tiles from the slab");
		helper.assertTrue(Materials.resolve(Blocks.DIAMOND_BLOCK).materials() == null, "diamond blocks have no stairs");
		helper.assertTrue(Materials.resolve(Blocks.GLASS).error() != null, "glass is refused with a message");
		helper.assertTrue(Materials.explicit(Blocks.OAK_SLAB, Blocks.OAK_SLAB, Blocks.OAK_PLANKS).error() != null, "a slab is not stairs");
		helper.assertTrue(Materials.explicit(Blocks.OAK_STAIRS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS).error() != null, "planks are not a slab");
		helper.assertTrue(Materials.explicit(Blocks.OAK_STAIRS, Blocks.SPRUCE_SLAB, Blocks.BIRCH_PLANKS).materials() != null, "mixing is fine");
		helper.succeed();
	}

	/** Macaw's Roofs is on the GameTest classpath only; its roof blocks are stairs by their state properties. */
	@GameTest
	public void macawsRoofBlocksWorkAsStairs(GameTestHelper helper) throws PlanException {
		Block macaws = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getOptional(Identifier.fromNamespaceAndPath("mcwroofs", "oak_roof")).orElse(null);
		helper.assertTrue(macaws != null, "Macaw's Roofs is loaded");
		Materials materials = Materials.resolve(macaws).materials();
		helper.assertTrue(materials != null && materials.stairs() == macaws && materials.slab() == Blocks.OAK_SLAB
				&& materials.full() == Blocks.OAK_PLANKS, "Macaw's oak roof with vanilla oak slab and planks: " + materials);

		walls(helper, ell());
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		RoofService.session(source(player)).setMaterials(materials);
		PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP), false);
		int roofBlocks = 0;
		ServerLevel level = helper.getLevel();
		for (Journal.Change change : job.journal().changes()) {
			BlockState placed = level.getBlockState(change.pos());
			if (placed.is(macaws)) {
				roofBlocks++;
				helper.assertValueEqual(Block.updateFromNeighbourShapes(placed, level, change.pos()), placed,
						"Macaw's own shape rule keeps " + change.pos());
			}
		}
		helper.assertTrue(roofBlocks > 20, "only " + roofBlocks + " Macaw's roof blocks placed");
		helper.succeed();
	}

	// --- AC14: preview ------------------------------------------------------------------------------

	@GameTest
	public void previewIsPacketOnlyCappedAndMarksBlocked(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		helper.setBlock(new BlockPos(3, TOP + 1, 1), Blocks.STONE);
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		RoofService.session(source).setSpec(RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE));
		RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));
		RoofService.Prepared prepared = RoofService.prepare(source, false);

		Preview.Built all = Preview.build(helper.getLevel(), prepared.targets(), 10_000);
		helper.assertValueEqual(all.ids().length, prepared.targets().size(), "every block previewed");
		helper.assertValueEqual(all.packets().size(), 2 * all.ids().length, "an add and a data packet per ghost");
		helper.assertFalse(all.surfaceOnly(), "small roof shown whole");
		BlockState redGlass = Blocks.STAINED_GLASS.pick(DyeColor.RED).defaultBlockState();
		int red = 0;
		for (Packet<? super ClientGamePacketListener> packet : all.packets()) {
			if (packet instanceof ClientboundSetEntityDataPacket data) {
				for (SynchedEntityData.DataValue<?> value : data.packedItems()) {
					if (redGlass.equals(value.value())) {
						red++;
					}
				}
			}
		}
		helper.assertValueEqual(red, 1, "the stone in the way shows as red glass");

		Preview.Built capped = Preview.build(helper.getLevel(), prepared.targets(), 8);
		helper.assertTrue(capped.surfaceOnly() && capped.ids().length <= 8, "capped preview: " + capped.ids().length);

		Preview.show(player, RoofService.session(source), prepared, RoofwrightConfig.get());
		AABB area = new AABB(Vec3.atCenterOf(helper.absolutePos(new BlockPos(-2, -1, -2))), Vec3.atCenterOf(helper.absolutePos(new BlockPos(9, 9, 9))));
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(Display.class, area).isEmpty(), "no ghost exists in the world");
		helper.assertTrue(RoofService.session(source).preview() != null, "the player has a preview");

		List<Packet<? super ClientGamePacketListener>> many = new ArrayList<>(Collections.nCopies(9_001, all.packets().getFirst()));
		List<List<Packet<? super ClientGamePacketListener>>> bundles = Preview.bundles(many);
		helper.assertTrue(bundles.stream().allMatch(b -> b.size() <= 4096), "bundles within the protocol limit");
		helper.assertValueEqual(bundles.stream().mapToInt(List::size).sum(), 9_001, "nothing lost when bundling");
		helper.succeed();
	}

	// --- AC15: wand -----------------------------------------------------------------------------------

	@GameTest
	public void wandIsRecognisedByItsMarker(GameTestHelper helper) {
		ItemStack wand = Wand.create();
		helper.assertTrue(Wand.is(wand), "the wand");
		helper.assertTrue(wand.is(Items.STICK), "a vanilla stick underneath");
		helper.assertFalse(Wand.is(new ItemStack(Items.STICK)), "a plain stick");
		ItemStack other = new ItemStack(Items.STICK);
		CompoundTag tag = new CompoundTag();
		tag.putString("roofwright", "something-else");
		other.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		helper.assertFalse(Wand.is(other), "a stick with another marker");
		helper.succeed();
	}

	// --- AC1: detection on real blocks ---------------------------------------------------------------

	@GameTest
	public void detectsAnEllFromRealWalls(GameTestHelper helper) throws PlanException {
		walls(helper, ell());
		CommandSourceStack source = source(builder(helper, GameType.CREATIVE));
		Footprint found = RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 4)));
		helper.assertValueEqual(found.cellCount(), 27, "6 x 6 minus the 3 x 3 corner");
		BlockPos inside = helper.absolutePos(new BlockPos(2, TOP, 5));
		helper.assertTrue(found.contains(inside.getX(), inside.getZ()), "room inside the walls");
		BlockPos notch = helper.absolutePos(new BlockPos(5, TOP, 2));
		helper.assertFalse(found.contains(notch.getX(), notch.getZ()), "the missing corner");

		helper.setBlock(new BlockPos(1, TOP, 5), Blocks.AIR);
		try {
			RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));
			helper.fail("an open wall top must be reported");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("do not enclose"), expected.getMessage());
		}
		helper.succeed();
	}

	// --- G5 security fixes ----------------------------------------------------------------------------

	/** H1: a far corner is refused from its coordinates, before a single column is collected. */
	@GameTest
	public void farSelectionIsRefusedBeforeAllocating(GameTestHelper helper) throws PlanException {
		CommandSourceStack source = source(builder(helper, GameType.CREATIVE));
		BlockPos corner = helper.absolutePos(new BlockPos(1, TOP, 1));
		long start = System.nanoTime();
		try {
			RoofService.selectRectangle(source, helper.getLevel(), corner, corner.offset(100_000, 0, 100_000), false);
			helper.fail("a 100,001-wide selection must be refused");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("the limit is"), expected.getMessage());
		}
		helper.assertTrue(System.nanoTime() - start < 1_000_000_000L, "refused in under a second");

		// "select add" counts the union: two small rectangles 200 blocks apart are too wide together.
		Footprint first = RoofService.selectRectangle(source, helper.getLevel(), corner, corner.offset(4, 0, 4), false);
		try {
			RoofService.selectRectangle(source, helper.getLevel(), corner.offset(200, 0, 0), corner.offset(204, 0, 4), true);
			helper.fail("a union wider than maxSpan must be refused");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("205 x 5"), expected.getMessage());
		}
		helper.assertValueEqual(RoofService.session(source).footprint(), first, "the earlier selection stays");
		helper.succeed();
	}

	/** M1: portals, containers, fluids, unbreakable, operator and falling blocks never become a roof. */
	@GameTest
	public void unsafeMaterialsAreRefused(GameTestHelper helper) throws PlanException, CommandSyntaxException {
		for (Block bad : new Block[] {Blocks.TNT, Blocks.CHEST, Blocks.BEDROCK, Blocks.SAND, Blocks.COMMAND_BLOCK, Blocks.BUDDING_AMETHYST,
				Blocks.SPAWNER, Blocks.GLASS_PANE}) {
			helper.assertTrue(Materials.explicit(Blocks.OAK_STAIRS, Blocks.OAK_SLAB, bad).error() != null, bad + " as the full block");
		}
		helper.assertTrue(Materials.explicit(Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_PLANKS).materials() != null, "planks are fine");
		helper.assertTrue(Materials.problem(Blocks.WATER.defaultBlockState(), true) != null, "water");
		helper.assertTrue(Materials.problem(Blocks.LAVA.defaultBlockState(), false) != null, "lava");
		helper.assertTrue(Materials.problem(Blocks.NETHER_PORTAL.defaultBlockState(), false) != null, "nether portal");
		helper.assertTrue(Materials.problem(Blocks.END_PORTAL.defaultBlockState(), true) != null, "end portal");
		helper.assertTrue(Materials.problem(Blocks.FIRE.defaultBlockState(), false) != null, "fire");
		helper.assertTrue(Materials.problem(Blocks.TNT.defaultBlockState(), true).contains("forbidden"), "the tag names itself");
		helper.assertTrue(Materials.problem(Blocks.STONE_BRICKS.defaultBlockState(), true) == null, "stone bricks are fine");

		MinecraftServer server = helper.getLevel().getServer();
		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		ServerPlayer gamemaster = builder(helper, GameType.CREATIVE);
		op(server, gamemaster);
		try {
			CommandSourceStack source = source(gamemaster);
			expectFailure(helper, dispatcher, "roof gable minecraft:end_portal", source, "an End portal as gable walls");
			expectFailure(helper, dispatcher, "roof gable minecraft:bedrock", source, "bedrock as gable walls");
			expectFailure(helper, dispatcher, "roof gable minecraft:tnt", source, "TNT as gable walls");
			expectFailure(helper, dispatcher, "roof material minecraft:oak_stairs minecraft:oak_slab minecraft:barrier", source, "barrier");
			helper.assertValueEqual(dispatcher.execute("roof gable minecraft:bricks", source), 1, "bricks as gable walls");
		} finally {
			server.getPlayerList().deop(gamemaster.nameAndId());
		}

		// Detection copies the clicked block for gable walls only when it is safe: a TNT corner is not copied.
		walls(helper, square(1, 5));
		helper.setBlock(new BlockPos(1, TOP, 1), Blocks.TNT);
		CommandSourceStack source = source(builder(helper, GameType.CREATIVE));
		RoofService.session(source).setSpec(RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE));
		RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));
		RoofService.Prepared prepared = RoofService.prepare(source, false);
		helper.assertTrue(prepared.targets().stream().noneMatch(t -> t.state().is(Blocks.TNT)), "no TNT in the roof");
		helper.assertTrue(prepared.targets().stream().anyMatch(t -> t.state().is(Blocks.SPRUCE_PLANKS)), "gable walls fall back to planks");
		helper.succeed();
	}

	/** M2: a burst of wand clicks acts once; the wand works again after the cooldown. */
	@GameTest(maxTicks = 60)
	public void wandClicksHaveACooldown(GameTestHelper helper) {
		walls(helper, square(1, 5));
		walls(helper, Footprint.rectangle(8, 1, 12, 5, TOP));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		MinecraftServer server = helper.getLevel().getServer();
		op(server, player);
		player.setItemInHand(InteractionHand.MAIN_HAND, Wand.create());
		RoofSession session = RoofService.session(player.getUUID());
		RoofStyle before = session.spec().style();
		BlockPos ground = helper.absolutePos(new BlockPos(3, 0, 3));
		for (int i = 0; i < 10; i++) {
			AttackBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, ground, Direction.UP);
		}
		RoofStyle once = RoofStyle.values()[(before.ordinal() + 1) % RoofStyle.values().length];
		helper.assertValueEqual(session.spec().style(), once, "ten clicks in one tick change the style once");

		helper.runAfterDelay(RoofwrightConfig.get().wandCooldownTicks() + 1, () -> {
			// Right-click the first house, then at once the second: only the first is selected.
			BlockPos first = helper.absolutePos(new BlockPos(1, TOP, 1));
			BlockPos second = helper.absolutePos(new BlockPos(8, TOP, 1));
			UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(first), Direction.UP, first, false));
			UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(second), Direction.UP, second, false));
			server.getPlayerList().deop(player.nameAndId());
			Footprint selected = session.footprint();
			helper.assertTrue(selected != null && selected.contains(first.getX(), first.getZ()), "the wand works again after the cooldown");
			helper.assertFalse(selected.contains(second.getX(), second.getZ()), "the second click came too soon");
			helper.succeed();
		});
	}

	/** L1: checking an unloaded position never loads its chunk. */
	@GameTest
	public void unloadedChunksAreNeverLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos far = new BlockPos(2_000_016, 70, 2_000_016);
		helper.assertFalse(level.getChunkSource().hasChunk(far.getX() >> 4, far.getZ() >> 4), "test setup: the chunk is not loaded");
		helper.assertValueEqual(Protection.check(level, far, null, false), Protection.Verdict.NOT_LOADED, "verdict");
		helper.assertFalse(level.getChunkSource().hasChunk(far.getX() >> 4, far.getZ() >> 4), "the check loaded the chunk");
		helper.succeed();
	}

	/** L2: replacing a block needs break rights; a claim that allows placing only keeps its blocks. */
	@GameTest
	public void replacingNeedsBreakRights(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		BlockPos stone = new BlockPos(2, TOP + 1, 1);
		helper.setBlock(stone, Blocks.STONE);
		AABB claim = new AABB(Vec3.atCenterOf(helper.absolutePos(new BlockPos(-2, 0, -2))), Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8))));
		Identifier id = Identifier.fromNamespaceAndPath("roofwright-gametest", "place-only-" + helper.absolutePos(BlockPos.ZERO).asLong());
		CommonProtection.register(id, new ProtectionProvider() {
			@Override
			public boolean isProtected(Level level, BlockPos pos) {
				return claim.contains(Vec3.atCenterOf(pos));
			}

			@Override
			public boolean isAreaProtected(Level level, AABB area) {
				return claim.intersects(area);
			}

			@Override
			public boolean canPlaceBlock(Level level, BlockPos pos, net.minecraft.server.players.NameAndId profile,
					net.minecraft.world.entity.player.Player player) {
				return true;
			}

			@Override
			public boolean canBreakBlock(Level level, BlockPos pos, net.minecraft.server.players.NameAndId profile,
					net.minecraft.world.entity.player.Player player) {
				return !isProtected(level, pos);
			}
		});
		try {
			ServerPlayer player = builder(helper, GameType.CREATIVE);
			PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), true);
			helper.assertBlockPresent(Blocks.STONE, stone);
			helper.assertTrue(job.placed() > 0, "air is still roofed");
			helper.assertTrue(job.skipped().getOrDefault(Protection.Verdict.PROTECTED, 0) >= 1, "the stone was protected: " + job.skipped());
			// Undo turns roof blocks back into air, which is breaking them: the claim keeps the roof.
			PlacementJob undo = RoofService.undo(source(player));
			Placements.runNow(undo);
			helper.assertValueEqual(undo.placed(), 0, "nothing undone inside a place-only claim");
			helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, new BlockPos(3, TOP, 0));
		} finally {
			CommonProtection.remove(id);
		}
		helper.succeed();
	}

	/** L3: redoing a forced roof needs the force permission again. */
	@GameTest
	public void redoNeedsTheSameRights(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		helper.setBlock(new BlockPos(2, TOP + 1, 1), Blocks.STONE);
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), true);
		Placements.runNow(RoofService.undo(source));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, TOP + 1, 1));
		try {
			RoofService.redo(source);
			helper.fail("a player without roofwright.force must not redo a forced roof");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("roofwright.force"), expected.getMessage());
		}
		helper.assertValueEqual(RoofService.session(source).history().redoCount(), 1, "the refused redo stays available");
		MinecraftServer server = helper.getLevel().getServer();
		op(server, player);
		try {
			// Level 2 may force but not exceed limits: a redo bigger than maxBlocks is refused like a new roof.
			Settings original = RoofwrightConfig.get();
			RoofwrightConfig.set(original.withMaxBlocks(1));
			try {
				RoofService.redo(source(player));
				helper.fail("a redo over maxBlocks must be refused");
			} catch (PlanException expected) {
				helper.assertTrue(expected.getMessage().contains("limit is 1"), expected.getMessage());
			} finally {
				RoofwrightConfig.set(original);
			}
			Placements.runNow(RoofService.redo(source(player)));
		} finally {
			server.getPlayerList().deop(player.nameAndId());
		}
		helper.assertBlockPresent(Blocks.SPRUCE_STAIRS, new BlockPos(2, TOP + 1, 1));
		helper.succeed();
	}

	// --- G6 review fixes ------------------------------------------------------------------------------

	/** F1: undo and redo steps pass the world checks too, not only claims: a border shrunk since counts. */
	@GameTest
	public void replayChecksTheWorldLimits(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		PlacementJob placed = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), false);
		Placements.runNow(RoofService.undo(source));
		ServerLevel level = helper.getLevel();
		net.minecraft.world.level.border.WorldBorder border = level.getWorldBorder();
		double centerX = border.getCenterX();
		double centerZ = border.getCenterZ();
		double size = border.getSize();
		PlacementJob redo;
		// Synchronous on the server thread, so no other test sees the small border.
		try {
			BlockPos far = helper.absolutePos(new BlockPos(1000, 0, 1000));
			border.setCenter(far.getX(), far.getZ());
			border.setSize(16);
			redo = RoofService.redo(source);
			Placements.runNow(redo);
		} finally {
			border.setCenter(centerX, centerZ);
			border.setSize(size);
		}
		helper.assertValueEqual(redo.placed(), 0, "nothing redone outside the border");
		helper.assertValueEqual(redo.skipped().get(Protection.Verdict.OUTSIDE_WORLD), placed.placed(), "every block refused as outside the world");
		helper.succeed();
	}

	/** F2: settling a wall at the edge of the loaded area never loads the chunk next to it. */
	@GameTest
	public void settlingNeverLoadsChunks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		for (int i = 0; i < 256 && level.getChunkSource().hasChunk(cx - 1, cz); i++) {
			cx--;
		}
		helper.assertTrue(level.getChunkSource().hasChunk(cx, cz) && !level.getChunkSource().hasChunk(cx - 1, cz),
				"test setup: a loaded chunk with an unloaded west neighbour");
		BlockPos edge = new BlockPos(cx << 4, origin.getY() + 30, (cz << 4) + 8);
		helper.assertTrue(level.getBlockState(edge).isAir(), "test setup: air at " + edge);
		PlacementJob job = new PlacementJob(PlacementJob.Kind.PLACE, java.util.UUID.randomUUID(), null, level, "test",
				List.of(new PlacementJob.Step(edge, null, Blocks.COBBLESTONE_WALL.defaultBlockState(), true)), false, done -> {
				});
		Placements.runNow(job);
		helper.assertFalse(level.getChunkSource().hasChunk(cx - 1, cz), "settling loaded the neighbouring chunk");
		helper.assertValueEqual(job.placed(), 1, "the wall itself is placed");
		level.setBlock(edge, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		helper.succeed();
	}

	/** F3: walls and stairs are settled within the block budget, not all at once after the last block. */
	@GameTest
	public void settlingStaysWithinTheBlockBudget(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		CommandSourceStack source = source(builder(helper, GameType.CREATIVE));
		RoofSession session = RoofService.session(source);
		session.setSpec(RoofSpec.DEFAULTS.withStyle(RoofStyle.FLAT));
		session.setMaterials(Materials.resolve(Blocks.STONE_BRICKS).materials());
		RoofService.detect(source, helper.getLevel(), helper.absolutePos(new BlockPos(1, TOP, 1)));
		PlacementJob job = RoofService.place(source, false);
		int calls = 0;
		int writes = 0;
		while (!job.isDone()) {
			int written = Placements.step(job, 5);
			helper.assertTrue(written <= 5, written + " blocks written in one turn with a budget of 5");
			writes += written;
			calls++;
		}
		Placements.runNow(job);
		helper.assertTrue(writes > job.placed(), "parapet walls were settled: " + writes + " writes for " + job.placed() + " blocks");
		helper.assertTrue(calls >= writes / 5, writes + " writes in " + calls + " turns");
		BlockPos wall = helper.absolutePos(new BlockPos(3, TOP + 1, 1));
		helper.assertValueEqual(Block.updateFromNeighbourShapes(helper.getLevel().getBlockState(wall), helper.getLevel(), wall),
				helper.getLevel().getBlockState(wall), "the parapet is connected");
		helper.succeed();
	}

	/** F4: a stair beside a block that was skipped gets the shape vanilla gives it there. */
	@GameTest
	public void blockedNeighboursLeaveNoWrongCorners(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		helper.setBlock(new BlockPos(2, TOP + 1, 1), Blocks.STONE);
		helper.setBlock(new BlockPos(1, TOP + 1, 2), Blocks.STONE);
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		PlacementJob job = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.HIP).withOverhang(0), false);
		helper.assertTrue(job.skipped().getOrDefault(Protection.Verdict.OCCUPIED, 0) >= 2, "the stones were skipped: " + job.skipped());
		ServerLevel level = helper.getLevel();
		int stairs = 0;
		for (Journal.Change change : job.journal().changes()) {
			BlockState placed = level.getBlockState(change.pos());
			if (placed.hasProperty(StairBlock.SHAPE)) {
				helper.assertValueEqual(Block.updateFromNeighbourShapes(placed, level, change.pos()), placed, "stair at " + change.pos());
				stairs++;
			}
		}
		helper.assertTrue(stairs > 10, "only " + stairs + " stairs");
		helper.succeed();
	}

	/** F5: a cancelled undo keeps the part it never reached undoable; history follows what really ran. */
	@GameTest
	public void cancelledUndoKeepsTheRestUndoable(GameTestHelper helper) throws PlanException {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		CommandSourceStack source = source(player);
		PlacementJob placed = roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), false);
		RoofSession session = RoofService.session(source);
		PlacementJob undo = RoofService.undo(source);
		Placements.step(undo, 10);
		undo.cancel();
		Placements.runNow(undo);
		helper.assertValueEqual(session.history().undoCount(), 1, "the rest of the roof can still be undone");
		helper.assertValueEqual(session.history().redoCount(), 1, "the undone part can be redone");
		Placements.runNow(RoofService.undo(source));
		for (Journal.Change change : placed.journal().changes()) {
			helper.assertValueEqual(helper.getLevel().getBlockState(change.pos()), change.before(), "undone " + change.pos());
		}
		helper.assertValueEqual(session.history().undoCount(), 0, "all undone");
		Placements.runNow(RoofService.redo(source));
		Placements.runNow(RoofService.redo(source));
		for (Journal.Change change : placed.journal().changes()) {
			helper.assertValueEqual(helper.getLevel().getBlockState(change.pos()), change.after(), "redone " + change.pos());
		}
		helper.assertValueEqual(session.history().redoCount(), 0, "nothing left to redo");
		helper.succeed();
	}

	/** F6: materials are checked again when building, so stale or default choices cannot bypass the rules. */
	@GameTest
	public void materialsAreCheckedAgainWhenBuilding(GameTestHelper helper) {
		walls(helper, square(1, 5));
		ServerPlayer player = builder(helper, GameType.CREATIVE);
		RoofService.session(source(player)).setMaterials(new Materials(Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.TNT, null));
		try {
			roof(helper, player, RoofSpec.DEFAULTS.withStyle(RoofStyle.GABLE), false);
			helper.fail("TNT chosen earlier must still be refused");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("tnt"), expected.getMessage());
		}
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, TOP, 0));
		helper.succeed();
	}

	/** F8: no ghosts at Overworld coordinates while the player is in another dimension. */
	@GameTest
	public void previewNeedsTheSelectionsDimension(GameTestHelper helper) throws PlanException {
		MinecraftServer server = helper.getLevel().getServer();
		ServerLevel nether = server.getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "the Nether exists");
		CommandSourceStack source = source(builder(helper, GameType.CREATIVE));
		BlockPos corner = new BlockPos(0, 64, 0);
		RoofService.selectRectangle(source, nether, corner, corner.offset(4, 0, 4), false);
		try {
			RoofService.preview(source);
			helper.fail("a preview of a Nether selection from the Overworld must be refused");
		} catch (PlanException expected) {
			helper.assertTrue(expected.getMessage().contains("the_nether"), expected.getMessage());
		}
		helper.assertTrue(RoofService.session(source).preview() == null, "nothing shown");
		helper.succeed();
	}
}
