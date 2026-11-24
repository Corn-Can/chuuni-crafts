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

/** 切石機成品格（StonecutterMenu$2）取出 → 斷岩一閃（同 tick 合併件數） */
@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2")
public abstract class StonecutterResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$cut(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level() instanceof ServerLevel level && player.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu menu) {
			ItemStack shown = stack.isEmpty() ? menu.getSlot(1).getItem() : stack;
			((StonecutterMenuAccessor)menu).chuuni$access().execute((l, pos) -> com.chuuni.net.FxNet.batch(level, player,
				new com.chuuni.net.FxPayload(Kind.STONECUTTER, pos, Math.max(1, stack.getCount()), 0, 0, player.getId(), com.chuuni.net.FxNet.copy(shown))));
		}
	}
}
