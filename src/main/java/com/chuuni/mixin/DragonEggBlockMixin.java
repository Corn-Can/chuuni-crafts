package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 龍蛋瞬移（伺服器端真的搬了位置）→ 龍之遷躍（pos = 原位置，at = 新位置） */
@Mixin(DragonEggBlock.class)
public abstract class DragonEggBlockMixin {
	@Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
	private void chuuni$warp(BlockState state, Level level, BlockPos pos, CallbackInfo ci, @Local(ordinal = 1) BlockPos dest) {
		if (level instanceof ServerLevel server) {
			FxNet.emit(server, new FxPayload(FxPayload.Kind.DRAGON_EGG, pos.immutable(), 1, 0, 0, Triggers.nearest(server, pos), ItemStack.EMPTY,
				Vec3.atCenterOf(dest), -1));
		}
	}
}
