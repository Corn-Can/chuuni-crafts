package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxPayload.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 花盆：種入（POT 1）／取出（POT 2） */
@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotBlockMixin {
	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void chuuni$plant(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState now = level.getBlockState(pos);

		if (cir.getReturnValue().consumesAction() && now != state) {
			Triggers.minor(level, Kind.POT, pos, 1, 0, now.is(Blocks.FLOWER_POT) ? 2 : 1, player, ItemStack.EMPTY, hit);
		}
	}

	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void chuuni$take(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && level.getBlockState(pos).is(Blocks.FLOWER_POT) && !state.is(Blocks.FLOWER_POT)) {
			Triggers.minor(level, Kind.POT, pos, 1, 0, 2, player, ItemStack.EMPTY, hit);
		}
	}
}
