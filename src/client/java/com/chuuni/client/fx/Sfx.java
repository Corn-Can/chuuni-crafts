package com.chuuni.client.fx;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.chuuni.Chuuni;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 播放 assets/chuuni/sounds.json 裡的原創音效（只存在客戶端資源，不需要註冊表，所以伺服器不裝也沒問題）。
 * <p>
 * 動漫素材對照表（assets/chuuni/anime_map.json，由 tools/fetch_anime_sfx.py 產生）：播放合成音效時查表，
 * 有對應的動漫素材就改播它（layer = 原本的合成音效降到一半疊在底下，保留低頻的重量）。
 */
public final class Sfx {
	private record Remap(String sound, boolean layer, float volume, float max) {
	}

	private static Map<String, Remap> remaps;
	/** 這些音效的音高帶有意義（音符），換成素材時照樣套用；其他的只套一點點，免得真實錄音被拉得怪怪的 */
	private static final Set<String> KEEP_PITCH = Set.of("note_glow");

	private Sfx() {
	}

	private static Map<String, Remap> remaps() {
		if (remaps == null) {
			remaps = new HashMap<>();

			try {
				var res = Minecraft.getInstance().getResourceManager().getResource(Chuuni.id("anime_map.json"));

				if (res.isPresent()) {
					try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
						JsonObject root = JsonParser.parseReader(in).getAsJsonObject();

						for (var e : root.entrySet()) {
							JsonObject o = e.getValue().getAsJsonObject();
							remaps.put(e.getKey(), new Remap(o.get("sound").getAsString(), o.get("layer").getAsBoolean(), o.get("volume").getAsFloat(), o.has("max") ? o.get("max").getAsFloat() : 0));
						}
					}
				}
			}
			catch (Exception e) {
				Chuuni.LOGGER.warn("cannot read anime_map.json", e);
			}
		}

		return remaps;
	}

	/** 查表：沒有對應素材（或素材沒裝）時回傳 null */
	private static Remap remap(String name) {
		Remap r = remaps().get(name);
		return r != null && has(r.sound) ? r : null;
	}

	private static float softPitch(String name, float pitch) {
		return KEEP_PITCH.contains(name) ? pitch : Math.clamp(1 + (pitch - 1) * 0.35f, 0.8f, 1.25f);
	}

	public static void at(String name, Vec3 pos, float volume, float pitch) {
		Remap r = remap(name);

		if (r != null) {
			rawAt(r.sound, pos, Math.min(1.5f, volume * r.volume), softPitch(name, pitch), r.max);

			if (!r.layer) {
				return;
			}

			volume *= 0.5f;
		}

		rawAt(name, pos, volume, pitch, 0);
	}

	/** 不受距離影響（觸發者本人的大招音效） */
	public static void ui(String name, float volume, float pitch) {
		Remap r = remap(name);

		if (r != null) {
			rawUi(r.sound, Math.min(1f, volume * r.volume), softPitch(name, pitch), r.max);

			if (!r.layer) {
				return;
			}

			volume *= 0.5f;
		}

		rawUi(name, volume, pitch, 0);
	}

	/** max &gt; 0：只播前 max 秒（淡出） */
	private static void rawAt(String name, Vec3 pos, float volume, float pitch, float max) {
		volume *= category(name);

		if (!Settings.sound || volume <= 0.001f) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		SoundEvent ev = SoundEvent.createVariableRangeEvent(Chuuni.id(name));

		if (max > 0) {
			mc.getSoundManager().play(new CappedSound(ev, volume, pitch, pos, Math.round(max * 20)));
			return;
		}

		mc.getSoundManager().play(new SimpleSoundInstance(ev, SoundSource.BLOCKS, volume, pitch, RandomSource.create(), pos.x, pos.y, pos.z));
	}

	private static void rawUi(String name, float volume, float pitch, float max) {
		volume *= category(name);

		if (!Settings.sound || volume <= 0.001f) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();

		if (max > 0) {
			mc.getSoundManager().play(new CappedSound(SoundEvent.createVariableRangeEvent(Chuuni.id(name)), volume, pitch, null, Math.round(max * 20)));
			return;
		}

		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Chuuni.id(name)), pitch, volume));
	}

	/** 正在播招式名的音效（Hud 呼叫 {@link #nameUi} 期間） */
	private static boolean nameCtx;

	/** 分類音量：招式名音效 / 動漫素材 / 合成音效 */
	private static float category(String name) {
		if (nameCtx) {
			return Settings.volTitle;
		}

		return name.startsWith("anime_") ? Settings.volAnime : Settings.volSynth;
	}

	/** 招式名的音效（音量跟著「招式名音效」那條滑桿） */
	public static void nameUi(String name, float volume, float pitch) {
		nameCtx = true;

		try {
			ui(name, volume, pitch);
		}
		finally {
			nameCtx = false;
		}
	}

	/** 招式名的動漫素材音效 */
	public static boolean nameAnimeUi(String key, String fallback, float volume, float pitch) {
		nameCtx = true;

		try {
			return animeUi(key, fallback, volume, pitch);
		}
		finally {
			nameCtx = false;
		}
	}

	/** 有沒有這個音效（sounds.json 裡有登記） */
	public static boolean has(String name) {
		return Minecraft.getInstance().getSoundManager().getSoundEvent(Chuuni.id(name)) != null;
	}

	/**
	 * 動漫音效素材（tools/fetch_anime_sfx.py 轉好的 anime_&lt;key&gt;）：有就播它，沒有就播 fallback（合成音效；null = 不播）。
	 * @return 是否播了動漫素材
	 */
	public static boolean anime(boolean mine, String key, String fallback, Vec3 pos, float volume, float pitch) {
		if (has("anime_" + key)) {
			play(mine, "anime_" + key, pos, volume, pitch);
			return true;
		}

		if (fallback != null) {
			play(mine, fallback, pos, volume, pitch);
		}

		return false;
	}

	/** UI 版（招式名） */
	public static boolean animeUi(String key, String fallback, float volume, float pitch) {
		if (has("anime_" + key)) {
			ui("anime_" + key, volume, pitch);
			return true;
		}

		if (fallback != null) {
			ui(fallback, volume, pitch);
		}

		return false;
	}

	/** 本人 → 不衰減；旁觀者 → 在方塊位置 */
	public static void play(boolean mine, String name, Vec3 pos, float volume, float pitch) {
		if (mine) {
			ui(name, volume, pitch);
		}
		else {
			at(name, pos, volume * 1.5f, pitch);
		}
	}
}
