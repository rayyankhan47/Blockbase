package com.blockbase;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side renderer for diff overlays (wireframe boxes).
 * Non-destructive: only draws overlays; does not mutate world.
 */
public class DiffOverlayRenderer {
	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
			var mode = DiffViewManager.getMode();
			if (mode == DiffViewManager.Mode.OFF) return;
			var camera = context.camera();
			Vec3 camPos = camera.getPosition();
			PoseStack poseStack = context.matrixStack();

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableTexture();
			RenderSystem.lineWidth(2.0F);

			if (mode == DiffViewManager.Mode.DIFF) {
				RenderSystem.setShader(GameRenderer::getPositionShader);
				for (BlockPos p : DiffViewManager.getAdded()) {
					drawBox(poseStack, camPos, p, 0f, 1f, 0f, 0.65f); // stronger green
				}
				for (BlockPos p : DiffViewManager.getRemoved()) {
					drawBox(poseStack, camPos, p, 1f, 0f, 0f, 0.65f); // stronger red
				}
			} else {
				// Current mode: no overlays
			}

			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		});
	}

	private static void drawBox(PoseStack poseStack, Vec3 cam, BlockPos pos,
								float r, float g, float b, float a) {
		double x = pos.getX() - cam.x;
		double y = pos.getY() - cam.y;
		double z = pos.getZ() - cam.z;

		double x2 = x + 1.0;
		double y2 = y + 1.0;
		double z2 = z + 1.0;

		var matrices = poseStack.last().pose();
		Tesselator tess = Tesselator.getInstance();
		BufferBuilder buffer = tess.getBuilder();

		// Filled translucent box
		RenderSystem.setShaderColor(r, g, b, a * 0.25f);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		// bottom
		buffer.vertex(matrices, (float)x, (float)y, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x, (float)y, (float)z2).endVertex();
		// top
		buffer.vertex(matrices, (float)x, (float)y2, (float)z).endVertex();
		buffer.vertex(matrices, (float)x, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z).endVertex();
		// sides
		buffer.vertex(matrices, (float)x, (float)y, (float)z).endVertex();
		buffer.vertex(matrices, (float)x, (float)y2, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y, (float)z).endVertex();

		buffer.vertex(matrices, (float)x2, (float)y, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y, (float)z2).endVertex();

		buffer.vertex(matrices, (float)x2, (float)y, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x2, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x, (float)y, (float)z2).endVertex();

		buffer.vertex(matrices, (float)x, (float)y, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x, (float)y2, (float)z2).endVertex();
		buffer.vertex(matrices, (float)x, (float)y2, (float)z).endVertex();
		buffer.vertex(matrices, (float)x, (float)y, (float)z).endVertex();
		tess.end();

		// Outline
		RenderSystem.setShaderColor(r, g, b, a);
		buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION);
		// bottom square
		line(buffer, matrices, x, y, z, x2, y, z);
		line(buffer, matrices, x2, y, z, x2, y, z2);
		line(buffer, matrices, x2, y, z2, x, y, z2);
		line(buffer, matrices, x, y, z2, x, y, z);
		// top square
		line(buffer, matrices, x, y2, z, x2, y2, z);
		line(buffer, matrices, x2, y2, z, x2, y2, z2);
		line(buffer, matrices, x2, y2, z2, x, y2, z2);
		line(buffer, matrices, x, y2, z2, x, y2, z);
		// verticals
		line(buffer, matrices, x, y, z, x, y2, z);
		line(buffer, matrices, x2, y, z, x2, y2, z);
		line(buffer, matrices, x2, y, z2, x2, y2, z2);
		line(buffer, matrices, x, y, z2, x, y2, z2);
		tess.end();
	}

	private static void line(BufferBuilder buffer, com.mojang.math.Matrix4f m,
							 double x1, double y1, double z1,
							 double x2, double y2, double z2) {
		buffer.vertex(m, (float)x1, (float)y1, (float)z1).endVertex();
		buffer.vertex(m, (float)x2, (float)y2, (float)z2).endVertex();
	}
}


