package com.chuuni.client.fx.effects;

import java.util.List;

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
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 萬象解放（工作台）——工作台一天會用很多次，所以是 3.6 秒的精簡分鏡，演出完回到合成介面（方便繼續做）：
 * <pre>
 * 鏡1 0.00 藍圖  俯視：藍圖陣在桌面展開、線稿一條條畫出，九塊「成品碎片」浮起
 * 鏡2 0.80 構築  環繞近拍：碎片螺旋收攏，碎片之間拉出光線，越轉越快
 * 鏡3 1.60 吸氣  中心極近特寫、荷蘭角：黑色集中線
 * 衝擊 1.95     萬象歸一：衝擊幀 + 頓幀 + 折射衝擊波 + RGB 分離，毛筆直書招式名（shift 量產 32 件以上：漫畫分格時停 + 四角大字）
 * 鏡4 1.95 英雄  低角度仰望：成品在旋轉的光芒中自轉
 * </pre>
 * 背包 2×2 合成是「掌中乾坤」：沒有鏡頭，在玩家面前組裝的小演出（大量合成時才出招式名）。
 */
public class CraftFx extends Effect {
	private static final Identifier BLUEPRINT = FxRender.tex("circle_blueprint");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");
	private static final Identifier SPARK = FxRender.tex("spark");

	private static final int CYAN = 0x4AC8FF;
	private static final int WHITE = 0xE8FBFF;
	private static final int GOLD = 0xFFD070;

	private static final double S2 = 0.8;
	private static final double S3 = 1.6;
	private static final int MANGA_MS = 900;

	private final boolean table;
	private final boolean big;
	private final double impact;
	private final double duration;
	private final Vec3 center;
	private final float scale;

	public CraftFx(FxPayload p) {
		super(p);
		table = p.extra() == 1;
		big = Settings.intensity >= 2 || p.power() >= 32;
		impact = table ? 1.95 : 0.6;
		duration = table ? 3.8 : 1.5;
		scale = table ? 1f : 0.55f;

		if (table) {
			center = new Vec3(c.x, pos.getY() + 1.6, c.z);
		}
		else {
			// 玩家面前、胸口高度
			Player player = Minecraft.getInstance().level == null ? null : (Player)Minecraft.getInstance().level.getEntity(p.actor());

			if (player != null) {
				Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
				center = player.position().add(look.scale(0.9)).add(0, 1.1, 0);
			}
			else {
				center = c.add(0, 0.6, 0);
			}
		}

		if (table && near()) {
			PostState.look("craft", 300, (int)(duration * 1000) - 600);
		}

		if (mine && table) {
			Frame f = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
			Vec3 ctr = f.at(0, 1.6, 0);
			Cinematic.play(List.of(
				Shot.of(S2, f.at(0.35, 3.6, 1.0), f.at(0.2, 3.2, 0.8), f.at(0, 1.0, 0)).look(f.at(0, 1.0, 0), f.at(0, 1.3, 0)).roll(0, 12),
				Shot.of(S3 - S2, f.at(-1.5, 1.75, 1.2), f.at(-0.7, 1.85, 1.65), ctr).roll(-3, -5),
				Shot.of(impact - S3, f.at(0.1, 1.68, 0.8), f.at(0.05, 1.64, 0.58), ctr).roll(-9, -13).fov(0.7f, 0.6f).accelerate(),
				Shot.of(duration - impact - 0.2, f.at(1.25, 0.55, 2.0), f.at(1.55, 0.62, 2.5), ctr.add(0, 0.3, 0)).look(ctr.add(0, 0.3, 0), ctr)
					.roll(11, 8).fov(1.12f, 1.06f)
			));
		}
	}

	@Override
	public double duration() {
		return duration;
	}

	private Vec3 fragment(int i, double t) {
		double start = table ? 0.25 : 0.0;
		double k = easeIn(span(t, start + i * 0.03, impact));
		double float0 = table ? easeOut(span(t, 0.15 + i * 0.04, 0.7 + i * 0.04)) : 1;
		double a0 = i * Math.PI * 2 / 9 + 0.3;
		double ang = a0 + k * Math.PI * 2.5 + t * 0.6;
		double r = (1 - k) * 1.7 * scale;
		double y = (1 - k) * (((i * 7) % 5) / 4.0 - 0.5) * 1.0 * scale - (1 - float0) * 0.55;
		return center.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
	}

	@Override
	protected void update(double t, double dt) {
		if (table) {
			if (cue(0.0)) {
				Sfx.play(mine, "craft_blueprint", center, 1f, 1f);
			}

			if (cue(0.55)) {
				Sfx.play(mine, "craft_assemble", center, 0.9f, 1f);
			}

			if (cue(S3)) {
				Sfx.play(mine, "whoosh", center, 0.8f, 0.55f);

				if (near()) {
					PostState.darken(0.3f);
				}

				if (mine) {
					PostState.lines(0.75f, true, (int)((impact - S3) * 1000) + 30, 0.32f);
				}
			}

			if (cue(impact - 0.1)) {
				Sfx.play(mine, "craft_forge", center, 1.2f, big ? 0.9f : 1f);
			}
		}
		else if (cue(0.0)) {
			Sfx.play(mine, "craft_assemble", center, 0.7f, 1.25f);
		}

		if (t < impact) {
			for (int i = 0; i < 9; i++) {
				if (Particles.RNG.nextDouble() < dt * 40) {
					Particles.spawn(Kind.GLOW, fragment(i, t), Vec3.ZERO, 0.35).size(0.08f * scale, 0.0f).color(WHITE, CYAN);
				}
			}
		}

		if (table && t > S3 && t < impact) {
			int n = rate(120, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = center.add(Particles.randomDir(1.4 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.SPARK, from, center.subtract(from).scale(3), 0.3).size(0.03f, 0.01f).color(0xFFFFFF, CYAN).physics(0, 0).attract(center, 30f);
			}
		}

		if (cue(impact)) {
			converge();
		}

		if (table && cue(impact + 0.07) && big && mine && Settings.titlesOn()) {
			FxClock.hitstop(MANGA_MS);
			PostState.manga(MANGA_MS);
			PostState.lines(1f, true, MANGA_MS, 0.3f);
			Hud.mangaName("craft", MANGA_MS / 1000.0);
		}

		if (table && cue(impact + 0.08) && mine) {
			FxClock.slowmo(0.4f, 700);
			PostState.lines(0.7f, false, 900, 0.42f);
			PostState.streak(0.8f, 700);
			PostState.zoom(0.6f, 400);
		}

		if (table && cue(impact + 0.25) && mine && Settings.titlesOn()) {
			Hud.brushName("craft", duration - impact - 0.45);
		}

		if (t > impact && t < duration - 0.4) {
			int n = rate(table ? 14 : 5, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = center.add(Particles.randomDir((0.4 + Particles.RNG.nextDouble() * 1.1) * scale));
				Particles.spawn(Kind.FLARE, at, new Vec3(0, 0.15, 0), 0.6).size(0.22f * scale, 0f).color(0xFFFFFF, CYAN).spin(2f);
			}
		}
	}

	private void converge() {
		if (table) {
			FxClock.hitstop(130);
			shake(0.55f);

			if (near()) {
				PostState.darken(0);
				PostState.flash(WHITE, 0.4f, 140, mine ? 0.1f : 0.05f);
			}

			if (mine) {
				PostState.impact(PostState.INK, 130);
				PostState.shock(0.9f, 600);
				PostState.split(1.4f, 0.5f, 400);
				CameraShake.fovPunch(0.1f);
			}

			Sfx.play(mine, "shockwave", center, 0.9f, 1f);
		}
		else {
			Sfx.play(mine, "craft_forge", center, 0.55f, 1.3f);
			FxClock.hitstop(40);
			shake(0.1f);

			if (mine && Settings.titlesOn() && (Settings.intensity >= 2 || p.power() >= 8)) {
				Hud.brushName("craft_hand", 1.8);
			}
		}

		if (mine && p.power() > 1) {
			Hud.combo(p.power(), 0x9AE8FF, "COMBO!");
			Sfx.ui("combo_ping", 0.8f, 0.9f + Math.min(p.power(), 32) * 0.012f);
		}

		Particles.spawn(Kind.HIT, center, Vec3.ZERO, 0.35).size((table ? 1.2f : 0.3f), table ? 2f : 1.2f).color(0xFFFFFF, CYAN);

		int n = (int)((table ? 120 : 30) * Math.min(3, 1 + p.power() * 0.05));

		for (int i = 0; i < n; i++) {
			Particles.spawn(Kind.SPARK, center, Particles.randomDir(Particles.rand(2f, 7f) * scale), Particles.rand(0.3f, 0.7f))
				.size(0.05f * scale, 0.01f).color(0xFFFFFF, CYAN).physics(2f, 2.5f);
		}

		for (int i = 0; i < (table ? 30 : 8); i++) {
			Particles.spawn(Kind.SHARD, center, Particles.randomDir(Particles.rand(2f, 6f) * scale), Particles.rand(0.6f, 1.1f))
				.size(0.16f * scale, 0.04f).color(0xFFFFFF, CYAN).physics(3f, 2f).spin(9f);
		}

		// 量產：多出的成品化成金色光點四散
		for (int i = 1; i < Math.min(p.power(), 48); i++) {
			Vec3 v = Particles.randomDir(3 * scale).add(0, 2 * scale, 0);
			Particles.spawn(Kind.FLARE, center, v, 0.9).size(0.25f * scale, 0.05f).color(0xFFFFFF, GOLD).physics(4f, 1.5f);
		}
	}

	// ------------------------------------------------------------------ 光源（後製）

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		if (!table) {
			return;
		}

		double t = time();
		float life = 1 - span(t, duration - 0.6, duration);
		float pull = span(t, 0.3, impact);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.0) : 0;
		lights.add(new CinemaPost.Light(center, 2.5f + burst * 3, r(CYAN), g(CYAN), b(CYAN), (0.3f + pull * 0.4f + burst * 0.5f) * life, true));
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 1.1, c.z), 3f, r(CYAN), g(CYAN), b(CYAN), 0.5f * span(t, 0, 0.4) * life, false));

		if (burst > 0.01f) {
			rays[0] = center;
			raysP[0] = r(WHITE);
			raysP[1] = g(WHITE);
			raysP[2] = b(WHITE);
			raysP[3] = burst * 0.5f;
		}
	}

	// ------------------------------------------------------------------ 繪製

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		if (t < impact) {
			for (int i = 0; i < 9; i++) {
				float s = 0.32f * scale * (0.6f + 0.4f * span(t, 0, 0.25));
				d.item(p.item(), fragment(i, t), s, (float)(t * 8 + i), (float)(t * 5 + i * 0.7));
			}
		}
		else {
			float pop = easeOutBack(span(t, impact, impact + 0.25));
			float out = span(t, duration - 0.35, duration);
			d.item(p.item(), center.add(0, Math.sin(t * 3) * 0.05, 0), (1.15f * pop * (1 - out)) * scale, (float)(t * 2.5), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.15, duration - 0.45, duration);
		Vec3 base = table ? new Vec3(c.x, pos.getY() + 1.02, c.z) : new Vec3(center.x, center.y - 1.05, center.z);

		// 藍圖陣：一圈一圈「畫」出來（半徑逐步展開 + 旋轉）
		float open = easeOutBack(span(t, 0, 0.4));
		d.flat(Mode.GLOW, BLUEPRINT, base, 1.35f * open * scale, (float)(t * 0.35), r(CYAN), g(CYAN), b(CYAN), 0.9f * life);

		if (table) {
			d.flat(Mode.GLOW, BLUEPRINT, base.add(0, 0.01, 0), 2.3f * easeOut(span(t, 0.2, 0.7)), (float)(-t * 0.2), r(WHITE), g(WHITE), b(WHITE), 0.35f * life);
			d.flat(Mode.GLOW, BLUEPRINT, center.add(0, 0.7, 0), 0.65f * easeOutBack(span(t, 0.4, 0.8)), (float)(-t * 0.9), r(WHITE), g(WHITE), b(WHITE), 0.5f * life);

			// 桌面四角往上的藍圖線框
			float frameA = span(t, 0.1, 0.5) * (1 - span(t, impact, impact + 0.4));

			if (frameA > 0.01f) {
				for (int k = 0; k < 4; k++) {
					double a = k * Math.PI / 2 + Math.PI / 4 + t * 0.4;
					Vec3 lo = base.add(Math.cos(a) * 0.7, 0, Math.sin(a) * 0.7);
					Vec3 hi = center.add(Math.cos(a) * 0.35, 0.7, Math.sin(a) * 0.35);
					float grow = easeOut(span(t, 0.1 + k * 0.08, 0.5 + k * 0.08));
					Vec3 e = lo.add(hi.subtract(lo).scale(grow));
					d.streak(Mode.GLOW, SPARK, lo.x, lo.y, lo.z, e.x, e.y, e.z, 0.03f, r(CYAN), g(CYAN), b(CYAN), 0.7f * frameA);
				}
			}
		}

		if (t < impact) {
			// 碎片之間的連線
			float k = span(t, 0.2, 0.5);

			for (int i = 0; i < 9; i++) {
				Vec3 f = fragment(i, t);
				Vec3 n = fragment((i + 1) % 9, t);
				d.streak(Mode.GLOW, SPARK, f.x, f.y, f.z, center.x, center.y, center.z, 0.022f * scale, r(CYAN), g(CYAN), b(CYAN), 0.45f * k);
				d.streak(Mode.GLOW, SPARK, f.x, f.y, f.z, n.x, n.y, n.z, 0.018f * scale, r(WHITE), g(WHITE), b(WHITE), 0.35f * k);
			}

			float pull = span(t, 0.3, impact);
			d.billboard(Mode.GLOW, GLOW, center.x, center.y, center.z, (0.2f + pull * 0.6f) * scale, 0, r(CYAN), g(CYAN), b(CYAN), pull * 0.7f);
		}
		else {
			float k = span(t, impact, impact + 0.6);
			float a = (1 - k) * (1 - k);
			d.flat(Mode.GLOW, JAG, base.add(0, 0.02, 0), (0.3f + easeOut(k) * (table ? 5f : 2.2f)) * scale, 0, 1f, 1f, 1f, a);
			d.disc(Mode.GLOW, RING, center, Draw.X, Draw.Y, (0.2f + easeOut(k) * 2.2f) * scale, 0, r(CYAN), g(CYAN), b(CYAN), a);

			// 成品背後的旋轉光芒
			float rays = Math.min(span(t, impact, impact + 0.15), 1 - span(t, duration - 0.5, duration)) * life;

			for (int i = 0; i < 12; i++) {
				float ang = (float)(t * 1.0 + i * Mth.TWO_PI / 12);
				double len = (1.0 + 0.35 * Math.sin(t * 5 + i)) * scale;
				Vec3 end = center.add(Math.cos(ang) * len, Math.sin(ang) * len * 0.8, Math.sin(ang) * len * 0.25);
				d.streak(Mode.GLOW, SPARK, center.x, center.y, center.z, end.x, end.y, end.z, 0.12f * scale, 1f, 0.95f, 0.8f, 0.4f * rays);
			}

			d.billboard(Mode.GLOW, GLOW, center.x, center.y, center.z, 1.0f * scale, 0, r(CYAN), g(CYAN), b(CYAN), 0.5f * rays);
			d.billboard(Mode.GLOW, FLARE, center.x, center.y, center.z, 1.3f * scale * (1 - k * 0.5f), (float)t * 0.4f, 1f, 1f, 1f, 0.3f * rays);
		}
	}
}
