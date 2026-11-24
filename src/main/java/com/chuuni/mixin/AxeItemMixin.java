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

/** 斧頭：刮除銅鏽（0）/ 除蠟（1）。剝樹皮屬於 C 級 */
@Mixin(net.minecraft.world.item.AxeItem.class)
public abstract class AxeItemMixin {
	@Unique
	private static final ThreadLocal<BlockState> chuuni$before = ThreadLocal.withInitial(() -> null);

	@Inject(method = "useOn", at = @At("HEAD"))
	private void chuuni$head(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$before.set(ctx.getLevel().getBlockState(ctx.getClickedPos()));
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void chuuni$use(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState old = chuuni$before.get();

		if (old == null || !cir.getReturnValue().consumesAction() || !(ctx.getLevel() instanceof ServerLevel)) {
			return;
		}

		int variant;

		if (net.minecraft.world.item.HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(old.getBlock())) {
			variant = 1;
		}
		else if (old.getBlock() instanceof WeatheringCopper) {
			variant = 0;
		}
		else {
			// 其他都是剝樹皮（原木 / 木頭 / 菌柄 / 竹塊）
			Triggers.minor(ctx.getLevel(), Kind.STRIP, ctx.getClickedPos(), 1, 0, 0, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(),
				ctx.getClickedFace().ordinal(), 0);
			return;
		}

		Triggers.minor(ctx.getLevel(), Kind.SCRAPE, ctx.getClickedPos(), 1, 0, variant, ctx.getPlayer(), ItemStack.EMPTY, ctx.getClickLocation(),
			ctx.getClickedFace().ordinal(), 4);
	}
}
