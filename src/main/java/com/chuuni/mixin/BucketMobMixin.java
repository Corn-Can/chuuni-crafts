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

/** 用水桶裝魚（0）/ 美西螈（1）/ 蝌蚪（2）→ BUCKET_MOB */
@Mixin({net.minecraft.world.entity.animal.fish.AbstractFish.class, net.minecraft.world.entity.animal.axolotl.Axolotl.class, net.minecraft.world.entity.animal.frog.Tadpole.class})
public abstract class BucketMobMixin {
	@Inject(method = "mobInteract", at = @At("RETURN"))
	private void chuuni$catch(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		Entity self = (Entity)(Object)this;

		if (cir.getReturnValue().consumesAction() && self.isRemoved()) {
			int variant = self instanceof net.minecraft.world.entity.animal.axolotl.Axolotl ? 1 : self instanceof net.minecraft.world.entity.animal.frog.Tadpole ? 2 : 0;
			Triggers.mob(player.level(), Kind.BUCKET_MOB, self, 1, variant, player, ItemStack.EMPTY, 0);
		}
	}
}
