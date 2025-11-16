package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks block changes in the world.
 * Thread-safe to handle both client and server-side changes.
 */
public class BlockTracker {
	private final List<BlockChange> changes;
	private boolean isTracking;

	public BlockTracker() {
		// Use CopyOnWriteArrayList for thread safety
		this.changes = new CopyOnWriteArrayList<>();
		this.isTracking = true;
	}

	/**
	 * Track when a block is placed.
	 * @param pos The position where the block was placed
	 * @param newState The new block state (the block that was placed)
	 * @param world The world (for getting timestamp)
	 */
	public void trackBlockPlace(BlockPos pos, BlockState newState, Level world) {
		if (!isTracking) return;
		
		long timestamp = world.getGameTime(); // Minecraft world time
		BlockChange change = new BlockChange(pos, null, newState, timestamp);
		changes.add(change);
		
		Blockbase.LOGGER.info("Tracked block place at ({}, {}, {}): {}", 
			pos.getX(), 
			pos.getY(), 
			pos.getZ(), 
			newState.getBlock().getName().getString());
	}

	/**
	 * Track when a block is broken.
	 * @param pos The position where the block was broken
	 * @param oldState The old block state (the block that was broken)
	 * @param world The world (for getting timestamp)
	 */
	public void trackBlockBreak(BlockPos pos, BlockState oldState, Level world) {
		if (!isTracking) return;
		
		long timestamp = world.getGameTime(); // Minecraft world time
		BlockChange change = new BlockChange(pos, oldState, null, timestamp);
		changes.add(change);
		
		Blockbase.LOGGER.info("Tracked block break at ({}, {}, {}): {}", 
			pos.getX(), 
			pos.getY(), 
			pos.getZ(), 
			oldState.getBlock().getName().getString());
	}

	/**
	 * Track when a block state is modified (e.g., redstone power level changes).
	 * @param pos The position of the modified block
	 * @param oldState The previous block state
	 * @param newState The new block state
	 * @param world The world (for getting timestamp)
	 */
	public void trackBlockModify(BlockPos pos, BlockState oldState, BlockState newState, Level world) {
		if (!isTracking) return;
		
		long timestamp = world.getGameTime();
		BlockChange change = new BlockChange(pos, oldState, newState, timestamp);
		changes.add(change);
		
		Blockbase.LOGGER.debug("Tracked block modify at ({}, {}, {}): {} -> {}", 
			pos.getX(), 
			pos.getY(), 
			pos.getZ(), 
			oldState.getBlock().getName().getString(), newState.getBlock().getName().getString());
	}

	/**
	 * Get all tracked changes.
	 * @return A copy of the list of all block changes
	 */
	public List<BlockChange> getChanges() {
		return new ArrayList<>(changes);
	}

	/**
	 * Clear all tracked changes.
	 */
	public void clearChanges() {
		changes.clear();
		Blockbase.LOGGER.debug("Cleared all tracked block changes");
	}

	/**
	 * Get the number of tracked changes.
	 * @return The count of changes
	 */
	public int getChangeCount() {
		return changes.size();
	}

	/**
	 * Enable or disable tracking.
	 * @param tracking True to enable tracking, false to disable
	 */
	public void setTracking(boolean tracking) {
		this.isTracking = tracking;
		Blockbase.LOGGER.debug("Block tracking {}", tracking ? "enabled" : "disabled");
	}

	/**
	 * Check if tracking is enabled.
	 * @return True if tracking is enabled
	 */
	public boolean isTracking() {
		return isTracking;
	}
	
	/**
	 * Save all tracked changes to a JSON file in the world folder.
	 * @param world The world (used to get the save directory)
	 */
	public void saveChanges(Level world) {
		if (world.isClientSide()) {
			Blockbase.LOGGER.warn("Cannot save changes on client side");
			return;
		}
		
		try {
			Path worldDir = getWorldDirectory(world);
			if (worldDir == null) {
				Blockbase.LOGGER.error("Could not determine world directory");
				return;
			}
			
			Path blockbaseDir = worldDir.resolve(".blockbase");
			Files.createDirectories(blockbaseDir);
			
			Path changesFile = blockbaseDir.resolve("changes.json");
			
			Registry<Block> blockRegistry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);
			
			StringBuilder json = new StringBuilder();
			json.append("[\n");
			
			for (int i = 0; i < changes.size(); i++) {
				BlockChange change = changes.get(i);
				json.append("  ").append(change.toJsonString(blockRegistry));
				if (i < changes.size() - 1) {
					json.append(",");
				}
				json.append("\n");
			}
			
			json.append("]");
			
			Files.writeString(changesFile, json.toString());
			Blockbase.LOGGER.info("Saved {} block changes to {}", changes.size(), changesFile);
			
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to save block changes", e);
		}
	}
	
	/**
	 * Load changes from a JSON file in the world folder.
	 * @param world The world (used to get the save directory)
	 */
	public void loadChanges(Level world) {
		if (world.isClientSide()) {
			Blockbase.LOGGER.warn("Cannot load changes on client side");
			return;
		}
		
		try {
			Path worldDir = getWorldDirectory(world);
			if (worldDir == null) {
				Blockbase.LOGGER.warn("Could not determine world directory, skipping load");
				return;
			}
			
			Path changesFile = worldDir.resolve(".blockbase").resolve("changes.json");
			
			if (!Files.exists(changesFile)) {
				Blockbase.LOGGER.debug("No changes file found at {}, starting fresh", changesFile);
				return;
			}
			
			String jsonContent = Files.readString(changesFile);
			
			// Parse JSON array (simple parser for MVP)
			// Remove outer brackets and split by }, (with lookahead for comma)
			String trimmed = jsonContent.trim();
			if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
				Blockbase.LOGGER.error("Invalid JSON format in changes file");
				return;
			}
			
			trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
			
			if (trimmed.isEmpty()) {
				Blockbase.LOGGER.debug("Changes file is empty");
				return;
			}
			
			Registry<Block> blockRegistry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);
			
			// Split by }, but keep the closing brace
			String[] changeStrings = trimmed.split("\\},\\s*");
			
			changes.clear();
			int loaded = 0;
			
			for (String changeStr : changeStrings) {
				// Add back the closing brace if it was removed
				if (!changeStr.trim().endsWith("}")) {
					changeStr = changeStr.trim() + "}";
				}
				
				// Remove leading whitespace and newlines
				changeStr = changeStr.trim();
				
				BlockChange change = BlockChange.fromJsonString(changeStr, blockRegistry);
				if (change != null) {
					changes.add(change);
					loaded++;
				}
			}
			
			Blockbase.LOGGER.info("Loaded {} block changes from {}", loaded, changesFile);
			
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to load block changes", e);
		}
	}
	
	/**
	 * Get the world save directory path.
	 * @param world The world
	 * @return The path to the world save directory, or null if not available
	 */
	private Path getWorldDirectory(Level world) {
		if (world.getServer() == null) {
			return null;
		}
		
		MinecraftServer server = world.getServer();
		Path worldPath = server.getWorldPath(LevelResource.ROOT);
		return worldPath;
	}
}

