package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Computes a lightweight diff between the current world and a target (previous) commit.
 * MVP strategy: Compare only positions that were touched in the target commit to keep it fast.
 */
public class DiffCalculator {

	public static class DiffResult {
		public final Map<BlockPos, BlockState> previousStates; // what the target commit had
		public final Set<BlockPos> added;     // exists now, absent (air) in previous
		public final Set<BlockPos> removed;   // was non-air in previous, now air
		public final Set<BlockPos> modified;  // both non-air but different state

		public DiffResult(Map<BlockPos, BlockState> previousStates,
						  Set<BlockPos> added,
						  Set<BlockPos> removed,
						  Set<BlockPos> modified) {
			this.previousStates = previousStates;
			this.added = added;
			this.removed = removed;
			this.modified = modified;
		}
	}

	/**
	 * Compute diff within a player-centered radius by sampling only positions touched
	 * in the target commit (second-latest if ≥2 commits; otherwise latest).
	 */
	public static DiffResult compute(Level world, BlockPos center, int radius) {
		Path targetCommitPath = getTargetCommitPath(world);
		if (targetCommitPath == null) {
			return new DiffResult(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}

		try {
			String json = Files.readString(targetCommitPath);
			var registry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);
			Commit commit = Commit.fromJson(json, registry);
			if (commit == null || commit.getChanges() == null || commit.getChanges().isEmpty()) {
				return new DiffResult(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
			}

			Map<BlockPos, BlockState> previousStates = new HashMap<>();
			Set<BlockPos> added = new HashSet<>();
			Set<BlockPos> removed = new HashSet<>();
			Set<BlockPos> modified = new HashSet<>();

			int r2 = radius * radius;
			for (BlockChange ch : commit.getChanges()) {
				BlockPos pos = ch.getPosition();
				if (!withinRadius(center, pos, r2)) continue;

				BlockState prev = ch.getNewState(); // previous commit's resulting state at this pos
				previousStates.put(pos, prev);

				BlockState now = world.getBlockState(pos);
				boolean prevIsAir = (prev == null) || prev.isAir();
				boolean nowIsAir = now == null || now.isAir();

				if (prevIsAir && !nowIsAir) {
					added.add(pos);
				} else if (!prevIsAir && nowIsAir) {
					removed.add(pos);
				} else if (!prevIsAir && !nowIsAir && !statesEqual(prev, now)) {
					modified.add(pos);
				}
			}

			return new DiffResult(previousStates, added, removed, modified);
		} catch (IOException e) {
			Blockbase.LOGGER.error("Failed to compute diff", e);
			return new DiffResult(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}
	}

	private static boolean withinRadius(BlockPos center, BlockPos pos, int r2) {
		int dx = pos.getX() - center.getX();
		int dy = pos.getY() - center.getY();
		int dz = pos.getZ() - center.getZ();
		return (dx * dx + dy * dy + dz * dz) <= r2;
	}

	private static boolean statesEqual(BlockState a, BlockState b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		// Compare block and all properties
		if (!a.getBlock().equals(b.getBlock())) return false;
		return a.getValues().equals(b.getValues());
	}

	/**
	 * Get the path to the target commit file:
	 * - if ≥2 commits: pick the second latest (previous commit)
	 * - if exactly 1 commit: pick that single commit
	 * - otherwise: null
	 */
	private static Path getTargetCommitPath(Level world) {
		Path commitsDir = Repository.getCommitsDirectory(world);
		if (commitsDir == null || !Files.exists(commitsDir)) return null;
		try {
			List<Path> files = Files.list(commitsDir)
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
				.toList();
			if (files.isEmpty()) return null;
			if (files.size() == 1) return files.get(0);
			return files.get(1); // previous commit
		} catch (IOException e) {
			return null;
		}
	}
}


