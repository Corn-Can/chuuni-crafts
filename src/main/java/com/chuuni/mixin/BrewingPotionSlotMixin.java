package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 從釀造台取出藥水 → 煉金祕藥（同 tick 取出多瓶合併，power = 瓶數） */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingPotionSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$brew(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level() instanceof ServerLevel level && ((Slot)(Object)this).container instanceof BrewingStandBlockEntity stand) {
			ItemStack shown = stack.isEmpty() ? new ItemStack(Items.POTION) : stack;
			FxNet.batch(level, player, new FxPayload(FxPayload.Kind.BREWING, stand.getBlockPos(), 1, 0, 0, player.getId(), FxNet.copy(shown)));
		}
	}
}
