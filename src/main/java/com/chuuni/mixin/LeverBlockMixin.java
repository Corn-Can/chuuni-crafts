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

/** 拉桿 → LEVER（power = 1 開 / 0 關） */
@Mixin(LeverBlock.class)
public abstract class LeverBlockMixin {
	@Inject(method = "pull", at = @At("RETURN"))
	private void chuuni$pull(BlockState state, Level level, BlockPos pos, Player player, CallbackInfo ci) {
		BlockState now = level.getBlockState(pos);
		int on = now.hasProperty(LeverBlock.POWERED) && now.getValue(LeverBlock.POWERED) ? 1 : 0;
		Triggers.minor(level, Kind.LEVER, pos, on, 0, 0, player, ItemStack.EMPTY, null, -1, 0);
	}
}
