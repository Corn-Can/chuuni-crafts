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

/** 講台翻頁（LECTERN 1，power = 新頁碼、extra = 往前翻 1） */
@Mixin(net.minecraft.world.level.block.entity.LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
	@Unique
	private int chuuni$old;

	@Inject(method = "setPage", at = @At("HEAD"))
	private void chuuni$head(int page, CallbackInfo ci) {
		chuuni$old = ((net.minecraft.world.level.block.entity.LecternBlockEntity)(Object)this).getPage();
	}

	@Inject(method = "setPage", at = @At("RETURN"))
	private void chuuni$page(int page, CallbackInfo ci) {
		var self = (net.minecraft.world.level.block.entity.LecternBlockEntity)(Object)this;
		int now = self.getPage();

		if (self.getLevel() instanceof ServerLevel level && self.hasBook() && now != chuuni$old) {
			Triggers.minor(level, Kind.LECTERN, self.getBlockPos(), now, now < chuuni$old ? 1 : 0, 1, null, ItemStack.EMPTY, null, -1, 0);
		}
	}
}
