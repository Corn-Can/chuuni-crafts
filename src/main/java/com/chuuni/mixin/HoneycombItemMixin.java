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

/** 蜜蠟封緘：上蠟到銅方塊（0）/ 告示牌（1） */
@Mixin(net.minecraft.world.item.HoneycombItem.class)
public abstract class HoneycombItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$wax(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			Triggers.minor(ctx.getLevel(), Kind.WAX, ctx.getClickedPos(), 1, 0, 0, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(),
				ctx.getClickedFace().ordinal(), 4);
		}
	}

	@Inject(method = "tryApplyToSign", at = @At("RETURN"))
	private void chuuni$sign(Level level, net.minecraft.world.level.block.entity.SignBlockEntity sign, boolean front, Player player,
		CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			Triggers.minor(level, Kind.WAX, sign.getBlockPos(), 1, 0, 1, player, ItemStack.EMPTY, null, -1, 4);
		}
	}
}
