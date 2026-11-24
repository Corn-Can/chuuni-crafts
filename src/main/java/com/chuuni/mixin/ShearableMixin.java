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

/** 剪羊毛（0）/ 哞菇（1）/ 雪人南瓜（2）/ 沼骸（3）/ 銅魔像（4）→ SHEAR */
@Mixin({net.minecraft.world.entity.animal.sheep.Sheep.class, net.minecraft.world.entity.animal.cow.MushroomCow.class, net.minecraft.world.entity.animal.golem.SnowGolem.class,
	net.minecraft.world.entity.monster.skeleton.Bogged.class, net.minecraft.world.entity.animal.golem.CopperGolem.class})
public abstract class ShearableMixin {
	@Inject(method = "shear", at = @At("HEAD"))
	private void chuuni$shear(ServerLevel level, net.minecraft.sounds.SoundSource source, ItemStack shears, CallbackInfo ci) {
		Object self = this;
		int variant = self instanceof net.minecraft.world.entity.animal.sheep.Sheep ? 0 : self instanceof net.minecraft.world.entity.animal.cow.MushroomCow ? 1
			: self instanceof net.minecraft.world.entity.animal.golem.SnowGolem ? 2 : self instanceof net.minecraft.world.entity.monster.skeleton.Bogged ? 3 : 4;
		int color = self instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep ? sheep.getColor().getTextureDiffuseColor() & 0xFFFFFF : 0;
		Triggers.mob(level, Kind.SHEAR, (Entity)self, color, variant, null, ItemStack.EMPTY, 0);
	}
}
