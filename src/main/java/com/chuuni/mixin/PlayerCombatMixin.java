package com.chuuni.mixin;

import com.chuuni.Triggers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 橫掃 → SWEEP（爆擊在 {@link ServerPlayerCritMixin}：ServerPlayer 覆寫了 crit 且不呼叫 super） */
@Mixin(Player.class)
public abstract class PlayerCombatMixin {
	@Inject(method = "doSweepAttack", at = @At("HEAD"))
	private void chuuni$sweep(Entity target, float damage, DamageSource source, float strength, CallbackInfo ci) {
		Triggers.sweep((Player)(Object)this, target);
	}
}
