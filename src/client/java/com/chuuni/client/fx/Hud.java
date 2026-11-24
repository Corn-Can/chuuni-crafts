package com.chuuni.client.fx;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chuuni.Chuuni;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * 畫面層（打架動漫風）：
 * <ul>
 *   <li>名卡：紅印「蓋」下去 → 黑色斜帶劃過 → 宋體大字從右側衝入、光暈一閃 → 英文字距展開 → 整張向左滑走</li>
 *   <li>招式名 A（直式毛筆）：畫面右側一字一字寫下的毛筆大字 + 片假名讀音 + 下方字幕</li>
 *   <li>招式名 B（漫畫四角）：四個極粗大字依「右上→右下→左上→左下」砸進四角，每字旁有讀音</li>
 *   <li>集中線：4 張預畫的線稿以每秒 12 張輪播（一拍二），白線 / 黑線兩種</li>
 *   <li>電影黑邊、UI 閃光、彩色暈影、COMBO、刻名</li>
 * </ul>
 */
public final class Hud {
	private static final Identifier VIGNETTE = Chuuni.id("textures/gui/vignette.png");
	private static final Identifier[] SPEED = {
		Chuuni.id("textures/gui/speed/0.png"), Chuuni.id("textures/gui/speed/1.png"),
		Chuuni.id("textures/gui/speed/2.png"), Chuuni.id("textures/gui/speed/3.png")
	};

	private record CardSize(int sw, int sh, int tw, int th, int ew, int eh) {
	}

	private static final Map<String, CardSize> SIZES = new HashMap<>();

	// 名卡
	private static String card;
	private static double cardStart = -100;
	private static double cardLen;
	private static float cardScale = 1f;
	private static float cardY = 0.3f;

	// 招式名（用真實時間，頓幀時也照樣動）
	private record NameSize(int bw, int bh, int[][] cells, int rw, int rh, int sw, int sh, int[][] corners, int[][] rubies,
		int erw, int erh, int esw, int esh, int[][] erubies) {
	}

	private static final Map<String, NameSize> NAMES = new HashMap<>();
	private static String brushKey;
	private static double brushStart = -100;
	private static double brushLen;
	private static String mangaKey;
	private static double mangaStart = -100;
	private static double mangaLen;
	private static int mangaSlams;
	/** 招式名若是在運鏡中出現，運鏡結束（回到介面）時一起收掉 */
	private static boolean brushCine;
	private static int brushHits;
	/** 小型招式名（B 級短演出）：右上角、約四成高，只有第一筆有落款聲 */
	private static boolean brushSmall;
	private static final Map<String, Double> SMALL_SHOWN = new HashMap<>();

	// 集中線
	private static double speedStart = -100;
	private static double speedLen;
	private static float speedAmt;
	private static boolean speedDark;

	// 白光淡出
	private static double whiteStart = -100;
	private static double whiteUp = 0.5;
	private static double whiteDown = 1;
	private static int whiteColor = 0xFFFFFF;

	// 閃光 / 暈影
	private static double flashStart = -100;
	private static double flashLen = 0.3;
	private static int flashColor = 0xFFFFFF;
	private static float flashPeak = 1f;
	private static double vigStart = -100;
	private static double vigLen;
	private static int vigColor;
	private static float vigPeak;

	// COMBO / 刻名
	private static int combo;
	private static double comboStart = -100;
	private static int comboColor = 0xFFE070;
	private static String comboLabel = "COMBO";
	private static String engraved;
	private static double engraveStart = -100;

	private Hud() {
	}

	// ------------------------------------------------------------------ API

	public static void title(String key, double duration, float scale, float y) {
		card = key;
		cardStart = FxClock.now();
		cardLen = duration;
		cardScale = scale;
		cardY = y;
	}

	/** 招式名 A：直式毛筆字（右側） */
	public static void brushName(String key, double duration) {
		brushKey = key;
		brushStart = real();
		brushLen = duration;
		brushCine = Cinematic.active();
		brushHits = 0;
		brushSmall = false;
	}

	/** 小型招式名（B 級）：同一個名字 20 秒內不重複出現，免得常用的方塊一直洗畫面 */
	public static void smallName(String key, double duration) {
		double now = real();
		Double last = SMALL_SHOWN.get(key);

		if (last != null && now - last < 20) {
			return;
		}

		// 正在播大的招式名時不要蓋掉
		if (brushKey != null && !brushSmall && now - brushStart < brushLen) {
			return;
		}

		SMALL_SHOWN.put(key, now);
		brushKey = key;
		brushStart = now;
		brushLen = duration;
		brushCine = false;
		brushHits = 0;
		brushSmall = true;
	}

	/** 招式名 B：漫畫四角大字 */
	public static void mangaName(String key, double duration) {
		mangaKey = key;
		mangaStart = real();
		mangaLen = duration;
		mangaSlams = 0;
	}

	private static double real() {
		return Util.getMillis() / 1000.0;
	}

	public static void speedLines(double duration, float amount, boolean dark) {
		speedStart = FxClock.now();
		speedLen = duration;
		speedAmt = amount;
		speedDark = dark;
	}

	public static void flash(int rgb, float peak, double duration) {
		flashColor = rgb;
		flashPeak = peak;
		flashLen = duration;
		flashStart = FxClock.now();
	}

	/** 淡入成一片光（up 秒）再淡出（down 秒）；真實時間，跨過演出結束也會照樣淡出 */
	public static void whiteout(int rgb, double up, double down) {
		whiteColor = rgb;
		whiteUp = up;
		whiteDown = down;
		whiteStart = real();
	}

	public static void vignette(int rgb, float peak, double duration) {
		vigColor = rgb;
		vigPeak = peak;
		vigLen = duration;
		vigStart = FxClock.now();
	}

	public static void combo(int count, int rgb, String label) {
		combo = count;
		comboColor = rgb;
		comboLabel = label;
		comboStart = FxClock.now();
	}

	public static void engrave(String text) {
		engraved = text;
		engraveStart = FxClock.now();
	}

	public static void clear() {
		cardStart = flashStart = vigStart = comboStart = engraveStart = speedStart = -100;
		brushStart = mangaStart = -100;
	}

	// ------------------------------------------------------------------ 繪製

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		int w = g.guiWidth();
		int h = g.guiHeight();
		double now = FxClock.now();

		double vt = (now - vigStart) / Math.max(0.01, vigLen);

		if (vt >= 0 && vt < 1) {
			float a = vigPeak * (float)(vt < 0.15 ? vt / 0.15 : 1 - (vt - 0.15) / 0.85);
			g.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0, 0, w, h, 256, 256, 256, 256, argb(a, vigColor));
		}

		renderSpeed(g, w, h, now);

		float lb = Cinematic.letterbox();

		if (lb > 0.001f) {
			int bar = (int)(h * 0.1f * lb);
			g.fill(0, 0, w, bar, 0xFF000000);
			g.fill(0, h - bar, w, h, 0xFF000000);
		}

		renderCard(g, w, h, now);
		renderBrush(g, w, h);
		renderManga(g, w, h);
		renderCombo(g, mc.font, w, h, now);
		renderEngrave(g, mc.font, w, h, now);

		double ft = (now - flashStart) / Math.max(0.01, flashLen);

		if (ft >= 0 && ft < 1) {
			float a = flashPeak * (float)Math.pow(1 - ft, 2.2);
			g.fill(0, 0, w, h, argb(a, flashColor));
		}

		double wt = real() - whiteStart;

		if (wt >= 0 && wt < whiteUp + whiteDown) {
			double a = wt < whiteUp ? wt / whiteUp : 1 - (wt - whiteUp) / whiteDown;
			a = a * a * (3 - 2 * a);
			g.fill(0, 0, w, h, argb((float)a * 0.92f, whiteColor));
		}
	}

	private static int argb(float a, int rgb) {
		return ARGB.color(Mth.clamp((int)(a * 255), 0, 255), rgb);
	}

	private static void tex(GuiGraphics g, Identifier id, float x, float y, float dw, float dh, int tw, int th, int color) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(dw / tw, dh / th);
		g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, tw, th, tw, th, tw, th, color);
		pose.popMatrix();
	}

	private static void renderSpeed(GuiGraphics g, int w, int h, double now) {
		double t = now - speedStart;

		if (t < 0 || t > speedLen) {
			return;
		}

		float a = speedAmt * (float)Math.min(1, Math.min(t / 0.05, (speedLen - t) / 0.2));
		// 一拍二：每秒 12 張
		int frame = (int)Math.floor(t * 12) % SPEED.length;
		tex(g, SPEED[frame], 0, 0, w, h, 1280, 720, argb(a, speedDark ? 0x000000 : 0xFFFFFF));
	}

	private static CardSize size(String key) {
		if (SIZES.isEmpty()) {
			try {
				var res = Minecraft.getInstance().getResourceManager().getResource(Chuuni.id("cards.json"));

				if (res.isPresent()) {
					try (Reader r = res.get().openAsReader()) {
						JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

						for (var e : root.entrySet()) {
							JsonObject o = e.getValue().getAsJsonObject();
							SIZES.put(e.getKey(), new CardSize(o.getAsJsonArray("seal").get(0).getAsInt(), o.getAsJsonArray("seal").get(1).getAsInt(),
								o.getAsJsonArray("title").get(0).getAsInt(), o.getAsJsonArray("title").get(1).getAsInt(),
								o.getAsJsonArray("en").get(0).getAsInt(), o.getAsJsonArray("en").get(1).getAsInt()));
						}
					}
				}
			}
			catch (Exception e) {
				Chuuni.LOGGER.error("cannot read cards.json", e);
			}
		}

		return SIZES.get(key);
	}

	private static void renderCard(GuiGraphics g, int w, int h, double now) {
		double t = now - cardStart;

		if (card == null || t < 0 || t > cardLen) {
			return;
		}

		CardSize cs = size(card);

		if (cs == null) {
			return;
		}

		float k = cardScale;
		float titleH = h * 0.19f * k;
		float titleW = titleH * cs.tw / cs.th;
		float sealH = titleH * 1.05f;
		float sealW = sealH * cs.sw / cs.sh;
		float total = sealW + titleH * 0.12f + titleW;
		float cx = w / 2f;
		float cy = h * cardY;
		float exit = (float)Mth.clamp((t - (cardLen - 0.3)) / 0.3, 0, 1);
		float exitX = -exit * exit * w * 0.35f;
		float alpha = 1 - exit;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx + exitX, cy);
		pose.rotate((float)Math.toRadians(-3));

		// 黑色斜帶（0.05~0.2 秒由左劃到右）
		float band = (float)Mth.clamp((t - 0.05) / 0.15, 0, 1);
		band = 1 - (1 - band) * (1 - band);

		if (band > 0) {
			int bw = (int)(w * 1.2f * band);
			int bh = (int)(titleH * 0.62f);
			g.fill((int)(-w * 0.6f), -bh / 2, (int)(-w * 0.6f) + bw, bh / 2, argb(0.78f * alpha, 0x050208));
			g.fill((int)(-w * 0.6f), bh / 2, (int)(-w * 0.6f) + bw, bh / 2 + 2, argb(0.9f * alpha, 0xC81E1A));
		}

		float left = -total / 2;
		// 印章：0~0.12 秒從 1.8 倍蓋下（帶一點旋轉）
		float stamp = (float)Mth.clamp(t / 0.12, 0, 1);
		float ss = 1.8f - 0.8f * (1 - (1 - stamp) * (1 - stamp));
		pose.pushMatrix();
		pose.translate(left + sealW / 2, 0);
		pose.rotate((float)Math.toRadians((1 - stamp) * 12 - 2));
		pose.scale(ss, ss);
		tex(g, Chuuni.id("textures/gui/card/" + card + "_seal.png"), -sealW / 2, -sealH / 2, sealW, sealH, cs.sw, cs.sh, argb(alpha * stamp, 0xFFFFFF));
		pose.popMatrix();

		// 標題：0.12 秒起從右衝入
		float in = (float)Mth.clamp((t - 0.12) / 0.14, 0, 1);
		float ease = 1 - (float)Math.pow(1 - in, 4);
		float tx = left + sealW + titleH * 0.12f + (1 - ease) * w * 0.25f;
		float flash = (float)Mth.clamp(1 - (t - 0.26) / 0.35, 0, 1) * (t > 0.24 ? 1 : 0);
		float glowA = alpha * Math.max(0.55f, flash) * in;
		tex(g, Chuuni.id("textures/gui/card/" + card + "_glow.png"), tx, -titleH / 2, titleW, titleH, cs.tw, cs.th, argb(glowA, 0xFFFFFF));

		// 衝入時的殘影（往右拖兩張）
		if (in < 1) {
			for (int i = 1; i <= 2; i++) {
				tex(g, Chuuni.id("textures/gui/card/" + card + ".png"), tx + i * w * 0.04f * (1 - ease), -titleH / 2, titleW, titleH, cs.tw, cs.th,
					argb(alpha * in * 0.3f / i, 0xFFFFFF));
			}
		}

		tex(g, Chuuni.id("textures/gui/card/" + card + ".png"), tx, -titleH / 2, titleW, titleH, cs.tw, cs.th, argb(alpha * in, 0xFFFFFF));

		// 英文：0.3 秒起淡入，字距由擠到開（以水平縮放表現）
		float en = (float)Mth.clamp((t - 0.3) / 0.35, 0, 1);

		if (en > 0) {
			float eh = titleH * 0.2f;
			float ew = eh * cs.ew / cs.eh * (0.85f + 0.15f * en);
			tex(g, Chuuni.id("textures/gui/card/" + card + "_en.png"), tx + titleW * 0.05f, titleH * 0.42f, ew, eh, cs.ew, cs.eh, argb(alpha * en, 0xFFFFFF));
		}

		pose.popMatrix();
	}

	private static NameSize names(String key) {
		if (NAMES.isEmpty()) {
			try {
				var res = Minecraft.getInstance().getResourceManager().getResource(Chuuni.id("names.json"));

				if (res.isPresent()) {
					try (Reader r = res.get().openAsReader()) {
						JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

						for (var e : root.entrySet()) {
							JsonObject o = e.getValue().getAsJsonObject();
							NAMES.put(e.getKey(), new NameSize(o.getAsJsonArray("brush").get(0).getAsInt(), o.getAsJsonArray("brush").get(1).getAsInt(),
								pairs(o.getAsJsonArray("cells")), o.getAsJsonArray("ruby").get(0).getAsInt(), o.getAsJsonArray("ruby").get(1).getAsInt(),
								o.getAsJsonArray("sub").get(0).getAsInt(), o.getAsJsonArray("sub").get(1).getAsInt(), pairs(o.getAsJsonArray("corners")),
								pairs(o.getAsJsonArray("rubies")), o.getAsJsonObject("en").getAsJsonArray("ruby").get(0).getAsInt(),
								o.getAsJsonObject("en").getAsJsonArray("ruby").get(1).getAsInt(), o.getAsJsonObject("en").getAsJsonArray("sub").get(0).getAsInt(),
								o.getAsJsonObject("en").getAsJsonArray("sub").get(1).getAsInt(), pairs(o.getAsJsonObject("en").getAsJsonArray("rubies"))));
						}
					}
				}
			}
			catch (Exception e) {
				Chuuni.LOGGER.error("cannot read names.json", e);
			}
		}

		return NAMES.get(key);
	}

	private static int[][] pairs(com.google.gson.JsonArray a) {
		int[][] out = new int[a.size()][2];

		for (int i = 0; i < a.size(); i++) {
			out[i][0] = a.get(i).getAsJsonArray().get(0).getAsInt();
			out[i][1] = a.get(i).getAsJsonArray().get(1).getAsInt();
		}

		return out;
	}

	private static Identifier name(String key, String part) {
		return Chuuni.id("textures/gui/name/" + key + "_" + part + ".png");
	}

	/** 中文、日文介面用假名讀音 + 中文字幕；其他語言用英文讀音 + 英文字幕（漢字本體當書法美術保留） */
	private static boolean english() {
		String code = Minecraft.getInstance().options.languageCode;
		return code == null || !(code.startsWith("zh") || code.startsWith("ja"));
	}

	/** A：右側直式毛筆字，一字一字由上往下「寫」出來；讀音隨後滑入，字幕在下方 */
	private static void renderBrush(GuiGraphics g, int w, int h) {
		double t = real() - brushStart;

		if (brushCine && !Cinematic.active()) {
			brushKey = null;
		}

		if (brushKey == null || t < 0 || t > brushLen) {
			return;
		}

		NameSize n = names(brushKey);

		if (n == null) {
			return;
		}

		float exit = (float)Mth.clamp((t - (brushLen - 0.5)) / 0.5, 0, 1);
		float alpha = 1 - exit;
		boolean small = brushSmall;
		float colH = h * (small ? 0.42f : 1.0f);
		float k = colH / n.bh;
		float colW = n.bw * k;
		float x0 = w - h * (small ? 0.1f : 0.2f) - colW / 2;
		float y0 = (small ? h * 0.12f : -h * 0.03f) - exit * h * 0.02f;
		double step = small ? 0.1 : 0.18;
		Identifier brush = name(brushKey, "brush");
		int count = n.cells.length;

		for (int i = 0; i < count; i++) {
			double ct = t - 0.06 - i * step;

			if (ct < 0) {
				break;
			}

			if (i >= brushHits) {
				// 第一筆是掃過來的重落款，之後每字一記重擊
				brushHits = i + 1;

				if (!small) {
					// 動漫素材：落款「ザッ」、之後每字「ズン」（疊在原本的毛筆重擊上）
					Sfx.nameUi(i == 0 ? "brush_hit" : "name_hit", i == 0 ? 0.6f : 0.45f, 0.85f - i * 0.03f);

					// 「ザッ」落款；「ズン」有 0.6 秒的蓄勢，開頭播一次，重音正好落在最後一個字寫完
					if (i == 0) {
						Sfx.nameAnimeUi("title", null, 1f, 1f);
						Sfx.nameAnimeUi("char", null, 1f, 1f);
					}
				}
				else if (i == 0) {
					// 小招式名：「キラッ」（沒有素材時維持原本的毛筆聲）
					Sfx.nameAnimeUi("kira", "brush_hit", 0.7f, 1.05f);
				}
			}

			float p = (float)Mth.clamp(ct / 0.15, 0, 1);
			p = 1 - (1 - p) * (1 - p);
			float top = i == 0 ? 0 : n.cells[i][0];
			float bot = i == count - 1 ? n.bh : n.cells[i][1];
			int sy0 = (int)Math.floor(y0 + top * k);
			int sy1 = (int)Math.ceil(y0 + (top + (bot - top) * p) * k);
			g.enableScissor((int)x0 - 2, sy0, (int)(x0 + colW) + 2, sy1);
			tex(g, brush, x0, y0, colW, colH, n.bw, n.bh, argb(alpha * Math.min(1, p * 1.6f), 0xFFFFFF));
			g.disableScissor();
		}

		float rubyIn = (float)Mth.clamp((t - 0.1 - count * step) / 0.35, 0, 1);

		if (rubyIn > 0) {
			float e = 1 - (1 - rubyIn) * (1 - rubyIn);
			boolean en = english();
			int tw = en ? n.erw : n.rw;
			int th = en ? n.erh : n.rh;
			float rw = tw * k;
			float rh = th * k;
			tex(g, name(brushKey, en ? "ruby_en" : "ruby"), x0 + colW * 0.84f - rw / 2, y0 + (colH - rh) / 2 - (1 - e) * h * 0.04f, rw, rh, tw, th, argb(alpha * e, 0xFFFFFF));
		}

		float subIn = (float)Mth.clamp((t - 0.2) / 0.2, 0, 1);

		if (subIn > 0) {
			boolean en = english();
			int tw = en ? n.esw : n.sw;
			int th = en ? n.esh : n.sh;
			float sh = h * (small ? 0.038f : 0.06f);
			float sw = Math.min(w * (small ? 0.4f : 0.6f), sh * tw / th);
			sh = sw * th / tw;

			if (small) {
				float sx = Math.min(x0 + colW / 2 - sw / 2, w - sw - 6);
				tex(g, name(brushKey, en ? "sub_en" : "sub"), sx, y0 + colH + h * 0.01f, sw, sh, tw, th, argb(alpha * subIn, 0xFFFFFF));
			}
			else {
				tex(g, name(brushKey, en ? "sub_en" : "sub"), (w - sw) / 2, h * 0.865f - sh / 2, sw, sh, tw, th, argb(alpha * subIn, 0xFFFFFF));
			}
		}
	}

	/** B：四個大字依「右上、右下、左上、左下」砸進四角，讀音貼在朝內的一側 */
	private static void renderManga(GuiGraphics g, int w, int h) {
		double t = real() - mangaStart;

		if (mangaKey == null || t < 0 || t > mangaLen) {
			return;
		}

		NameSize n = names(mangaKey);

		if (n == null) {
			return;
		}

		float exit = (float)Mth.clamp((t - (mangaLen - 0.22)) / 0.22, 0, 1);
		float alpha = 1 - exit;
		float tile = h * 0.56f;
		float glyph = tile * 0.71f;
		float[][] at = {
			{w - tile * 0.42f, tile * 0.38f, 1}, {w - tile * 0.42f, h - tile * 0.38f, 1}, {tile * 0.42f, tile * 0.38f, -1}, {tile * 0.42f, h - tile * 0.38f, -1}
		};
		float[] tilt = {3, -4, -3, 4};
		var pose = g.pose();

		for (int i = 0; i < Math.min(4, n.corners.length); i++) {
			double ct = t - i * 0.16;

			if (ct < 0) {
				break;
			}

			if (i >= mangaSlams) {
				// 每砸一個字一聲（頓幀中照樣響）
				mangaSlams = i + 1;
				Sfx.nameUi("name_hit", 0.8f, 0.92f + i * 0.05f);

				// 時停的第一個字：「シャキーン」
				if (i == 0) {
					Sfx.nameAnimeUi("shakin", null, 0.9f, 1f);
				}
			}

			float slam = ct < 0.07 ? (float)(2.1 - 1.1 * (ct / 0.07)) : 1f;
			float jx = 0;
			float jy = 0;

			if (ct > 0.07 && ct < 0.25) {
				float kk = (float)(1 - (ct - 0.07) / 0.18);
				jx = (float)Math.sin(ct * 140 + i) * 5 * kk;
				jy = (float)Math.cos(ct * 113 + i) * 5 * kk;
			}

			float a = alpha * (float)Math.min(1, ct / 0.03);
			float sc = slam * (1 + exit * 0.15f);
			float cx = at[i][0] + jx;
			float cy = at[i][1] + jy;
			pose.pushMatrix();
			pose.translate(cx, cy);
			pose.rotate((float)Math.toRadians(tilt[i]));
			pose.scale(sc, sc);
			tex(g, name(mangaKey, "c" + i), -tile / 2, -tile / 2, tile, tile, n.corners[i][0], n.corners[i][1], argb(a, 0xFFFFFF));
			pose.popMatrix();

			float rIn = (float)Mth.clamp((ct - 0.06) / 0.08, 0, 1);

			if (rIn > 0 && i < n.rubies.length) {
				boolean en = english();
				int[] rs = en ? n.erubies[i] : n.rubies[i];
				float rk = Math.min(h * 0.06f / 82f, glyph * 0.95f / rs[1]);
				float rw = rs[0] * rk;
				float rh = rs[1] * rk;
				float rx = at[i][2] < 0 ? cx + glyph * 0.5f + rw * 0.05f : cx - glyph * 0.5f - rw * 1.05f;
				tex(g, name(mangaKey, en ? "r" + i + "_en" : "r" + i), rx, cy - glyph * 0.48f, rw, rh, rs[0], rs[1], argb(alpha * rIn, 0xFFFFFF));
			}
		}
	}

	private static void renderCombo(GuiGraphics g, Font font, int w, int h, double now) {
		double t = now - comboStart;

		if (combo < 2 || t < 0 || t > 1.6) {
			return;
		}

		float pop = t < 0.12 ? (float)(1.8 - 0.8 * t / 0.12) : 1f;
		float alpha = (float)Mth.clamp(1 - (t - 1.2) / 0.4, 0, 1);
		String num = "×" + combo;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(w * 0.78f, h * 0.62f);
		pose.rotate(-0.12f);
		pose.scale(3.2f * pop, 3.2f * pop);
		int a = Mth.clamp((int)(alpha * 255), 0, 255);
		g.drawString(font, num, -font.width(num) / 2 + 1, 1, ARGB.color(a, 0x000000), false);
		g.drawString(font, num, -font.width(num) / 2, 0, ARGB.color(a, comboColor), false);
		pose.scale(0.42f, 0.42f);
		g.drawString(font, comboLabel, -font.width(comboLabel) / 2, 22, ARGB.color(a, 0xFFFFFF), true);
		pose.popMatrix();
	}

	private static void renderEngrave(GuiGraphics g, Font font, int w, int h, double now) {
		double t = now - engraveStart;

		if (engraved == null || t < 0 || t > 2.4) {
			return;
		}

		float alpha = (float)Mth.clamp(1 - (t - 1.9) / 0.5, 0, 1);
		int[] cps = engraved.codePoints().toArray();
		double perChar = Math.min(0.08, 0.8 / Math.max(1, cps.length));
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(w / 2f, h * 0.6f);
		pose.scale(2.4f, 2.4f);
		int x = -font.width(engraved) / 2;

		for (int i = 0; i < cps.length; i++) {
			double ct = t - i * perChar;

			if (ct < 0) {
				break;
			}

			String ch = new String(Character.toChars(cps[i]));
			float heat = (float)Mth.clamp(1 - ct / 0.5, 0, 1);
			int col = lerpColor(0xFFC040, 0xFFFFFF, heat);
			int a = Mth.clamp((int)(alpha * 255), 0, 255);
			float jump = (float)(ct < 0.1 ? (1 - ct / 0.1) * -4 : 0);
			pose.pushMatrix();
			pose.translate(0, jump);
			g.drawString(font, ch, x + 1, 1, ARGB.color(a, 0x401000), false);
			g.drawString(font, ch, x, 0, ARGB.color(a, col), false);
			pose.popMatrix();
			x += font.width(ch);
		}

		pose.popMatrix();
	}

	static int lerpColor(int a, int b, float t) {
		int r = (int)Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
		int gg = (int)Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
		int bb = (int)Mth.lerp(t, a & 255, b & 255);
		return r << 16 | gg << 8 | bb;
	}
}
