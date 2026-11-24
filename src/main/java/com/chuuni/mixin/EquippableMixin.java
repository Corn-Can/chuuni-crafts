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

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 鞍、馬鎧、狼鎧、挽具、駱馬地毯…裝到生物身上 → EQUIP（帶裝備） */
@Mixin(net.minecraft.world.item.equipment.Equippable.class)
public abstract class EquippableMixin {
	@Unique
	private static final ThreadLocal<ItemStack> chuuni$gear = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

	@Inject(method = "equipOnTarget", at = @At("HEAD"))
	private void chuuni$head(Player player, LivingEntity target, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
		chuuni$gear.set(stack.copyWithCount(1));
	}

	@Inject(method = "equipOnTarget", at = @At("RETURN"))
	private void chuuni$equip(Player player, LivingEntity target, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			Triggers.mob(player.level(), Kind.EQUIP, target, 1, 0, player, chuuni$gear.get(), 4);
		}
	}
}
