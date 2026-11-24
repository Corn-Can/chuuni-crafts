package com.chuuni.mixin;

import com.chuuni.Triggers;
import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 地獄門成形 → 冥界開門（pos = 左下角，at = 門的中心，variant = 軸 X0 / Z1，power = 寬 × 100 + 高） */
@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {
	@Shadow
	@Final
	private Direction.Axis axis;

	@Shadow
	@Final
	private Direction rightDir;

	@Shadow
	@Final
	private BlockPos bottomLeft;

	@Shadow
	@Final
	private int height;

	@Shadow
	@Final
	private int width;

	@Inject(method = "createPortalBlocks", at = @At("HEAD"))
	private void chuuni$open(LevelAccessor level, CallbackInfo ci) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		Vec3 center = Vec3.atBottomCenterOf(bottomLeft)
			.add(rightDir.getStepX() * (width - 1) / 2.0, height / 2.0, rightDir.getStepZ() * (width - 1) / 2.0);
		FxNet.emit(server, new FxPayload(FxPayload.Kind.NETHER_PORTAL, bottomLeft, width * 100 + height, rightDir.ordinal(), axis == Direction.Axis.X ? 0 : 1,
			Triggers.nearest(server, bottomLeft), ItemStack.EMPTY, center, -1));
	}
}
