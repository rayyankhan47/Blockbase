package com.blockbase;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registers all /blockbase commands.
 */
public class BlockbaseCommands {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// Register under both /bb and /blockbase (alias), prefer /bb in help text
		registerRoot(dispatcher, "bb");
		registerRoot(dispatcher, "blockbase");
	}

	private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
		dispatcher.register(
			Commands.literal(root)
				.executes(BlockbaseCommands::rootCommand)
				.then(
					Commands.literal("init")
						.executes(BlockbaseCommands::initCommand)
				)
				.then(
					Commands.literal("commit")
						.then(
							Commands.argument("message", StringArgumentType.greedyString())
								.executes(BlockbaseCommands::commitCommand)
						)
				)
				.then(
					Commands.literal("add")
						.then(
							Commands.literal(".")
								.executes(BlockbaseCommands::stageCommand)
						)
				)
				.then(
					Commands.literal("help")
						.executes(BlockbaseCommands::helpCommand)
				)
				.then(
					Commands.literal("log")
						.executes(BlockbaseCommands::logCommand)
				)
				.then(
					Commands.literal("reset")
						.then(
							Commands.literal("--hard")
								.then(
									Commands.argument("commitId", StringArgumentType.string())
										.executes(BlockbaseCommands::resetHardCommand)
								)
						)
				)
				.then(
					Commands.literal("status")
						.executes(BlockbaseCommands::statusCommand)
				)
				.then(
					Commands.literal("push")
						.executes(BlockbaseCommands::pushCommand)
				)
				.then(
					Commands.literal("diff")
						.executes(BlockbaseCommands::diffEnterCommand)
						.then(
							Commands.literal("clear")
								.executes(BlockbaseCommands::diffClearCommand)
						)
				)
				.then(
					Commands.literal("remote")
						.then(
							Commands.literal("add")
								.then(
									Commands.literal("origin")
										.then(
											Commands.argument("url", StringArgumentType.greedyString())
												.executes(BlockbaseCommands::remoteAddCommand)
										)
								)
						)
						.then(
							Commands.literal("show")
								.executes(BlockbaseCommands::remoteShowCommand)
						)
				)
		);
	}

	private static int helpCommand(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent(
			"[Blockbase] Commands:\n" +
			" - /bb init   : Initialize Blockbase repository in this world\n" +
			" - /bb commit \"message\" : Commit staged changes with a message\n" +
			" - /bb add .  : Stage all currently tracked changes\n" +
			" - /bb log    : Show recent commits\n" +
			" - /bb reset --hard <commitId> : Reset world to a specific commit (destructive)\n" +
			" - /bb push   : Push local commits to remote backend\n" +
			" - /bb diff   : Enter visual diff mode (G to cycle modes, Shift+G to exit)\n" +
			" - /bb diff clear : Exit visual diff mode\n" +
			" - /bb remote add origin <url> : Set remote backend URL for this repo\n" +
			" - /bb remote show : Display current remote URL\n" +
			" - /bb help   : Show this help message\n" +
			" - /bb status : Show tracked change status"
		), false);
		return 1;
	}

	private static int statusCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		// Repo status
		Repository repo = Repository.load(world);
		boolean isRepoInitialized = repo != null;

		List<BlockChange> changes = Blockbase.blockTracker.getChanges();
		int totalChanges = changes.size();

		List<BlockChange> staged = Blockbase.stagingArea.getStagedChanges();
		int stagedCount = staged.size();

		// Aggregate tracked changes by block type (using human-readable block name)
		Map<String, Integer> trackedByBlock = new HashMap<>();
		for (BlockChange change : changes) {
			// Prefer newState if present; otherwise fall back to oldState
			net.minecraft.world.level.block.state.BlockState state =
				change.getNewState() != null ? change.getNewState() : change.getOldState();
			if (state == null) continue;

			String name = state.getBlock().getName().getString();
			trackedByBlock.merge(name, 1, Integer::sum);
		}

		StringBuilder trackedBreakdown = new StringBuilder();
		if (trackedByBlock.isEmpty()) {
			trackedBreakdown.append("  (no tracked block changes yet)\n");
		} else {
			for (Map.Entry<String, Integer> entry : trackedByBlock.entrySet()) {
				trackedBreakdown.append(String.format("  - %s: %d changes\n", entry.getKey(), entry.getValue()));
			}
		}

		// Aggregate staged changes by block type
		Map<String, Integer> stagedByBlock = new HashMap<>();
		for (BlockChange change : staged) {
			net.minecraft.world.level.block.state.BlockState state =
				change.getNewState() != null ? change.getNewState() : change.getOldState();
			if (state == null) continue;

			String name = state.getBlock().getName().getString();
			stagedByBlock.merge(name, 1, Integer::sum);
		}

		StringBuilder stagedBreakdown = new StringBuilder();
		if (stagedByBlock.isEmpty()) {
			stagedBreakdown.append("  (no staged changes)\n");
		} else {
			for (Map.Entry<String, Integer> entry : stagedByBlock.entrySet()) {
				stagedBreakdown.append(String.format("  - %s: %d changes\n", entry.getKey(), entry.getValue()));
			}
		}

		// Compose repo/branch info
		String branchInfo = "main";
		String repoInfo;
		String headInfo = "";
		if (isRepoInitialized) {
			repoInfo = String.format("initialized (repo: %s)", repo.getName());
			// Try to show HEAD (latest commit short id)
			Path commitsDir = Repository.getCommitsDirectory(world);
			String head = null;
			if (commitsDir != null && java.nio.file.Files.exists(commitsDir)) {
				try {
					head = java.nio.file.Files.list(commitsDir)
						.filter(p -> p.getFileName().toString().endsWith(".json"))
						.max((a, b) -> {
							try {
								long at = java.nio.file.Files.getLastModifiedTime(a).toMillis();
								long bt = java.nio.file.Files.getLastModifiedTime(b).toMillis();
								return Long.compare(at, bt);
							} catch (java.io.IOException e) {
								return 0;
							}
						})
						.map(p -> {
							try {
								String json = java.nio.file.Files.readString(p);
								String id = extractString(json, "\"id\":\"");
								if (id == null) return null;
								return id.length() > 7 ? id.substring(0, 7) : id;
							} catch (java.io.IOException e) {
								return null;
							}
						})
						.orElse(null);
				} catch (java.io.IOException ignore) {}
			}
			if (head != null) {
				headInfo = String.format(" (HEAD %s)", head);
			}
		} else {
			repoInfo = "not initialized (use /bb init)";
		}
		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Status:\n" +
				" - Tracked changes (total): %d\n" +
				" - Tracked changes by block:\n%s" +
				" - Staged changes (total): %d\n" +
				" - Staged changes by block:\n%s" +
				" - Current branch: %s%s\n" +
				" - Repository: %s",
				totalChanges,
				trackedBreakdown.toString(),
				stagedCount,
				stagedBreakdown.toString(),
				branchInfo,
				headInfo,
				repoInfo
			)),
			false
		);
		return 1;
	}

	private static int diffEnterCommand(CommandContext<CommandSourceStack> context) {
		try {
			var player = context.getSource().getPlayerOrException();
			DiffNetwork.send((net.minecraft.server.level.ServerPlayer) player, DiffNetwork.Action.ENTER);
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Entered diff mode. Press G to cycle, Shift+G to exit."), false);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] Failed to enter diff mode: " + e.getMessage()));
			return 0;
		}
	}

	private static int diffClearCommand(CommandContext<CommandSourceStack> context) {
		try {
			var player = context.getSource().getPlayerOrException();
			DiffNetwork.send((net.minecraft.server.level.ServerPlayer) player, DiffNetwork.Action.CLEAR);
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Exited diff mode."), false);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] Failed to exit diff mode: " + e.getMessage()));
			return 0;
		}
	}

	private static int rootCommand(CommandContext<CommandSourceStack> context) {
		// No subcommand: show basic usage hint
		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent("[Blockbase] Use /bb help for available commands."),
			false
		);
		return 1;
	}

	private static int stageCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		// Ensure repository is initialized
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No repository found. Run /bb init first."
				)
			);
			return 0;
		}

		List<BlockChange> changes = Blockbase.blockTracker.getChanges();
		if (changes.isEmpty()) {
			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent("[Blockbase] No changes to add."),
				false
			);
			return 1;
		}

		Blockbase.stagingArea.stageAll(changes);

		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Added %d changes to staging (use /bb status to view details).",
				changes.size()
			)),
			false
		);

		return 1;
	}

	private static int commitCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		// Ensure repository is initialized
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No repository found. Run /bb init first."
				)
			);
			return 0;
		}

		List<BlockChange> staged = Blockbase.stagingArea.getStagedChanges();
		if (staged.isEmpty()) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No staged changes. Use /bb add . first."
				)
			);
			return 0;
		}

		// Enforce quotes around commit message to mimic git usage
		String rawInput = context.getInput();
		if (!rawInput.contains("\"")) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Please wrap the commit message in double quotes, e.g. /bb commit \"my message\""
				)
			);
			return 0;
		}

		String message = StringArgumentType.getString(context, "message");
		if (message == null || message.trim().isEmpty()) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Commit message cannot be empty."
				)
			);
			return 0;
		}

		// Determine parent commit ID (if any)
		String parentId = Repository.getLatestCommitId(world);

		// Author: prefer integrated server's singleplayer name, then player's GameProfile, then text name
		String author;
		try {
			String spName = world.getServer() != null ? world.getServer().getSingleplayerName() : null;
			if (spName != null && !spName.isEmpty()) {
				author = spName;
			} else {
				var player = context.getSource().getPlayerOrException();
				author = player.getGameProfile().getName();
			}
		} catch (Exception e) {
			author = context.getSource().getTextName();
		}

		// Create commit and compute its ID
		Commit commit = Commit.create(message.trim(), author, parentId, staged, world);

		// Save commit to disk
		Repository.saveCommit(world, commit);

		// Clear staged changes and current tracked changes (working directory is now "clean")
		Blockbase.stagingArea.clear();
		Blockbase.blockTracker.clearChanges();

		// Show short commit ID (first 7 chars) for readability
		String shortId = commit.getId().length() > 7 ? commit.getId().substring(0, 7) : commit.getId();

		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Created commit %s: \"%s\" (%d changes)",
				shortId,
				commit.getMessage(),
				commit.getChanges().size()
			)),
			false
		);

		return 1;
	}

	private static int logCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		// Ensure repository is initialized
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No repository found. Run /blockbase init first."
				)
			);
			return 0;
		}

		Path commitsDir = Repository.getCommitsDirectory(world);
		if (commitsDir == null || !Files.exists(commitsDir)) {
			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent("[Blockbase] No commits yet."),
				false
			);
			return 1;
		}

		try {
			List<Path> commitFiles = Files.list(commitsDir)
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.sorted((a, b) -> {
					try {
						long at = Files.getLastModifiedTime(a).toMillis();
						long bt = Files.getLastModifiedTime(b).toMillis();
						return Long.compare(bt, at); // newest first
					} catch (IOException e) {
						return 0;
					}
				})
				.limit(10) // show latest 10 commits
				.collect(Collectors.toList());

			if (commitFiles.isEmpty()) {
				context.getSource().sendSuccess(
					new net.minecraft.network.chat.TextComponent("[Blockbase] No commits yet."),
					false
				);
				return 1;
			}

			for (Path path : commitFiles) {
				String json = Files.readString(path);

				String id = extractString(json, "\"id\":\"");
				String message = unescapeJson(extractString(json, "\"message\":\""));
				String author = extractString(json, "\"author\":\"");

				String shortId = id != null && id.length() > 7 ? id.substring(0, 7) : id;

				String line = String.format("commit %s | %s | %s",
					shortId,
					author,
					message
				);

				context.getSource().sendSuccess(
					new net.minecraft.network.chat.TextComponent(line),
					false
				);
			}

			return 1;

		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to read commit log", e);
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Failed to read commit log. Check logs for details."
				)
			);
			return 0;
		}
	}

	// Simple JSON helpers for extracting fields in commit log
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
		if (end == -1) {
			return "";
		}
		return json.substring(start, end);
	}

	// Minimal JSON unescape for common sequences in our stored fields
	private static String unescapeJson(String s) {
		if (s == null || s.isEmpty()) return s;
		// Order matters: unescape escaped backslashes first
		return s.replace("\\\\", "\\")
				.replace("\\\"", "\"")
				.replace("\\n", "\n")
				.replace("\\t", "\t")
				.replace("\\r", "\r");
	}

	// Split a JSON array inner content into top-level object strings
	private static List<String> splitTopLevelJsonObjects(String arrayInner) {
		java.util.ArrayList<String> out = new java.util.ArrayList<>();
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
	private static int resetHardCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		// Ensure repository is initialized
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No repository found. Run /blockbase init first."
				)
			);
			return 0;
		}

		// Safety warning if there are uncommitted changes
		if (Blockbase.blockTracker.getChangeCount() > 0 || Blockbase.stagingArea.getStagedCount() > 0) {
			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Warning: uncommitted or staged changes will be lost by reset --hard."
				),
				false
			);
		}

		String prefix = StringArgumentType.getString(context, "commitId");

		Path commitsDir = Repository.getCommitsDirectory(world);
		if (commitsDir == null || !Files.exists(commitsDir)) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No commits directory found. Nothing to reset."
				)
			);
			return 0;
		}

		try {
			// Load all commit files sorted by time (oldest first)
			List<Path> allCommits = Files.list(commitsDir)
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.sorted(Comparator.comparing(p -> {
					try {
						return Files.getLastModifiedTime(p).toMillis();
					} catch (IOException e) {
						return 0L;
					}
				}))
				.collect(Collectors.toList());

			if (allCommits.isEmpty()) {
				context.getSource().sendFailure(
					new net.minecraft.network.chat.TextComponent(
						"[Blockbase] No commits found. Nothing to reset."
					)
				);
				return 0;
			}

			// Find target commit by prefix (match against id inside JSON)
			List<Path> matching = allCommits.stream().filter(p -> {
				try {
					String json = Files.readString(p);
					String id = extractString(json, "\"id\":\"");
					return id != null && id.startsWith(prefix);
				} catch (IOException e) {
					return false;
				}
			}).collect(Collectors.toList());

			if (matching.isEmpty()) {
				context.getSource().sendFailure(
					new net.minecraft.network.chat.TextComponent(
						String.format("[Blockbase] No commit found with id starting with '%s'.", prefix)
					)
				);
				return 0;
			}

			if (matching.size() > 1) {
				String ids = matching.stream().map(p -> {
						try {
							String json = Files.readString(p);
							String id = extractString(json, "\"id\":\"");
							if (id == null) return "?";
							return id.length() > 7 ? id.substring(0, 7) : id;
						} catch (IOException e) {
							return "?";
						}
					})
					.collect(Collectors.joining(", "));
				context.getSource().sendFailure(
					new net.minecraft.network.chat.TextComponent(
						String.format("[Blockbase] Ambiguous commit id '%s'. Matches: %s", prefix, ids)
					)
				);
				return 0;
			}

			Path targetPath = matching.get(0);
			String targetJson = Files.readString(targetPath);
			String fullId = extractString(targetJson, "\"id\":\"");
			if (fullId == null || fullId.isEmpty()) {
				context.getSource().sendFailure(
					new net.minecraft.network.chat.TextComponent(
						"[Blockbase] Failed to parse commit file (missing id)."
					)
				);
				return 0;
			}

			// Find index of target in ordered list
			int targetIndex = allCommits.indexOf(targetPath);
			if (targetIndex == -1) {
				context.getSource().sendFailure(
					new net.minecraft.network.chat.TextComponent(
						"[Blockbase] Internal error: target commit not in commit list."
					)
				);
				return 0;
			}

			// Commits after the target need to be reverted (from newest to oldest)
			List<Path> toRevert = allCommits.subList(targetIndex + 1, allCommits.size());
			if (toRevert.isEmpty()) {
				context.getSource().sendSuccess(
					new net.minecraft.network.chat.TextComponent(
						"[Blockbase] Already at the specified commit; nothing to reset."
					),
					false
				);
				return 1;
			}

			var registry = world.registryAccess().registryOrThrow(net.minecraft.core.Registry.BLOCK_REGISTRY);
			int applied = 0;

			for (int i = toRevert.size() - 1; i >= 0; i--) {
				Path commitPath = toRevert.get(i);
				String json = Files.readString(commitPath);

				// Parse changes array
				int idx = json.indexOf("\"changes\":[");
				if (idx == -1) {
					continue;
				}
				int start = idx + "\"changes\":[".length();
				int end = json.indexOf("]", start);
				if (end == -1) {
					continue;
				}
				String changesPart = json.substring(start, end).trim();
				if (changesPart.isEmpty()) {
					continue;
				}

				java.util.List<String> changeObjs = splitTopLevelJsonObjects(changesPart);
				for (int j = changeObjs.size() - 1; j >= 0; j--) {
					String changeStr = changeObjs.get(j);
					BlockChange change = BlockChange.fromJsonString(changeStr, registry);
					if (change == null) continue;

					net.minecraft.core.BlockPos pos = change.getPosition();
					net.minecraft.world.level.block.state.BlockState oldState = change.getOldState();

					if (oldState != null) {
						// Revert to old state
						world.setBlock(pos, oldState, 3);
					} else {
						// Old state was null -> block did not exist before this commit, so set to AIR
						world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					}
					applied++;
				}
			}

			// Clear tracking and staging after reset
			Blockbase.blockTracker.clearChanges();
			Blockbase.stagingArea.clear();

			// Prune commits newer than the target (like git reset --hard)
			for (Path p : toRevert) {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignore) {
				}
			}

			String shortId = fullId.length() > 7 ? fullId.substring(0, 7) : fullId;
			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent(String.format(
					"[Blockbase] Reset --hard to commit %s (%d block changes reverted). Pruned %d commits.",
					shortId,
					applied,
					toRevert.size()
				)),
				false
			);

			return 1;

		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to reset to commit", e);
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Failed to reset to commit. Check logs for details."
				)
			);
			return 0;
		}
	}

	private static int initCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();

		Repository repo = Repository.init(world);
		if (repo == null) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent("[Blockbase] Failed to initialize repository. Check logs for details.")
			);
			return 0;
		}

		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Repository initialized.\n" +
				" - Name: %s\n" +
				" - ID: %s\n" +
				" - Default branch: %s",
				repo.getName(),
				repo.getId(),
				repo.getDefaultBranch()
			)),
			false
		);
		return 1;
	}

	private static int pushCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] No repository found. Run /bb init first."));
			return 0;
		}
		String remote = repo.getRemoteUrl();
		if (remote == null || remote.isEmpty()) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] No remote set. Use /bb remote add origin <url>"));
			return 0;
		}

		Path commitsDir = Repository.getCommitsDirectory(world);
		if (commitsDir == null || !Files.exists(commitsDir)) {
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] No commits to push."), false);
			return 1;
		}

		try {
			List<Path> commitFiles = Files.list(commitsDir)
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.sorted((a, b) -> {
					try {
						long at = Files.getLastModifiedTime(a).toMillis();
						long bt = Files.getLastModifiedTime(b).toMillis();
						return Long.compare(at, bt); // oldest first
					} catch (IOException e) {
						return 0;
					}
				})
				.collect(Collectors.toList());

			if (commitFiles.isEmpty()) {
				context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] No commits to push."), false);
				return 1;
			}

			ApiClient client = new ApiClient(remote, null);
			String remoteRepoId = repo.getRemoteRepoId();
			String targetRepoId = (remoteRepoId != null && !remoteRepoId.isEmpty()) ? remoteRepoId : repo.getId();
			int pushed = 0;
			int failed = 0;
			for (Path path : commitFiles) {
				String json = Files.readString(path);
				String id = extractString(json, "\"id\":\"");
				String message = unescapeJson(extractString(json, "\"message\":\""));
				String author = extractString(json, "\"author\":\"");
				long ts = extractLong(json, "\"timestamp\":");
				String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(new Date(ts == 0L ? System.currentTimeMillis() : ts));

				// Extract raw changes array to forward as-is
				String changesArray = "[]";
				int idx = json.indexOf("\"changes\":[");
				if (idx != -1) {
					int start = idx + "\"changes\":".length();
					// start should point to '['
					int bracket = json.indexOf("[", start);
					if (bracket != -1) {
						int depth = 0;
						int end = -1;
						for (int i = bracket; i < json.length(); i++) {
							char c = json.charAt(i);
							if (c == '[') depth++;
							else if (c == ']') {
								depth--;
								if (depth == 0) {
                                    end = i;
									break;
								}
							}
						}
						if (end != -1) {
							changesArray = json.substring(bracket, end + 1);
						}
					}
				}

				ApiClient.ApiResult res = client.createCommit(targetRepoId, id, message, author, timestamp, changesArray);
				if (res.ok) {
					pushed++;
				} else {
					failed++;
					Blockbase.LOGGER.warn("Push failed for commit {}: status={}, body={}, error={}", id, res.status, res.body, res.error);
				}
			}

			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent(String.format("[Blockbase] Push complete. Pushed %d commit(s)%s.",
					pushed,
					failed > 0 ? String.format(" (%d failed - see logs)", failed) : "")),
				false
			);
			return failed > 0 ? 0 : 1;
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to read commits for push", e);
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] Failed to push commits. Check logs for details."));
			return 0;
		}
	}

	private static int remoteShowCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] No repository found. Run /bb init first."));
			return 0;
		}
		String url = repo.getRemoteUrl();
		if (url == null || url.isEmpty()) {
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] No remote set. Use /bb remote add <url>"), false);
		} else {
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Remote: " + url), false);
		}
		return 1;
	}

	private static int remoteAddCommand(CommandContext<CommandSourceStack> context) {
		Level world = context.getSource().getLevel();
		Repository repo = Repository.load(world);
		if (repo == null) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] No repository found. Run /bb init first."));
			return 0;
		}
		String url = StringArgumentType.getString(context, "url").trim();
		if (url.isEmpty()) {
			context.getSource().sendFailure(new net.minecraft.network.chat.TextComponent("[Blockbase] Remote URL cannot be empty."));
			return 0;
		}

		// Allow repo-scoped URLs like https://host/api/repos/<repoId>
		String remoteBase = url;
		String remoteRepoId = null;
		int at = url.indexOf("/repos/");
		if (at != -1) {
			int start = at + "/repos/".length();
			int end = url.indexOf("/", start);
			if (end == -1) end = url.length();
			remoteRepoId = url.substring(start, end);
			remoteBase = url.substring(0, at);
		}

		Repository updated = repo.withRemote(remoteBase, remoteRepoId);
		Repository.save(world, updated);

		// Optionally attempt to create repo on backend
		try {
			ApiClient client = new ApiClient(remoteBase, null);
			String createId = remoteRepoId != null && !remoteRepoId.isEmpty() ? remoteRepoId : updated.getId();
			ApiClient.ApiResult result = client.createRepository(createId, updated.getName(), java.util.Map.of("branch", updated.getDefaultBranch()));
			if (result.ok) {
				context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Remote set and repository created on backend."), false);
			} else {
				context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Remote set. Backend create returned " + result.status + (result.body != null ? (": " + result.body) : "")), false);
			}
		} catch (Exception e) {
			// Non-fatal: remote set locally even if backend failed
			context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("[Blockbase] Remote set. Failed to reach backend: " + e.getMessage()), false);
		}

		return 1;
	}
}


