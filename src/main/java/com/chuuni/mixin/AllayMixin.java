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

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 給悅靈物品（0）/ 拿回（1）→ ALLAY（帶物品） */
@Mixin(net.minecraft.world.entity.animal.allay.Allay.class)
public abstract class AllayMixin {
	@Unique
	private ItemStack chuuni$held = ItemStack.EMPTY;

	@Inject(method = "mobInteract", at = @At("HEAD"))
	private void chuuni$head(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$held = player.getItemInHand(hand).copyWithCount(1);
	}

	@Inject(method = "mobInteract", at = @At("RETURN"))
	private void chuuni$give(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			Triggers.mob(player.level(), Kind.ALLAY, (Entity)(Object)this, 1, chuuni$held.isEmpty() ? 1 : 0, player, chuuni$held, 4);
		}
	}
}
