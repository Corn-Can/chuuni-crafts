package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 附魔成功（clickMenuButton 回傳 true 且確實花了等級）→ 禁咒・星辰刻印 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	@Final
	private Container enchantSlots;

	@Shadow
	@Final
	public int[] costs;

	@Unique
	private int chuuni$cost;

	@Unique
	private int chuuni$levelBefore;

	@Inject(method = "clickMenuButton", at = @At("HEAD"))
	private void chuuni$before(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		chuuni$cost = id >= 0 && id < costs.length ? costs[id] : 0;
		chuuni$levelBefore = player.experienceLevel;
	}

	@Inject(method = "clickMenuButton", at = @At("RETURN"))
	private void chuuni$after(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || player.level().isClientSide()) {
			return;
		}

		// 創造模式不扣等級，用 cost 判斷；生存模式則確認等級真的被扣了
		if (!player.getAbilities().instabuild && player.experienceLevel >= chuuni$levelBefore) {
			return;
		}

		int cost = chuuni$cost;
		access.execute((level, pos) -> {
			FxNet.emit((ServerLevel)level, new FxPayload(FxPayload.Kind.ENCHANT, pos, cost, id, 0, player.getId(), FxNet.copy(enchantSlots.getItem(0))));
			// 演出最後物品在聖光中降下 → 客戶端演完會來領取，變成只有本人能撿的掉落物
			com.chuuni.Rewards.expect(player, com.chuuni.Rewards.Source.ENCHANT, pos, enchantSlots.getItem(0), 0xFFE6B0);
		});
	}
}
