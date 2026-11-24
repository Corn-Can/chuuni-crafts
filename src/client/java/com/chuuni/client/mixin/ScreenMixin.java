package com.chuuni.client.mixin;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 電影運鏡期間不畫介面（附魔台、鐵砧……的視窗暫時消失，讓玩家看清楚世界中的演出），結束後自動回來。
 * 其他時候，名卡 / COMBO 疊在介面之上（例如在背包裡合成）。
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
	private void chuuni$hide(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		if (Cinematic.hidesScreen()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
	private void chuuni$overlay(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		graphics.nextStratum();
		Hud.render(graphics);
	}
}
