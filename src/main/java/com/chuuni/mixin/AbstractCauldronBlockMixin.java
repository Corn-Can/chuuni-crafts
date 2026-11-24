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

/** 鍋釜：注水(0) / 注熔岩(1) / 細雪(2) / 舀出(3) / 洗掉染色(4)；power = 之後的水位 */
@Mixin(AbstractCauldronBlock.class)
public abstract class AbstractCauldronBlockMixin {
	@Unique
	private static final ThreadLocal<Item> chuuni$held = ThreadLocal.withInitial(() -> null);

	@Unique
	private static int chuuni$level(BlockState s) {
		if (s.is(Blocks.LAVA_CAULDRON)) {
			return 3;
		}

		return s.hasProperty(LayeredCauldronBlock.LEVEL) ? s.getValue(LayeredCauldronBlock.LEVEL) : 0;
	}

	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void chuuni$head(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$held.set(stack.getItem());
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void chuuni$use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel) || !cir.getReturnValue().consumesAction()) {
			return;
		}

		BlockState now = level.getBlockState(pos);
		int before = chuuni$level(state);
		int after = chuuni$level(now);

		if (now == state) {
			return;
		}

		int variant;

		if (after > before || state.is(Blocks.CAULDRON) && !now.is(Blocks.CAULDRON)) {
			variant = now.is(Blocks.LAVA_CAULDRON) ? 1 : now.is(Blocks.POWDER_SNOW_CAULDRON) ? 2 : 0;
		}
		else {
			variant = chuuni$held.get() == Items.BUCKET || chuuni$held.get() == Items.GLASS_BOTTLE ? 3 : 4;
		}

		int extra = state.is(Blocks.LAVA_CAULDRON) ? 1 : state.is(Blocks.POWDER_SNOW_CAULDRON) ? 2 : 0;
		Triggers.minor(level, Kind.CAULDRON, pos, after, extra, variant, player, ItemStack.EMPTY, hit);
	}
}
