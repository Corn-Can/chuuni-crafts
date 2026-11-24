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

/** 裝飾陶罐放入物品 → POT 0（帶物品） */
@Mixin(DecoratedPotBlock.class)
public abstract class DecoratedPotBlockMixin {
	@Unique
	private static final ThreadLocal<ItemStack> chuuni$held = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void chuuni$head(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$held.set(stack.copyWithCount(1));
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void chuuni$insert(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && !(cir.getReturnValue() instanceof InteractionResult.TryEmptyHandInteraction) && !chuuni$held.get().isEmpty()) {
			Triggers.minor(level, Kind.POT, pos, 1, 0, 0, player, chuuni$held.get(), hit);
		}
	}
}
