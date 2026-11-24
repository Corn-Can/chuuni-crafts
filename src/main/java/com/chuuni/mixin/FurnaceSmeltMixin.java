package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 爐子每煉好一件 → 爐口噴出火之精靈（小演出，只有附近有人才送） */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceSmeltMixin {
	@ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/NonNullList;I)Z"))
	private static boolean chuuni$smelted(boolean burned, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) BlockState state, @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
		if (burned && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, false) != null) {
			FxNet.emit(level, new FxPayload(FxPayload.Kind.FURNACE_SMELT, pos.immutable(), 1, 0, Triggers.furnaceVariant(state), -1, FxNet.copy(furnace.getItem(2))));
		}

		return burned;
	}

}
