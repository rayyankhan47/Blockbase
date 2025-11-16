package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
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
	 * Compute diff within a player-centered radius by building:
	 * - Previous snapshot: replay commits up to target to get per-pos previous state
	 * - Current snapshot: scan current world in radius for non-air blocks
	 * Then diff the union.
	 */
	public static DiffResult compute(Level world, BlockPos center, int radius) {
		List<Path> chronological = getCommitsChronological(world);
		if (chronological == null || chronological.isEmpty()) {
			return new DiffResult(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}

		try {
			int targetIdx = targetIndex(chronological);
			var registry = world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY);

			Map<BlockPos, BlockState> previousStates = new HashMap<>();

			// Build previous snapshot by replaying commits 0..targetIdx
			for (int i = 0; i <= targetIdx; i++) {
				String json = Files.readString(chronological.get(i));
				Commit commit = Commit.fromJson(json, registry);
				if (commit == null || commit.getChanges() == null) continue;
				for (BlockChange ch : commit.getChanges()) {
					BlockPos pos = ch.getPosition();
					if (!withinRadius(center, pos, radius * radius)) continue;
					// Apply resulting state at this position as of this commit
					previousStates.put(pos, ch.getNewState());
				}
			}

			// Build current snapshot by scanning radius, but cap work
			Map<BlockPos, BlockState> currentStates = new HashMap<>();
			final int cap = 250_000; // safety cap
			int processed = 0;
			int r = Math.max(1, radius);
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						if (processed++ > cap) break;
						BlockPos pos = center.offset(dx, dy, dz);
						if (!withinRadius(center, pos, r * r)) continue;
						BlockState now = world.getBlockState(pos);
						if (now != null && !now.isAir()) {
							currentStates.put(pos, now);
						}
					}
					if (processed > cap) break;
				}
				if (processed > cap) break;
			}

			Set<BlockPos> added = new HashSet<>();
			Set<BlockPos> removed = new HashSet<>();
			Set<BlockPos> modified = new HashSet<>();

			// Analyze additions/modifications
			for (Map.Entry<BlockPos, BlockState> e : currentStates.entrySet()) {
				BlockPos pos = e.getKey();
				BlockState now = e.getValue();
				BlockState prev = previousStates.get(pos);
				boolean prevIsAir = (prev == null) || prev.isAir();
				boolean nowIsAir = now == null || now.isAir();
				if (prevIsAir && !nowIsAir) {
					added.add(pos);
				} else if (!prevIsAir && !nowIsAir && !statesEqual(prev, now)) {
					modified.add(pos);
				}
			}
			// Analyze removals
			for (Map.Entry<BlockPos, BlockState> e : previousStates.entrySet()) {
				BlockPos pos = e.getKey();
				BlockState prev = e.getValue();
				BlockState now = currentStates.get(pos);
				boolean prevIsAir = (prev == null) || prev.isAir();
				boolean nowIsAir = (now == null) || now.isAir();
				if (!prevIsAir && nowIsAir) {
					removed.add(pos);
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
	 * Get all commit files chronological (oldest first).
	 */
	private static List<Path> getCommitsChronological(Level world) {
		// Primary path via server world dir (works on dedicated)
		Path commitsDir = Repository.getCommitsDirectory(world);
		List<Path> files = listChronological(commitsDir);
		if (!files.isEmpty()) return files;
		// Fallback for integrated singleplayer client where world.getServer() is null on client thread
		try {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			MinecraftServer server = mc.getSingleplayerServer();
			if (server != null) {
				Path worldDir = server.getWorldPath(LevelResource.ROOT);
				if (worldDir != null) {
					Path alt = worldDir.resolve(".blockbase").resolve("commits");
					files = listChronological(alt);
					if (!files.isEmpty()) return files;
				}
			}
		} catch (Throwable ignore) {}
		return Collections.emptyList();
	}

	private static List<Path> listChronological(Path dir) {
		if (dir == null || !Files.exists(dir)) return Collections.emptyList();
		try {
			return Files.list(dir)
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
				.toList();
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Get index of target commit:
	 * - if ≥2 commits: pick the second latest (previous commit)
	 * - if exactly 1 commit: pick that single commit
	 * - otherwise: -1
	 */
	private static int targetIndex(List<Path> chronological) {
		if (chronological.isEmpty()) return -1;
		if (chronological.size() == 1) return 0;
		return chronological.size() - 2;
	}
}


