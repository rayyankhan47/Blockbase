package com.blockbase;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Blockbase implements ModInitializer {
	public static final String MOD_ID = "blockbase";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// BlockTracker instance to track all block changes
	public static final BlockTracker blockTracker = new BlockTracker();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		
		// Register block event listeners
		registerBlockEvents();
		
		// Register commands
		registerCommands();
		
		// Register server lifecycle events for loading/saving changes
		registerServerEvents();
		
		LOGGER.info("Blockbase mod initialized - block tracking enabled");
	}

	private void registerBlockEvents() {
		// Register block break event
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!world.isClientSide()) { // Only track on server side
				blockTracker.trackBlockBreak(pos, state, world);
				
				// Get player's actual position (with decimal precision)
				Vec3 playerPos = player.position();
				
				LOGGER.info("Block broken at block ({}, {}, {}), player position ({}, {}, {}) by {}", 
					pos.getX(), 
					pos.getY(), 
					pos.getZ(),
					String.format("%.4f", playerPos.x),
					String.format("%.4f", playerPos.y),
					String.format("%.4f", playerPos.z),
					player.getName().getString());
			}
			return true; // Allow the break to proceed
		});

		// Block placement is tracked via BlockItemMixin which hooks into BlockItem.place()
		// This is more reliable than using UseItemCallback
		
		LOGGER.info("Block event listeners registered");
	}
	
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			BlockbaseCommands.register(dispatcher);
		});
		LOGGER.info("Blockbase commands registered");
	}
	
	private void registerServerEvents() {
		// Load changes when server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Load changes from the overworld
			Level overworld = server.getLevel(Level.OVERWORLD);
			if (overworld != null) {
				blockTracker.loadChanges(overworld);
			}
		});
		
		// Save changes periodically (every 15 seconds = 300 ticks)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 300 == 0) { // Every 15 seconds
				Level overworld = server.getLevel(Level.OVERWORLD);
				if (overworld != null && blockTracker.getChangeCount() > 0) {
					blockTracker.saveChanges(overworld);
				}
			}
		});
		
		// Save changes when server stops
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Level overworld = server.getLevel(Level.OVERWORLD);
			if (overworld != null && blockTracker.getChangeCount() > 0) {
				blockTracker.saveChanges(overworld);
			}
		});
		
		LOGGER.info("Server lifecycle events registered");
	}
}