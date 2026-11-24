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

/** 治療殭屍村民：開始（CURE 0）/ 完成（CURE 1） */
@Mixin(net.minecraft.world.entity.monster.zombie.ZombieVillager.class)
public abstract class ZombieVillagerMixin {
	@Inject(method = "startConverting", at = @At("HEAD"))
	private void chuuni$start(java.util.UUID who, int time, CallbackInfo ci) {
		Entity self = (Entity)(Object)this;
		Entity player = who == null ? null : self.level().getPlayerByUUID(who);
		Triggers.mob(self.level(), Kind.CURE, self, 1, 0, player, ItemStack.EMPTY, 0);
	}

	@Inject(method = "finishConversion", at = @At("HEAD"))
	private void chuuni$finish(ServerLevel level, CallbackInfo ci) {
		Triggers.mob(level, Kind.CURE, (Entity)(Object)this, 1, 1, null, ItemStack.EMPTY, 0);
	}
}
