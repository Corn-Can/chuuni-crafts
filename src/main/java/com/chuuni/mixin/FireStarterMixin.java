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

/** 打火石 / 火焰彈點燃：蠟燭（CANDLE 0，power = 根數）、蠟燭蛋糕（CANDLE 2）、營火（CAMPFIRE 0） */
@Mixin({net.minecraft.world.item.FlintAndSteelItem.class, net.minecraft.world.item.FireChargeItem.class})
public abstract class FireStarterMixin {
	@Unique
	private static final ThreadLocal<BlockState> chuuni$before = ThreadLocal.withInitial(() -> null);

	@Inject(method = "useOn", at = @At("HEAD"))
	private void chuuni$head(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$before.set(ctx.getLevel().getBlockState(ctx.getClickedPos()));
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$use(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState old = chuuni$before.get();
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();

		if (old == null || !cir.getReturnValue().consumesAction() || !(level instanceof ServerLevel)) {
			return;
		}

		BlockState now = level.getBlockState(pos);

		if (!now.hasProperty(BlockStateProperties.LIT) || !now.getValue(BlockStateProperties.LIT) || old.hasProperty(BlockStateProperties.LIT)
			&& old.getValue(BlockStateProperties.LIT)) {
			return;
		}

		if (now.getBlock() instanceof CampfireBlock) {
			Triggers.minor(level, Kind.CAMPFIRE, pos, 1, 0, 0, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(), ctx.getClickedFace().ordinal(), 4);
		}
		else if (now.getBlock() instanceof CandleCakeBlock) {
			Triggers.minor(level, Kind.CANDLE, pos, 1, 0, 2, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(), ctx.getClickedFace().ordinal(), 4);
		}
		else if (now.getBlock() instanceof CandleBlock) {
			Triggers.minor(level, Kind.CANDLE, pos, now.getValue(CandleBlock.CANDLES), 0, 0, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(),
				ctx.getClickedFace().ordinal(), 4);
		}
	}
}
