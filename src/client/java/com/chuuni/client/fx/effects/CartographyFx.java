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
import com.chuuni.client.render.FxMesh;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 全知之眼（製圖台）——打架動漫分鏡：
 * <pre>
 * 鏡1 0.00 攤開  貼著桌緣的低角度：羊皮紙地圖「唰」地攤開，全息掃描陣啟動
 * 鏡2 1.10 測繪  高空廣角：全息地球升起、經緯環轉動，羅盤指針狂轉，資料符文往上飄
 * 鏡3 2.40 吸氣  指針 / 地球極近特寫、荷蘭角：黑色集中線
 * 衝擊 2.80     指針「喀」地鎖定北方：衝擊幀 + 頓幀 + 折射衝擊波，地球中心睜開光之眼，毛筆直書招式名
 * 鏡4 2.80 凝視  從下方仰望睜開的眼（慢動作）：白色集中線、橫向光芒
 * 鏡5 4.20 全知  鏡頭往高空拉遠：三道聲納般的測繪波掃過整片大地 → 直接回到世界
 * </pre>
 */
public class CartographyFx extends Effect {
	private static final Identifier COMPASS = FxRender.tex("circle_compass");
	private static final Identifier GRID = FxRender.tex("holo_grid");
	private static final Identifier SCAN = FxRender.tex("scan_wedge");
	private static final Identifier MAP = FxRender.tex("map_sheet");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");
	private static final Identifier BAND = FxRender.tex("rune_band");
	private static final Identifier SPARK = FxRender.tex("spark");

	private static final int TEAL = 0x3CF0D0;
	private static final int GOLD = 0xFFD890;
	private static final int PALE = 0xD8FFF6;

	private static final double S2 = 1.1;
	private static final double S3 = 2.4;
	private static final double LOCK = 2.8;
	private static final double S5 = 4.2;
	private static final double[] PULSES = {4.3, 5.05, 5.8};
	private static final double DURATION = 6.7;

	private final Vec3 top;
	private final Vec3 globe;
	private final boolean near;

	public CartographyFx(FxPayload p) {
		super(p);
		top = new Vec3(c.x, pos.getY() + 1.01, c.z);
		globe = new Vec3(c.x, pos.getY() + 2.4, c.z);
		near = near();

		if (near) {
			PostState.look("cartography", 400, (int)(S5 * 1000));
		}

		if (mine) {
			Frame f = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
			Vec3 g = f.at(0, 2.4, 0);
			Cinematic.play(List.of(
				Shot.of(S2, f.at(0.7, 1.22, 1.15), f.at(0.45, 1.3, 0.9), f.at(0, 1.0, 0)).look(f.at(0, 1.0, 0), f.at(0, 1.25, 0)).roll(5, 8).fov(0.9f, 0.85f),
				Shot.of(S3 - S2, f.at(-1.9, 3.7, 2.1), f.at(-1.4, 3.3, 1.7), f.at(0, 1.8, 0)).roll(-3, -5),
				Shot.of(LOCK - S3, f.at(0.15, 2.5, 0.95), f.at(0.1, 2.46, 0.7), g).roll(-9, -13).fov(0.7f, 0.6f).accelerate(),
				Shot.of(S5 - LOCK, f.at(1.3, 0.75, 1.9), f.at(1.6, 0.85, 2.4), g).roll(12, 9).fov(1.12f, 1.05f),
				// 全知：往高空拉遠，俯瞰測繪波掃過大地
				Shot.of(DURATION - S5, f.at(0.3, 4.2, 3.2), f.at(0.6, 10.5, 6.5), f.at(0, 1.5, 0)).look(f.at(0, 1.5, 0), f.at(0, 0, -2)).roll(0, -4)
					.fov(1f, 1.15f)
			), closeMenu(CartographyTableMenu.class));
		}
	}

	@Override
	public double duration() {
		return DURATION;
	}

	/** 指針角度（弧度）：先加速狂轉、再減速，在 LOCK 時剛好停在北方（-Z） */
	private float needle(double t) {
		if (t >= LOCK) {
			double k = t - LOCK;
			return (float)(Math.PI / 2 + Math.sin(k * 30) * Math.exp(-k * 9) * 0.25);
		}

		double x = Math.max(0, (t - 0.9) / (LOCK - 0.9));
		double turns = 8 * (x - Math.sin(2 * Math.PI * x) / (2 * Math.PI));
		return (float)(Math.PI / 2 - turns * Math.PI * 2 + 8 * Math.PI * 2);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.05)) {
			Sfx.play(mine, "map_unfurl", c, 1f, 1f);
		}

		if (cue(0.6)) {
			Sfx.play(mine, "holo_boot", c, 0.9f, 1f);
		}

		if (cue(1.0)) {
			Sfx.play(mine, "compass_spin", c, 0.9f, 1f);
		}

		// 上升的資料符文
		if (t > 0.6 && t < S5) {
			int n = rate(22, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = 0.6 + Particles.RNG.nextDouble() * 1.4;
				Vec3 at = new Vec3(c.x + Math.cos(a) * r, top.y, c.z + Math.sin(a) * r);
				Particles.spawn(Kind.GLYPH, at, new Vec3(0, Particles.rand(0.6f, 1.4f), 0), 1.3).size(0.08f, 0.05f).color(PALE, TEAL).physics(0, 0.2f);
			}
		}

		// ---------------------------------------------------------- 鏡3：吸氣
		if (cue(S3)) {
			if (near) {
				PostState.darken(0.35f);
			}

			if (mine) {
				PostState.lines(0.8f, true, (int)((LOCK - S3) * 1000) + 30, 0.32f);
			}

			Sfx.play(mine, "whoosh", c, 0.8f, 0.5f);
		}

		if (t > S3 && t < LOCK) {
			int n = rate(60, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = globe.add(Particles.randomDir(1.6 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.SPARK, from, globe.subtract(from).scale(3), 0.3).size(0.025f, 0.008f).color(0xFFFFFF, TEAL).physics(0, 0).attract(globe, 30f);
			}
		}

		if (cue(LOCK - 0.45)) {
			// eye_open 有 0.45 秒的吸入，鎖定的「喀」正好落在 LOCK
			Sfx.play(mine, "eye_open", c, 1.2f, 1f);
		}

		// ---------------------------------------------------------- 衝擊：鎖定、睜眼
		if (cue(LOCK)) {
			FxClock.hitstop(140);
			shake(0.6f);

			if (near) {
				PostState.darken(0);
				PostState.flash(PALE, 0.35f, 150, mine ? 0.1f : 0.05f);
			}

			if (mine) {
				PostState.impact(PostState.FROST, 140);
				PostState.shock(1.0f, 650);
				PostState.split(1.6f, 1.1f, 450);
				CameraShake.fovPunch(0.1f);
			}

			Sfx.play(mine, "shockwave", c, 0.9f, 0.9f);
			Particles.spawn(Kind.HIT, globe, Vec3.ZERO, 0.35).size(1.3f, 2f).color(0xFFFFFF, TEAL);

			for (int i = 0; i < 90; i++) {
				Particles.spawn(Kind.SPARK, globe, Particles.randomDir(Particles.rand(3f, 8f)), Particles.rand(0.4f, 0.8f)).size(0.05f, 0.01f).color(0xFFFFFF, TEAL)
					.physics(0, 2f);
			}

			for (int i = 0; i < 24; i++) {
				Particles.spawn(Kind.GLYPH, globe, Particles.randomDir(Particles.rand(2f, 4f)), 1.0).size(0.14f, 0.08f).color(GOLD, TEAL).physics(0, 1.5f);
			}
		}

		if (cue(LOCK + 0.08)) {
			FxClock.slowmo(0.35f, 1100);

			if (mine) {
				PostState.lines(0.7f, false, 1000, 0.42f);
				PostState.streak(1f, 900);
				PostState.zoom(0.6f, 450);
			}
		}

		if (cue(LOCK + 0.3) && mine && Settings.titlesOn()) {
			Hud.brushName("cartography", 3.0);
		}

		// ---------------------------------------------------------- 鏡5：全知（測繪波）
		if (cue(S5 - 0.1)) {
			Sfx.play(mine, "world_scan", c, 1.1f, 1f);

			if (near) {
				PostState.look("survey", 900, (int)((DURATION - S5) * 1000));
			}
		}

		for (int i = 0; i < PULSES.length; i++) {
			if (cue(PULSES[i]) && mine) {
				PostState.shock(0.35f - i * 0.08f, 900);
			}
		}

		for (int i = 0; i < PULSES.length; i++) {
			float k = span(t, PULSES[i], PULSES[i] + 1.6);

			if (k > 0 && k < 1) {
				// 波前掃過的地方冒出符文
				double r = pulseRadius(k);
				int n = rate(90 * (1 - k), dt);

				for (int j = 0; j < n; j++) {
					double a = Particles.RNG.nextDouble() * Math.PI * 2;
					Vec3 at = new Vec3(c.x + Math.cos(a) * r, pos.getY() + 0.1, c.z + Math.sin(a) * r);
					Particles.spawn(Kind.GLYPH, at, new Vec3(0, Particles.rand(0.8f, 2f), 0), 1.0).size(0.16f, 0.08f).color(PALE, TEAL).physics(0, 1f);
				}
			}
		}

		if (cue(DURATION - 0.5) && mine) {
			Hud.whiteout(PALE, 0.45, 0.9);
		}
	}

	private static double pulseRadius(float k) {
		return 0.6 + (1 - Math.pow(1 - k, 2.2)) * 28;
	}

	// ------------------------------------------------------------------ 光源（後製）

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, DURATION - 0.6, DURATION);
		float charge = span(t, 1.0, LOCK);
		float eye = t > LOCK ? 1 - span(t, LOCK, S5 + 0.6) * 0.6f : 0;
		lights.add(new CinemaPost.Light(globe, 3f + eye * 3, r(TEAL), g(TEAL), b(TEAL), (0.25f + charge * 0.25f + eye * 0.35f) * life, true));
		lights.add(new CinemaPost.Light(top.add(0, 0.3, 0), 3f, r(TEAL), g(TEAL), b(TEAL), 0.6f * span(t, 0.3, 0.8) * life, false));

		float burst = t > LOCK ? 1 - span(t, LOCK, LOCK + 1.3) : 0;

		if (burst > 0.01f) {
			rays[0] = globe;
			raysP[0] = r(PALE);
			raysP[1] = g(PALE);
			raysP[2] = b(PALE);
			raysP[3] = burst * 0.45f;
		}
	}

	// ------------------------------------------------------------------ 繪製

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, DURATION - 0.6, DURATION);
		float main = fade(t, 0.2, S5 + 0.6, S5 + 1.4);

		// ---- 地圖攤開（先橫向、再縱向展開）
		float ux = easeOut(span(t, 0.15, 0.4));
		float uz = easeOut(span(t, 0.35, 0.6));
		drawMap(d, ux, uz, life);

		// ---- 全息掃描陣
		float open = easeOutBack(span(t, 0.55, 1.0));
		Vec3 holo = top.add(0, 0.03, 0);
		d.flat(Mode.GLOW, GRID, holo, 2.3f * open, 0, r(TEAL), g(TEAL), b(TEAL), 0.55f * main);
		d.flat(Mode.GLOW, SCAN, holo.add(0, 0.005, 0), 2.3f * open, (float)(t * 4.5), r(TEAL), g(TEAL), b(TEAL), 0.7f * main);
		d.flat(Mode.GLOW, COMPASS, holo.add(0, 0.01, 0), 1.25f * open, (float)(-t * 0.3), r(GOLD), g(GOLD), b(GOLD), 0.85f * main);

		// ---- 投影光錐（桌面 → 地球）
		float cone = span(t, 0.9, 1.4) * main;
		d.cylinder(Mode.GLOW, GRID, holo, 1.0f, (float)(globe.y - holo.y) - 0.2f, 40, 5, (float)(t * 0.2), 1, 0, 0.55f, r(TEAL), g(TEAL), b(TEAL), 0.5f * cone,
			0.05f * cone);

		// ---- 全息地球
		float grow = easeOutBack(span(t, 1.0, 1.6));

		if (grow > 0.01f) {
			double spin = 9 * (1 - Math.exp(-Math.max(0, t - 1) * 1.1)) + t * 0.4;
			float rad = 0.72f * grow;
			Matrix4f m = new Matrix4f().rotateZ(0.41f).rotateY((float)spin).scale(rad);
			d.mesh(Mode.GLOW, FxMesh.get("globe"), globe, m, r(TEAL), g(TEAL), b(TEAL), 0.55f * main, false);
			d.disc(Mode.GLOW, RING, globe, Draw.X, Draw.Y, rad * 1.35f, (float)(t * 0.7), r(GOLD), g(GOLD), b(GOLD), 0.55f * main);
			d.disc(Mode.GLOW, RING, globe, Draw.Z, Draw.Y, rad * 1.5f, (float)(-t * 0.5), r(TEAL), g(TEAL), b(TEAL), 0.45f * main);
			d.flat(Mode.GLOW, RING, globe, rad * 1.65f, 0, r(GOLD), g(GOLD), b(GOLD), 0.5f * main);
			d.billboard(Mode.GLOW, GLOW, globe.x, globe.y, globe.z, rad * 2.2f, 0, r(TEAL), g(TEAL), b(TEAL), 0.18f * main);
		}

		// ---- 全知之眼（鎖定後）：光芒 + 旋轉羅盤虹膜 + 豎立的金色瞳孔
		if (t > LOCK) {
			float k = span(t, LOCK, LOCK + 0.25);
			float out = 1 - span(t, S5 + 0.8, S5 + 1.6);
			float blink = easeOut(span(t, LOCK + 0.05, LOCK + 0.3));
			d.billboard(Mode.GLOW, FLARE, globe.x, globe.y, globe.z, 2.0f * easeOutBack(k), (float)(t * 0.3), 1f, 1f, 1f, 0.4f * out);
			d.billboard(Mode.GLOW, RING, globe.x, globe.y, globe.z, 0.75f * blink, 0, r(TEAL), g(TEAL), b(TEAL), out);
			d.streak(Mode.GLOW, SPARK, globe.x, globe.y - 0.32 * blink, globe.z, globe.x, globe.y + 0.32 * blink, globe.z, 0.12f, r(GOLD), g(GOLD), b(GOLD), out);

			float w = span(t, LOCK, LOCK + 0.8);
			d.flat(Mode.GLOW, JAG, top.add(0, 0.02, 0), 0.5f + easeOut(w) * 6f, 0, r(TEAL), g(TEAL), b(TEAL), (1 - w) * (1 - w));
			d.disc(Mode.GLOW, JAG, globe, Draw.X, Draw.Y, 0.4f + easeOut(w) * 3f, 0.5f, 1f, 1f, 1f, (1 - w) * (1 - w));
		}

		// ---- 羅盤指針
		float na = span(t, 0.8, 1.0) * main;

		if (na > 0.01f) {
			Matrix4f m = new Matrix4f().rotateY(needle(t)).scale(0.75f);
			d.mesh(Mode.SOLID, FxMesh.get("needle"), top.add(0, 0.55, 0), m, 1f, 1f, 1f, na, true);
			d.billboard(Mode.GLOW, GLOW, top.x, top.y + 0.55, top.z, 0.35f, 0, r(GOLD), g(GOLD), b(GOLD), 0.7f * na);
		}

		// ---- 測繪波：貼地的光環 + 一道往外推的全息光牆
		for (int i = 0; i < PULSES.length; i++) {
			float k = span(t, PULSES[i], PULSES[i] + 1.6);

			if (k <= 0 || k >= 1) {
				continue;
			}

			float rr = (float)pulseRadius(k);
			float a = (1 - k) * (1 - k * 0.5f) * life;
			Vec3 ground = new Vec3(c.x, pos.getY() + 0.06, c.z);
			d.flat(Mode.GLOW, RING, ground, rr, 0, r(TEAL), g(TEAL), b(TEAL), a);
			d.flat(Mode.GLOW, GRID, ground.add(0, -0.01, 0), rr, 0, r(TEAL), g(TEAL), b(TEAL), a * 0.25f);
			d.cylinder(Mode.GLOW, GRID, ground.add(0, -0.5, 0), rr, 3.2f - i * 0.6f, 64, 16, (float)(t * 0.1), 1, 0, 1f, r(PALE), g(PALE), b(PALE), a * 0.6f, 0);
		}
	}

	private void drawMap(Draw d, float ux, float uz, float life) {
		if (ux <= 0.01f) {
			return;
		}

		Vec3 at = top.add(0, 0.015, 0);
		float hx = 0.62f * ux;
		float hz = 0.62f * Math.max(0.04f, uz);
		// disc 的兩軸各自縮放 → 長方形
		d.disc(Mode.ALPHA, MAP, at, new Vector3f(hx / 0.62f, 0, 0), new Vector3f(0, 0, hz / 0.62f), 0.62f, 0, 1f, 1f, 1f, life);
	}
}
