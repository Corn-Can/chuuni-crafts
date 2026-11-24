package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 取出合成成品 → 萬象構築・解放（shift 量產時同 tick 合併計數成 COMBO） */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
	// shift 一鍵取出時 onTake 拿到的是已被搬空的物品；checkTakeAchievements 在兩種取出方式都會拿到真正的成品
	@Shadow
	@Final
	private Player player;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void chuuni$craft(ItemStack stack, CallbackInfo ci) {
		Player player = this.player;

		if (!(player.level() instanceof ServerLevel level) || stack.isEmpty()) {
			return;
		}

		if (player.containerMenu instanceof CraftingMenu menu) {
			((CraftingMenuAccessor)menu).chuuni$access().execute((l, pos) -> FxNet.batch(level, player,
				new FxPayload(FxPayload.Kind.CRAFT, pos, 1, 1, 0, player.getId(), FxNet.copy(stack))));
		}
		else {
			// 背包 2×2：沒有方塊，演出在玩家身上
			FxNet.batch(level, player, new FxPayload(FxPayload.Kind.CRAFT, player.blockPosition(), 1, 0, 0, player.getId(), FxNet.copy(stack)));
		}
	}
}
