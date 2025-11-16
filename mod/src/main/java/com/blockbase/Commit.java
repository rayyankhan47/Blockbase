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


