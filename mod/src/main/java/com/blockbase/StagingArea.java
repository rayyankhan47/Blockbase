package com.blockbase;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks staged block changes, similar to Git's staging area (index).
 *
 * For the MVP:
 * - Staging is global per world (no per-player separation).
 * - /blockbase stage will stage all currently tracked changes.
 */
public class StagingArea {

	private final List<BlockChange> stagedChanges = new CopyOnWriteArrayList<>();

	/**
	 * Stage all provided changes, replacing any previously staged changes.
	 * @param changes The list of changes to stage
	 */
	public void stageAll(List<BlockChange> changes) {
		stagedChanges.clear();
		stagedChanges.addAll(changes);
		Blockbase.LOGGER.info("Staged {} block changes", stagedChanges.size());
	}

	/**
	 * Clear all staged changes.
	 */
	public void clear() {
		stagedChanges.clear();
		Blockbase.LOGGER.info("Cleared staged block changes");
	}

	/**
	 * @return a copy of the staged changes list
	 */
	public List<BlockChange> getStagedChanges() {
		return new ArrayList<>(stagedChanges);
	}

	/**
	 * @return number of staged changes
	 */
	public int getStagedCount() {
		return stagedChanges.size();
	}
}


