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

/** 擠牛奶（0）/ 山羊奶（1）/ 蘑菇燉湯（2）→ MILK */
@Mixin({net.minecraft.world.entity.animal.cow.AbstractCow.class, net.minecraft.world.entity.animal.goat.Goat.class})
public abstract class MilkMixin {
	@Unique
	private Item chuuni$held;

	@Inject(method = "mobInteract", at = @At("HEAD"))
	private void chuuni$head(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$held = player.getItemInHand(hand).getItem();
	}

	@Inject(method = "mobInteract", at = @At("RETURN"))
	private void chuuni$milk(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}

		Object self = this;
		int variant = chuuni$held == Items.BUCKET ? (self instanceof net.minecraft.world.entity.animal.goat.Goat ? 1 : 0)
			: chuuni$held == Items.BOWL && self instanceof net.minecraft.world.entity.animal.cow.MushroomCow ? 2 : -1;

		if (variant >= 0) {
			Triggers.mob(player.level(), Kind.MILK, (Entity)self, 1, variant, player, ItemStack.EMPTY, 4);
		}
	}
}
