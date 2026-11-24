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

/** 用拴繩拴住生物 → LEASH 0（C 級） */
@Mixin(net.minecraft.world.entity.Mob.class)
public abstract class MobLeashMixin {
	@Unique
	private boolean chuuni$was;

	@Inject(method = "interact", at = @At("HEAD"))
	private void chuuni$head(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$was = this instanceof net.minecraft.world.entity.Leashable l && l.isLeashed();
	}

	@Inject(method = "interact", at = @At("RETURN"))
	private void chuuni$leash(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && !chuuni$was && this instanceof net.minecraft.world.entity.Leashable l && l.isLeashed()) {
			Triggers.mob(player.level(), Kind.LEASH, (Entity)(Object)this, 1, 0, player, ItemStack.EMPTY, 2);
		}
	}
}
