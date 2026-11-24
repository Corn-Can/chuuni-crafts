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

/** 告示牌：染色（SIGN 0，extra = 顏色）／發光墨囊（1）／墨囊取消發光（2） */
@Mixin({net.minecraft.world.item.DyeItem.class, net.minecraft.world.item.GlowInkSacItem.class, net.minecraft.world.item.InkSacItem.class})
public abstract class SignApplicatorMixin {
	@Inject(method = "tryApplyToSign", at = @At("RETURN"))
	private void chuuni$sign(Level level, net.minecraft.world.level.block.entity.SignBlockEntity sign, boolean front, Player player,
		CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}

		Object self = this;
		int variant = self instanceof net.minecraft.world.item.DyeItem ? 0 : self instanceof net.minecraft.world.item.GlowInkSacItem ? 1 : 2;
		int color = self instanceof net.minecraft.world.item.DyeItem dye ? dye.getDyeColor().getTextColor() : 0;
		Triggers.minor(level, Kind.SIGN, sign.getBlockPos(), front ? 1 : 0, color, variant, player, ItemStack.EMPTY, null, -1, 4);
	}
}
