package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 砂輪成品格（匿名類別 GrindstoneMenu$4）取出 → 魔力剝奪。展示用的是「剝除前」的輸入物品，power = 被剝掉的附魔數 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$grind(Player player, ItemStack stack, CallbackInfo ci) {
		if (!(player.level() instanceof ServerLevel level) || !(player.containerMenu instanceof GrindstoneMenu menu)) {
			return;
		}

		ItemStack input = menu.getSlot(0).getItem().isEmpty() ? menu.getSlot(1).getItem() : menu.getSlot(0).getItem();
		int count = EnchantmentHelper.getEnchantmentsForCrafting(input).size();
		((GrindstoneMenuAccessor)menu).chuuni$access().execute((l, pos) -> FxNet.emit(level,
			new FxPayload(FxPayload.Kind.GRINDSTONE, pos, count, 0, 0, player.getId(), FxNet.copy(input))));
	}
}
