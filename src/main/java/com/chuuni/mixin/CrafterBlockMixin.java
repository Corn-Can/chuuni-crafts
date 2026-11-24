package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxPayload.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 合成器成功吐出成品 → 機巧自律（同一台 1 秒內只演一次） */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
	@Inject(method = "dispenseItem", at = @At("HEAD"))
	private void chuuni$craft(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.entity.CrafterBlockEntity be, ItemStack result, BlockState state,
		net.minecraft.world.item.crafting.RecipeHolder<?> recipe, CallbackInfo ci) {
		Triggers.minor(level, Kind.CRAFTER, pos, result.getCount(), 0, 0, null, result, null, -1, 20);
	}
}
