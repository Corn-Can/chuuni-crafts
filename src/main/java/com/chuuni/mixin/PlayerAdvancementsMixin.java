package com.chuuni.mixin;

import com.chuuni.Triggers;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 達成有顯示（會出現在進度畫面）的進度 → ADVANCEMENT */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Unique
	private boolean chuuni$wasDone;

	@Inject(method = "award", at = @At("HEAD"))
	private void chuuni$before(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		chuuni$wasDone = ((PlayerAdvancements)(Object)this).getOrStartProgress(holder).isDone();
	}

	@Inject(method = "award", at = @At("RETURN"))
	private void chuuni$after(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || chuuni$wasDone || !((PlayerAdvancements)(Object)this).getOrStartProgress(holder).isDone()) {
			return;
		}

		holder.value().display().ifPresent(d -> {
			if (d.shouldAnnounceChat() || d.shouldShowToast()) {
				int variant = d.getType() == AdvancementType.CHALLENGE ? 1 : d.getType() == AdvancementType.GOAL ? 2 : 0;
				Triggers.advancement(player, variant, d.getIcon());
			}
		});
	}
}
