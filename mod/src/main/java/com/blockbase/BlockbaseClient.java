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
		toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blockbase.toggle_diff_mode",
			GLFW.GLFW_KEY_P,
			"key.categories.blockbase"
		));

		exitDiffKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.blockbase.exit_diff",
			GLFW.GLFW_KEY_P,
			"key.categories.blockbase"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) return;

			// Shift+P exits
			if (exitDiffKey.consumeClick() && hasShift()) {
				DiffViewManager.exit();
				Blockbase.LOGGER.info("[blockbase] Exited diff mode");
				return;
			}

			// P cycles modes
			if (toggleModeKey.consumeClick() && !hasShift()) {
				DiffViewManager.cycle(client.level, client.player.blockPosition());
				Blockbase.LOGGER.info("[blockbase] Diff mode: {}", DiffViewManager.getMode());
			}
		});
	}

	private boolean hasShift() {
		return org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}
}


