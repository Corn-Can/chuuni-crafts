package com.chuuni.client.fx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.chuuni.Chuuni;
import com.chuuni.FxGroup;
import com.chuuni.ServerConfig;
import com.chuuni.net.FxPayload;
import com.chuuni.net.FxPayload.Kind;
import com.chuuni.net.PrefsPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.resources.language.I18n;

/**
 * 設定（config/chuuni.properties；遊戲內按 K 或輸入 /chuuni 開設定畫面）。完整說明見 CONFIG.md。
 * <ul>
 *   <li>enabled：總開關（關掉 = 完全原版）</li>
 *   <li>cinematic：觸發者是否進入電影運鏡</li>
 *   <li>intensity：0 日常／1 中二（預設）／2 末期中二</li>
 *   <li>titles / post / sound / shake：招式名、畫面後製、音效、鏡頭震動</li>
 *   <li>volAnime / volSynth / volTitle：動漫素材、合成音效、招式名音效的音量 0~1</li>
 *   <li>tierA / tierB / tierC / mobs / throws / combat / events：整組開關</li>
 *   <li>fx.&lt;演出&gt;：每一種演出各自的開關</li>
 * </ul>
 * 伺服器（config/chuuni-server.properties）強制的項目會覆蓋本地值並鎖住（設定畫面上變灰；本地設定檔保持原樣，離開伺服器就恢復）。
 */
public final class Settings {
	public static boolean enabled = true;
	public static int intensity = 1;
	public static boolean cinematic = true;
	public static float shake = 1f;
	public static boolean titles = true;
	public static boolean post = true;
	public static boolean sound = true;
	public static float volAnime = 1f;
	public static float volSynth = 1f;
	public static float volTitle = 1f;
	public static boolean tierA = true;
	public static boolean tierB = true;
	public static boolean tierC = true;
	public static boolean mobs = true;
	public static boolean throwsOn = true;
	public static boolean combat = true;
	public static boolean events = true;
	/** 個別關掉的演出 */
	private static final Set<Kind> OFF = EnumSet.noneOf(Kind.class);

	/** 被伺服器強制（鎖住）的項目 */
	private static final Set<String> FORCED = new HashSet<>();
	/** 本地設定檔的內容（被強制時存檔仍寫回本地值） */
	private static Properties local = new Properties();

	/** 伺服器管理員（OP / 單人世界主人）才看得到「伺服器設定」 */
	public static boolean serverAdmin;
	/** 伺服器是否開了 forceClient */
	public static boolean serverForce;
	/** 伺服器設定檔目前的內容（管理員才有） */
	public static Properties serverRaw = new Properties();
	/** 收到新的伺服器設定時呼叫（設定畫面重畫） */
	public static Runnable onServerChange = () -> {
	};

	private Settings() {
	}

	/** 這一項是不是被伺服器鎖住 */
	public static boolean forced(String key) {
		return FORCED.contains(key);
	}

	public static boolean locked() {
		return !FORCED.isEmpty();
	}

	/** 收到伺服器設定：values 裡列的項目強制套用並鎖住，其他回到本地值 */
	public static void applyServer(boolean force, String values, boolean admin, String raw) {
		FORCED.clear();
		Properties p = ServerConfig.parse(values);
		read(merge(local, p));
		FORCED.addAll(p.stringPropertyNames());
		serverForce = force;
		serverAdmin = admin;
		serverRaw = ServerConfig.parse(raw);

		if (!FORCED.isEmpty()) {
			Chuuni.LOGGER.info("server enforces chuuni settings: {}", FORCED);
		}

		sendPrefs();
		onServerChange.run();
	}

	/** 離開伺服器：解除鎖定 */
	public static void releaseServer() {
		serverAdmin = false;
		serverForce = false;
		serverRaw = new Properties();

		if (locked()) {
			FORCED.clear();
			read(local);
		}
	}

	private static Properties merge(Properties base, Properties over) {
		Properties m = new Properties();
		m.putAll(base);
		m.putAll(over);
		return m;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("chuuni.properties");
	}

	public static void load() {
		Path file = file();
		Properties p = new Properties();

		try {
			if (Files.exists(file)) {
				try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					p.load(in);
				}
			}
		}
		catch (IOException e) {
			Chuuni.LOGGER.warn("cannot read {}", file, e);
		}

		local = p;
		read(p);
		save();
	}

	private static void read(Properties p) {
		enabled = bool(p, "enabled", true);
		intensity = Math.round(num(p, "intensity", 1, 0, 2));
		cinematic = bool(p, "cinematic", true);
		shake = num(p, "shake", 1, 0, 2);
		titles = bool(p, "titles", true);
		post = bool(p, "post", true);
		sound = bool(p, "sound", true);
		volAnime = num(p, "volAnime", 1, 0, 1);
		volSynth = num(p, "volSynth", 1, 0, 1);
		volTitle = num(p, "volTitle", 1, 0, 1);
		tierA = bool(p, "tierA", true);
		tierB = bool(p, "tierB", true);
		tierC = bool(p, "tierC", true);
		mobs = bool(p, "mobs", true);
		throwsOn = bool(p, "throws", true);
		combat = bool(p, "combat", true);
		events = bool(p, "events", true);
		OFF.clear();

		for (FxGroup g : FxGroup.values()) {
			for (Kind k : g.kinds()) {
				if (!bool(p, FxGroup.key(k), true)) {
					OFF.add(k);
				}
			}
		}
	}

	/** 存檔用的值：被伺服器鎖住的項目寫回本地原本的值（不讓伺服器的設定蓋掉玩家自己的檔案） */
	private static String keep(String key, Object now) {
		return forced(key) ? local.getProperty(key, String.valueOf(now)).trim() : String.valueOf(now);
	}

	public static void save() {
		StringBuilder sb = new StringBuilder("""
			# 中二工坊 Chuuni Crafts（遊戲內按 K 或輸入 /chuuni 也能調整；完整說明見模組附的 CONFIG.md）
			# 總開關：false = 完全原版
			enabled=%s
			# 電影運鏡（觸發者的鏡頭演出；關掉時特效只在世界中播放）
			cinematic=%s
			# 強度：0 = 日常（無鏡頭、無招式名）, 1 = 中二（預設）, 2 = 末期中二（全部加倍）
			intensity=%s
			# 招式名
			titles=%s
			# 畫面後製（調色、集中線、衝擊幀、漫畫分格）
			post=%s
			# 音效
			sound=%s
			# 鏡頭震動倍率 0 ~ 2
			shake=%s
			# 音量 0 ~ 1：動漫素材音效、合成音效、招式名音效
			volAnime=%s
			volSynth=%s
			volTitle=%s
			# 整組開關：A 級（電影演出）、B 級（短演出）、C 級（微型）、生物互動、投擲物、戰鬥、事件
			tierA=%s
			tierB=%s
			tierC=%s
			mobs=%s
			throws=%s
			combat=%s
			events=%s

			# 個別演出（false = 關掉這一種）
			""".formatted(keep("enabled", enabled), keep("cinematic", cinematic), keep("intensity", intensity), keep("titles", titles), keep("post", post),
			keep("sound", sound), keep("shake", shake), keep("volAnime", volAnime), keep("volSynth", volSynth), keep("volTitle", volTitle), keep("tierA", tierA),
			keep("tierB", tierB), keep("tierC", tierC), keep("mobs", mobs), keep("throws", throwsOn), keep("combat", combat), keep("events", events)));

		for (FxGroup g : FxGroup.values()) {
			sb.append("# --- ").append(name("chuuni.settings." + g.key)).append(" ---\n");

			for (Kind k : g.kinds()) {
				String key = FxGroup.key(k);
				sb.append("# ").append(name(nameKey(k))).append('\n');
				sb.append(key).append('=').append(keep(key, !OFF.contains(k))).append('\n');
			}
		}

		String text = sb.toString();

		try {
			Files.writeString(file(), text, StandardCharsets.UTF_8);
			local = new Properties();
			local.load(new java.io.StringReader(text));
		}
		catch (IOException e) {
			Chuuni.LOGGER.warn("cannot write {}", file(), e);
		}

		sendPrefs();
	}

	/** 註解用的顯示名稱（語言還沒載入時用 key） */
	private static String name(String key) {
		try {
			return I18n.exists(key) ? I18n.get(key) : key;
		}
		catch (Exception e) {
			return key;
		}
	}

	/** 演出的名稱（語言檔 key） */
	public static String nameKey(Kind k) {
		return "chuuni.fx." + k.name().toLowerCase(Locale.ROOT);
	}

	/** 實際生效的運鏡：強度 0 時也不運鏡 */
	public static boolean cinematicOn() {
		return enabled && cinematic && intensity > 0;
	}

	public static boolean titlesOn() {
		return titles && intensity > 0;
	}

	/** 告訴伺服器：A 級演出的「物品從空中降下」結尾要不要由伺服器保管物品（關掉時維持原版） */
	public static void sendPrefs() {
		if (ClientPlayNetworking.canSend(PrefsPayload.TYPE)) {
			ClientPlayNetworking.send(new PrefsPayload(enabled && tierA && cinematicOn()));
		}
	}

	// ------------------------------------------------------------------ 分組 / 個別開關

	public static boolean group(FxGroup g) {
		return switch (g) {
			case A -> tierA;
			case B -> tierB;
			case C -> tierC;
			case MOBS -> mobs;
			case THROWS -> throwsOn;
			case COMBAT -> combat;
			case EVENTS -> events;
		};
	}

	public static void setGroup(FxGroup g, boolean v) {
		switch (g) {
			case A -> tierA = v;
			case B -> tierB = v;
			case C -> tierC = v;
			case MOBS -> mobs = v;
			case THROWS -> throwsOn = v;
			case COMBAT -> combat = v;
			case EVENTS -> events = v;
		}
	}

	/** 這一種演出自己的開關（不含整組開關） */
	public static boolean on(Kind k) {
		return !OFF.contains(k);
	}

	public static void set(Kind k, boolean v) {
		if (v) {
			OFF.remove(k);
		}
		else {
			OFF.add(k);
		}
	}

	/** 這種演出要不要播 */
	public static boolean allowed(FxPayload.Kind kind) {
		FxGroup g = FxGroup.of(kind);

		if (g == null) {
			return true;
		}

		// 伺服器強制開的演出（fx.xxx=true）不受玩家的整組開關影響
		boolean forcedOn = forced(FxGroup.key(kind)) && on(kind);
		return enabled && (group(g) || forcedOn) && on(kind);
	}

	private static boolean bool(Properties p, String key, boolean def) {
		return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)).trim());
	}

	private static float num(Properties p, String key, float def, float lo, float hi) {
		try {
			String v = p.getProperty(key, "").trim();
			return v.isEmpty() ? def : Math.clamp(Float.parseFloat(v), lo, hi);
		}
		catch (NumberFormatException e) {
			Chuuni.LOGGER.warn("bad chuuni setting {}", key);
			return def;
		}
	}
}
