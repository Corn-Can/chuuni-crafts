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

/** 堆肥桶：投入且層數上升（COMPOSTER 0，power = 層數）／取出骨粉（1） */
@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
	@Inject(method = "addItem", at = @At("RETURN"))
	private static void chuuni$insert(Entity entity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack stack, CallbackInfoReturnable<BlockState> cir) {
		int before = state.getValue(ComposterBlock.LEVEL);
		BlockState now = cir.getReturnValue();

		if (entity instanceof Player && now.hasProperty(ComposterBlock.LEVEL) && now.getValue(ComposterBlock.LEVEL) > before) {
			if (level instanceof Level l) {
				Triggers.minor(l, Kind.COMPOSTER, pos, now.getValue(ComposterBlock.LEVEL), 0, 0, entity, stack, null, -1, 0);
			}
		}
	}

	@Inject(method = "extractProduce", at = @At("HEAD"))
	private static void chuuni$extract(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (entity instanceof Player) {
			Triggers.minor(level, Kind.COMPOSTER, pos, 8, 0, 1, entity, new ItemStack(Items.BONE_MEAL), null, -1, 4);
		}
	}
}
