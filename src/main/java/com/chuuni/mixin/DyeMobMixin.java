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

/** 染羊 → DYE_MOB（power = 顏色） */
@Mixin(net.minecraft.world.item.DyeItem.class)
public abstract class DyeMobMixin {
	@Inject(method = "interactLivingEntity", at = @At("RETURN"))
	private void chuuni$dye(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			int color = ((net.minecraft.world.item.DyeItem)(Object)this).getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
			Triggers.mob(player.level(), Kind.DYE_MOB, target, color, 0, player, ItemStack.EMPTY, 4);
		}
	}
}
