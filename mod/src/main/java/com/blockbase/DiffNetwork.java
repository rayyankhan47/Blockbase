package com.blockbase;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server->Client packets to control diff view from commands.
 */
public class DiffNetwork {
	public static final ResourceLocation TOGGLE = new ResourceLocation(Blockbase.MOD_ID, "diff_toggle");

	public enum Action {
		ENTER((byte)1),
		CLEAR((byte)2);
		public final byte code;
		Action(byte c) { this.code = c; }
		public static Action from(byte b) {
			return b == 1 ? ENTER : CLEAR;
		}
	}

	// Server -> send to a specific player
	public static void send(ServerPlayer player, Action action) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByte(action.code);
		ServerPlayNetworking.send(player, TOGGLE, buf);
	}

	// Client register receiver
	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(TOGGLE, (client, handler, buf, responseSender) -> {
			byte code = buf.readByte();
			Action action = Action.from(code);
			client.execute(() -> {
				if (client.player == null || client.level == null) return;
				switch (action) {
					case ENTER -> DiffViewManager.cycle(client.level, client.player.blockPosition());
					case CLEAR -> DiffViewManager.exit();
				}
			});
		});
	}
}


