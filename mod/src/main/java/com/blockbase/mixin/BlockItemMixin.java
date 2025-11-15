package com.blockbase.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import com.blockbase.Blockbase;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@Inject(
		method = "place",
		at = @At("RETURN")
	)
	private void onBlockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		// Only track if block was successfully placed
		InteractionResult result = cir.getReturnValue();
		if (result.consumesAction() && !context.getLevel().isClientSide()) {
			Level world = context.getLevel();
			BlockPos blockPos = context.getClickedPos();
			
			// Get the actual block state from the world (after placement)
			BlockState placedState = world.getBlockState(blockPos);
			
			// Get player's actual position (with decimal precision)
			Vec3 playerPos = context.getPlayer().position();
			
			// Track the block placement (using block position for tracking, but log player position)
			Blockbase.blockTracker.trackBlockPlace(blockPos, placedState, world);
			Blockbase.LOGGER.info("Block placed at block ({}, {}, {}), player position ({}, {}, {}): {}", 
				blockPos.getX(), 
				blockPos.getY(), 
				blockPos.getZ(),
				String.format("%.4f", playerPos.x),
				String.format("%.4f", playerPos.y),
				String.format("%.4f", playerPos.z),
				placedState.getBlock().getName().getString());
		}
	}
}

