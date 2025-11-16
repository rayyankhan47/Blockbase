package com.blockbase;

import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single commit in a Blockbase repository.
 *
 * For the MVP:
 * - Single branch ("main")
 * - Commits are stored as JSON files in .blockbase/commits/<commitId>.json
 */
public class Commit {

	private final String id;               // Commit ID (SHA-1 hash)
	private final String message;          // Commit message
	private final String author;           // Author name (for now, simple string)
	private final long timestamp;          // When the commit was created (ms since epoch)
	private final String parentId;         // Previous commit ID (null if initial commit)
	private final List<BlockChange> changes; // List of block changes included in this commit

	public Commit(String id,
				  String message,
				  String author,
				  long timestamp,
				  String parentId,
				  List<BlockChange> changes) {
		this.id = id;
		this.message = message;
		this.author = author;
		this.timestamp = timestamp;
		this.parentId = parentId;
		this.changes = new ArrayList<>(changes);
	}

	public String getId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public String getAuthor() {
		return author;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getParentId() {
		return parentId;
	}

	public List<BlockChange> getChanges() {
		return Collections.unmodifiableList(changes);
	}

	/**
	 * Create a new Commit from the given data and compute its SHA-1 ID.
	 *
	 * @param message   Commit message
	 * @param author    Author name
	 * @param parentId  Parent commit ID (null for initial commit)
	 * @param changes   List of BlockChange objects to include
	 * @param world     World used for block registry (for stable serialization of changes)
	 * @return Commit with computed ID
	 */
	public static Commit create(String message,
								String author,
								String parentId,
								List<BlockChange> changes,
								Level world) {
		long timestamp = System.currentTimeMillis();

		Registry<Block> blockRegistry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);

		// Build a canonical string representation for hashing
		StringBuilder sb = new StringBuilder();
		sb.append("message=").append(message).append("\n");
		sb.append("author=").append(author).append("\n");
		sb.append("timestamp=").append(timestamp).append("\n");
		sb.append("parent=").append(parentId == null ? "null" : parentId).append("\n");
		sb.append("changes=[\n");
		for (BlockChange change : changes) {
			sb.append("  ").append(change.toJsonString(blockRegistry)).append("\n");
		}
		sb.append("]\n");

		String id = sha1Hex(sb.toString());

		return new Commit(id, message, author, timestamp, parentId, changes);
	}

	/**
	 * Serialize this commit to JSON string.
	 */
	public String toJson(Level world) {
		Registry<Block> blockRegistry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"id\":\"").append(escape(id)).append("\",");
		sb.append("\"message\":\"").append(escape(message)).append("\",");
		sb.append("\"author\":\"").append(escape(author)).append("\",");
		sb.append("\"timestamp\":").append(timestamp).append(",");
		sb.append("\"parentId\":").append(parentId == null ? "null" : "\"" + escape(parentId) + "\"").append(",");
		sb.append("\"changes\":[");

		for (int i = 0; i < changes.size(); i++) {
			BlockChange change = changes.get(i);
			sb.append(change.toJsonString(blockRegistry));
			if (i < changes.size() - 1) {
				sb.append(",");
			}
		}

		sb.append("]");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Parse a Commit from its JSON string (as we serialize it), using the block registry
	 * to reconstruct BlockChange states.
	 */
	public static Commit fromJson(String json, Registry<Block> blockRegistry) {
		try {
			String id = extractString(json, "\"id\":\"");
			String message = unescapeJson(extractString(json, "\"message\":\""));
			String author = extractString(json, "\"author\":\"");
			long timestamp = extractLong(json, "\"timestamp\":");
			String parentId = extractNullableString(json, "\"parentId\":");

			// Parse changes array
			int idx = json.indexOf("\"changes\":[");
			List<BlockChange> changes = new ArrayList<>();
			if (idx != -1) {
				int start = idx + "\"changes\":".length();
				int bracket = json.indexOf("[", start);
				if (bracket != -1) {
					int depth = 0;
					int end = -1;
					for (int i = bracket; i < json.length(); i++) {
						char c = json.charAt(i);
						if (c == '[') depth++;
						else if (c == ']') {
							depth--;
							if (depth == 0) { end = i; break; }
						}
					}
					if (end != -1) {
						String inner = json.substring(bracket + 1, end).trim();
						if (!inner.isEmpty()) {
							for (String obj : splitTopLevelJsonObjects(inner)) {
								BlockChange ch = BlockChange.fromJsonString(obj, blockRegistry);
								if (ch != null) changes.add(ch);
							}
						}
					}
				}
			}
			return new Commit(id, message, author, timestamp, parentId, changes);
		} catch (Exception e) {
			Blockbase.LOGGER.error("Failed to parse commit JSON", e);
			return null;
		}
	}

	private static long extractLong(String json, String key) {
		int start = json.indexOf(key);
		if (start == -1) return 0L;
		start += key.length();
		int end = json.indexOf(",", start);
		if (end == -1) end = json.indexOf("}", start);
		return Long.parseLong(json.substring(start, end).trim());
	}

	private static String extractString(String json, String key) {
		int start = json.indexOf(key);
		if (start == -1) return "";
		start += key.length();
		int end = json.indexOf("\"", start);
		if (end == -1) return "";
		return json.substring(start, end);
	}

	private static String extractNullableString(String json, String key) {
		int idx = json.indexOf(key);
		if (idx == -1) return null;
		int start = idx + key.length();
		// Could be null or "value"
		if (json.startsWith("null", start)) return null;
		int q = json.indexOf("\"", start);
		if (q == -1) return null;
		int end = json.indexOf("\"", q + 1);
		if (end == -1) return null;
		return json.substring(q + 1, end);
	}

	private static String unescapeJson(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.replace("\\\\", "\\")
			.replace("\\\"", "\"")
			.replace("\\n", "\n")
			.replace("\\t", "\t")
			.replace("\\r", "\r");
	}

	private static List<String> splitTopLevelJsonObjects(String arrayInner) {
		ArrayList<String> out = new ArrayList<>();
		int depth = 0;
		int start = -1;
		for (int i = 0; i < arrayInner.length(); i++) {
			char c = arrayInner.charAt(i);
			if (c == '{') {
				if (depth == 0) start = i;
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0 && start != -1) {
					out.add(arrayInner.substring(start, i + 1).trim());
					start = -1;
				}
			}
		}
		return out;
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hashBytes) {
				String h = Integer.toHexString(0xff & b);
				if (h.length() == 1) hex.append('0');
				hex.append(h);
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			// SHA-1 should always be available; if not, log and fall back to random UUID
			Blockbase.LOGGER.error("SHA-1 algorithm not available, falling back to random ID", e);
			return java.util.UUID.randomUUID().toString().replace("-", "");
		}
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}


