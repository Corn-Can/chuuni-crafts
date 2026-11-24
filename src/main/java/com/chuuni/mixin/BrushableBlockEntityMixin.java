package com.chuuni.mixin;

import com.chuuni.Rewards;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 刷出可疑方塊裡的寶物 → 古代遺物。
 * 裝了模組的玩家：寶物先由伺服器保管，演出最後在光中降下時才變成只有本人能撿的掉落物（逾時沒領也會照樣掉出來，不會消失）。
 */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin {
	@WrapOperation(method = "dropContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean chuuni$relic(ServerLevel level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity user) {
		var pos = ((BlockEntity)(Object)this).getBlockPos();

		if (entity instanceof ItemEntity item && !item.getItem().isEmpty()) {
			FxNet.emit(level, new FxPayload(FxPayload.Kind.BRUSH, pos, 1, 0, 0, user.getId(), FxNet.copy(item.getItem())));

			if (user instanceof ServerPlayer player && ServerPlayNetworking.canSend(player, FxPayload.TYPE) && Rewards.wants(player)) {
				Rewards.hold(player, level, pos, item.getItem().copy(), 0xFFD8A0);
				return true;
			}
		}

		return original.call(level, entity);
	}
}
