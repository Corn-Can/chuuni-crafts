package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 製圖台的成品格（匿名類別 CartographyTableMenu$5）取出 → 天地測繪・全知之眼 */
@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$5")
public abstract class CartographyResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$map(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level() instanceof ServerLevel level && player.containerMenu instanceof CartographyTableMenu menu) {
			((CartographyTableMenuAccessor)menu).chuuni$access().execute((l, pos) -> FxNet.batch(level, player,
				new FxPayload(FxPayload.Kind.CARTOGRAPHY, pos, 1, 0, 0, player.getId(), FxNet.copy(stack))));
		}
	}
}
