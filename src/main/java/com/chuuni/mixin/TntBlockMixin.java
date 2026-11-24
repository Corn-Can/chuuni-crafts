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

/** TNT 被點燃（打火石、火焰彈、紅石、火箭…；連鎖爆炸不算）→ 爆炎序曲 */
@Mixin(TntBlock.class)
public abstract class TntBlockMixin {
	@Inject(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
	private static void chuuni$prime(Level level, BlockPos pos, LivingEntity igniter, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			Triggers.minor(level, Kind.TNT, pos, 1, 0, 0, igniter, ItemStack.EMPTY, null, -1, 20);
		}
	}
}
