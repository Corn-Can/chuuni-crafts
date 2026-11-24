package com.chuuni.client.mixin;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 電影運鏡中：隱藏原版 HUD（準心、快捷欄……），只畫黑邊與名卡。
 * 一般時候：在 HUD 最上層疊加演出層；若有介面開著則交給 ScreenMixin 畫在介面之上。
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void chuuni$cinematic(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (Cinematic.active()) {
			Hud.render(graphics);
			ci.cancel();
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void chuuni$overlay(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (Minecraft.getInstance().screen == null || Cinematic.hidesScreen()) {
			Hud.render(graphics);
		}
	}
}
