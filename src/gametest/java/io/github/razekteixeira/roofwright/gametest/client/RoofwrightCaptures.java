package io.github.razekteixeira.roofwright.gametest.client;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.razekteixeira.roofwright.Placements;
import io.github.razekteixeira.roofwright.RoofService;
import io.github.razekteixeira.roofwright.RoofwrightConfig;
import io.github.razekteixeira.roofwright.core.Footprint;
import io.github.razekteixeira.roofwright.core.Settings;

/**
 * Builds a small village of bare-walled houses in a real client and captures the media used on the
 * project page: {@code before} and {@code after} shots of the village, close-ups, a night shot, the same
 * house in six styles ({@code style_*}), the ghost preview, the chat summary and the {@code grow_NNN}
 * frames of a roof going up across ticks.
 *
 * <p>Run with {@code ./gradlew runClientGameTest}; screenshots land in {@code build/run/clientGameTest/screenshots}.
 * Every roof is real mod output from {@code /roof} commands or the same calls the wand makes.
 */
public class RoofwrightCaptures implements FabricClientGameTest {
	private static final int GROW_FRAMES_MAX = 120;

	/** A house: its outline (relative to the village origin), wall height, wall block, and its roof. */
	private record House(Footprint outline, int height, String wall, String style, String material, String extra) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> client.options.guiScale().set(3));

		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(settings -> {
					settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
					settings.setAllowCommands(true);
				})
				.create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			BlockPos o = feet.offset(0, 0, 10);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("time set 11800");

			List<House> village = village();
			ground(server, o);
			for (House house : village) {
				walls(server, o, house);
			}
			decorate(server, o);
			BlockPos showcase = o.offset(64, 0, 0);
			House show = showcase();
			walls(server, showcase, show);
			context.waitTicks(40);

			// Spectators render no hand or hotbar.
			server.runCommand("gamerule show_death_messages false");
			server.runCommand("gamemode spectator @p");

			BlockPos eye = o.offset(0, 12, -8);
			BlockPos target = o.offset(0, 3, 16);
			cinematic(context, world, server, eye, target, "before");

			for (House house : village) {
				roof(server, o, house);
			}
			cinematic(context, world, server, eye, target, "after");
			cinematic(context, world, server, o.offset(-30, 9, 6), o.offset(-14, 4, 22), "gallery_barn");
			cinematic(context, world, server, o.offset(10, 8, 6), o.offset(-3, 6, 24), "gallery_tower");
			cinematic(context, world, server, o.offset(-16, 9, -9), o.offset(-4, 4, 8), "gallery_valleys");
			server.runCommand("time set 13600");
			cinematic(context, world, server, o.offset(14, 12, -16), o.offset(-2, 3, 16), "gallery_dusk");
			server.runCommand("time set 11800");

			// The same house in six styles, same camera.
			BlockPos showEye = showcase.offset(-3, 8, -7);
			BlockPos showTarget = showcase.offset(5, 2, 4);
			String[][] styles = {
					{"gable", "spruce_planks", "normal"},
					{"hip", "deepslate_tiles", "normal"},
					{"dutch_gable", "dark_oak_planks", "normal"},
					{"gambrel", "bricks", "normal"},
					{"mansard", "polished_blackstone_bricks", "normal"},
					{"flat", "stone_bricks", "normal"}};
			for (String[] style : styles) {
				server.runCommand("roof style " + style[0]);
				server.runCommand("roof material minecraft:" + style[1]);
				server.runCommand("roof pitch " + style[2]);
				detectAndPlace(server, showcase.offset(0, show.height() - 1, 0));
				cinematic(context, world, server, showEye, showTarget, "style_" + style[0]);
				server.runCommand("roof undo");
				waitForPlacement(server);
			}

			// Chat summary of a preview, typed by the player (creative: spectators may not build, so
			// every block would show as protected). make_media.sh crops the chat, leaving out hand and hotbar.
			server.runCommand("roof pitch normal");
			server.runCommand("gamemode creative @p");
			server.runCommand("tp @p %d %d %d".formatted(showEye.getX(), showEye.getY(), showEye.getZ()));
			fly(server);
			context.waitTicks(10);
			context.getInput().lookAt(showTarget);
			world.getConnection().waitForChunksRender();
			clearChat(context);
			typeCommand(context, world, "roof style hip");
			clearChat(context);
			BlockPos wallTop = showcase.offset(0, show.height() - 1, 0);
			typeCommand(context, world, "roof detect %d %d %d".formatted(wallTop.getX(), wallTop.getY(), wallTop.getZ()));
			context.waitTicks(10);
			context.takeScreenshot("chat");
			server.runCommand("say ready");

			// Ghost preview with one block in the way (red), HUD hidden.
			BlockPos inTheWay = showcase.offset(3, show.height(), 0);
			server.runCommand("setblock %d %d %d minecraft:oak_leaves[persistent=true]".formatted(inTheWay.getX(), inTheWay.getY(), inTheWay.getZ()));
			clearChat(context);
			typeCommand(context, world, "roof style dutch_gable");
			clearChat(context);
			hideGui(context, true);
			context.waitTicks(10);
			context.takeScreenshot("preview");
			hideGui(context, false);

			// A roof going up, a few blocks per tick, from the same calls the wand makes.
			server.runCommand("setblock %d %d %d minecraft:air".formatted(inTheWay.getX(), inTheWay.getY(), inTheWay.getZ()));
			server.runOnServer(s -> RoofwrightConfig.set(RoofwrightConfig.get().withBlocksPerTick(4)));
			hideGui(context, true);
			typeCommandHidden(context, world, server, "roof style gable");
			for (int frame = 0; frame < 6; frame++) {
				context.waitTicks(2);
				context.takeScreenshot("grow_%03d".formatted(frame));
			}
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				CommandSourceStack source = player.createCommandSourceStack();
				try {
					RoofService.place(source, false);
				} catch (io.github.razekteixeira.roofwright.core.PlanException e) {
					throw new IllegalStateException("capture roof refused: " + e.getMessage(), e);
				}
			});
			for (int frame = 6; frame < GROW_FRAMES_MAX; frame++) {
				context.waitTicks(2);
				context.takeScreenshot("grow_%03d".formatted(frame));
				boolean done = server.computeOnServer(s -> Placements.active().isEmpty());
				if (done) {
					break;
				}
			}
			for (int frame = 0; frame < 8; frame++) {
				context.waitTicks(2);
				context.takeScreenshot("grow_end_%03d".formatted(frame));
			}
			hideGui(context, false);
			server.runOnServer(s -> RoofwrightConfig.set(Settings.DEFAULTS));
		}
	}

	// --- the village ----------------------------------------------------------------------------------

	private static List<House> village() {
		Footprint.Builder ell = new Footprint.Builder(0);
		for (int x = -8; x <= 2; x++) {
			for (int z = 4; z <= 12; z++) {
				if (!(x >= -2 && z <= 7)) {
					ell.add(x, z);
				}
			}
		}
		Footprint tee = new Footprint.Builder(0).addRectangle(6, 4, 18, 8).addRectangle(10, 9, 14, 14).build();
		Footprint.Builder tower = new Footprint.Builder(0);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				if (x * x + z * z <= 18) {
					tower.add(x - 3, z + 24);
				}
			}
		}
		return List.of(
				new House(Footprint.rectangle(-20, 6, -12, 12, 0), 4, "minecraft:spruce_planks", "gable", "spruce_planks", ""),
				new House(ell.build(), 4, "minecraft:stone_bricks", "hip", "deepslate_tiles", ""),
				new House(tee, 4, "minecraft:white_terracotta", "dutch_gable", "dark_oak_planks", ""),
				new House(Footprint.rectangle(-22, 18, -12, 32, 0), 5, "minecraft:red_terracotta", "gambrel", "dark_oak_planks", ""),
				new House(tower.build(), 8, "minecraft:cobblestone", "cone", "deepslate_tiles", "roof pitch steep"),
				new House(Footprint.rectangle(4, 18, 14, 28, 0), 5, "minecraft:bricks", "mansard", "polished_blackstone_bricks", ""),
				new House(Footprint.rectangle(18, 18, 24, 24, 0), 4, "minecraft:stone_bricks", "flat", "stone_bricks", ""));
	}

	/** The L-shaped showcase house, relative to its own origin. */
	private static House showcase() {
		Footprint.Builder ell = new Footprint.Builder(0);
		for (int x = 0; x <= 10; x++) {
			for (int z = 0; z <= 8; z++) {
				if (!(x >= 6 && z <= 3)) {
					ell.add(x, z);
				}
			}
		}
		return new House(ell.build(), 4, "minecraft:stripped_spruce_log", "", "", "");
	}

	private static void ground(TestServerContext server, BlockPos o) {
		int y = o.getY() - 1;
		// Grass field and a gravel-free dirt path along the front of the houses.
		server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(o.getX() - 40, y, o.getZ() - 30, o.getX() + 90, y, o.getZ() + 45));
		server.runCommand("fill %d %d %d %d %d %d minecraft:dirt_path".formatted(o.getX() - 26, y, o.getZ() + 14, o.getX() + 28, y, o.getZ() + 16));
		server.runCommand("fill %d %d %d %d %d %d minecraft:dirt_path".formatted(o.getX() + 0, y, o.getZ() - 6, o.getX() + 2, y, o.getZ() + 3));
	}

	private static void walls(TestServerContext server, BlockPos o, House house) {
		Footprint f = house.outline();
		int y0 = o.getY();
		int top = y0 + house.height() - 1;
		int edgeIndex = 0;
		for (int x = f.minX(); x <= f.maxX(); x++) {
			for (int z = f.minZ(); z <= f.maxZ(); z++) {
				if (!f.isEdge(x, z)) {
					continue;
				}
				int wx = o.getX() + x;
				int wz = o.getZ() + z;
				server.runCommand("fill %d %d %d %d %d %d %s".formatted(wx, y0, wz, wx, top, wz, house.wall()));
				boolean corner = isCorner(f, x, z);
				if (!corner && house.height() >= 4 && (edgeIndex++ % 3 == 1)) {
					server.runCommand("setblock %d %d %d minecraft:glass_pane".formatted(wx, y0 + 1, wz));
					server.runCommand("setblock %d %d %d minecraft:glass_pane".formatted(wx, y0 + 2, wz));
				}
			}
		}
		// Light inside, for the dusk shot.
		BlockPos centre = new BlockPos(o.getX() + (f.minX() + f.maxX()) / 2, y0, o.getZ() + (f.minZ() + f.maxZ()) / 2);
		if (f.contains(centre.getX() - o.getX(), centre.getZ() - o.getZ())) {
			server.runCommand("setblock %d %d %d minecraft:lantern".formatted(centre.getX(), y0, centre.getZ()));
		}
	}

	private static boolean isCorner(Footprint f, int x, int z) {
		boolean alongX = f.contains(x - 1, z) && f.contains(x + 1, z);
		boolean alongZ = f.contains(x, z - 1) && f.contains(x, z + 1);
		return !alongX && !alongZ;
	}

	private static void decorate(TestServerContext server, BlockPos o) {
		int y = o.getY();
		int[][] trees = {{-30, 0}, {-28, 20}, {30, 2}, {32, 30}, {-6, 38}, {20, 38}, {-34, 36}, {8, -12}, {-14, -10}};
		for (int[] tree : trees) {
			server.runCommand("place feature minecraft:%s %d %d %d".formatted(tree[0] % 2 == 0 ? "oak" : "birch", o.getX() + tree[0], y, o.getZ() + tree[1]));
		}
		for (int x = -26; x <= 28; x += 6) {
			server.runCommand("setblock %d %d %d minecraft:oak_fence".formatted(o.getX() + x, y, o.getZ() + 13));
			server.runCommand("setblock %d %d %d minecraft:lantern".formatted(o.getX() + x, y + 1, o.getZ() + 13));
		}
	}

	/** Roofs one village house through the console, as an operator would. */
	private static void roof(TestServerContext server, BlockPos o, House house) {
		server.runCommand("roof style " + house.style());
		server.runCommand("roof material minecraft:" + house.material());
		server.runCommand("roof pitch normal");
		if (!house.extra().isEmpty()) {
			server.runCommand(house.extra());
		}
		Footprint f = house.outline();
		int x = f.minX();
		int z = f.minZ();
		while (!f.contains(x, z)) {
			x++;
		}
		detectAndPlace(server, o.offset(x, house.height() - 1, z));
	}

	private static void detectAndPlace(TestServerContext server, BlockPos wallTop) {
		server.runCommand("roof detect %d %d %d".formatted(wallTop.getX(), wallTop.getY(), wallTop.getZ()));
		server.runCommand("roof place");
		waitForPlacement(server);
	}

	private static void waitForPlacement(TestServerContext server) {
		server.waitFor(s -> Placements.active().isEmpty(), 20 * 60);
	}

	// --- camera and input ----------------------------------------------------------------------------

	/** A HUD-free shot from {@code eye} towards {@code target}. */
	private static void cinematic(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server,
			BlockPos eye, BlockPos target, String name) {
		// Locale.ROOT: a decimal comma (pt, de, ...) would make /tp fail silently.
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f", eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5));
		context.waitTicks(10);
		context.getInput().lookAt(target);
		world.getConnection().waitForChunksRender();
		hideGui(context, true);
		context.waitTicks(20);
		context.takeScreenshot(name);
		hideGui(context, false);
	}

	/** Keeps the creative player hovering where it was teleported. */
	private static void fly(TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
	}

	/** F1 toggles the HUD; calls come in pairs (hide, then show). */
	private static void hideGui(ClientGameTestContext context, boolean hidden) {
		context.getInput().pressKey(options -> options.keyToggleGui);
		context.waitTicks(1);
	}

	private static void typeCommand(ClientGameTestContext context, TestSingleplayerContext world, String command) {
		context.getInput().pressKey(options -> options.keyChat);
		context.getInput().typeChars("/" + command);
		context.getInput().holdKeyFor(InputConstants.KEY_RETURN, 0);
		world.getConnection().waitForServerboundPackets();
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	/** Runs a command as the player without opening chat (for shots where the chat box must not show). */
	private static void typeCommandHidden(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, String command) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			s.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
		});
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static void clearChat(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
	}
}
