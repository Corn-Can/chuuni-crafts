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

/** 水桶：舀起（BUCKET 0，extra = 液體 0 水 / 1 熔岩 / 2 細雪）／倒出（1）（C 級） */
@Mixin(net.minecraft.world.item.BucketItem.class)
public abstract class BucketItemMixin {
	@Unique
	private static final ThreadLocal<Item> chuuni$held = ThreadLocal.withInitial(() -> null);

	@Inject(method = "use", at = @At("HEAD"))
	private void chuuni$head(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$held.set(player.getItemInHand(hand).getItem());
	}

	@Inject(method = "use", at = @At("RETURN"))
	private void chuuni$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}

		boolean fill = chuuni$held.get() == Items.BUCKET;
		Item fluid = fill ? player.getItemInHand(hand).getItem() : chuuni$held.get();
		int extra = fluid == Items.LAVA_BUCKET ? 1 : fluid == Items.POWDER_SNOW_BUCKET ? 2 : 0;
		Vec3 at = player.pick(5, 0, !fill).getLocation();
		Triggers.minor(level, Kind.BUCKET, BlockPos.containing(at), 1, extra, fill ? 0 : 1, player, ItemStack.EMPTY, at, 1, 2);
	}
}
