package com.chuuni.client.fx.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic.Frame;
import com.chuuni.client.fx.Effect;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * A 級演出的共用骨架（和附魔台同一套打架動漫節奏）：
 * <pre>
 * ……鋪陳（各自的分鏡）
 * S3      吸氣：畫面壓暗、黑色集中線、光點被吸向焦點
 * IMPACT  衝擊：頓幀 + 衝擊幀 + 閃光殘影 + 折射衝擊波 + RGB 分離 + FOV 衝擊 →（manga：漫畫分格時停 + 四角大字）
 *         英雄：慢動作、白色集中線、橫向光芒、毛筆直書招式名
 * ……收尾（各自的分鏡）→ 白光淡出
 * </pre>
 * 子類別只要決定顏色、時間點、分鏡與自己的特效，呼叫 {@link #beats(double, double, Vec3)} 就有完整的節拍。
 */
public abstract class AnimeFx extends Effect {
	protected static final Identifier CIRCLE = FxRender.tex("circle_enchant");
	protected static final Identifier SEAL = FxRender.tex("circle_seal");
	protected static final Identifier COMPASS = FxRender.tex("circle_compass");
	protected static final Identifier FORGE = FxRender.tex("circle_forge");
	protected static final Identifier BAND = FxRender.tex("rune_band");
	protected static final Identifier BEAM = FxRender.tex("beam");
	protected static final Identifier BOLT = FxRender.tex("bolt");
	protected static final Identifier JAG = FxRender.tex("jag_ring");
	protected static final Identifier RING = FxRender.tex("ring");
	protected static final Identifier GLOW = FxRender.tex("glow");
	protected static final Identifier FLARE = FxRender.tex("flare");
	protected static final Identifier SPARK = FxRender.tex("spark");
	protected static final Identifier TRAIL = FxRender.tex("slash_trail");
	protected static final Identifier SMOKE = FxRender.tex("smoke");
	protected static final Identifier STARS = FxRender.tex("starfield");
	protected static final Identifier GRID = FxRender.tex("holo_grid");

	protected final Frame frame;
	protected final boolean near;

	/** 招式名 key（names.json），null = 不出招式名 */
	protected String name;
	protected int impactMode = PostState.INK;
	protected int main = 0xFFFFFF;
	protected int pale = 0xFFFFFF;
	/** 衝擊後是否「時間停止」漫畫分格 + 四角大字 */
	protected boolean manga;
	/** 結尾白光淡出的顏色（-1 = 不淡出） */
	protected int fadeColor = -1;

	protected double s3 = 2.2;
	protected double impact = 2.6;
	protected double end = 6.0;

	protected AnimeFx(FxPayload p) {
		super(p);
		frame = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
		near = near();
	}

	@Override
	public double duration() {
		return end;
	}

	/** 色調：衝擊後再撐一段時間才淡出 */
	protected void look(String key, double until) {
		if (near) {
			PostState.look(key, 350, (int)(until * 1000));
		}
	}

	/** 共用節拍：吸氣 → 衝擊 → 英雄 → 招式名 → 白光淡出 */
	protected void beats(double t, double dt, Vec3 focus) {
		if (cue(s3)) {
			if (near) {
				PostState.darken(0.35f);
			}

			if (mine) {
				PostState.lines(0.8f, true, (int)((impact - s3) * 1000) + 30, 0.32f);
			}

			Sfx.play(mine, "whoosh", c, 0.8f, 0.5f);
		}

		// 「超必殺技発動」的高峰在 0.63 秒：提早播，讓它正好撞上衝擊
		if (cue(Math.max(0, impact - 0.63))) {
			Sfx.anime(mine, "inhale", null, c, 1f, 1f);
		}

		if (t > s3 && t < impact) {
			int n = rate(110, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = focus.add(Particles.randomDir(1.5 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.SPARK, from, focus.subtract(from).scale(3), 0.3).size(0.03f, 0.008f).color(0xFFFFFF, main).physics(0, 0).attract(focus, 30f);
			}
		}

		if (cue(impact)) {
			FxClock.hitstop(150);
			shake(0.75f);

			if (near) {
				PostState.darken(0);
				PostState.flash(pale, 0.45f, 160, mine ? 0.12f : 0.05f);
			}

			if (mine) {
				if (impactMode >= 0) {
					PostState.impact(impactMode, 150);
				}

				PostState.shock(1.0f, 650);
				PostState.split(1.6f, 0.4f, 450);
				CameraShake.fovPunch(0.11f);
			}

			// 動漫素材「必殺技ヒット」當主角，原本的合成衝擊波退到底下補低頻
			boolean hit = Sfx.anime(mine, "hit", null, c, 1f, 1f);
			Sfx.play(mine, "shockwave", c, hit ? 0.4f : 1f, 0.85f);
			Particles.spawn(Kind.HIT, focus, Vec3.ZERO, 0.38).size(1.4f, 2f).color(0xFFFFFF, pale);

			for (int i = 0; i < 110; i++) {
				Particles.spawn(Kind.SPARK, focus, Particles.randomDir(Particles.rand(3f, 9f)), Particles.rand(0.4f, 0.9f)).size(0.05f, 0.012f).color(0xFFFFFF, main)
					.physics(4f, 1.2f);
			}
		}

		if (manga && cue(impact + 0.07) && mine && Settings.titlesOn() && name != null) {
			FxClock.hitstop(1200);
			PostState.manga(1200);
			PostState.lines(1f, true, 1200, 0.3f);
			Hud.mangaName(name, 1.2);
		}

		if (cue(impact + 0.08)) {
			FxClock.slowmo(0.35f, 1100);
			Sfx.anime(mine, "slowmo", null, c, 1f, 1f);

			if (mine) {
				PostState.lines(0.7f, false, 1000, 0.42f);
				PostState.streak(0.9f, 850);
				PostState.zoom(0.6f, 450);
			}
		}

		if (cue(impact + 0.3) && mine && Settings.titlesOn() && name != null) {
			Hud.brushName(name, 3.0);
		}

		if (fadeColor >= 0 && cue(end - 0.5) && mine) {
			Hud.whiteout(fadeColor, 0.45, 0.9);
		}
	}

	// ------------------------------------------------------------------ 共用小工具

	/** 衝擊後擴散的環（地面 JAG + 白環） */
	protected void shockRings(Draw d, double t, Vec3 ground, Vec3 focus, float maxR) {
		if (t < impact || t > impact + 1) {
			return;
		}

		float k = span(t, impact, impact + 0.8);
		float a = (1 - k) * (1 - k);
		d.flat(Mode.GLOW, JAG, ground, 0.5f + easeOut(k) * maxR, 0, r(pale), g(pale), b(pale), a);
		d.flat(Mode.GLOW, RING, ground.add(0, 0.01, 0), 0.5f + easeOut(k) * maxR * 1.3f, 0, r(main), g(main), b(main), a * 0.8f);
		d.disc(Mode.GLOW, JAG, focus, Draw.X, Draw.Y, 0.3f + easeOut(k) * maxR * 0.45f, 0.4f, 1f, 1f, 1f, a);
	}

	/** 鋸齒雷光（中點位移 3 層） */
	protected static void bolt(Draw d, Vec3 a, Vec3 b, Random rng, float width, float alpha, int col) {
		List<Vec3> pts = new ArrayList<>();
		pts.add(a);
		pts.add(b);
		double amp = a.distanceTo(b) * 0.22;

		for (int level = 0; level < 3; level++) {
			List<Vec3> next = new ArrayList<>();

			for (int i = 0; i < pts.size() - 1; i++) {
				Vec3 p0 = pts.get(i);
				Vec3 p1 = pts.get(i + 1);
				next.add(p0);
				next.add(p0.add(p1).scale(0.5).add((rng.nextDouble() - 0.5) * amp, (rng.nextDouble() - 0.5) * amp, (rng.nextDouble() - 0.5) * amp));
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

	protected static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}

	protected static Vec3 lerp(Vec3 a, Vec3 b, double k) {
		return a.add(b.subtract(a).scale(k));
	}
}
