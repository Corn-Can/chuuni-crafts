package com.chuuni.client.fx.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Frame;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Effect;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxMesh;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 附魔台——打架動漫分鏡，依花費分三階：
 * <pre>
 * 一階 星屑祝福（&lt;10 級）：冰藍、一層法陣、星光綻放（沒有光柱）、毛筆直書招式名
 * 二階 星辰刻印（10~19 級）：靛紫、兩層法陣、紫色衝擊幀、天柱落下、毛筆直書招式名
 * 三階 天穹禁咒（20 級以上）：暗紫 + 金、三層法陣、衝擊幀、金紫雙層天柱、時間停止的漫畫分格 + 四角大字、星空穹頂、符文雨
 *
 * 鏡1 0.00 覺醒  低角度仰拍：世界沉入色調（賽璐珞描線），物品浮起，書架的符文化成光流飛來
 * 鏡2 1.20 鎖定  高空廣角：法陣一層一層「喀」地鎖上（每鎖一層 RGB 錯開），雷光亂竄
 * 鏡3 2.30 吸氣  物品極近特寫、望遠壓縮、荷蘭角：畫面再壓暗，黑色集中線
 * 衝擊 2.65     衝擊幀 + 頓幀 + 折射衝擊波 + RGB 分離 →（三階：漫畫分格時停 + 四角大字）
 * 鏡4 2.65 英雄  超低角度仰望（慢動作）：白色集中線、橫向光芒、毛筆直書招式名
 * 鏡5 4.00 餘韻  拉遠：結晶環繞
 * 鏡6 4.90 降臨  色調轉為暖白的聖光：物品在光柱中緩緩降下、散發光芒、光塵飄落 → 白光淡出，直接回到世界（不回介面）
 * </pre>
 */
public class EnchantFx extends Effect {
	private static final Identifier CIRCLE = FxRender.tex("circle_enchant");
	private static final Identifier BAND = FxRender.tex("rune_band");
	private static final Identifier BEAM = FxRender.tex("beam");
	private static final Identifier BOLT = FxRender.tex("bolt");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");
	private static final Identifier STARS = FxRender.tex("starfield");

	private static final int HOLY = 0xFFE6B0;
	private static final int HOLY_WHITE = 0xFFF8EC;

	private static final double S2 = 1.2;
	private static final double[] SNAPS = {1.32, 1.62, 1.92};
	private static final double S3 = 2.3;
	private static final double IMPACT = 2.65;
	private static final double S5 = 4.0;
	private static final double S6 = 4.9;
	private static final double DURATION = 7.6;
	private static final int MANGA_MS = 1450;

	private static final float[] HEIGHTS = {1.25f, 2.05f, 2.75f};
	private static final float[] RADII = {1.25f, 0.9f, 0.58f};

	private final int tier;
	private final int main;
	private final int accent;
	private final int pale;
	private final float level;
	private final List<Vec3> shelves = new ArrayList<>();
	private final Vec3 floor;
	private final Frame frame;
	private final boolean near;

	public EnchantFx(FxPayload p) {
		super(p);
		tier = Settings.intensity >= 2 ? 3 : p.power() >= 20 ? 3 : p.power() >= 10 ? 2 : 1;
		main = tier == 1 ? 0x58A8FF : tier == 2 ? 0xA060FF : 0xFFB040;
		accent = tier == 1 ? 0xBFE6FF : tier == 2 ? 0xFFD27A : 0xA060FF;
		pale = tier == 1 ? 0xE0F2FF : tier == 2 ? 0xE6D8FF : 0xFFF0D0;
		level = Mth.clamp(p.power() / 30f, 0.2f, 1f);
		floor = new Vec3(c.x, pos.getY() + 0.02, c.z);
		frame = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
		Level lvl = Minecraft.getInstance().level;

		if (lvl != null) {
			for (BlockPos o : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 1, 2))) {
				if (lvl.getBlockState(o).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
					shelves.add(Vec3.atCenterOf(o));
				}
			}
		}

		var player = Minecraft.getInstance().player;
		near = mine || player != null && player.position().distanceTo(c) < 14;

		if (near) {
			PostState.look(tier == 1 ? "enchant1" : tier == 2 ? "enchant" : "enchant3", 350, (int)(S6 * 1000));
		}

		if (mine) {
			Vec3 rise0 = frame.at(0, 1.35, 0);
			Vec3 rise1 = frame.at(0, 1.8, 0);
			Vec3 mid = frame.at(0, 1.5, 0);
			Vec3 apex = frame.at(0, 1.85, 0);
			Vec3 rest = frame.at(0, 1.15, 0);
			Cinematic.play(List.of(
				Shot.of(S2, frame.at(0.75, 0.55, 1.55), frame.at(0.45, 0.62, 1.15), rise0).look(rise0, rise1).roll(0, 6).fov(0.9f, 0.82f),
				Shot.of(S3 - S2, frame.at(-1.6, 4.2, 1.5), frame.at(-1.3, 3.7, 1.2), mid).roll(-3, -4),
				Shot.of(IMPACT - S3, frame.at(0.05, 1.95, 1.05), frame.at(0.03, 1.92, 0.78), rise1).roll(-8, -13).fov(0.7f, 0.58f).accelerate(),
				Shot.of(S5 - IMPACT, frame.at(1.5, 0.22, 2.6), frame.at(1.9, 0.3, 3.3), frame.at(0, 3.0, 0)).look(frame.at(0, 3.0, 0), frame.at(0, 2.3, 0))
					.roll(13, 9).fov(1.18f, 1.1f),
				Shot.of(S6 - S5, frame.at(-0.9, 2.5, 1.5), frame.at(-1.5, 3.4, 1.9), mid),
				// 降臨：略低的角度仰望，跟著物品慢慢往下、推近
				Shot.of(DURATION - S6, frame.at(1.4, 0.95, 3.0), frame.at(0.95, 1.05, 2.2), apex).look(apex.add(0, 0.25, 0), rest).roll(5, 1).fov(0.9f, 0.8f)
			), claimAndClose(EnchantmentMenu.class));
		}
	}

	@Override
	public double duration() {
		return DURATION;
	}

	/** 物品位置：浮起 → 鎖定時顫抖 → 吸氣下沉蓄力 → 爆發後懸停 → 聖光中緩緩降下 */
	private Vec3 itemPos(double t) {
		float rise = easeOut(span(t, 0.1, 1.0));
		float sink = span(t, S3, IMPACT) * (1 - span(t, IMPACT, IMPACT + 0.1));
		double apex = pos.getY() + 1.85;
		double rest = pos.getY() + 1.15;
		double y = pos.getY() + 0.95 + rise * 0.9 - sink * 0.08;

		if (t > S6) {
			float d = span(t, S6, DURATION - 0.35);
			d = d * d * (3 - 2 * d);
			y = apex + (rest - apex) * d + Math.sin(t * 2.2) * 0.03 * (1 - d);
		}

		double shake = t > S2 && t < IMPACT ? 0.015 + 0.03 * span(t, S2, IMPACT) : 0;
		return new Vec3(c.x + shake * Math.sin(t * 91), y, c.z + shake * Math.cos(t * 77));
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 item = itemPos(t);

		// ---------------------------------------------------------- 鏡1：覺醒
		if (cue(0.0)) {
			Sfx.play(mine, "enchant_charge", c, 1f, tier == 1 ? 1.12f : tier == 2 ? 1f : 0.88f);
		}

		if (t > 0.1 && t < S3) {
			int n = rate(18 * tier * (0.6 + level), dt);

			for (int i = 0; i < n; i++) {
				Vec3 from;

				if (!shelves.isEmpty()) {
					from = shelves.get(Particles.RNG.nextInt(shelves.size())).add(Particles.randomDir(0.4));
				}
				else {
					double a = Particles.RNG.nextDouble() * Math.PI * 2;
					from = new Vec3(c.x + Math.cos(a) * 2.6, pos.getY() + 0.2 + Particles.RNG.nextDouble() * 1.4, c.z + Math.sin(a) * 2.6);
				}

				Particles.spawn(Kind.GLYPH, from, new Vec3(0, 1.4, 0).add(Particles.randomDir(0.5)), 1.2).size(0.12f, 0.05f).color(accent, pale)
					.physics(0, 1.8f).attract(item, 8f).fadeIn(0.15f);
			}
		}

		// ---------------------------------------------------------- 鏡2：法陣鎖定（一階一層、二階兩層、三階三層）
		for (int i = 0; i < tier; i++) {
			if (cue(SNAPS[i])) {
				snap(i);
			}
		}

		// ---------------------------------------------------------- 鏡3：吸氣
		if (cue(S3)) {
			if (near) {
				PostState.darken(0.12f + tier * 0.08f);
			}

			if (mine) {
				PostState.lines(0.5f + tier * 0.15f, true, (int)((IMPACT - S3) * 1000) + 30, 0.34f);
			}

			Sfx.play(mine, "whoosh", c, 1f, 0.5f);
		}

		if (t > S3 - 0.1 && t < IMPACT) {
			int n = rate(80 + 70 * tier, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = item.add(Particles.randomDir(1.6 + Particles.RNG.nextDouble() * 1.4));
				Particles.spawn(Kind.SPARK, from, item.subtract(from).scale(3), 0.3).size(0.035f, 0.01f).color(0xFFFFFF, main).physics(0, 0).attract(item, 30f);
			}
		}

		// ---------------------------------------------------------- 衝擊
		if (cue(IMPACT - 0.1)) {
			Sfx.play(mine, "enchant_beam", c, 1.3f, tier == 1 ? 1.15f : tier == 2 ? 1f : 0.85f);
		}

		if (cue(IMPACT)) {
			impact(item);
		}

		// 三階：天柱一落下就「時間停止」——漫畫分格 + 四角大字
		if (tier == 3 && cue(IMPACT + 0.07) && mine) {
			FxClock.hitstop(MANGA_MS);

			if (Settings.titlesOn()) {
				PostState.manga(MANGA_MS);
				PostState.lines(1f, true, MANGA_MS, 0.3f);
				Hud.mangaName("enchant3", MANGA_MS / 1000.0);
			}
		}

		if (cue(IMPACT + 0.08)) {
			FxClock.slowmo(tier == 1 ? 0.5f : 0.3f, tier == 1 ? 700 : 1300);

			if (mine) {
				PostState.lines(0.5f + tier * 0.12f, false, 1100, 0.42f);
				PostState.streak(0.5f + tier * 0.35f, 900);
				PostState.zoom(0.4f + tier * 0.25f, 500);
			}
		}

		if (cue(IMPACT + 0.3) && mine && Settings.titlesOn()) {
			Hud.brushName("enchant" + tier, 3.3);
		}

		// 爆發後的星光
		if (t > IMPACT && t < S5 + 0.6) {
			int n = rate(12 + 10 * tier, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = item.add(Particles.randomDir(0.4 + Particles.RNG.nextDouble() * 1.6));
				Particles.spawn(Kind.FLARE, at, new Vec3(0, 0.15, 0), 0.7).size(0.28f, 0f).color(0xFFFFFF, pale).spin(2f);
			}
		}

		if (cue(IMPACT + 0.4)) {
			Sfx.play(mine, "sparkle", c, 0.8f, 1f);
		}

		// 三階：符文雨
		if (tier == 3 && t > IMPACT && t < S5 + 0.8) {
			int n = rate(70, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = Math.sqrt(Particles.RNG.nextDouble()) * 6;
				Vec3 at = new Vec3(c.x + Math.cos(a) * r, pos.getY() + 6 + Particles.RNG.nextDouble() * 3, c.z + Math.sin(a) * r);
				Particles.spawn(Kind.GLYPH, at, new Vec3(0, -4.5, 0), 1.6).size(0.17f, 0.12f).color(main, accent).physics(0, 0).fadeIn(0.1f);
			}
		}

		// ---------------------------------------------------------- 鏡6：聖光降臨
		if (cue(S6 - 0.25)) {
			if (near) {
				PostState.look("holy", 1300, (int)((DURATION - S6 + 0.6) * 1000));
				PostState.darken(0);
			}

			Sfx.play(mine, "holy_descend", c, 1f, tier == 1 ? 1.06f : tier == 2 ? 1f : 0.94f);
		}

		if (t > S6 - 0.3) {
			float holy = span(t, S6 - 0.3, S6 + 0.5);
			// 光塵從上方緩緩飄落
			int n = rate(26 * holy, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = Math.sqrt(Particles.RNG.nextDouble()) * 1.6;
				Vec3 at = new Vec3(c.x + Math.cos(a) * r, item.y + 1.2 + Particles.RNG.nextDouble() * 2.5, c.z + Math.sin(a) * r);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.35, 0), 2.2).size(0.1f, 0.02f).color(HOLY_WHITE, HOLY).physics(0, 0).fadeIn(0.4f).spin(1f);
			}

			// 像羽毛一樣緩慢旋落的金色光片
			int m = rate(5 * holy, dt);

			for (int i = 0; i < m; i++) {
				Vec3 at = item.add(Particles.randomDir(1.2)).add(0, 1.5, 0);
				Particles.spawn(Kind.SHARD, at, new Vec3(0, -0.25, 0), 2.4).size(0.12f, 0.06f).color(HOLY_WHITE, HOLY).physics(0, 0).spin(1.5f).fadeIn(0.3f);
			}

			// 物品本身往外散發的細光
			int k = rate(14 * holy, dt);

			for (int i = 0; i < k; i++) {
				Particles.spawn(Kind.SPARK, item, Particles.randomDir(0.6 + Particles.RNG.nextDouble() * 0.5), 0.9).size(0.03f, 0.005f).color(HOLY_WHITE, HOLY)
					.physics(0, 0.6f);
			}
		}

		// 結尾：柔和的白光淡出，直接回到世界
		if (cue(DURATION - 0.55) && mine) {
			Hud.whiteout(HOLY_WHITE, 0.5, 0.9);
		}
	}

	private void snap(int i) {
		Vec3 at = new Vec3(c.x, pos.getY() + HEIGHTS[i], c.z);
		Sfx.play(mine, "combo_ping", c, 0.8f, 0.6f + i * 0.15f);
		Sfx.play(mine, "name_hit", c, 0.55f, 1.25f + i * 0.08f);
		shake(0.18f + i * 0.06f);

		if (mine) {
			PostState.zoom(0.25f + i * 0.08f, 140);
			PostState.split(0.5f + i * 0.35f, (float)(Math.PI * (0.15 + i * 0.6)), 180);
		}

		int col = i == 1 ? accent : main;

		for (int k = 0; k < 4; k++) {
			double a = Particles.RNG.nextDouble() * Math.PI * 2;
			Vec3 rim = at.add(Math.cos(a) * RADII[i], 0, Math.sin(a) * RADII[i]);
			Particles.spawn(Kind.HIT, rim, Vec3.ZERO, 0.28).size(0.55f + i * 0.1f, 0.6f).color(0xFFFFFF, pale);
		}

		for (int k = 0; k < 24; k++) {
			double a = Particles.RNG.nextDouble() * Math.PI * 2;
			Vec3 rim = at.add(Math.cos(a) * RADII[i], 0, Math.sin(a) * RADII[i]);
			Particles.spawn(Kind.SPARK, rim, new Vec3(Math.cos(a), 0.2, Math.sin(a)).scale(Particles.rand(2f, 4f)), 0.35).size(0.04f, 0.01f)
				.color(0xFFFFFF, col).physics(2f, 3f);
		}
	}

	private void impact(Vec3 item) {
		FxClock.hitstop(tier == 1 ? 80 : 150);
		shake(0.45f + tier * 0.2f);

		if (near) {
			PostState.darken(0);
			PostState.flash(pale, 0.55f + tier * 0.12f, 170, mine ? 0.2f + tier * 0.1f : 0.15f);
		}

		if (mine) {
			if (tier >= 2) {
				PostState.impact(PostState.VIOLET, 150);
			}

			PostState.shock(0.4f + tier * 0.35f, 650);
			PostState.split(0.8f + tier * 0.6f, 0.4f, 450);
			CameraShake.fovPunch(0.06f + tier * 0.04f);
		}

		Sfx.play(mine, "shockwave", c, 1.2f, tier == 1 ? 1.1f : 0.85f);
		Particles.spawn(Kind.HIT, item, Vec3.ZERO, 0.45).size(1.2f + tier * 0.5f, 2.6f).color(0xFFFFFF, pale);

		for (int i = 0; i < 2 + tier * 2; i++) {
			Particles.spawn(Kind.HIT, item.add(Particles.randomDir(1.2)), Vec3.ZERO, 0.35 + Particles.RNG.nextDouble() * 0.2).size(Particles.rand(0.8f, 1.6f), 1.8f)
				.color(0xFFFFFF, main);
		}

		for (int i = 0; i < 30 * tier * (0.5 + level); i++) {
			Particles.spawn(Kind.SHARD, item, Particles.randomDir(4 + Particles.RNG.nextDouble() * 6), 0.9 + Particles.RNG.nextDouble() * 0.6)
				.size(0.24f, 0.05f).color(0xFFFFFF, main).physics(3f, 2.2f).spin(9f);
		}

		for (int i = 0; i < 50 * tier; i++) {
			Particles.spawn(Kind.SPARK, item, Particles.randomDir(6 + Particles.RNG.nextDouble() * 8), 0.5 + Particles.RNG.nextDouble() * 0.4)
				.size(0.06f, 0.015f).color(0xFFFFFF, accent).physics(5f, 1.2f);
		}
	}

	// ------------------------------------------------------------------ 光源（後製）

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 item = itemPos(t);
		float charge = span(t, 0.2, IMPACT);
		float after = t > IMPACT ? 1 - span(t, IMPACT, S5 + 0.8) : 0;
		float holy = span(t, S6 - 0.3, S6 + 0.6) * (1 - span(t, DURATION - 0.4, DURATION));
		float life = 1 - span(t, S6 - 0.5, S6 + 0.2);
		lights.add(new CinemaPost.Light(item, 3f + charge * 2 + after * 4, r(main), g(main), b(main), (0.35f + charge * 0.5f + after * 1.0f) * life, true));
		lights.add(new CinemaPost.Light(floor.add(0, 0.4, 0), 3.4f, r(main), g(main), b(main), 0.7f * span(t, 0, 0.5) * life, false));

		if (holy > 0.01f) {
			lights.add(new CinemaPost.Light(item, 2.2f, 1f, 0.9f, 0.7f, 0.25f * holy, true));
			lights.add(new CinemaPost.Light(floor.add(0, 0.5, 0), 2.6f, 1f, 0.92f, 0.75f, 0.3f * holy, false));
		}

		float beam = tier == 1 || t < IMPACT ? 0 : 1 - span(t, IMPACT + 0.1, IMPACT + 1.3);

		if (beam > 0.01f) {
			segs.add(new CinemaPost.Seg(item, item.add(0, 30, 0), 8, r(main), g(main), b(main), beam * (0.6f + tier * 0.25f), 0.35f));
		}

		float burst = t < IMPACT ? 0 : 1 - span(t, IMPACT + 0.1, IMPACT + 1.3);

		if (holy > 0.01f) {
			rays[0] = item;
			raysP[0] = 1f;
			raysP[1] = 0.88f;
			raysP[2] = 0.65f;
			raysP[3] = holy * 0.45f;
		}
		else if (burst > 0.01f) {
			rays[0] = item;
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst;
		}
	}

	// ------------------------------------------------------------------ 繪製

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		double tt = Math.min(t, S6);
		float spin = (float)(tt * 1.5 + Math.pow(Math.min(tt, IMPACT), 3) * 1.1);

		// 降臨時轉速慢慢收住
		if (t > S6) {
			double k = t - S6;
			double len = DURATION - S6;
			spin += (float)(1.5 * (k - k * k / (2 * len)));
		}

		d.item(p.item(), itemPos(t), 0.95f + span(t, 0.1, 0.9) * 0.4f, spin, 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, S6 - 0.2, S6 + 0.5);
		Vec3 item = itemPos(t);

		// ---- 地面法陣
		float open = easeOutBack(span(t, 0.0, 0.5));
		float kick = t > IMPACT ? (1 - span(t, IMPACT, IMPACT + 0.5)) * 0.25f : 0;

		if (life > 0.01f) {
			d.flat(Mode.GLOW, CIRCLE, floor, (2.4f + tier * 0.4f + kick) * open, (float)(t * 0.35), r(main), g(main), b(main), 0.9f * life);
			d.annulus(Mode.GLOW, BAND, floor.add(0, 0.015, 0), 3.3f * open, 3.65f * open, 64, 6, (float)(t * 0.06), r(accent), g(accent), b(accent), 0.75f * life,
				0.75f * life);
		}

		// ---- 法陣層：鎖定前不存在，鎖上時從 1.5 倍「喀」地縮到位
		for (int i = 0; i < tier; i++) {
			if (t < SNAPS[i]) {
				continue;
			}

			float k = span(t, SNAPS[i], SNAPS[i] + 0.12);
			float sc = 1.5f - 0.5f * easeOut(k);
			float charge = span(t, SNAPS[i], IMPACT);
			float burst = t > IMPACT ? 1 + easeOut(span(t, IMPACT, IMPACT + 0.4)) * (0.6f + i * 0.3f) : 1;
			float fadeOut = t > IMPACT ? 1 - span(t, IMPACT + 0.2, IMPACT + 0.8) * 0.6f : 1;
			int col = i == 1 ? accent : main;
			float spin = (float)((t - SNAPS[i]) * (i == 1 ? 1.6 : -1.1 - i * 0.6)) * (1 + charge * 1.5f);
			Vec3 at = new Vec3(c.x, pos.getY() + HEIGHTS[i], c.z);
			d.flat(Mode.GLOW, CIRCLE, at, RADII[i] * sc * burst, spin, r(col), g(col), b(col), (0.75f + charge * 0.25f) * life * fadeOut);

			if (k < 1) {
				d.flat(Mode.GLOW, RING, at, RADII[i] * (1.1f + k * 0.5f), 0, 1f, 1f, 1f, (1 - k));
			}
		}

		// ---- 雷光（一拍二）
		if (t > SNAPS[0] && t < IMPACT + 0.5) {
			long beat = (long)Math.floor(t * 12);
			Random rng = new Random(beat * 31 + pos.asLong());
			int count = t < IMPACT ? tier + (int)(span(t, SNAPS[0], IMPACT) * (2 + tier)) : 2 + tier * 2;
			float a = t < IMPACT ? 0.9f : 1 - span(t, IMPACT, IMPACT + 0.5);

			for (int k = 0; k < count; k++) {
				int ring = rng.nextInt(tier);

				if (t < SNAPS[ring]) {
					ring = 0;
				}

				double ang = rng.nextDouble() * Math.PI * 2;
				Vec3 from = new Vec3(c.x + Math.cos(ang) * RADII[ring], pos.getY() + HEIGHTS[ring], c.z + Math.sin(ang) * RADII[ring]);
				bolt(d, from, item, rng, 0.05f + tier * 0.012f, a, k % 2 == 0 ? main : accent);
			}
		}

		// ---- 物品核心光
		float charge = span(t, 0.4, IMPACT);
		d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 0.45f + charge * 0.55f, (float)t * 0.5f, 1f, 1f, 1f, (0.3f + charge * 0.4f) * life * (1 - span(t, S6 - 0.5, S6)));

		if (tier == 1) {
			// 一階：沒有光柱，改成一朵綻放的星光
			float bloom = t < IMPACT ? 0 : 1 - span(t, IMPACT, IMPACT + 1.1);

			if (bloom > 0.01f) {
				float g0 = easeOut(span(t, IMPACT, IMPACT + 0.25));
				d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 1.2f + g0 * 2.2f, (float)t * 0.3f, r(pale), g(pale), b(pale), bloom);
				d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 0.8f + g0 * 1.4f, (float)-t * 0.5f + 0.7f, r(main), g(main), b(main), bloom * 0.8f);
			}
		}
		else {
			// ---- 天柱：白熱細芯 + 色殼（三階再加一層外殼）
			float beamLife = t < IMPACT ? 0 : 1 - span(t, IMPACT + 0.15, IMPACT + 1.3);

			if (beamLife > 0.01f) {
				float open2 = easeOut(span(t, IMPACT, IMPACT + 0.06));
				float width = (tier == 2 ? 0.75f : 1.1f) * open2 * (0.6f + 0.4f * beamLife);
				Vec3 base = new Vec3(c.x, pos.getY() + 0.75, c.z);
				float scroll = (float)(t * 3);
				d.cylinder(Mode.GLOW, BEAM, base, width, 40f, 32, 1, 0, 6, scroll, 1.25f, r(main), g(main), b(main), beamLife * 0.75f, beamLife * 0.2f);
				d.cylinder(Mode.GLOW, BEAM, base, width * 0.35f, 40f, 16, 1, 0.5f, 5, scroll * 1.7f, 1.1f, 1f, 1f, 1f, beamLife, beamLife * 0.5f);

				if (tier == 3) {
					d.cylinder(Mode.GLOW, BEAM, base, width * 1.6f, 40f, 32, 1, 0.3f, 4, -scroll * 0.6f, 1.2f, r(accent), g(accent), b(accent), beamLife * 0.4f,
						beamLife * 0.1f);
				}

				Random rng = new Random((long)Math.floor(t * 12) * 17);

				for (int k = 0; k < tier; k++) {
					double a0 = rng.nextDouble() * Math.PI * 2;
					Vec3 lo = base.add(Math.cos(a0) * width * 1.2, rng.nextDouble() * 2, Math.sin(a0) * width * 1.2);
					Vec3 hi = base.add(Math.cos(a0 + 2) * width * 1.4, 4 + rng.nextDouble() * 4, Math.sin(a0 + 2) * width * 1.4);
					bolt(d, lo, hi, rng, 0.09f, beamLife, k % 2 == 0 ? main : accent);
				}
			}
		}

		// ---- 衝擊波
		if (t > IMPACT && t < IMPACT + 1) {
			for (int k = 0; k < Math.min(2, tier); k++) {
				float w = span(t, IMPACT + k * 0.08, IMPACT + 0.7 + k * 0.1);
				float a = (1 - w) * (1 - w);
				d.flat(Mode.GLOW, JAG, floor.add(0, 0.06 + k * 0.01, 0), 0.6f + easeOut(w) * (4f + tier * 1.5f + k * 2.5f), k * 1.3f, k == 0 ? 1f : r(main),
					k == 0 ? 1f : g(main), k == 0 ? 1f : b(main), a);
			}

			float w = span(t, IMPACT, IMPACT + 0.5);
			d.disc(Mode.GLOW, JAG, item, Draw.X, Draw.Y, 0.3f + easeOut(w) * (1.6f + tier * 0.9f), 0.4f, 1f, 1f, 1f, (1 - w) * 0.9f);
		}

		// ---- 三階：星空穹頂
		if (tier == 3) {
			float sky = Math.min(span(t, IMPACT, IMPACT + 0.5), 1 - span(t, S5 + 0.4, S6 + 0.3)) * 0.9f;

			if (sky > 0.01f) {
				d.cylinder(Mode.GLOW, STARS, new Vec3(c.x, pos.getY() - 1, c.z), 11f, 10f, 48, 2, (float)(t * 0.01), 1, 0, 0.7f,
					r(pale), g(pale), b(pale), sky * 0.35f, sky);
				d.flat(Mode.GLOW, STARS, new Vec3(c.x, pos.getY() + 9, c.z), 7.8f, (float)(t * 0.05), r(pale), g(pale), b(pale), sky * 0.8f);
			}
		}

		// ---- 結晶環繞（二階以上）
		if (tier >= 2 && t > IMPACT) {
			FxMesh crystal = FxMesh.get("crystal");
			float k = span(t, IMPACT, IMPACT + 0.35);
			float a = (1 - span(t, S5 + 0.3, S6)) * k;

			if (a > 0.01f) {
				int count = tier == 2 ? 6 : 8;

				for (int i = 0; i < count; i++) {
					double ang = t * 1.2 + i * Math.PI * 2 / count;
					double rr = 1.0 + (1 - easeOut(k)) * 1.5;
					Matrix4f m = new Matrix4f().translate((float)(Math.cos(ang) * rr), (float)(Math.sin(t * 3 + i) * 0.2), (float)(Math.sin(ang) * rr))
						.rotateY((float)-ang).rotateZ(0.35f).scale(0.55f);
					d.mesh(Mode.GLOW, crystal, item, m, r(pale), g(pale), b(pale), a, false);
				}
			}
		}

		// ---- 聖光降臨
		float holy = span(t, S6 - 0.3, S6 + 0.6) * (1 - span(t, DURATION - 0.3, DURATION));

		if (holy > 0.01f) {
			float breathe = 0.85f + 0.15f * (float)Math.sin(t * 2.6);
			Vec3 top = new Vec3(c.x, pos.getY() + 0.9, c.z);
			float scroll = (float)(t * 0.4);
			// 從天而降的光柱：柔和的寬光 + 較亮的芯
			d.cylinder(Mode.GLOW, BEAM, top, 1.5f, 16f, 32, 1, 0, 2, -scroll, 1.0f, r(HOLY), g(HOLY), b(HOLY), 0.05f * holy, 0.09f * holy);
			d.cylinder(Mode.GLOW, BEAM, top, 0.55f, 16f, 24, 1, 0.3f, 3, -scroll * 1.6f, 1.0f, r(HOLY_WHITE), g(HOLY_WHITE), b(HOLY_WHITE), 0.14f * holy,
				0.18f * holy);
			// 物品背後的光暈與往外散發的光芒
			d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.3f * breathe, 0, r(HOLY), g(HOLY), b(HOLY), 0.12f * holy);
			d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 1.4f * breathe, (float)t * 0.15f, r(HOLY_WHITE), g(HOLY_WHITE), b(HOLY_WHITE), 0.22f * holy);
			d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 1.0f, (float)-t * 0.22f + 0.4f, r(HOLY), g(HOLY), b(HOLY), 0.18f * holy);
			// 頭頂的光環（水平），隨物品一起降下
			d.flat(Mode.GLOW, RING, item.add(0, 0.55, 0), 0.38f * breathe, (float)t * 0.5f, r(HOLY), g(HOLY), b(HOLY), 0.8f * holy);
			// 下方桌面上一圈溫暖的光，物品越接近越亮
			float land = span(t, S6 + 0.8, DURATION - 0.3);
			d.flat(Mode.GLOW, GLOW, new Vec3(c.x, pos.getY() + 0.78, c.z), 1.1f + land * 0.5f, 0, r(HOLY), g(HOLY), b(HOLY), (0.12f + land * 0.25f) * holy);
			d.flat(Mode.GLOW, CIRCLE, new Vec3(c.x, pos.getY() + 0.77, c.z), 0.9f + land * 0.3f, (float)t * 0.12f, r(HOLY), g(HOLY), b(HOLY), 0.35f * land * holy);
		}
	}

	/** 鋸齒雷光：中點位移 3 層，每段一條硬邊亮線 */
	private static void bolt(Draw d, Vec3 a, Vec3 b, Random rng, float width, float alpha, int col) {
		List<Vec3> pts = new ArrayList<>();
		pts.add(a);
		pts.add(b);
		double amp = a.distanceTo(b) * 0.22;

		for (int level = 0; level < 3; level++) {
			List<Vec3> next = new ArrayList<>();

			for (int i = 0; i < pts.size() - 1; i++) {
				Vec3 p0 = pts.get(i);
				Vec3 p1 = pts.get(i + 1);
				Vec3 m = p0.add(p1).scale(0.5).add((rng.nextDouble() - 0.5) * amp, (rng.nextDouble() - 0.5) * amp, (rng.nextDouble() - 0.5) * amp);
				next.add(p0);
				next.add(m);
			}

			next.add(pts.getLast());
			pts = next;
			amp *= 0.5;
		}

		for (int i = 0; i < pts.size() - 1; i++) {
			Vec3 p0 = pts.get(i);
			Vec3 p1 = pts.get(i + 1);
			d.streak(Mode.GLOW, BOLT, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, width * 2.4f, r(col), g(col), b(col), alpha * 0.7f);
			d.streak(Mode.GLOW, BOLT, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, width, 1f, 1f, 1f, alpha);
		}
	}
}
