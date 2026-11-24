package com.chuuni.client.mixin;

import com.chuuni.client.fx.Cinematic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 運鏡時暫停顯示成就 / 教學提示，結束後照常出現 */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void chuuni$hide(GuiGraphics graphics, CallbackInfo ci) {
		if (Cinematic.active()) {
			ci.cancel();
		}
	}
}
