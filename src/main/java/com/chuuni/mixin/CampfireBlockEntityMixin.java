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

/** 營火放上食材 → CAMPFIRE 2（帶食材） */
@Mixin(net.minecraft.world.level.block.entity.CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
	@Inject(method = "placeFood", at = @At("RETURN"))
	private void chuuni$food(ServerLevel level, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			BlockPos pos = ((net.minecraft.world.level.block.entity.BlockEntity)(Object)this).getBlockPos();
			Triggers.minor(level, Kind.CAMPFIRE, pos, 1, 0, 2, entity, stack.isEmpty() ? new ItemStack(Items.BEEF) : stack, null, -1, 4);
		}
	}
}
