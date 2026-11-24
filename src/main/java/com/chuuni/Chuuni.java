package com.chuuni;

import com.chuuni.net.FxNet;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中二工坊：和可互動方塊互動時放出「必殺技」等級的演出。
 * 伺服器端只負責偵測「真正成功」的時機（附魔成功、取出成品、門真的打開了……）並廣播給附近玩家，
 * 所有畫面與聲音都在客戶端（client optional：沒裝的玩家照樣能進服）。
 */
public class Chuuni implements ModInitializer {
	public static final String MOD_ID = "chuuni";
	public static final Logger LOGGER = LoggerFactory.getLogger("Chuuni");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		FxNet.init();
		ServerConfig.init();
	}
}
