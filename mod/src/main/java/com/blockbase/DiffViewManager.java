package com.blockbase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds client-side diff view state and cycles modes.
 * Rendering hooks will consult this to tint/override.
 */
public class DiffViewManager {
	public enum Mode {
		OFF, DIFF, CURRENT, PREVIOUS
	}

	private static final AtomicReference<Mode> mode = new AtomicReference<>(Mode.OFF);
	private static DiffCalculator.DiffResult lastResult;
	private static BlockPos anchor;
	private static int radius = 256;

	public static Mode getMode() {
		return mode.get();
	}

	public static Map<BlockPos, net.minecraft.world.level.block.state.BlockState> getPreviousStates() {
		return lastResult != null ? lastResult.previousStates : java.util.Collections.emptyMap();
	}

	public static Set<BlockPos> getAdded() {
		return lastResult != null ? lastResult.added : java.util.Collections.emptySet();
	}

	public static Set<BlockPos> getRemoved() {
		return lastResult != null ? lastResult.removed : java.util.Collections.emptySet();
	}

	public static Set<BlockPos> getModified() {
		return lastResult != null ? lastResult.modified : java.util.Collections.emptySet();
	}

	public static void cycle(Level world, BlockPos center) {
		switch (mode.get()) {
			case OFF -> {
				anchor = center;
				lastResult = DiffCalculator.compute(world, center, radius);
				Blockbase.LOGGER.info("[blockbase] Diff computed: added={}, removed={}, modified={}",
					lastResult.added.size(), lastResult.removed.size(), lastResult.modified.size());
				mode.set(Mode.DIFF);
			}
			case DIFF -> mode.set(Mode.CURRENT);
			case CURRENT -> mode.set(Mode.PREVIOUS);
			case PREVIOUS -> mode.set(Mode.DIFF);
		}
	}

	public static void exit() {
		mode.set(Mode.OFF);
		lastResult = null;
		anchor = null;
	}
}


