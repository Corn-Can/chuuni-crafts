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

/** 展示框：放入（0）/ 旋轉（1）→ ITEM_FRAME（C 級，帶物品） */
@Mixin(net.minecraft.world.entity.decoration.ItemFrame.class)
public abstract class ItemFrameMixin {
	@Unique
	private boolean chuuni$had;

	@Inject(method = "interact", at = @At("HEAD"))
	private void chuuni$head(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$had = !((net.minecraft.world.entity.decoration.ItemFrame)(Object)this).getItem().isEmpty();
	}

	@Inject(method = "interact", at = @At("RETURN"))
	private void chuuni$use(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		var self = (net.minecraft.world.entity.decoration.ItemFrame)(Object)this;

		if (cir.getReturnValue().consumesAction() && !self.getItem().isEmpty()) {
			Triggers.mob(self.level(), Kind.ITEM_FRAME, self, self.getRotation(), chuuni$had ? 1 : 0, player, self.getItem(), 0);
		}
	}
}
