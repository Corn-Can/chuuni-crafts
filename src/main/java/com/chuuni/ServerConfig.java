package com.chuuni;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.chuuni.net.FxPayload.Kind;
import com.chuuni.net.ServerEditPayload;
import com.chuuni.net.ServerSettingsPayload;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * 伺服器端設定（config/chuuni-server.properties）。
 * <ul>
 *   <li>forceClient=true：下面「一般設定」裡有填值的項目會強制套用到所有玩家（客戶端設定畫面鎖住）</li>
 *   <li>fx.&lt;演出&gt;=false：整個伺服器關掉這種演出（伺服器不送、客戶端也鎖成關），不受 forceClient 影響</li>
 *   <li>fx.&lt;演出&gt;=true：強制開著（玩家不能關）；留空 = 玩家自己決定</li>
 * </ul>
 * 改法：直接改檔案（玩家重新進服、或 /chuuniserver reload 後生效），
 * 或 OP 在遊戲內按 K →「伺服器設定」，或指令 /chuuniserver set &lt;key&gt; &lt;value&gt;（立刻套用到所有在線玩家）。
 */
public final class ServerConfig {
	/** 可以由伺服器強制的一般設定（和客戶端 config/chuuni.properties 同名） */
	public static final List<String> KEYS = List.of("enabled", "cinematic", "intensity", "titles", "post", "sound", "shake", "volAnime", "volSynth", "volTitle",
		"tierA", "tierB", "tierC", "mobs", "throws", "combat", "events");
	private static final Set<String> NUMBERS = Set.of("intensity", "shake", "volAnime", "volSynth", "volTitle");

	private static final Properties VALUES = new Properties();
	/** 整個伺服器關掉的演出 */
	private static final Set<Kind> OFF = EnumSet.noneOf(Kind.class);

	private ServerConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("chuuni-server.properties");
	}

	public static void init() {
		// 讀檔後馬上重寫一次：舊版設定檔會補上新的項目（原本的值保留）
		load();
		write();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			load();
			send(handler.player);
		});
		ServerPlayNetworking.registerGlobalReceiver(ServerEditPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> {
			ServerPlayer p = ctx.player();

			if (!admin(p)) {
				Chuuni.LOGGER.warn("{} tried to edit chuuni server settings without permission", p.getName().getString());
				return;
			}

			if (payload.key().isEmpty()) {
				load();
				broadcast(ctx.server());
			}
			else if (!set(ctx.server(), payload.key(), payload.value())) {
				p.sendSystemMessage(Component.translatable("chuuni.server.bad", payload.key(), payload.value()));
			}
		}));
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("chuuniserver")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("reload").executes(ctx -> {
				load();
				broadcast(ctx.getSource().getServer());
				ctx.getSource().sendSuccess(() -> Component.translatable("chuuni.server.reloaded"), true);
				return 1;
			}))
			.then(Commands.literal("set").then(Commands.argument("key", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(allKeys(), b))
				.then(Commands.argument("value", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("true", "false", "default"), b))
					.executes(ServerConfig::cmdSet))))
			.then(Commands.literal("get").then(Commands.argument("key", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(allKeys(), b))
				.executes(ctx -> {
					String k = StringArgumentType.getString(ctx, "key");
					String v = VALUES.getProperty(k, "").trim();
					ctx.getSource().sendSuccess(() -> Component.literal(k + " = " + (v.isEmpty() ? "(default)" : v)), false);
					return 1;
				})))));
	}

	private static int cmdSet(CommandContext<CommandSourceStack> ctx) {
		String k = StringArgumentType.getString(ctx, "key");
		String v = StringArgumentType.getString(ctx, "value");

		if (!set(ctx.getSource().getServer(), k, v)) {
			ctx.getSource().sendFailure(Component.translatable("chuuni.server.bad", k, v));
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal(k + " = " + VALUES.getProperty(k, "")), true);
		return 1;
	}

	/** 設定檔裡所有可以改的 key */
	public static List<String> allKeys() {
		List<String> out = new ArrayList<>();
		out.add("forceClient");
		out.addAll(KEYS);

		for (FxGroup g : FxGroup.values()) {
			for (Kind k : g.kinds()) {
				out.add(FxGroup.key(k));
			}
		}

		return out;
	}

	/** OP（指令權限 2 以上）或單人世界的主人 */
	public static boolean admin(ServerPlayer p) {
		return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || p.level().getServer().isSingleplayerOwner(p.nameAndId());
	}

	/** 這種演出被伺服器整個關掉了 */
	public static boolean off(Kind kind) {
		return OFF.contains(kind);
	}

	public static synchronized void load() {
		VALUES.clear();

		if (Files.exists(file())) {
			try (var in = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
				VALUES.load(in);
			}
			catch (IOException e) {
				Chuuni.LOGGER.warn("cannot read {}", file(), e);
			}
		}

		OFF.clear();

		for (String k : VALUES.stringPropertyNames()) {
			Kind kind = FxGroup.kind(k);

			if (kind != null && VALUES.getProperty(k).trim().equalsIgnoreCase("false")) {
				OFF.add(kind);
			}
		}
	}

	/**
	 * 改一項設定並存檔、立刻套用到所有在線玩家。value 為空字串或 "default" = 讓玩家自己決定。
	 * @return 值不合法時回傳 false（不改）
	 */
	public static boolean set(MinecraftServer server, String key, String value) {
		String v = value == null ? "" : value.trim();

		if (v.equalsIgnoreCase("default")) {
			v = "";
		}

		if (!valid(key, v)) {
			return false;
		}

		synchronized (ServerConfig.class) {
			VALUES.setProperty(key, v);
			write();
			load();
		}

		broadcast(server);
		return true;
	}

	private static boolean valid(String key, String v) {
		boolean bool = v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false");

		if (key.equals("forceClient")) {
			return bool;
		}

		if (FxGroup.kind(key) != null) {
			return v.isEmpty() || bool;
		}

		if (!KEYS.contains(key)) {
			return false;
		}

		if (v.isEmpty()) {
			return true;
		}

		if (!NUMBERS.contains(key)) {
			return bool;
		}

		try {
			float f = Float.parseFloat(v);
			return switch (key) {
				case "intensity" -> f == 0 || f == 1 || f == 2;
				case "shake" -> f >= 0 && f <= 2;
				default -> f >= 0 && f <= 1;
			};
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	public static void broadcast(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			send(p);
		}
	}

	private static void send(ServerPlayer p) {
		if (ServerPlayNetworking.canSend(p, ServerSettingsPayload.TYPE)) {
			ServerPlayNetworking.send(p, payload(admin(p)));
		}
	}

	/** 組出要送給客戶端的封包 */
	public static synchronized ServerSettingsPayload payload(boolean admin) {
		boolean force = Boolean.parseBoolean(VALUES.getProperty("forceClient", "false").trim());
		StringBuilder sb = new StringBuilder();

		if (force) {
			for (String k : KEYS) {
				String v = VALUES.getProperty(k, "").trim();

				// 留空 = 這一項讓玩家自己決定
				if (!v.isEmpty()) {
					sb.append(k).append('=').append(v).append('\n');
				}
			}
		}

		// 個別演出：有填值就一定套用（false = 整個伺服器關掉）
		for (String k : VALUES.stringPropertyNames()) {
			String v = VALUES.getProperty(k).trim();

			if (FxGroup.kind(k) != null && !v.isEmpty()) {
				sb.append(k).append('=').append(v).append('\n');
			}
		}

		String raw = "";

		if (admin) {
			StringWriter w = new StringWriter();

			try {
				VALUES.store(w, null);
			}
			catch (IOException ignored) {
			}

			raw = w.toString();
		}

		return new ServerSettingsPayload(force, sb.toString(), admin, raw);
	}

	/** 演出的中文名稱（寫進設定檔的註解；讀不到語言檔時不寫） */
	private static Map<String, String> names() {
		Map<String, String> out = new HashMap<>();

		try (var in = ServerConfig.class.getResourceAsStream("/assets/chuuni/lang/zh_tw.json")) {
			if (in != null) {
				JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

				for (var e : o.entrySet()) {
					out.put(e.getKey(), e.getValue().getAsString());
				}
			}
		}
		catch (Exception e) {
			Chuuni.LOGGER.debug("no lang for config comments", e);
		}

		return out;
	}

	private static String v(String key, String def) {
		return VALUES.getProperty(key, def).trim();
	}

	/** 依目前的值重新產生整份設定檔（含說明註解） */
	private static void write() {
		Map<String, String> names = names();
		StringBuilder sb = new StringBuilder();
		sb.append("""
			# 中二工坊 Chuuni Crafts —— 伺服器端設定（完整說明見模組附的 CONFIG.md）
			#
			# 改設定的三種方法：
			#   1. 直接改這個檔案 → 玩家重新進服、或輸入 /chuuniserver reload 後生效
			#   2. OP 在遊戲內按 K →「伺服器設定」，點一下就切換（立刻套用到所有在線玩家）
			#   3. 指令 /chuuniserver set <key> <true|false|default|數字>
			#
			# 值的意思：留空 = 讓玩家自己決定；true / false / 數字 = 由伺服器決定（玩家的設定畫面會鎖住那一項）

			# ===== 一般設定 =====
			# forceClient=true：下面這段「一般設定」有填值的項目才會強制套用；false = 這一段全部讓玩家自己決定
			""");
		sb.append("forceClient=").append(v("forceClient", "false")).append("\n\n");
		sb.append("""
			# 總開關（false = 完全原版）
			enabled=%s
			# 電影運鏡（觸發者的鏡頭演出）
			cinematic=%s
			# 強度：0 = 日常（無鏡頭、無招式名）, 1 = 中二, 2 = 末期中二（全部加倍）
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

			# ===== 個別演出 =====
			# 不受 forceClient 影響：false = 整個伺服器關掉這種演出（伺服器不送、玩家也不能開）
			#                        true = 強制開著（玩家不能關）；留空 = 玩家自己決定
			""".formatted(v("enabled", ""), v("cinematic", ""), v("intensity", ""), v("titles", ""), v("post", ""), v("sound", ""), v("shake", ""),
			v("volAnime", ""), v("volSynth", ""), v("volTitle", ""), v("tierA", ""), v("tierB", ""), v("tierC", ""), v("mobs", ""), v("throws", ""), v("combat", ""),
			v("events", "")));

		for (FxGroup g : FxGroup.values()) {
			sb.append("\n# --- ").append(names.getOrDefault("chuuni.settings." + g.key, g.key)).append(" ---\n");

			for (Kind k : g.kinds()) {
				String key = FxGroup.key(k);
				String name = names.get("chuuni.fx." + k.name().toLowerCase(Locale.ROOT));
				// properties 不支援行尾註解 → 名稱寫在上一行
				if (name != null) {
					sb.append("# ").append(name).append('\n');
				}

				sb.append(key).append('=').append(v(key, "")).append('\n');
			}
		}

		try {
			Files.writeString(file(), sb.toString(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			Chuuni.LOGGER.warn("cannot write {}", file(), e);
		}
	}

	/** 解析 properties 字串（客戶端共用） */
	public static Properties parse(String text) {
		Properties p = new Properties();

		try {
			p.load(new StringReader(text));
		}
		catch (IOException e) {
			Chuuni.LOGGER.warn("bad settings text", e);
		}

		return p;
	}
}
