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

/** 銅魔像雕像換姿勢 → 銅像覺醒（power = 新姿勢） */
@Mixin(CopperGolemStatueBlock.class)
public abstract class CopperGolemStatueBlockMixin {
	@Inject(method = "updatePose", at = @At("RETURN"))
	private void chuuni$pose(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
		BlockState now = level.getBlockState(pos);

		if (now.hasProperty(CopperGolemStatueBlock.POSE)) {
			Triggers.minor(level, Kind.COPPER_GOLEM, pos, now.getValue(CopperGolemStatueBlock.POSE).ordinal(), 0, 0, player, ItemStack.EMPTY, null, -1, 0);
		}
	}
}
