package com.chuuni.client.mixin;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 電影運鏡（覆寫鏡頭位置與角度、切成第三人稱以畫出玩家）＋ 鏡頭震動 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private float xRot;

	@Shadow
	private float yRot;

	@Shadow
	private Vec3 position;

	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void move(float forward, float up, float right);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "setup", at = @At("TAIL"))
	private void chuuni$camera(Level level, Entity entity, boolean detachedIn, boolean mirrored, float partialTick, CallbackInfo ci) {
		double[] shot = Cinematic.pose(this.position, this.yRot, this.xRot);

		if (shot != null) {
			this.setPosition(shot[0], shot[1], shot[2]);
			this.setRotation((float)shot[3], (float)shot[4]);
			this.detached = shot[5] > 0;
		}

		CameraShake.advance();
		float yaw = CameraShake.yaw(partialTick);
		float pitch = CameraShake.pitch(partialTick);

		if (yaw != 0f || pitch != 0f) {
			this.setRotation(this.yRot + yaw, this.xRot + pitch);
			this.move(0f, CameraShake.bob(partialTick), CameraShake.sway(partialTick));
		}
	}
}
