package com.chuuni.client.mixin;

import com.chuuni.client.post.CinemaPost;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 記下畫世界用的矩陣，後製才能從深度重建世界座標 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void chuuni$capture(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, Matrix4f view, Matrix4f projection,
		Matrix4f culling, GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo ci) {
		CinemaPost.capture(camera, view, projection);
	}
}
