package com.blockbase;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;

public class DiffHudOverlay {
	public static void init() {
		HudRenderCallback.EVENT.register((poseStack, tickDelta) -> {
			if (DiffViewManager.getMode() == DiffViewManager.Mode.OFF) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.gui == null || mc.font == null) return;

			PoseStack ps = poseStack;
			var mode = DiffViewManager.getMode();
			String title = mode == DiffViewManager.Mode.DIFF
				? "Diff mode (G to toggle, Shift+G to exit)"
				: "Current mode (G to toggle, Shift+G to exit)";

			int x = 12;
			int y = 12;
			int color = 0xE5E5E5; // light gray
			mc.font.draw(ps, new TextComponent(title), x, y, color);

			if (mode == DiffViewManager.Mode.DIFF) {
				int added = DiffViewManager.getAdded().size();
				int removed = DiffViewManager.getRemoved().size();
				mc.font.draw(ps, new TextComponent(String.format("Added: %d  Removed: %d", added, removed)), x, y + 12, color);
			}
		});
	}
}


