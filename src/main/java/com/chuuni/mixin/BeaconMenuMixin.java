package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 烽火台確認效果（付款真的被收走）→ 天光降臨（power = 金字塔層數） */
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	@Final
	private ContainerData beaconData;

	@Shadow
	public abstract boolean hasPayment();

	@Unique
	private boolean chuuni$paid;

	@Inject(method = "updateEffects", at = @At("HEAD"))
	private void chuuni$head(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary, CallbackInfo ci) {
		chuuni$paid = hasPayment();
	}

	@Inject(method = "updateEffects", at = @At("TAIL"))
	private void chuuni$tail(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary, CallbackInfo ci) {
		if (!chuuni$paid || hasPayment()) {
			return;
		}

		int levels = beaconData.get(0);
		access.execute((level, pos) -> {
			if (level instanceof ServerLevel server) {
				FxNet.emit(server, new FxPayload(FxPayload.Kind.BEACON, pos, levels, 0, 0, Triggers.nearest(server, pos), ItemStack.EMPTY));
			}
		});
	}
}
