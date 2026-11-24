package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 鐵砧取出成品 → 千錘百鍊・魂鑄（花費與是否改名一併送出） */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private String itemName;

	protected AnvilMenuMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition def) {
		super(type, id, inv, access, def);
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$forge(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}

		int c = Math.max(1, cost.get());
		int renamed = itemName != null && !itemName.isBlank() && itemName.equals(stack.getHoverName().getString()) ? 1 : 0;
		this.access.execute((level, pos) -> FxNet.emit((ServerLevel)level,
			new FxPayload(FxPayload.Kind.ANVIL, pos, c, renamed, 0, player.getId(), FxNet.copy(stack))));
	}
}
