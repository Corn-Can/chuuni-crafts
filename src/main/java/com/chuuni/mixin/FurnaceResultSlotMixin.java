package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 從熔爐 / 高爐 / 煙燻爐取出成品 → 獄炎煉成 */
@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotMixin {
	// shift 一鍵取出時 onTake 拿到的是已被搬空的物品；checkTakeAchievements 在兩種取出方式都會拿到真正的成品
	@Shadow
	@Final
	private Player player;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void chuuni$take(ItemStack stack, CallbackInfo ci) {
		Player player = this.player;

		if (player.level() instanceof ServerLevel level && !stack.isEmpty()
			&& ((Slot)(Object)this).container instanceof AbstractFurnaceBlockEntity furnace) {
			FxNet.batch(level, player, new FxPayload(FxPayload.Kind.FURNACE_TAKE, furnace.getBlockPos(), stack.getCount(), 0,
				Triggers.furnaceVariant(furnace.getBlockState()), player.getId(), FxNet.copy(stack)));
			int[] colors = {0xFFB060, 0x80C0FF, 0xE0B080};
			com.chuuni.Rewards.expect(player, com.chuuni.Rewards.Source.INVENTORY, furnace.getBlockPos(), stack,
				colors[Triggers.furnaceVariant(furnace.getBlockState())]);
		}
	}
}
