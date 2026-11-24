package com.chuuni.mixin;

import com.chuuni.Triggers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 睡過一夜（所有人被叫醒的那一刻還在床上的玩家）→ SLEEP */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {
	@Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
	private void chuuni$dawn(CallbackInfo ci) {
		for (ServerPlayer p : ((ServerLevel)(Object)this).players()) {
			if (p.isSleeping()) {
				Triggers.sleep(p);
			}
		}
	}
}
