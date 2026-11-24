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

/** 比較器切模式 → DIODE 1（power = 1 減法 / 0 比較） */
@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void chuuni$mode(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState now = level.getBlockState(pos);

		if (cir.getReturnValue().consumesAction() && now.hasProperty(ComparatorBlock.MODE)) {
			int sub = now.getValue(ComparatorBlock.MODE) == net.minecraft.world.level.block.state.properties.ComparatorMode.SUBTRACT ? 1 : 0;
			Triggers.minor(level, Kind.DIODE, pos, sub, 0, 1, player, ItemStack.EMPTY, null, -1, 0);
		}
	}
}
