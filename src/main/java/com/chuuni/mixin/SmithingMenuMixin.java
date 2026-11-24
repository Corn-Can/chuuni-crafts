package com.chuuni.mixin;

import com.chuuni.LastResult;
import com.chuuni.Rewards;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 鍛造台取出成品 → 神匠鍛冶（最後成品在光中降下 → 專屬掉落物） */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {
	protected SmithingMenuMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition def) {
		super(type, id, inv, access, def);
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$smith(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}

		ItemStack result = ((LastResult)this).chuuni$result(stack);

		if (result.isEmpty()) {
			return;
		}

		this.access.execute((level, pos) -> {
			FxNet.emit((ServerLevel)level, new FxPayload(FxPayload.Kind.SMITHING, pos, 1, 0, 0, player.getId(), FxNet.copy(result)));
			Rewards.expect(player, Rewards.Source.INVENTORY, pos, result, 0xFFD890);
		});
	}
}
