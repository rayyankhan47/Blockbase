package com.blockbase.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.FenceGateBlock;

import com.blockbase.Blockbase;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(Level.class)
public class LevelMixin {
	@Inject(
		method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
		at = @At("HEAD")
	)
	private void onBlockStateChange(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
		Level self = (Level)(Object)this;
		
		// Only track on server side
		if (!self.isClientSide()) {
			// Get the old block state before it changes
			BlockState oldState = self.getBlockState(pos);
			
			// Only track if the state actually changed (not just setting the same state)
			if (oldState != newState && !oldState.equals(newState)) {
				// Skip if this is a placement (oldState is air, newState is not)
				// Skip if this is a break (oldState is not air, newState is air)
				// These are already tracked by BlockItemMixin and PlayerBlockBreakEvents
				boolean isPlacement = oldState.isAir() && !newState.isAir();
				boolean isBreak = !oldState.isAir() && newState.isAir();
				
				if (!isPlacement && !isBreak) {
					Block block = oldState.getBlock();
					
					// Filter out noisy blocks that change state frequently (doors, trapdoors, buttons, etc.)
					// These are usually player interactions that we don't need to track for version control
					if (block instanceof DoorBlock || 
						block instanceof TrapDoorBlock || 
						block instanceof ButtonBlock || 
						block instanceof PressurePlateBlock || 
						block instanceof FenceGateBlock) {
						// Skip these - they're too noisy and not relevant for build version control
						return;
					}
					
					// This is a state modification (e.g., redstone power level change)
					Blockbase.blockTracker.trackBlockModify(pos, oldState, newState, self);
					
					// Find what properties changed to make the log more descriptive
					String changeDescription = getStateChangeDescription(oldState, newState);
					
					Blockbase.LOGGER.info("Block state changed at ({}, {}, {}): {} ({})", 
						pos.getX(), pos.getY(), pos.getZ(),
						block.getName().getString(),
						changeDescription);
				}
			}
		}
	}
	
	/**
	 * Get a descriptive string of what changed between two block states.
	 * For example: "power: 0 -> 15" for redstone wire
	 */
	private String getStateChangeDescription(BlockState oldState, BlockState newState) {
		Collection<Property<?>> oldProperties = oldState.getProperties();
		Collection<Property<?>> newProperties = newState.getProperties();
		
		// Find properties that changed
		java.util.List<String> changes = oldProperties.stream()
			.filter(prop -> newProperties.contains(prop))
			.filter(prop -> {
				Comparable<?> oldValue = oldState.getValue(prop);
				Comparable<?> newValue = newState.getValue(prop);
				return oldValue != null && newValue != null && !oldValue.equals(newValue);
			})
			.map(prop -> {
				Comparable<?> oldValue = oldState.getValue(prop);
				Comparable<?> newValue = newState.getValue(prop);
				return prop.getName() + ": " + oldValue + " -> " + newValue;
			})
			.collect(Collectors.toList());
		
		if (changes.isEmpty()) {
			return "state changed (unknown property)";
		}
		
		return String.join(", ", changes);
	}
}

