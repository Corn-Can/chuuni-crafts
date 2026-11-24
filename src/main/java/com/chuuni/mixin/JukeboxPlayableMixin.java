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

/** 唱片放進唱片機 → 天籟之音 */
@Mixin(net.minecraft.world.item.JukeboxPlayable.class)
public abstract class JukeboxPlayableMixin {
	@Unique
	private static final ThreadLocal<ItemStack> chuuni$disc = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

	@Inject(method = "tryInsertIntoJukebox", at = @At("HEAD"))
	private static void chuuni$head(Level level, BlockPos pos, ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$disc.set(stack.copyWithCount(1));
	}

	@Inject(method = "tryInsertIntoJukebox", at = @At("RETURN"))
	private static void chuuni$insert(Level level, BlockPos pos, ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && level.getBlockState(pos).is(Blocks.JUKEBOX)) {
			Triggers.minor(level, Kind.JUKEBOX, pos, 1, 0, 0, player, chuuni$disc.get(), null, -1, 4);
		}
	}
}
