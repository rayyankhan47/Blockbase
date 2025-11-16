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
		// Root command: /blockbase
		dispatcher.register(
			Commands.literal("blockbase")
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
					Commands.literal("status")
						.executes(BlockbaseCommands::statusCommand)
				)
		);
	}

	private static int helpCommand(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent(
			"[Blockbase] Commands:\n" +
			" - /blockbase init   : Initialize Blockbase repository in this world\n" +
			" - /blockbase commit <message> : Commit staged changes with a message\n" +
			" - /blockbase add .  : Stage all currently tracked changes\n" +
			" - /blockbase log    : Show recent commits\n" +
			" - /blockbase help   : Show this help message\n" +
			" - /blockbase status : Show tracked change status"
		), false);
		return 1;
	}

	private static int statusCommand(CommandContext<CommandSourceStack> context) {
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

		// For now, staging/branch/repo status are placeholders; will be implemented in Step 4.
		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Status:\n" +
				" - Tracked changes (total): %d\n" +
				" - Tracked changes by block:\n%s" +
				" - Staged changes (total): %d\n" +
				" - Staged changes by block:\n%s" +
				" - Current branch: %s\n" +
				" - Repository: %s",
				totalChanges,
				trackedBreakdown.toString(),
				stagedCount,
				stagedBreakdown.toString(),
				"main (placeholder)",
				"not initialized (use /blockbase init in Step 4)"
			)),
			false
		);
		return 1;
	}

	private static int rootCommand(CommandContext<CommandSourceStack> context) {
		// No subcommand: show basic usage hint
		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent("[Blockbase] Use /blockbase help for available commands."),
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
					"[Blockbase] No repository found. Run /blockbase init first."
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
				"[Blockbase] Added %d changes to staging (use /blockbase status to view details).",
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
					"[Blockbase] No repository found. Run /blockbase init first."
				)
			);
			return 0;
		}

		List<BlockChange> staged = Blockbase.stagingArea.getStagedChanges();
		if (staged.isEmpty()) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] No staged changes. Use /blockbase add . first."
				)
			);
			return 0;
		}

		// Enforce quotes around commit message to mimic git usage
		String rawInput = context.getInput();
		if (!rawInput.contains("\"")) {
			context.getSource().sendFailure(
				new net.minecraft.network.chat.TextComponent(
					"[Blockbase] Please wrap the commit message in double quotes, e.g. /blockbase commit \"my message\""
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

		// Author: use the command source's display name
		String author = context.getSource().getTextName();

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

			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			StringBuilder sb = new StringBuilder();
			sb.append("[Blockbase] Commit log (latest first):\n");

			for (Path path : commitFiles) {
				String json = Files.readString(path);

				String id = extractString(json, "\"id\":\"");
				String message = extractString(json, "\"message\":\"");
				String author = extractString(json, "\"author\":\"");
				long timestamp = extractLong(json, "\"timestamp\":");

				String shortId = id != null && id.length() > 7 ? id.substring(0, 7) : id;
				String dateStr = fmt.format(new Date(timestamp));

				sb.append(String.format(" - %s | %s | %s | %s\n",
					shortId,
					author,
					dateStr,
					message
				));
			}

			context.getSource().sendSuccess(
				new net.minecraft.network.chat.TextComponent(sb.toString()),
				false
			);
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
}


