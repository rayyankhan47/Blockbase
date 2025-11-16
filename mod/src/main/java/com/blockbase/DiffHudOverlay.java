package com.blockbase;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class DiffHudOverlay {
	public static void init() {
		HudRenderCallback.EVENT.register((poseStack, tickDelta) -> {
			if (DiffViewManager.getMode() == DiffViewManager.Mode.OFF) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.gui == null || mc.font == null) return;

			PoseStack ps = poseStack;
			var mode = DiffViewManager.getMode();
			String title = switch (mode) {
				case DIFF -> "Diff mode (P to cycle, Shift+P to exit)";
				case CURRENT -> "Current mode (P to cycle, Shift+P to exit)";
				case PREVIOUS -> "Previous mode (P to cycle, Shift+P to exit)";
				default -> "";
			};

			int x = 12;
			int y = 12;
			int color = 0xE5E5E5; // light gray
			mc.font.draw(ps, Component.literal(title), x, y, color);

			if (mode == DiffViewManager.Mode.DIFF) {
				int added = DiffViewManager.getAdded().size();
				int removed = DiffViewManager.getRemoved().size();
				int modified = DiffViewManager.getModified().size();
				mc.font.draw(ps, Component.literal(String.format("Added: %d  Removed: %d  Modified: %d", added, removed, modified)), x, y + 12, color);
			}
		});
	}
}


