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

/** 織布機成品格（LoomMenu$6）取出 → 天衣織紋 */
@Mixin(targets = "net.minecraft.world.inventory.LoomMenu$6")
public abstract class LoomResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void chuuni$weave(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level() instanceof ServerLevel level && player.containerMenu instanceof net.minecraft.world.inventory.LoomMenu menu) {
			((LoomMenuAccessor)menu).chuuni$access().execute((l, pos) -> Triggers.minor(level, Kind.LOOM, pos, 1, 0, 0, player, stack, null, -1, 4));
		}
	}
}
