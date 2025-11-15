package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

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
		
		Blockbase.LOGGER.debug("Tracked block place at {}: {}", pos, newState.getBlock().getName().getString());
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
		
		Blockbase.LOGGER.debug("Tracked block break at {}: {}", pos, oldState.getBlock().getName().getString());
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
		
		Blockbase.LOGGER.debug("Tracked block modify at {}: {} -> {}", 
			pos, oldState.getBlock().getName().getString(), newState.getBlock().getName().getString());
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
}

