package com.chuuni.mixin;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 羅盤綁定磁石 → 磁極指引 */
@Mixin(CompassItem.class)
public abstract class CompassItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$lodestone(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		if (ctx.getLevel() instanceof ServerLevel level && cir.getReturnValue().consumesAction() && ctx.getPlayer() != null
			&& level.getBlockState(ctx.getClickedPos()).is(Blocks.LODESTONE)) {
			FxNet.emit(level, new FxPayload(FxPayload.Kind.LODESTONE, ctx.getClickedPos().immutable(), 1, 0, 0, ctx.getPlayer().getId(), new ItemStack(Items.COMPASS),
				ctx.getClickLocation(), ctx.getClickedFace().ordinal()));
		}
	}
}
