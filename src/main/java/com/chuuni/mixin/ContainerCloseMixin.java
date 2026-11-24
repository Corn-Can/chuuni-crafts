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

/** 箱子 / 陷阱箱 / 銅箱 / 木桶 / 漏斗 / 發射器 / 投擲器：關掉介面（蓋子闔上）→ CONTAINER，variant 依方塊 */
@Mixin({net.minecraft.world.inventory.ChestMenu.class, net.minecraft.world.inventory.HopperMenu.class, net.minecraft.world.inventory.DispenserMenu.class})
public abstract class ContainerCloseMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void chuuni$close(Player player, CallbackInfo ci) {
		net.minecraft.world.Container box = com.chuuni.Triggers.menuContainer(this);

		if (box instanceof net.minecraft.world.CompoundContainer compound) {
			box = ((CompoundContainerAccessor)compound).chuuni$first();
		}

		if (box instanceof net.minecraft.world.level.block.entity.BlockEntity be && be.getLevel() instanceof ServerLevel level) {
			BlockState s = be.getBlockState();
			int variant = s.is(Blocks.TRAPPED_CHEST) ? 1 : s.is(Blocks.BARREL) ? 2 : s.is(Blocks.HOPPER) ? 3 : s.is(Blocks.DISPENSER) ? 4 : s.is(Blocks.DROPPER) ? 5
				: s.getBlock() instanceof CopperChestBlock ? 6 : 0;
			Triggers.minor(level, Kind.CONTAINER, be.getBlockPos(), 1, 0, variant, player, ItemStack.EMPTY, null, -1, 2);
		}
	}
}
