package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a single block change (placement, break, or modification).
 */
public class BlockChange {
	private final BlockPos position;
	private final BlockState oldState;  // null if block was placed (didn't exist before)
	private final BlockState newState;  // null if block was broken (doesn't exist now)
	private final long timestamp;
	private final ChangeType type;

	public enum ChangeType {
		PLACED,    // Block was placed (oldState is null)
		BROKEN,    // Block was broken (newState is null)
		MODIFIED   // Block state changed (both states exist)
	}

	public BlockChange(BlockPos position, BlockState oldState, BlockState newState, long timestamp) {
		this.position = position;
		this.oldState = oldState;
		this.newState = newState;
		this.timestamp = timestamp;
		
		// Determine change type
		if (oldState == null && newState != null) {
			this.type = ChangeType.PLACED;
		} else if (oldState != null && newState == null) {
			this.type = ChangeType.BROKEN;
		} else {
			this.type = ChangeType.MODIFIED;
		}
	}

	public BlockPos getPosition() {
		return position;
	}

	public BlockState getOldState() {
		return oldState;
	}

	public BlockState getNewState() {
		return newState;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public ChangeType getType() {
		return type;
	}

	@Override
	public String toString() {
		return String.format("BlockChange{pos=%s, type=%s, timestamp=%d}", 
			position, type, timestamp);
	}
}

