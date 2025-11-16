package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

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
	
	/**
	 * Serialize to a simple JSON-like string representation.
	 * Format: {"x":1,"y":2,"z":3,"oldState":"minecraft:stone","newState":"minecraft:dirt","timestamp":123,"type":"MODIFIED"}
	 */
	public String toJsonString(Registry<Block> blockRegistry) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"x\":").append(position.getX()).append(",");
		json.append("\"y\":").append(position.getY()).append(",");
		json.append("\"z\":").append(position.getZ()).append(",");
		
		if (oldState != null) {
			ResourceLocation oldId = blockRegistry.getKey(oldState.getBlock());
			json.append("\"oldState\":\"").append(oldId != null ? oldId.toString() : "unknown").append("\",");
		} else {
			json.append("\"oldState\":null,");
		}
		
		if (newState != null) {
			ResourceLocation newId = blockRegistry.getKey(newState.getBlock());
			json.append("\"newState\":\"").append(newId != null ? newId.toString() : "unknown").append("\",");
		} else {
			json.append("\"newState\":null,");
		}
		
		json.append("\"timestamp\":").append(timestamp).append(",");
		json.append("\"type\":\"").append(type.name()).append("\"");
		json.append("}");
		return json.toString();
	}
	
	/**
	 * Create a BlockChange from JSON string.
	 * Note: This is a simplified parser - for production, use a proper JSON library.
	 */
	public static BlockChange fromJsonString(String json, Registry<Block> blockRegistry) {
		// Simple JSON parsing (for MVP - in production, use Gson or Jackson)
		// This is a basic implementation - assumes well-formed JSON
		try {
			int x = extractInt(json, "\"x\":");
			int y = extractInt(json, "\"y\":");
			int z = extractInt(json, "\"z\":");
			long timestamp = extractLong(json, "\"timestamp\":");
			String typeStr = extractString(json, "\"type\":\"");
			
			BlockPos pos = new BlockPos(x, y, z);
			
			String oldStateStr = extractStringOrNull(json, "\"oldState\":");
			String newStateStr = extractStringOrNull(json, "\"newState\":");
			
			BlockState oldState = null;
			BlockState newState = null;
			
			if (oldStateStr != null && !oldStateStr.equals("null")) {
				ResourceLocation oldId = ResourceLocation.tryParse(oldStateStr);
				if (oldId != null) {
					Block oldBlock = blockRegistry.get(oldId);
					if (oldBlock != null) {
						oldState = oldBlock.defaultBlockState();
					}
				}
			}
			
			if (newStateStr != null && !newStateStr.equals("null")) {
				ResourceLocation newId = ResourceLocation.tryParse(newStateStr);
				if (newId != null) {
					Block newBlock = blockRegistry.get(newId);
					if (newBlock != null) {
						newState = newBlock.defaultBlockState();
					}
				}
			}
			
			return new BlockChange(pos, oldState, newState, timestamp);
		} catch (Exception e) {
			Blockbase.LOGGER.error("Failed to parse BlockChange from JSON: {}", json, e);
			return null;
		}
	}
	
	private static int extractInt(String json, String key) {
		int start = json.indexOf(key) + key.length();
		int end = json.indexOf(",", start);
		if (end == -1) end = json.indexOf("}", start);
		return Integer.parseInt(json.substring(start, end).trim());
	}
	
	private static long extractLong(String json, String key) {
		int start = json.indexOf(key) + key.length();
		int end = json.indexOf(",", start);
		if (end == -1) end = json.indexOf("}", start);
		return Long.parseLong(json.substring(start, end).trim());
	}
	
	private static String extractString(String json, String key) {
		int start = json.indexOf(key) + key.length();
		int end = json.indexOf("\"", start);
		return json.substring(start, end);
	}
	
	private static String extractStringOrNull(String json, String key) {
		int start = json.indexOf(key) + key.length();
		if (json.charAt(start) == '"') {
			int end = json.indexOf("\"", start + 1);
			return json.substring(start + 1, end);
		} else {
			// It's null
			int end = json.indexOf(",", start);
			if (end == -1) end = json.indexOf("}", start);
			return json.substring(start, end).trim();
		}
	}
}

