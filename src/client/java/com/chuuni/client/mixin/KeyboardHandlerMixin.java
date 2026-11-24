package com.chuuni.client.mixin;

import com.chuuni.client.fx.Cinematic;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 電影運鏡中按任意鍵 → 跳過演出 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void chuuni$skip(long window, int action, KeyEvent event, CallbackInfo ci) {
		// 放開（action 0）一律放行：否則運鏡開始前按著的鍵永遠收不到「放開」，結束後會一直走 / 一直按右鍵
		if (Cinematic.active() && action != 0) {
			if (action == 1) {
				Cinematic.skip();
			}

			ci.cancel();
		}
	}
}
