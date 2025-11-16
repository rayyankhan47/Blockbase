package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

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
	 * Format:
	 * {
	 *   "x":1,"y":2,"z":3,
	 *   "oldStateId":"minecraft:stone","oldProps":{"facing":"north","powered":"true"},
	 *   "newStateId":"minecraft:dirt","newProps":{...},
	 *   "timestamp":123,"type":"MODIFIED"
	 * }
	 */
	public String toJsonString(Registry<Block> blockRegistry) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"x\":").append(position.getX()).append(",");
		json.append("\"y\":").append(position.getY()).append(",");
		json.append("\"z\":").append(position.getZ()).append(",");
		
		if (oldState != null) {
			ResourceLocation oldId = blockRegistry.getKey(oldState.getBlock());
			json.append("\"oldStateId\":\"").append(oldId != null ? oldId.toString() : "unknown").append("\",");
			json.append("\"oldProps\":").append(serializeProperties(oldState)).append(",");
		} else {
			json.append("\"oldStateId\":null,");
			json.append("\"oldProps\":null,");
		}
		
		if (newState != null) {
			ResourceLocation newId = blockRegistry.getKey(newState.getBlock());
			json.append("\"newStateId\":\"").append(newId != null ? newId.toString() : "unknown").append("\",");
			json.append("\"newProps\":").append(serializeProperties(newState)).append(",");
		} else {
			json.append("\"newStateId\":null,");
			json.append("\"newProps\":null,");
		}
		
		json.append("\"timestamp\":").append(timestamp).append(",");
		json.append("\"type\":\"").append(type.name()).append("\"");
		json.append("}");
		return json.toString();
	}

	private static String serializeProperties(BlockState state) {
		if (state == null) return "null";
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean first = true;
		for (Property<?> prop : state.getProperties()) {
			String name = prop.getName();
			String value = getPropertyValueAsString(state, prop);
			if (!first) sb.append(",");
			sb.append("\"").append(escape(name)).append("\":\"").append(escape(value)).append("\"");
			first = false;
		}
		sb.append("}");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String getPropertyValueAsString(BlockState state, Property<T> prop) {
		T val = state.getValue(prop);
		return val.toString();
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
			
			// Support both new keys (...StateId) and legacy keys (...State)
			String oldStateId = extractStringOrNull(json, "\"oldStateId\":");
			if (oldStateId == null) oldStateId = extractStringOrNull(json, "\"oldState\":"); // legacy
			String newStateId = extractStringOrNull(json, "\"newStateId\":");
			if (newStateId == null) newStateId = extractStringOrNull(json, "\"newState\":"); // legacy
			String oldPropsStr = extractObjectOrNull(json, "\"oldProps\":");
			String newPropsStr = extractObjectOrNull(json, "\"newProps\":");
			
			BlockState oldState = null;
			BlockState newState = null;
			
			if (oldStateId != null && !oldStateId.equals("null")) {
				ResourceLocation oldId = ResourceLocation.tryParse(oldStateId);
				if (oldId != null) {
					Block oldBlock = blockRegistry.get(oldId);
					if (oldBlock != null) {
						oldState = applyProperties(oldBlock.defaultBlockState(), oldPropsStr);
					}
				}
			}
			
			if (newStateId != null && !newStateId.equals("null")) {
				ResourceLocation newId = ResourceLocation.tryParse(newStateId);
				if (newId != null) {
					Block newBlock = blockRegistry.get(newId);
					if (newBlock != null) {
						newState = applyProperties(newBlock.defaultBlockState(), newPropsStr);
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
		if (start < 0 || start >= json.length()) return null;
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

	private static String extractObjectOrNull(String json, String key) {
		int idx = json.indexOf(key);
		if (idx == -1) return null;
		int start = idx + key.length();
		if (start < json.length() && json.charAt(start) == 'n') { // null
			return "null";
		}
		int objStart = json.indexOf("{", start);
		if (objStart == -1) return null;
		int depth = 0;
		for (int i = objStart; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '{') depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0) {
					return json.substring(objStart, i + 1);
				}
			}
		}
		return null;
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static BlockState applyProperties(BlockState base, String propsJson) {
		if (base == null || propsJson == null || propsJson.equals("null")) return base;
		for (Property<?> prop : base.getProperties()) {
			String key = "\"" + prop.getName() + "\":\"";
			int idx = propsJson.indexOf(key);
			if (idx == -1) continue;
			int start = idx + key.length();
			int end = propsJson.indexOf("\"", start);
			if (end == -1) continue;
			String valueStr = propsJson.substring(start, end);
			base = setPropertyFromString(base, prop, valueStr);
		}
		return base;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState setPropertyFromString(BlockState state, Property<T> prop, String valueStr) {
		for (T allowed : prop.getPossibleValues()) {
			if (allowed.toString().equals(valueStr)) {
				return state.setValue(prop, allowed);
			}
		}
		return state;
	}
}

