package com.blockbase;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Represents a Blockbase repository stored in the world folder.
 *
 * For MVP this is intentionally simple: a single repo per world, single branch ("main").
 */
public class Repository {

	private final String id;
	private final String name;
	private final String defaultBranch;
	private final long createdAt;

	public Repository(String id, String name, String defaultBranch, long createdAt) {
		this.id = id;
		this.name = name;
		this.defaultBranch = defaultBranch;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDefaultBranch() {
		return defaultBranch;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	/**
	 * Initialize a repository in the given world's .blockbase folder.
	 * If a repo already exists, it is left unchanged and returned.
	 */
	public static Repository init(Level world) {
		Path repoFile = getRepoFile(world);
		if (repoFile == null) {
			Blockbase.LOGGER.error("Cannot initialize repository: world directory not available");
			return null;
		}

		try {
			if (Files.exists(repoFile)) {
				// Repo already exists, load and return it
				String json = Files.readString(repoFile);
				Repository existing = fromJson(json);
				if (existing != null) {
					Blockbase.LOGGER.info("Blockbase repository already initialized at {}", repoFile);
					return existing;
				}
			}

			// Create a new repository
			String id = UUID.randomUUID().toString();
			String name = getWorldName(world);
			if (name == null || name.isBlank()) {
				name = "Blockbase Repository";
			}
			String defaultBranch = "main";
			long createdAt = System.currentTimeMillis();

			Repository repo = new Repository(id, name, defaultBranch, createdAt);

			// Ensure directory exists
			Files.createDirectories(repoFile.getParent());

			// Write repo.json
			Files.writeString(repoFile, repo.toJson());
			Blockbase.LOGGER.info("Initialized Blockbase repository '{}' (id={}) at {}", name, id, repoFile);

			return repo;
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to initialize Blockbase repository", e);
			return null;
		}
	}

	/**
	 * Load an existing repository from disk.
	 * @return Repository or null if none exists / failed to parse
	 */
	public static Repository load(Level world) {
		Path repoFile = getRepoFile(world);
		if (repoFile == null || !Files.exists(repoFile)) {
			return null;
		}

		try {
			String json = Files.readString(repoFile);
			return fromJson(json);
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to load Blockbase repository", e);
			return null;
		}
	}

	/**
	 * Get the directory where commits are stored for this world.
	 * @param world The world
	 * @return Path to .blockbase/commits
	 */
	public static Path getCommitsDirectory(Level world) {
		if (world.getServer() == null) {
			return null;
		}
		MinecraftServer server = world.getServer();
		Path worldDir = server.getWorldPath(LevelResource.ROOT);
		if (worldDir == null) {
			return null;
		}
		return worldDir.resolve(".blockbase").resolve("commits");
	}

	/**
	 * Save a commit JSON file in .blockbase/commits.
	 */
	public static void saveCommit(Level world, Commit commit) {
		Path commitsDir = getCommitsDirectory(world);
		if (commitsDir == null) {
			Blockbase.LOGGER.error("Cannot save commit: world directory not available");
			return;
		}
		try {
			Files.createDirectories(commitsDir);
			Path commitFile = commitsDir.resolve(commit.getId() + ".json");
			Files.writeString(commitFile, commit.toJson(world));
			Blockbase.LOGGER.info("Saved commit {} to {}", commit.getId(), commitFile);
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to save commit {}", commit.getId(), e);
		}
	}

	/**
	 * Get the latest commit ID for this world by looking at the newest commit file.
	 * @return commit ID or null if none exist
	 */
	public static String getLatestCommitId(Level world) {
		Path commitsDir = getCommitsDirectory(world);
		if (commitsDir == null || !Files.exists(commitsDir)) {
			return null;
		}
		try {
			return Files.list(commitsDir)
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.max((a, b) -> {
					try {
						long at = Files.getLastModifiedTime(a).toMillis();
						long bt = Files.getLastModifiedTime(b).toMillis();
						return Long.compare(at, bt);
					} catch (IOException e) {
						return 0;
					}
				})
				.map(p -> {
					String name = p.getFileName().toString();
					if (name.endsWith(".json")) {
						return name.substring(0, name.length() - 5);
					}
					return name;
				})
				.orElse(null);
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to list commit files", e);
			return null;
		}
	}

	private static Path getRepoFile(Level world) {
		if (world.getServer() == null) {
			return null;
		}
		MinecraftServer server = world.getServer();
		Path worldDir = server.getWorldPath(LevelResource.ROOT);
		if (worldDir == null) {
			return null;
		}
		return worldDir.resolve(".blockbase").resolve("repo.json");
	}

	private static String getWorldName(Level world) {
		if (world.getServer() == null) {
			return null;
		}
		return world.getServer().getWorldData().getLevelName();
	}

	/**
	 * Serialize this repository to a simple JSON string.
	 */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"id\":\"").append(escape(id)).append("\",");
		sb.append("\"name\":\"").append(escape(name)).append("\",");
		sb.append("\"defaultBranch\":\"").append(escape(defaultBranch)).append("\",");
		sb.append("\"createdAt\":").append(createdAt);
		sb.append("}");
		return sb.toString();
	}

	public static Repository fromJson(String json) {
		try {
			String id = extractString(json, "\"id\":\"");
			String name = extractString(json, "\"name\":\"");
			String defaultBranch = extractString(json, "\"defaultBranch\":\"");
			long createdAt = extractLong(json, "\"createdAt\":");
			return new Repository(id, name, defaultBranch, createdAt);
		} catch (Exception e) {
			Blockbase.LOGGER.error("Failed to parse repo.json: {}", json, e);
			return null;
		}
	}

	private static long extractLong(String json, String key) {
		int start = json.indexOf(key);
		if (start == -1) {
			return 0L;
		}
		start += key.length();
		int end = json.indexOf(",", start);
		if (end == -1) {
			end = json.indexOf("}", start);
		}
		return Long.parseLong(json.substring(start, end).trim());
	}

	private static String extractString(String json, String key) {
		int start = json.indexOf(key);
		if (start == -1) {
			return "";
		}
		start += key.length();
		int end = json.indexOf("\"", start);
		return json.substring(start, end);
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}


