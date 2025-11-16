package com.blockbase;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class BlockbaseClient implements ClientModInitializer {
	private KeyMapping toggleModeKey;
	private KeyMapping exitDiffKey;

	@Override
	public void onInitializeClient() {
		DiffOverlayRenderer.init();
		DiffHudOverlay.init();
		DiffNetwork.registerClient();
		toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blockbase.toggle_diff_mode",
			GLFW.GLFW_KEY_G,
			"key.categories.blockbase"
		));

		exitDiffKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blockbase.exit_diff",
			GLFW.GLFW_KEY_G,
			"key.categories.blockbase"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) return;

			// Use single keybinding: Shift+G exits; G cycles
			if (toggleModeKey.consumeClick()) {
				if (hasShift(client)) {
					DiffViewManager.exit();
					Blockbase.LOGGER.info("[blockbase] Exited diff mode");
				} else {
					DiffViewManager.cycle(client.level, client.player.blockPosition());
					Blockbase.LOGGER.info("[blockbase] Toggled diff mode: {}", DiffViewManager.getMode());
				}
			}
		});
	}

	private boolean hasShift(net.minecraft.client.Minecraft client) {
		return client.options.keyShift.isDown();
	}
}


