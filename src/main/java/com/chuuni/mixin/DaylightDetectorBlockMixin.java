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

/** 陽光感測器切換 → DAYLIGHT（power = 1 夜間模式） */
@Mixin(DaylightDetectorBlock.class)
public abstract class DaylightDetectorBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void chuuni$flip(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState now = level.getBlockState(pos);

		if (cir.getReturnValue().consumesAction() && now.hasProperty(DaylightDetectorBlock.INVERTED)) {
			Triggers.minor(level, Kind.DAYLIGHT, pos, now.getValue(DaylightDetectorBlock.INVERTED) ? 1 : 0, 0, 0, player, ItemStack.EMPTY, null, -1, 0);
		}
	}
}
