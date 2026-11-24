package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 終界之眼嵌入門框（variant 0，power = 附近已嵌入的眼數）／12 顆滿、門開啟（variant 1，at = 門的中心） */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$eye(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(ctx.getLevel() instanceof ServerLevel level) || !cir.getReturnValue().consumesAction() || ctx.getPlayer() == null) {
			return;
		}

		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);

		if (!state.is(Blocks.END_PORTAL_FRAME) || !state.getValue(EndPortalFrameBlock.HAS_EYE)) {
			return;
		}

		BlockPattern.BlockPatternMatch match = EndPortalFrameBlock.getOrCreatePortalShape().find(level, pos);
		int actor = ctx.getPlayer().getId();

		if (match != null) {
			BlockPos center = match.getFrontTopLeft().offset(-2, 0, -2);
			FxNet.emit(level, new FxPayload(FxPayload.Kind.END_PORTAL, center, 12, 0, 1, actor, ItemStack.EMPTY, Vec3.atCenterOf(center), -1));
			return;
		}

		int eyes = 0;

		for (BlockPos o : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 0, 4))) {
			BlockState s = level.getBlockState(o);

			if (s.is(Blocks.END_PORTAL_FRAME) && s.getValue(EndPortalFrameBlock.HAS_EYE)) {
				eyes++;
			}
		}

		FxNet.emit(level, new FxPayload(FxPayload.Kind.END_PORTAL, pos.immutable(), eyes, 0, 0, actor, ItemStack.EMPTY));
	}
}
