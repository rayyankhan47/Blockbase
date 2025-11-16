package com.blockbase;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
					Commands.literal("help")
						.executes(BlockbaseCommands::helpCommand)
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
			" - /blockbase help   : Show this help message\n" +
			" - /blockbase status : Show tracked change status"
		), false);
		return 1;
	}

	private static int statusCommand(CommandContext<CommandSourceStack> context) {
		List<BlockChange> changes = Blockbase.blockTracker.getChanges();
		int totalChanges = changes.size();

		// Aggregate changes by block type (using human-readable block name)
		Map<String, Integer> byBlock = new HashMap<>();
		for (BlockChange change : changes) {
			// Prefer newState if present; otherwise fall back to oldState
			net.minecraft.world.level.block.state.BlockState state =
				change.getNewState() != null ? change.getNewState() : change.getOldState();
			if (state == null) continue;

			String name = state.getBlock().getName().getString();
			byBlock.merge(name, 1, Integer::sum);
		}

		StringBuilder breakdown = new StringBuilder();
		if (byBlock.isEmpty()) {
			breakdown.append("  (no tracked block changes yet)\n");
		} else {
			for (Map.Entry<String, Integer> entry : byBlock.entrySet()) {
				breakdown.append(String.format("  - %s: %d changes\n", entry.getKey(), entry.getValue()));
			}
		}

		// For now, staging/branch/repo status are placeholders; will be implemented in Step 4.
		context.getSource().sendSuccess(
			new net.minecraft.network.chat.TextComponent(String.format(
				"[Blockbase] Status:\n" +
				" - Tracked changes (total): %d\n" +
				" - Tracked changes by block:\n%s" +
				" - Staged changes: %s\n" +
				" - Current branch: %s\n" +
				" - Repository: %s",
				totalChanges,
				breakdown.toString(),
				"0 (staging not implemented yet)",
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
}


