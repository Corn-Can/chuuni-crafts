package com.chuuni.mixin;

import com.chuuni.Triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 爆擊 → CRIT */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerCritMixin {
	@Inject(method = "crit", at = @At("HEAD"))
	private void chuuni$crit(Entity target, CallbackInfo ci) {
		Triggers.crit((ServerPlayer)(Object)this, target);
	}
}
