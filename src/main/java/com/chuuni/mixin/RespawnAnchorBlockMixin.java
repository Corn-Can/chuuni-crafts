package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 重生錨：充能（variant 0，power = 充能數）／設定重生（1）／在錯的世界爆炸（2） */
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {
	@Unique
	private int chuuni$before;

	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void chuuni$head(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
		CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$before = state.getValue(RespawnAnchorBlock.CHARGE);
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void chuuni$charged(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
		CallbackInfoReturnable<InteractionResult> cir) {
		BlockState now = level.getBlockState(pos);

		if (level instanceof ServerLevel server && now.getBlock() instanceof RespawnAnchorBlock && now.getValue(RespawnAnchorBlock.CHARGE) > chuuni$before) {
			FxNet.emit(server, new FxPayload(FxPayload.Kind.ANCHOR, pos.immutable(), now.getValue(RespawnAnchorBlock.CHARGE), 0, 0, player.getId(), ItemStack.EMPTY,
				hit.getLocation(), hit.getDirection().ordinal()));
		}
	}

	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void chuuni$spawn(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (level instanceof ServerLevel server && cir.getReturnValue().consumesAction() && state.getValue(RespawnAnchorBlock.CHARGE) > 0
			&& RespawnAnchorBlock.canSetSpawn(server, pos) && level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
			FxNet.emit(server, new FxPayload(FxPayload.Kind.ANCHOR, pos.immutable(), state.getValue(RespawnAnchorBlock.CHARGE), 0, 1, player.getId(), ItemStack.EMPTY));
		}
	}

	@Inject(method = "explode", at = @At("HEAD"))
	private void chuuni$explode(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
		FxNet.emit(level, new FxPayload(FxPayload.Kind.ANCHOR, pos.immutable(), state.getValue(RespawnAnchorBlock.CHARGE), 0, 2, Triggers.nearest(level, pos),
			ItemStack.EMPTY));
	}
}
