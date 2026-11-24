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

/** 生怪蛋用在生怪磚（0）/ 試煉生怪磚（1）→ 魂籠召喚 */
@Mixin(net.minecraft.world.item.SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
	@Unique
	private static final ThreadLocal<ItemStack> chuuni$egg = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

	@Inject(method = "useOn", at = @At("HEAD"))
	private void chuuni$head(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$egg.set(ctx.getItemInHand().copyWithCount(1));
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$use(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState s = ctx.getLevel().getBlockState(ctx.getClickedPos());

		if (cir.getReturnValue().consumesAction() && (s.is(Blocks.SPAWNER) || s.is(Blocks.TRIAL_SPAWNER))) {
			Triggers.minor(ctx.getLevel(), Kind.SPAWNER, ctx.getClickedPos(), 1, 0, s.is(Blocks.TRIAL_SPAWNER) ? 1 : 0, ctx.getPlayer(), chuuni$egg.get(),
				ctx.getClickLocation(), ctx.getClickedFace().ordinal(), 4);
		}
	}
}
