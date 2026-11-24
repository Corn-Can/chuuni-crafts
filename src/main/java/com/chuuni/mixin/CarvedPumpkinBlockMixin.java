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

/** 南瓜頭完成鐵巨人（GOLEM 0，A 級）/ 雪人（1）/ 銅魔像（2）→ extra = 生物 id */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
	@Inject(method = "spawnGolemInWorld", at = @At("RETURN"))
	private static void chuuni$golem(Level level, net.minecraft.world.level.block.state.pattern.BlockPattern.BlockPatternMatch match, Entity golem, BlockPos pos,
		CallbackInfo ci) {
		int variant = golem instanceof net.minecraft.world.entity.animal.golem.IronGolem ? 0 : golem instanceof net.minecraft.world.entity.animal.golem.SnowGolem ? 1 : 2;
		Triggers.mob(level, Kind.GOLEM, golem, 1, variant, null, ItemStack.EMPTY, 0);
	}
}
