package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 床：真的睡下去（variant 0）→ 夢境結界；在地獄 / 終界爆炸（variant 1） */
@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void chuuni$bed(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		if (player.isSleeping()) {
			BlockPos head = player.getSleepingPos().orElse(pos);
			FxNet.emit(server, new FxPayload(FxPayload.Kind.BED, head.immutable(), 1, 0, 0, player.getId(), ItemStack.EMPTY));
		}
		else if (!(level.getBlockState(pos).getBlock() instanceof BedBlock) && cir.getReturnValue().consumesAction()) {
			FxNet.emit(server, new FxPayload(FxPayload.Kind.BED, pos.immutable(), 1, 0, 1, player.getId(), ItemStack.EMPTY));
		}
	}
}
