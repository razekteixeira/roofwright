package io.github.razekteixeira.roofwright;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import io.github.razekteixeira.roofwright.core.PlanException;
import io.github.razekteixeira.roofwright.core.RoofStyle;

/**
 * The wand: a vanilla stick with a {@code custom_data} marker, a name, lore, the enchantment glint and the
 * blaze rod's look, so vanilla clients show it properly. Holding one grants nothing: every click checks
 * the {@code roofwright.use} permission like the commands do. Clicks within {@code wandCooldownTicks} of
 * the last one are consumed and ignored, so a modified client cannot flood the server with detections.
 *
 * <ul>
 * <li>Right-click the top of a wall: detect the building and preview its roof.</li>
 * <li>Sneak + right-click: build the previewed roof.</li>
 * <li>Left-click a block: next roof style (sneak: next pitch), and preview again.</li>
 * </ul>
 */
public final class Wand {
	private static final String MARKER = "roofwright";

	private Wand() {
	}

	public static ItemStack create() {
		ItemStack stack = new ItemStack(Items.STICK);
		CompoundTag tag = new CompoundTag();
		tag.putString(MARKER, "wand");
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.ITEM_NAME, Component.literal("Roofwright Wand").withStyle(ChatFormatting.GOLD));
		stack.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("blaze_rod"));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				lore("Right-click a wall top: preview a roof"),
				lore("Sneak + right-click: build it"),
				lore("Left-click: next style (sneak: next pitch)"),
				lore("/roof help for everything else"))));
		return stack;
	}

	private static Component lore(String text) {
		return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	public static boolean is(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && "wand".equals(data.copyTag().getStringOr(MARKER, ""));
	}

	static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !is(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
				CommandSourceStack source = serverPlayer.createCommandSourceStack();
				if (!RoofwrightCommands.canUse(source)) {
					return InteractionResult.PASS;
				}
				if (!ready(serverPlayer)) {
					return InteractionResult.SUCCESS;
				}
				run(source, () -> {
					if (serverPlayer.isShiftKeyDown()) {
						RoofService.place(source, false);
					} else {
						RoofService.detect(source, serverLevel, hit.getBlockPos());
						RoofService.preview(source);
					}
				});
			}
			return InteractionResult.SUCCESS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand != InteractionHand.MAIN_HAND || !is(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				CommandSourceStack source = serverPlayer.createCommandSourceStack();
				if (!RoofwrightCommands.canUse(source)) {
					return InteractionResult.PASS;
				}
				if (!ready(serverPlayer)) {
					return InteractionResult.SUCCESS;
				}
				if (serverPlayer.isShiftKeyDown()) {
					run(source, () -> RoofService.place(source, false));
				} else {
					source.sendSuccess(() -> Component.literal("Right-click the top block of a wall to preview a roof.")
							.withStyle(ChatFormatting.GRAY), false);
				}
			}
			return InteractionResult.SUCCESS;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (hand != InteractionHand.MAIN_HAND || !is(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				CommandSourceStack source = serverPlayer.createCommandSourceStack();
				if (!RoofwrightCommands.canUse(source)) {
					return InteractionResult.PASS;
				}
				if (!ready(serverPlayer)) {
					return InteractionResult.SUCCESS;
				}
				RoofSession session = RoofService.session(source);
				if (serverPlayer.isShiftKeyDown()) {
					session.spec = session.spec.withPitch(next(session.spec.pitch()));
				} else {
					session.spec = session.spec.withStyle(next(session.spec.style()));
				}
				if (session.footprint != null) {
					run(source, () -> RoofService.preview(source));
				} else {
					source.sendSuccess(() -> Component.literal("Roof: " + session.spec.describe()).withStyle(ChatFormatting.AQUA), false);
				}
			}
			// Never break the block with the wand.
			return InteractionResult.SUCCESS;
		});
	}

	/**
	 * Whether a click by this player may act now; starts the cooldown when it may. Callers check the
	 * permission first, so a player without it never touches a session.
	 */
	static boolean ready(ServerPlayer player) {
		RoofSession session = RoofService.session(player.getUUID());
		long now = player.level().getServer().getTickCount();
		if (now < session.wandReadyAt) {
			return false;
		}
		session.wandReadyAt = now + RoofwrightConfig.get().wandCooldownTicks();
		return true;
	}

	private static <E extends Enum<E>> E next(E value) {
		E[] values = value.getDeclaringClass().getEnumConstants();
		return values[(value.ordinal() + 1) % values.length];
	}

	@FunctionalInterface
	interface Action {
		void run() throws PlanException;
	}

	/** Runs an action, turning a {@link PlanException} into a red chat line. */
	static void run(CommandSourceStack source, Action action) {
		try {
			action.run();
		} catch (PlanException e) {
			source.sendFailure(Component.literal(e.getMessage()));
		}
	}

	static RoofStyle nextStyle(RoofStyle style) {
		return next(style);
	}
}
