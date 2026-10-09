package io.github.razekteixeira.roofwright;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import me.lucko.fabric.api.permissions.v0.Permissions;

import io.github.razekteixeira.roofwright.core.Dir;
import io.github.razekteixeira.roofwright.core.Footprint;
import io.github.razekteixeira.roofwright.core.Pitch;
import io.github.razekteixeira.roofwright.core.PlanException;
import io.github.razekteixeira.roofwright.core.RidgeAxis;
import io.github.razekteixeira.roofwright.core.RoofSpec;
import io.github.razekteixeira.roofwright.core.RoofStyle;

/** {@code /roof}, alias {@code /roofwright}. Everything needs {@code roofwright.use} (vanilla level 2 by default). */
public final class RoofwrightCommands {
	private static final SimpleCommandExceptionType NO_TARGET = new SimpleCommandExceptionType(
			Component.literal("Look at the top block of a wall, or give its position"));
	private static final SimpleCommandExceptionType PLAYERS_ONLY = new SimpleCommandExceptionType(
			Component.literal("Only players can hold a wand"));
	private static final SimpleCommandExceptionType INVENTORY_FULL = new SimpleCommandExceptionType(
			Component.literal("Your inventory is full"));
	private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
			text -> Component.literal(text.toString()));

	private RoofwrightCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		var root = dispatcher.register(literal("roof").requires(RoofwrightCommands::canUse)
				.executes(RoofwrightCommands::help)
				.then(literal("help").executes(RoofwrightCommands::help))
				.then(literal("wand").executes(RoofwrightCommands::wand))
				.then(literal("detect")
						.executes(c -> detect(c, lookedAt(c)))
						.then(argument("pos", BlockPosArgument.blockPos()).executes(c -> detect(c, BlockPosArgument.getLoadedBlockPos(c, "pos")))))
				.then(literal("select")
						.then(argument("from", BlockPosArgument.blockPos()).then(argument("to", BlockPosArgument.blockPos())
								.executes(c -> select(c, false))))
						.then(literal("add").then(argument("from", BlockPosArgument.blockPos()).then(argument("to", BlockPosArgument.blockPos())
								.executes(c -> select(c, true))))))
				.then(choice("style", RoofStyle.values(), RoofStyle::id, RoofStyle::parse, (spec, v) -> spec.withStyle(v)))
				.then(choice("pitch", Pitch.values(), Pitch::id, Pitch::parse, (spec, v) -> spec.withPitch(v)))
				.then(choice("ridge", RidgeAxis.values(), RidgeAxis::id, RidgeAxis::parse, (spec, v) -> spec.withRidge(v)))
				.then(choice("shed", Dir.values(), Dir::id, text -> Arrays.stream(Dir.values()).filter(d -> d.id().equals(text)).findFirst(),
						(spec, v) -> spec.withShedRise(v)))
				.then(literal("ridgecap")
						.then(literal("slab").executes(c -> change(c, spec -> spec.withSlabRidge(true))))
						.then(literal("full").executes(c -> change(c, spec -> spec.withSlabRidge(false)))))
				.then(literal("overhang").then(argument("blocks", IntegerArgumentType.integer(0, RoofSpec.MAX_OVERHANG))
						.executes(c -> change(c, spec -> spec.withOverhang(IntegerArgumentType.getInteger(c, "blocks"))))))
				.then(literal("material")
						.then(argument("block", BlockStateArgument.block(context))
								.executes(RoofwrightCommands::material)
								.then(argument("slab", BlockStateArgument.block(context)).then(argument("full", BlockStateArgument.block(context))
										.executes(RoofwrightCommands::explicitMaterial)))))
				.then(literal("gable")
						.then(literal("match").executes(c -> gable(c, null)))
						.then(argument("block", BlockStateArgument.block(context))
								.executes(c -> gable(c, safeGable(BlockStateArgument.getBlock(c, "block").getState())))))
				.then(literal("preview").executes(c -> act(c, () -> RoofService.preview(c.getSource()))))
				.then(literal("place")
						.executes(c -> act(c, () -> RoofService.place(c.getSource(), false)))
						.then(literal("force").requires(RoofwrightCommands::canForce)
								.executes(c -> act(c, () -> RoofService.place(c.getSource(), true)))))
				.then(literal("undo").executes(c -> act(c, () -> RoofService.undo(c.getSource()))))
				.then(literal("redo").executes(c -> act(c, () -> RoofService.redo(c.getSource()))))
				.then(literal("cancel").executes(RoofwrightCommands::cancel))
				.then(literal("info").executes(RoofwrightCommands::info))
				.then(literal("reload").requires(RoofwrightCommands::canAdmin).executes(RoofwrightCommands::reload)));
		dispatcher.register(literal("roofwright").requires(RoofwrightCommands::canUse).redirect(root));
	}

	// Permission nodes win when a permissions mod is installed; otherwise the configured vanilla levels apply.
	// Levels are read on every check so /roof reload takes effect immediately.

	private static PermissionLevel level(int configured) {
		return PermissionLevel.byId(configured);
	}

	static boolean canUse(CommandSourceStack source) {
		return Permissions.check(source, "roofwright.use", level(RoofwrightConfig.get().permissionLevels().use()));
	}

	static boolean canForce(CommandSourceStack source) {
		return Permissions.check(source, "roofwright.force", level(RoofwrightConfig.get().permissionLevels().force()));
	}

	static boolean canExceedLimits(CommandSourceStack source) {
		return Permissions.check(source, "roofwright.unlimited", level(RoofwrightConfig.get().permissionLevels().unlimited()));
	}

	static boolean canAdmin(CommandSourceStack source) {
		return Permissions.check(source, "roofwright.admin", level(RoofwrightConfig.get().permissionLevels().admin()));
	}

	@FunctionalInterface
	private interface SpecChange<T> {
		RoofSpec apply(RoofSpec spec, T value);
	}

	/** {@code /roof <name> <value>} with tab completion over the enum's ids. */
	private static <T> LiteralArgumentBuilder<CommandSourceStack> choice(String name, T[] values, Function<T, String> id,
			Function<String, Optional<T>> parse, SpecChange<T> change) {
		return literal(name).then(argument("value", StringArgumentType.word())
				.suggests((c, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(values).map(id), builder))
				.executes(c -> {
					String text = StringArgumentType.getString(c, "value").toLowerCase(Locale.ROOT);
					T value = parse.apply(text).orElseThrow(() -> UNKNOWN.create("Unknown " + name + " '" + text + "'. Use one of: "
							+ String.join(", ", Arrays.stream(values).map(id).toList())));
					return change(c, spec -> change.apply(spec, value));
				}));
	}

	private static int change(CommandContext<CommandSourceStack> c, Function<RoofSpec, RoofSpec> change) {
		RoofSession session = RoofService.session(c.getSource());
		session.spec = change.apply(session.spec);
		return refresh(c, "Roof: " + session.spec.describe());
	}

	/** After a settings change: preview again when something is selected, otherwise just confirm. */
	private static int refresh(CommandContext<CommandSourceStack> c, String message) {
		RoofSession session = RoofService.session(c.getSource());
		if (session.footprint != null && c.getSource().getPlayer() != null) {
			return act(c, () -> RoofService.preview(c.getSource()));
		}
		c.getSource().sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.AQUA), false);
		return 1;
	}

	private static int act(CommandContext<CommandSourceStack> c, Wand.Action action) {
		try {
			action.run();
			return 1;
		} catch (PlanException e) {
			c.getSource().sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int help(CommandContext<CommandSourceStack> c) {
		c.getSource().sendSuccess(() -> Component.literal(String.join("\n",
				"Roofwright: roofs from the tops of your walls.",
				"  /roof wand: get the wand (right-click a wall top to preview, sneak + right-click to build)",
				"  /roof detect [pos] or /roof select <from> <to> [add]: choose the building",
				"  /roof style gable|hip|dutch_gable|gambrel|mansard|pyramid|shed|flat|cone|dome",
				"  /roof pitch low|normal|steep, /roof overhang 0-3, /roof ridge auto|x|z, /roof shed <side>",
				"  /roof material <block> [slab full], /roof gable match|<block>, /roof ridgecap slab|full",
				"  /roof preview, /roof place [force], /roof undo, /roof redo, /roof cancel, /roof info")), false);
		return 1;
	}

	private static int wand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayer();
		if (player == null) {
			throw PLAYERS_ONLY.create();
		}
		if (!player.getInventory().add(Wand.create())) {
			throw INVENTORY_FULL.create();
		}
		c.getSource().sendSuccess(() -> Component.literal("Here is your Roofwright Wand. Right-click the top block of a wall.")
				.withStyle(ChatFormatting.GOLD), false);
		return 1;
	}

	private static BlockPos lookedAt(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayer();
		if (player == null) {
			throw NO_TARGET.create();
		}
		HitResult hit = player.pick(64, 1.0f, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return blockHit.getBlockPos();
		}
		throw NO_TARGET.create();
	}

	private static int detect(CommandContext<CommandSourceStack> c, BlockPos pos) {
		return act(c, () -> {
			Footprint footprint = RoofService.detect(c.getSource(), c.getSource().getLevel(), pos);
			if (c.getSource().getPlayer() != null) {
				RoofService.preview(c.getSource());
			} else {
				c.getSource().sendSuccess(() -> Component.literal("Selected " + footprint), false);
			}
		});
	}

	private static int select(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
		BlockPos from = BlockPosArgument.getLoadedBlockPos(c, "from");
		BlockPos to = BlockPosArgument.getLoadedBlockPos(c, "to");
		return act(c, () -> {
			Footprint footprint = RoofService.selectRectangle(c.getSource(), c.getSource().getLevel(), from, to, add);
			if (c.getSource().getPlayer() != null) {
				RoofService.preview(c.getSource());
			} else {
				c.getSource().sendSuccess(() -> Component.literal("Selected " + footprint), false);
			}
		});
	}

	private static int material(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		Block block = BlockStateArgument.getBlock(c, "block").getState().getBlock();
		return useMaterials(c, Materials.resolve(block));
	}

	private static int explicitMaterial(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		return useMaterials(c, Materials.explicit(BlockStateArgument.getBlock(c, "block").getState().getBlock(),
				BlockStateArgument.getBlock(c, "slab").getState().getBlock(), BlockStateArgument.getBlock(c, "full").getState().getBlock()));
	}

	private static int useMaterials(CommandContext<CommandSourceStack> c, Materials.Result result) throws CommandSyntaxException {
		if (result.materials() == null) {
			throw UNKNOWN.create(result.error());
		}
		RoofService.session(c.getSource()).materials = result.materials();
		Materials m = result.materials();
		return refresh(c, "Material: " + Materials.id(m.stairs()) + ", " + Materials.id(m.slab()) + ", " + Materials.id(m.full())
				+ (m.wall() != null ? ", " + Materials.id(m.wall()) : ""));
	}

	private static BlockState safeGable(BlockState state) throws CommandSyntaxException {
		String problem = Materials.problem(state, true);
		if (problem != null) {
			throw UNKNOWN.create("Gable walls: " + problem);
		}
		return state;
	}

	private static int gable(CommandContext<CommandSourceStack> c, BlockState state) {
		RoofService.session(c.getSource()).gableBlock = state;
		return refresh(c, state == null ? "Gable walls match the walls" : "Gable walls: " + Materials.id(state.getBlock()));
	}

	private static int cancel(CommandContext<CommandSourceStack> c) {
		RoofSession session = RoofService.session(c.getSource());
		ServerPlayer player = c.getSource().getPlayer();
		boolean hadPreview = session.preview != null;
		if (player != null) {
			Preview.clear(player, session);
		}
		PlacementJob job = Placements.activeFor(player == null ? new java.util.UUID(0, 0) : player.getUUID());
		if (job != null) {
			job.cancel();
		}
		String text = job != null ? "Stopped; the blocks already placed can be undone with /roof undo."
				: hadPreview ? "Preview cleared." : "Nothing to cancel.";
		c.getSource().sendSuccess(() -> Component.literal(text), false);
		return job != null || hadPreview ? 1 : 0;
	}

	private static int info(CommandContext<CommandSourceStack> c) {
		RoofSession session = RoofService.session(c.getSource());
		Materials m = session.materials;
		String text = "Roof: " + session.spec.describe()
				+ "\nMaterial: " + Materials.id(m.stairs()) + ", " + Materials.id(m.slab()) + ", " + Materials.id(m.full())
				+ "\nGable walls: " + (session.gableBlock == null ? "match the walls" : Materials.id(session.gableBlock.getBlock()))
				+ "\nSelected: " + (session.footprint == null ? "nothing" : session.footprint.toString())
				+ "\nUndo: " + session.history.undoCount() + ", redo: " + session.history.redoCount();
		c.getSource().sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> c) {
		String status = RoofwrightConfig.load();
		// Permission levels may have changed: resend command trees so tab completion matches.
		MinecraftServer server = c.getSource().getServer();
		server.getPlayerList().getPlayers().forEach(player -> server.getCommands().sendCommands(player));
		c.getSource().sendSuccess(() -> Component.literal(status), true);
		return 1;
	}
}
