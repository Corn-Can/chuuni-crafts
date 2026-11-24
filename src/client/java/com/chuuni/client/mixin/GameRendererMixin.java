package com.chuuni.client.mixin;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.post.CinemaPost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 鏡頭翻滾（震動的 roll 分量）與 FOV 衝擊 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** 世界畫完、手還沒畫之前跑後製（深度緩衝還在） */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0))
	private void chuuni$post(DeltaTracker delta, CallbackInfo ci) {
		CinemaPost.render();
	}

	/** 運鏡時不畫第一人稱的手 */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void chuuni$noHand(float partial, boolean sleeping, org.joml.Matrix4f projection, CallbackInfo ci) {
		if (Cinematic.active()) {
			ci.cancel();
		}
	}

	@Inject(method = "bobHurt", at = @At("TAIL"))
	private void chuuni$roll(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		float roll = CameraShake.roll(partialTick) + Cinematic.roll();

		if (roll != 0f) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
		}
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void chuuni$fov(Camera camera, float partial, boolean useSetting, CallbackInfoReturnable<Float> cir) {
		float punch = CameraShake.fovScale() * Cinematic.fovScale();

		if (Math.abs(punch - 1f) > 1e-4f) {
			cir.setReturnValue(cir.getReturnValue() * punch);
		}
	}
}
