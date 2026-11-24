package com.chuuni.mixin;

import com.chuuni.Triggers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 頭目倒下 → BOSS */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDieMixin {
	@Shadow
	protected boolean dead;

	@Inject(method = "die", at = @At("HEAD"))
	private void chuuni$die(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;

		if (dead || self.level().isClientSide()) {
			return;
		}

		int variant = self instanceof EnderDragon ? 0 : self instanceof WitherBoss ? 1 : self instanceof Warden ? 2 : self instanceof ElderGuardian ? 3 : -1;

		if (variant >= 0) {
			Triggers.boss(self, variant, source.getEntity());
		}
	}
}
