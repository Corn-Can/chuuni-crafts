package com.chuuni.client.post;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * 後製的目前狀態（移植自 1014 天災的 PostState）：每種演出有自己的「色調」（壓暗的顏色、霧、調色、泛光、暗角），
 * 淡入淡出；加上短促的提示：衝擊幀、閃光殘影、放射縮放、過曝、色差。全部用真實時間，不受頓幀影響。
 */
public final class PostState {
	/** 一種演出的色調 */
	public record Look(float dim, int dimTint, float fog, int fogColor, float fogMax, float contrast, float saturation, int lift, int gain,
		float bloom, int vignette, float vignetteAmt, float skyDark, float rays, int impactMode,
		float cel, float outline, float streak, int streakColor) {
	}

	/** 衝擊幀模式（fx_composite.fsh）：0 墨白、1 餘燼、2 冰霜、3 血紅、4 故障、5 光譜、6 紫、7 負片 */
	public static final int INK = 0;
	public static final int EMBER = 1;
	public static final int FROST = 2;
	public static final int CRIMSON = 3;
	public static final int SPECTRAL = 5;
	public static final int VIOLET = 6;
	public static final int NEGATIVE = 7;

	private static final Map<String, Look> LOOKS = new HashMap<>();

	static {
		// 壓暗量 + 世界沉入的顏色、霧、調色（對比、飽和、暗部、亮部）、泛光、暗角、天空壓暗、光束、預設衝擊幀
		LOOKS.put("enchant", new Look(0.62F, 0x6A58B0, 0.004F, 0x140A2A, 0.5F, 1.2F, 1.1F, 0x080214, 0xF4ECFF, 1.0F, 0x0A0018, 0.7F, 0.7F, 0.9F, VIOLET, 0.55F, 0.75F, 0.3F, 0xB080FF));
		LOOKS.put("enchant1", new Look(0.45F, 0x5A80C0, 0.003F, 0x08142A, 0.4F, 1.12F, 1.05F, 0x020818, 0xEAF4FF, 1.1F, 0x000A1C, 0.55F, 0.4F, 0.5F, FROST, 0.45F, 0.6F, 0.25F, 0x80C0FF));
		LOOKS.put("enchant3", new Look(0.72F, 0x503070, 0.005F, 0x10040C, 0.55F, 1.28F, 1.15F, 0x0A0208, 0xFFF0DA, 1.1F, 0x120006, 0.8F, 0.85F, 1.0F, VIOLET, 0.6F, 0.85F, 0.45F, 0xFFC860));
		// 神聖降臨：暖白、提亮暗部、光束強、泛光高，描線變淡
		LOOKS.put("holy", new Look(0.3F, 0xE8D4B0, 0.002F, 0x2A1E0C, 0.3F, 1.08F, 0.9F, 0x0C0804, 0xF6EEE0, 0.75F, 0x1A1206, 0.45F, 0.25F, 0.55F, INK, 0.25F, 0.35F, 0.2F, 0xFFE0A0));
		LOOKS.put("anvil", new Look(0.55F, 0xA0583A, 0.004F, 0x2A0E04, 0.5F, 1.2F, 1.1F, 0x0C0402, 0xFFEEDC, 1.5F, 0x1A0600, 0.7F, 0.5F, 0.7F, EMBER, 0.55F, 0.8F, 0.45F, 0xFF9040));
		LOOKS.put("craft", new Look(0.45F, 0x5A8AB0, 0.003F, 0x061624, 0.45F, 1.15F, 1.0F, 0x020810, 0xEAF6FF, 1.4F, 0x000A18, 0.6F, 0.4F, 0.5F, INK, 0.5F, 0.7F, 0.35F, 0x80C8FF));
		LOOKS.put("cartography", new Look(0.5F, 0x4A9A90, 0.003F, 0x04201C, 0.45F, 1.15F, 1.0F, 0x020A08, 0xEAFFF8, 1.4F, 0x001410, 0.6F, 0.5F, 0.6F, FROST, 0.5F, 0.7F, 0.35F, 0x60FFE0));
		LOOKS.put("furnace", new Look(0.55F, 0xA05030, 0.004F, 0x300C02, 0.5F, 1.2F, 1.15F, 0x0C0402, 0xFFEAD8, 1.6F, 0x200600, 0.7F, 0.5F, 0.8F, EMBER, 0.55F, 0.75F, 0.45F, 0xFF7030));
		LOOKS.put("quench", new Look(0.42F, 0x6888A8, 0.004F, 0x0A1420, 0.45F, 1.15F, 0.8F, 0x02060C, 0xE8F4FF, 0.9F, 0x020A14, 0.55F, 0.4F, 0.5F, FROST, 0.45F, 0.65F, 0.35F, 0xA8D8FF));
		LOOKS.put("blast", new Look(0.55F, 0x3A60B0, 0.004F, 0x040C30, 0.5F, 1.2F, 1.1F, 0x020414, 0xE8F0FF, 1.5F, 0x000820, 0.7F, 0.5F, 0.8F, FROST, 0.55F, 0.75F, 0.45F, 0x5090FF));
		LOOKS.put("smoker", new Look(0.55F, 0x806048, 0.006F, 0x1C1008, 0.6F, 1.15F, 0.95F, 0x0A0604, 0xFFEEDC, 1.3F, 0x140A04, 0.7F, 0.5F, 0.6F, INK, 0.55F, 0.75F, 0.35F, 0xE0A060));
		LOOKS.put("survey", new Look(0.3F, 0x60A0A0, 0.002F, 0x04201C, 0.3F, 1.1F, 0.9F, 0x020A08, 0xEAFFF8, 1.0F, 0x001410, 0.45F, 0.3F, 0.4F, FROST, 0.35F, 0.55F, 0.25F, 0x80FFE8));
		LOOKS.put("anchor", new Look(0.62F, 0x5A3080, 0.005F, 0x14062A, 0.55F, 1.22F, 1.1F, 0x08020F, 0xF2E6FF, 1.2F, 0x0C0018, 0.75F, 0.75F, 0.8F, VIOLET, 0.55F, 0.8F, 0.4F, 0xB070FF));
		LOOKS.put("beacon", new Look(0.5F, 0x5AA0B0, 0.003F, 0x06182A, 0.45F, 1.15F, 1.0F, 0x020810, 0xF0FCFF, 1.3F, 0x000A14, 0.6F, 0.7F, 0.9F, FROST, 0.5F, 0.7F, 0.45F, 0xA0F0FF));
		LOOKS.put("smithing", new Look(0.6F, 0x705038, 0.004F, 0x1A0C04, 0.5F, 1.22F, 1.0F, 0x0A0604, 0xFFF0DC, 1.3F, 0x100600, 0.7F, 0.5F, 0.7F, EMBER, 0.55F, 0.8F, 0.4F, 0xFFC870));
		LOOKS.put("grindstone", new Look(0.55F, 0x6A6A8A, 0.004F, 0x0C0C18, 0.5F, 1.2F, 0.9F, 0x04040A, 0xF0F0FF, 1.2F, 0x060610, 0.7F, 0.5F, 0.6F, SPECTRAL, 0.55F, 0.8F, 0.35F, 0xC0A0FF));
		LOOKS.put("brewing", new Look(0.55F, 0x7A4A80, 0.004F, 0x180A1C, 0.5F, 1.2F, 1.15F, 0x080408, 0xFFEEFA, 1.3F, 0x10040E, 0.7F, 0.5F, 0.6F, SPECTRAL, 0.55F, 0.75F, 0.35F, 0xFF90E0));
		LOOKS.put("nether", new Look(0.45F, 0x7A2860, 0.006F, 0x200414, 0.6F, 1.25F, 1.15F, 0x0C0208, 0xFFE6F4, 1.4F, 0x18020C, 0.8F, 0.8F, 0.8F, CRIMSON, 0.6F, 0.85F, 0.45F, 0xD060FF));
		LOOKS.put("end", new Look(0.72F, 0x2A4A50, 0.006F, 0x020A0C, 0.6F, 1.28F, 1.05F, 0x020606, 0xE8FFF8, 1.3F, 0x000808, 0.85F, 0.95F, 0.7F, NEGATIVE, 0.6F, 0.9F, 0.45F, 0x80FFD8));
		LOOKS.put("vault", new Look(0.55F, 0x806030, 0.004F, 0x1C1004, 0.5F, 1.2F, 1.1F, 0x0A0602, 0xFFF4DC, 1.4F, 0x140A00, 0.7F, 0.5F, 0.8F, EMBER, 0.55F, 0.8F, 0.45F, 0xFFD060));
		LOOKS.put("vault_ominous", new Look(0.65F, 0x30606A, 0.005F, 0x041418, 0.55F, 1.25F, 1.05F, 0x02080A, 0xE0FFFA, 1.3F, 0x00100E, 0.8F, 0.7F, 0.7F, CRIMSON, 0.6F, 0.85F, 0.45F, 0x60FFE0));
		LOOKS.put("dream", new Look(0.35F, 0x7080C0, 0.004F, 0x0A1030, 0.5F, 1.05F, 0.85F, 0x060818, 0xF4F0FF, 1.1F, 0x080A20, 0.6F, 0.6F, 0.4F, INK, 0.35F, 0.45F, 0.25F, 0xC0B0FF));
		LOOKS.put("dragon", new Look(0.45F, 0x4A2070, 0.006F, 0x0C0418, 0.6F, 1.25F, 1.1F, 0x060210, 0xF0E0FF, 1.3F, 0x0A0014, 0.8F, 0.8F, 0.7F, VIOLET, 0.6F, 0.85F, 0.45F, 0xD080FF));
		LOOKS.put("relic", new Look(0.55F, 0x8A7050, 0.005F, 0x1E1408, 0.55F, 1.18F, 0.95F, 0x0A0804, 0xFFF4E0, 1.2F, 0x120C04, 0.7F, 0.6F, 0.7F, EMBER, 0.55F, 0.8F, 0.35F, 0xFFD8A0));
		LOOKS.put("magnet", new Look(0.55F, 0x50708A, 0.004F, 0x081018, 0.5F, 1.2F, 0.95F, 0x04060A, 0xEEF6FF, 1.2F, 0x060A10, 0.7F, 0.5F, 0.6F, FROST, 0.55F, 0.8F, 0.35F, 0xFF6060));
		LOOKS.put("golem", new Look(0.55F, 0x8A7060, 0.004F, 0x180C06, 0.5F, 1.22F, 0.95F, 0x080604, 0xFFF0E0, 1.3F, 0x100600, 0.7F, 0.6F, 0.7F, EMBER, 0.6F, 0.85F, 0.4F, 0xFF8040));
		LOOKS.put("wither", new Look(0.5F, 0x5A4078, 0.006F, 0x06020A, 0.7F, 1.3F, 0.7F, 0x040208, 0xE8E0F0, 1.2F, 0x020004, 0.9F, 0.95F, 0.6F, NEGATIVE, 0.65F, 0.95F, 0.45F, 0xB070FF));
		LOOKS.put("door", new Look(0.25F, 0x80A090, 0.0F, 0x000000, 0F, 1.08F, 1.0F, 0x020604, 0xF0FFF4, 1.2F, 0x041008, 0.4F, 0.1F, 0.2F, INK, 0.35F, 0.5F, 0.2F, 0xC0FFD0));
	}

	static final PostState CURRENT = new PostState();

	// 目前混合後的值（CinemaPost 讀取）
	float dim, dimR = 1, dimG = 1, dimB = 1;
	float fog, fogR, fogG, fogB, fogFalloff = 0.02F, fogMax;
	double groundY = 64;
	float contrast = 1, saturation = 1, liftR, liftG, liftB, gainR = 1, gainG = 1, gainB = 1;
	float bloom, vignette, vigR, vigG, vigB, heat, skyDark, rays;
	float exposure = 1;
	float flash, flashR = 1, flashG = 1, flashB = 1, after;
	float impact, impactMode;
	float zoom, aberration;
	float grain;
	float cel, outline, lines, linesDark, linesInner = 0.3F, manga;
	float streak, streakR = 1, streakG = 1, streakB = 1;
	float split, splitAngle, shockR, shockAmp;

	private static Look look;
	private static Look fromLook;
	private static float blend = 1;
	private static float blendRate = 1;
	private static float lookWeight;
	private static float lookTarget;
	private static float lookRate = 1;
	private static int defaultImpact;
	private static long lookUntil;

	private static long impactUntil;
	private static long impactStart;
	private static int impactCueMode;
	private static long flashStart;
	private static long flashMs = 1;
	private static float flashPeak;
	private static float afterPeak;
	private static long afterMs = 1;
	private static boolean holdRequest;
	private static long zoomStart;
	private static long zoomMs = 1;
	private static float zoomPeak;
	private static long exposeStart;
	private static long exposeMs = 1;
	private static float exposePeak = 1;
	private static float extraAberration;
	private static float dimBoost;
	private static float dimBoostTarget;
	private static long linesStart;
	private static long linesMs = 1;
	private static float linesPeak;
	private static boolean linesBlack;
	private static float linesR0 = 0.3F;
	private static long mangaStart;
	private static long mangaMs = 1;
	private static long shockStart;
	private static long shockMs = 1;
	private static float shockPeak;
	private static long splitStart;
	private static long splitMs = 1;
	private static float splitPeak;
	private static float splitDir;
	private static long streakStart;
	private static long streakMs = 1;
	private static float streakPeak;
	private static long last = Util.getMillis();

	private PostState() {
	}

	static boolean active() {
		long now = Util.getMillis();
		return lookWeight > 0.001F || now < impactUntil || now - flashStart < flashMs + afterMs || now - zoomStart < zoomMs || now - exposeStart < exposeMs
			|| now - linesStart < linesMs || now - mangaStart < mangaMs || now - shockStart < shockMs || now - splitStart < splitMs;
	}

	static boolean takeHoldRequest() {
		boolean r = holdRequest;
		holdRequest = false;
		return r;
	}

	// ------------------------------------------------------------------ API

	/** 套用色調 fadeMs 毫秒淡入，持續 holdMs 毫秒後自動淡出 */
	public static void look(String key, int fadeMs, int holdMs) {
		Look l = LOOKS.get(key);

		if (l == null) {
			return;
		}

		// 演出中換色調：從目前（可能正在混合的）色調平滑過渡
		if (look != null && lookWeight > 0.01F && look != l) {
			fromLook = blend < 1 && fromLook != null ? mix(fromLook, look, blend) : look;
			blend = 0;
			blendRate = 1000F / Math.max(50, fadeMs);
		}
		else {
			blend = 1;
		}

		look = l;
		defaultImpact = l.impactMode();
		lookTarget = 1;
		lookRate = 1000F / Math.max(50, fadeMs);
		lookUntil = Util.getMillis() + holdMs;
	}

	/** B 級短演出的輕色調：以主色微微染世界、淡淡的描線與泛光（依顏色動態建立，不切鏡頭也不會太暗） */
	public static void tint(int rgb, int fadeMs, int holdMs) {
		String key = "tint_" + Integer.toHexString(rgb);

		if (!LOOKS.containsKey(key)) {
			int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			int dark = (r / 14) << 16 | (g / 14) << 8 | b / 14;
			int pale = (200 + r / 5) << 16 | (200 + g / 5) << 8 | (200 + b / 5);
			LOOKS.put(key, new Look(0.22F, rgb, 0.0F, dark, 0F, 1.1F, 1.05F, dark, pale, 1.25F, dark, 0.45F, 0.15F, 0.3F, INK, 0.35F, 0.5F, 0.25F, rgb));
		}

		look(key, fadeMs, holdMs);
	}

	/** 衝擊幀（mode &lt; 0 = 該色調預設） */
	public static void impact(int mode, int ms) {
		long now = Util.getMillis();
		impactCueMode = mode < 0 ? defaultImpact : mode;
		impactStart = now;
		impactUntil = now + Math.max(40, ms);
	}

	/** 閃光；after &gt; 0 時亮部會殘留成燒進視網膜的殘影 */
	public static void flash(int rgb, float peak, int ms, float after) {
		CURRENT.flashR = (rgb >> 16 & 255) / 255F;
		CURRENT.flashG = (rgb >> 8 & 255) / 255F;
		CURRENT.flashB = (rgb & 255) / 255F;
		flashPeak = peak;
		afterPeak = after;
		flashStart = Util.getMillis();
		flashMs = Math.max(50, ms);
		afterMs = after > 0 ? flashMs * 4 : 0;

		if (after > 0) {
			holdRequest = true;
		}
	}

	/** 放射縮放模糊（衝刺感） */
	public static void zoom(float peak, int ms) {
		zoomPeak = peak;
		zoomStart = Util.getMillis();
		zoomMs = Math.max(50, ms);
	}

	/** 過曝後回落 */
	public static void expose(float peak, int ms) {
		exposePeak = peak;
		exposeStart = Util.getMillis();
		exposeMs = Math.max(50, ms);
	}

	/** 額外壓暗（蓄力前的「吸一口氣」），0 = 回到色調預設 */
	public static void darken(float extra) {
		dimBoostTarget = extra;
	}

	/** 集中線（shader 畫，一拍二換形）：dark = 黑線，inner = 中心留白半徑（畫面高比例） */
	public static void lines(float amount, boolean dark, int ms, float inner) {
		linesPeak = amount;
		linesBlack = dark;
		linesStart = Util.getMillis();
		linesMs = Math.max(50, ms);
		linesR0 = inner;
	}

	/** 漫畫分格：整個畫面變成印刷頁（紙白、墨線、網點、交叉排線） */
	public static void manga(int ms) {
		mangaStart = Util.getMillis();
		mangaMs = Math.max(50, ms);
	}

	/** 衝擊波折射環，從畫面中心往外推 */
	public static void shock(float amp, int ms) {
		shockPeak = amp;
		shockStart = Util.getMillis();
		shockMs = Math.max(50, ms);
	}

	/** RGB 硬分離（角度為弧度），迅速衰減 */
	public static void split(float amount, float angle, int ms) {
		splitPeak = amount;
		splitDir = angle;
		splitStart = Util.getMillis();
		splitMs = Math.max(50, ms);
	}

	/** 額外的橫向光芒脈衝（疊在色調預設之上） */
	public static void streak(float amount, int ms) {
		streakPeak = amount;
		streakStart = Util.getMillis();
		streakMs = Math.max(50, ms);
	}

	public static void aberration(float a) {
		extraAberration = a;
	}

	public static void reset() {
		lookTarget = 0;
		lookWeight = 0;
		impactUntil = 0;
		flashStart = 0;
		flashMs = 1;
		afterMs = 1;
		zoomMs = 1;
		exposeMs = 1;
		dimBoost = dimBoostTarget = 0;
		extraAberration = 0;
		linesMs = mangaMs = shockMs = splitMs = streakMs = 1;
		linesStart = mangaStart = shockStart = splitStart = streakStart = 0;
	}

	// ------------------------------------------------------------------ 每幀

	private static int mixc(int a, int b, float t) {
		int r = (int)lerp(a >> 16 & 255, b >> 16 & 255, t);
		int g = (int)lerp(a >> 8 & 255, b >> 8 & 255, t);
		int bb = (int)lerp(a & 255, b & 255, t);
		return r << 16 | g << 8 | bb;
	}

	private static Look mix(Look a, Look b, float t) {
		return new Look(lerp(a.dim(), b.dim(), t), mixc(a.dimTint(), b.dimTint(), t), lerp(a.fog(), b.fog(), t), mixc(a.fogColor(), b.fogColor(), t),
			lerp(a.fogMax(), b.fogMax(), t), lerp(a.contrast(), b.contrast(), t), lerp(a.saturation(), b.saturation(), t), mixc(a.lift(), b.lift(), t),
			mixc(a.gain(), b.gain(), t), lerp(a.bloom(), b.bloom(), t), mixc(a.vignette(), b.vignette(), t), lerp(a.vignetteAmt(), b.vignetteAmt(), t),
			lerp(a.skyDark(), b.skyDark(), t), lerp(a.rays(), b.rays(), t), b.impactMode(), lerp(a.cel(), b.cel(), t), lerp(a.outline(), b.outline(), t),
			lerp(a.streak(), b.streak(), t), mixc(a.streakColor(), b.streakColor(), t));
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float r(int c) {
		return (c >> 16 & 255) / 255F;
	}

	private static float g(int c) {
		return (c >> 8 & 255) / 255F;
	}

	private static float b(int c) {
		return (c & 255) / 255F;
	}

	static void update() {
		long now = Util.getMillis();
		float dt = Math.min(0.1F, (now - last) / 1000F);
		last = now;

		if (lookTarget > 0 && now > lookUntil) {
			lookTarget = 0;
			dimBoostTarget = 0;
		}

		if (lookWeight < lookTarget) {
			lookWeight = Math.min(lookTarget, lookWeight + dt * lookRate);
		}
		else if (lookWeight > lookTarget) {
			lookWeight = Math.max(lookTarget, lookWeight - dt * lookRate);
		}

		dimBoost += (dimBoostTarget - dimBoost) * Math.min(1, dt * 6);
		PostState s = CURRENT;
		float w = lookWeight;
		blend = Math.min(1, blend + dt * blendRate);
		Look l = blend < 1 && fromLook != null && look != null ? mix(fromLook, look, blend) : look;

		if (l == null) {
			w = 0;
			l = LOOKS.get("door");
		}

		s.dim = Math.min(0.95F, (l.dim() + dimBoost) * w);
		s.dimR = r(l.dimTint());
		s.dimG = g(l.dimTint());
		s.dimB = b(l.dimTint());
		s.fog = l.fog() * w;
		s.fogR = r(l.fogColor());
		s.fogG = g(l.fogColor());
		s.fogB = b(l.fogColor());
		s.fogMax = l.fogMax() * w;
		s.fogFalloff = 0.02F;
		Minecraft mc = Minecraft.getInstance();

		if (mc.player != null) {
			s.groundY = mc.player.getY();
		}

		s.contrast = lerp(1, l.contrast(), w);
		s.saturation = lerp(1, l.saturation(), w);
		s.liftR = r(l.lift()) * w;
		s.liftG = g(l.lift()) * w;
		s.liftB = b(l.lift()) * w;
		s.gainR = lerp(1, r(l.gain()), w);
		s.gainG = lerp(1, g(l.gain()), w);
		s.gainB = lerp(1, b(l.gain()), w);
		s.bloom = l.bloom() * Math.max(w, 0.6F);
		s.vigR = r(l.vignette());
		s.vigG = g(l.vignette());
		s.vigB = b(l.vignette());
		s.vignette = l.vignetteAmt() * w;
		s.heat = 0;
		s.skyDark = l.skyDark() * w;
		s.rays = l.rays() * w + (1 - w) * 0.5F;
		s.grain = 0.35F * w;

		float ft = (now - flashStart) / (float)flashMs;
		s.flash = ft < 1 ? flashPeak * (1 - ft) * (1 - ft) : 0;
		float at = (now - flashStart - flashMs * 0.3F) / (float)Math.max(1, afterMs);
		s.after = afterMs > 0 && at > -0.3F && at < 1 ? afterPeak * (1 - Math.max(0, at)) : 0;

		if (now < impactUntil) {
			// 中間閃掉一格，做出「啪、啪」兩拍的頻閃
			long since = now - impactStart;
			long total = impactUntil - impactStart;
			s.impact = total < 400 && since > total * 0.45 && since < total * 0.55 ? 0 : 1;
			s.impactMode = impactCueMode;
		}
		else {
			s.impact = 0;
		}

		float zt = (now - zoomStart) / (float)zoomMs;
		s.zoom = zt < 1 ? zoomPeak * (float)Math.sin(Math.PI * Math.min(1, zt * 1.5)) * (1 - zt) : 0;
		s.aberration = 0.35F * w + s.zoom * 1.5F + (s.impact > 0 ? 0.8F : 0) + extraAberration * w;
		s.cel = l.cel() * w;
		s.outline = l.outline() * w;
		float stt = (now - streakStart) / (float)streakMs;
		s.streak = l.streak() * w + (stt < 1 ? streakPeak * (1 - stt) * (1 - stt) : 0);
		s.streakR = r(l.streakColor());
		s.streakG = g(l.streakColor());
		s.streakB = b(l.streakColor());

		long lt = now - linesStart;
		s.lines = lt < linesMs ? linesPeak * Math.min(1, Math.min(lt / 40F, (linesMs - lt) / 150F)) : 0;
		s.linesDark = linesBlack ? 1 : 0;
		s.linesInner = linesR0;

		long mt = now - mangaStart;
		// 啪地切進去（一格），結尾 90ms 溶回
		s.manga = mt < mangaMs ? Math.min(1, (mangaMs - mt) / 90F) : 0;

		float sht = (now - shockStart) / (float)shockMs;
		s.shockR = sht < 1 ? sht * 1.4F : 0;
		s.shockAmp = sht < 1 ? shockPeak * (1 - sht) : 0;

		float spt = (now - splitStart) / (float)splitMs;
		s.split = spt < 1 ? splitPeak * (1 - spt) * (1 - spt) : 0;
		s.splitAngle = splitDir;

		float et = (now - exposeStart) / (float)exposeMs;
		s.exposure = et < 1 ? lerp(exposePeak, 1, et * et) : 1;
	}
}
