package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 試煉鑰匙插入寶庫、開始解鎖 → 祕寶解封（variant = 不祥寶庫 1） */
@Mixin(VaultBlock.class)
public abstract class VaultBlockMixin {
	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void chuuni$vault(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
		CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		BlockState now = level.getBlockState(pos);

		if (now.getBlock() instanceof VaultBlock && state.getValue(VaultBlock.STATE) != VaultState.UNLOCKING && now.getValue(VaultBlock.STATE) == VaultState.UNLOCKING) {
			FxNet.emit(server, new FxPayload(FxPayload.Kind.VAULT, pos.immutable(), 1, 0, now.getValue(VaultBlock.OMINOUS) ? 1 : 0, player.getId(), ItemStack.EMPTY,
				hit.getLocation(), hit.getDirection().ordinal()));
		}
	}
}
