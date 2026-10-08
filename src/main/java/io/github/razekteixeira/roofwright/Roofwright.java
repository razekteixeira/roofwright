package io.github.razekteixeira.roofwright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class Roofwright implements ModInitializer {
	public static final String MOD_ID = "roofwright";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info(RoofwrightConfig.load());
		CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> RoofwrightCommands.register(dispatcher, context));
		Wand.register();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Placements.onServerTick(server);
			if (server.getTickCount() % 20 == 0) {
				Preview.expire(server);
			}
		});
		// The client drops entities when it changes dimension, so a preview there is simply forgotten.
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
				RoofService.session(player.getUUID()).preview = null);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RoofService.session(handler.getPlayer().getUUID()).preview = null);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			Placements.clear();
			RoofService.clearAll();
		});
		LOGGER.info("Roofwright ready: /roof (alias /roofwright), /roof wand");
	}
}
