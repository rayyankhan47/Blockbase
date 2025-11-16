package com.blockbase;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-side renderer stub for diff overlays.
 * Next step: draw tinted boxes/quads for added/removed/modified positions.
 */
public class DiffOverlayRenderer {
	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
			if (DiffViewManager.getMode() != DiffViewManager.Mode.DIFF) {
				return;
			}
			// Placeholder: we will draw overlays in a follow-up step.
			// Iterate sets to assert flow without rendering yet.
			int count = DiffViewManager.getAdded().size()
				+ DiffViewManager.getRemoved().size()
				+ DiffViewManager.getModified().size();
			if (count > 0 && Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 100 == 0) {
				Blockbase.LOGGER.info("[blockbase] DiffOverlayRenderer active: {} changed blocks in view", count);
			}
		});
	}
}


