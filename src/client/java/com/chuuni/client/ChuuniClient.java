package com.chuuni.client;

import com.chuuni.Chuuni;
import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Effects;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.FxRender;
import com.chuuni.client.fx.ClientWatch;
import com.chuuni.net.FxPayload;
import com.chuuni.net.ServerSettingsPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

public class ChuuniClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Chuuni.id("main"));
	private static KeyMapping settingsKey;
	private static boolean openNextTick;

	@Override
	public void onInitializeClient() {
		Settings.load();
		FxRender.init();
		CinemaPost.init();
		CameraShake.init();
		Effects.init();
		ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE, (payload, context) -> Effects.handle(payload));
		// 伺服器強制的設定（forceClient）
		ClientPlayNetworking.registerGlobalReceiver(ServerSettingsPayload.TYPE, (payload, context) -> Settings.applyServer(payload.force(), payload.values(), payload.admin(), payload.raw()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(Settings::releaseServer));
		// 純客戶端偵測的演出：丟出的終界珍珠 / 終界之眼、世界中的常駐光（終界門框的眼、雕紋書架的書）
		ClientWatch.init();

		// 進入世界時告訴伺服器目前的設定（關掉動畫時，伺服器不保管成品）
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(Settings::sendPrefs));

		// 設定畫面：K 鍵（可在「控制」裡改）或 /chuuni
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.chuuni.settings", GLFW.GLFW_KEY_K, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (settingsKey.consumeClick()) {
				mc.setScreen(new SettingsScreen(mc.screen));
			}

			if (openNextTick) {
				openNextTick = false;
				mc.setScreen(new SettingsScreen(null));
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(ClientCommandManager.literal("chuuni").executes(ctx -> {
			// 指令執行時聊天欄還開著，下一個 tick 再開設定畫面
			openNextTick = true;
			return 1;
		})));
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
