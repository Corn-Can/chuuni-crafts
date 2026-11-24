package com.chuuni.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** shift 一鍵取出時 onTake 拿到的是已被搬空的物品：先記下成品，鍛造台 / 鐵砧的演出才知道做出了什麼 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin implements com.chuuni.LastResult {
	@Unique
	private ItemStack chuuni$last = ItemStack.EMPTY;

	@Inject(method = "quickMoveStack", at = @At("HEAD"))
	private void chuuni$remember(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		ItemCombinerMenu self = (ItemCombinerMenu)(Object)this;

		if (index == self.getResultSlot()) {
			chuuni$last = self.getSlot(index).getItem().copy();
		}
	}

	@Override
	public ItemStack chuuni$result(ItemStack taken) {
		return taken.isEmpty() ? chuuni$last : taken;
	}
}
