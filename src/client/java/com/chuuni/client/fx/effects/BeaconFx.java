package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 烽火台・天光降臨（power = 金字塔層數 1~4；4 層：漫畫分格時停 + 四角大字）。
 * <pre>
 * 鏡1 0.00 共鳴  貼著金字塔仰拍：一層一層從下往上亮起符文環，每亮一層一聲共鳴
 * 鏡2 1.40 昇華  環繞廣角：光環沿著光束往上爬
 * 鏡3 2.40 吸氣  順著光束往上看的極端仰角
 * 衝擊 2.80     天光直落：一道巨大的光柱從天而降砸在烽火台上，衝擊波掃過金字塔
 * 鏡4 2.80 英雄  低角度仰望（慢動作）
 * 鏡5 4.20 庇佑  鏡頭拉上高空：效果範圍的光環擴散過整片大地 → 回到世界
 * </pre>
 */
public class BeaconFx extends AnimeFx {
	private static final int CYAN = 0x7FF0FF;
	private static final int WHITE = 0xF4FEFF;

	private final int levels;
	private final double range;
	private final Vec3 core;

	public BeaconFx(FxPayload p) {
		super(p);
		levels = Math.max(1, Math.min(4, p.power()));
		range = levels * 10 + 10;
		core = new Vec3(c.x, pos.getY() + 0.6, c.z);
		name = "beacon";
		main = CYAN;
		pale = WHITE;
		impactMode = PostState.FROST;
		manga = levels >= 4 || Settings.intensity >= 2;
		s3 = 2.4;
		impact = 2.8;
		end = 6.8;
		fadeColor = WHITE;
		look("beacon", 4.4);

		if (mine) {
			Vec3 up = frame.at(0, 8, 0);
			Cinematic.play(List.of(
				Shot.of(1.4, frame.at(1.6 + levels * 0.6, -levels + 0.3, 2.2 + levels * 0.5), frame.at(1.2 + levels * 0.5, -levels + 0.6, 1.9 + levels * 0.4),
					frame.at(0, -levels * 0.5, 0)).look(frame.at(0, -levels * 0.6, 0), frame.at(0, 0.4, 0)).roll(6, 9).fov(0.95f, 0.88f),
				Shot.of(s3 - 1.4, frame.at(-3.5, 2.0, 3.0), frame.at(-2.0, 4.5, 3.8), frame.at(0, 2.5, 0)).look(frame.at(0, 1.5, 0), frame.at(0, 5, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(1.2, 0.9, 1.6), frame.at(1.0, 0.8, 1.3), up).roll(-6, -10).fov(0.75f, 0.62f).accelerate(),
				Shot.of(4.2 - impact, frame.at(2.0, -0.4, 3.0), frame.at(2.4, -0.3, 3.6), frame.at(0, 3.5, 0)).look(frame.at(0, 4.5, 0), frame.at(0, 2.0, 0))
					.roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(end - 4.2, frame.at(0.6, 6, 7), frame.at(1.2, 16, 14), frame.at(0, 0, 0)).look(frame.at(0, 1, 0), frame.at(0, -2, -4)).roll(0, -4).fov(1f, 1.15f)
			));
		}
	}

	/** 第 i 層（1 = 最上層）亮起的時間 */
	private double layerAt(int i) {
		return 0.15 + (levels - i) * 0.3;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "beacon_rise", c, 1f, 0.9f + levels * 0.04f);
		}

		for (int i = 1; i <= levels; i++) {
			if (cue(layerAt(i))) {
				shake(0.08f);

				if (mine) {
					PostState.split(0.3f + i * 0.1f, i * 1.1f, 150);
				}

				Vec3 at = new Vec3(c.x, pos.getY() - i + 1.02, c.z);

				for (int k = 0; k < 24; k++) {
					double a = Particles.RNG.nextDouble() * Math.PI * 2;
					Vec3 rim = at.add(Math.cos(a) * (i + 0.5), 0, Math.sin(a) * (i + 0.5));
					Particles.spawn(Kind.GLYPH, rim, new Vec3(0, Particles.rand(0.5f, 1.5f), 0), 0.9).size(0.12f, 0.06f).color(WHITE, CYAN).physics(0, 0.5f);
				}
			}
		}

		// 沿光束往上爬的光點
		if (t > 1.2 && t < s3) {
			int n = rate(60, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = core.add(Particles.randomDir(0.4));
				Particles.spawn(Kind.FLARE, at, new Vec3(0, Particles.rand(6f, 12f), 0), 1.0).size(0.12f, 0.02f).color(0xFFFFFF, CYAN).physics(0, 0);
			}
		}

		if (cue(impact - 0.45)) {
			Sfx.play(mine, "beacon_descent", c, 1.3f, 1.04f - levels * 0.03f);
		}

		// 天光落地後：光羽從天飄下
		if (t > impact && t < end - 0.6) {
			int n = rate(40 * (1 - span(t, impact, end)), dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double rr = Math.sqrt(Particles.RNG.nextDouble()) * (3 + levels);
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, pos.getY() + 6 + Particles.RNG.nextDouble() * 4, c.z + Math.sin(a) * rr);
				Particles.spawn(Kind.SHARD, at, new Vec3(0, -1.2, 0), 3.0).size(0.12f, 0.06f).color(WHITE, CYAN).physics(0, 0).spin(1.5f).fadeIn(0.3f);
			}
		}

		// 庇佑：效果範圍的光環掃過的地方冒出符文
		if (t > 4.3) {
			float k = span(t, 4.3, 5.9);
			double rr = 1 + easeOut(k) * range;
			int n = rate(100 * (1 - k), dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, pos.getY() - levels + 1.1, c.z + Math.sin(a) * rr);
				Particles.spawn(Kind.GLYPH, at, new Vec3(0, Particles.rand(1f, 2.5f), 0), 1.0).size(0.2f, 0.1f).color(WHITE, CYAN).physics(0, 1f);
			}
		}

		beats(t, dt, core);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.6, end);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.6) : 0;
		lights.add(new CinemaPost.Light(core, 3f + burst * 5, r(CYAN), g(CYAN), b(CYAN), (0.35f + span(t, 0.2, impact) * 0.3f + burst * 0.4f) * life, true));

		for (int i = 1; i <= levels; i++) {
			float on = span(t, layerAt(i), layerAt(i) + 0.2) * life;
			lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() - i + 1.3, c.z), i + 1.5f, r(CYAN), g(CYAN), b(CYAN), 0.35f * on, false));
		}

		if (burst > 0.01f) {
			segs.add(new CinemaPost.Seg(core, core.add(0, 60, 0), 8, r(WHITE), g(WHITE), b(WHITE), burst * 0.4f, 0.3f));
			rays[0] = core;
			raysP[0] = r(WHITE);
			raysP[1] = g(WHITE);
			raysP[2] = b(WHITE);
			raysP[3] = burst * 0.3f;
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.15, end - 0.6, end);

		// ---- 金字塔每一層的符文環
		for (int i = 1; i <= levels; i++) {
			float on = easeOutBack(span(t, layerAt(i), layerAt(i) + 0.25)) * life;

			if (on <= 0.01f) {
				continue;
			}

			Vec3 at = new Vec3(c.x, pos.getY() - i + 1.02, c.z);
			float rr = i + 0.55f;
			d.annulus(Mode.GLOW, GLOW, at, rr * 0.82f * on, rr * on, 64, 4 + i * 2, (float)(t * 0.1 * (i % 2 == 0 ? 1 : -1)), r(CYAN), g(CYAN), b(CYAN), 0.7f, 0.7f);
			float k = span(t, layerAt(i), layerAt(i) + 0.5);

			if (k < 1) {
				d.flat(Mode.GLOW, RING, at.add(0, 0.01, 0), rr * (1 + k * 0.5f), 0, 1f, 1f, 1f, (1 - k) * 0.8f);
			}
		}

		// ---- 沿光束爬升的光環
		if (t > 1.2 && t < impact + 0.2) {
			for (int k = 0; k < 4; k++) {
				double h = ((t - 1.2) * 4 + k * 2.5) % 10;
				float a = (1 - (float)h / 10) * Math.min(span(t, 1.2, 1.5), 1 - span(t, impact, impact + 0.2));
				d.flat(Mode.GLOW, RING, core.add(0, h, 0), 0.9f - (float)h * 0.05f, 0, r(CYAN), g(CYAN), b(CYAN), a);
			}
		}

		// ---- 天光：從天上往下長的巨大光柱，落地後慢慢變細
		float fall = easeIn(span(t, s3 + 0.1, impact));
		float after = t > impact ? 1 - span(t, impact + 0.3, end - 0.8) : 1;

		if (fall > 0.01f && after > 0.01f) {
			double topY = core.y + 70;
			double bottom = topY - (topY - core.y) * fall;
			float w = (t < impact ? 0.6f : 1.2f * (1 - span(t, impact, impact + 0.6)) + 0.55f) * after;
			Vec3 base = new Vec3(c.x, bottom, c.z);
			float h = (float)(topY - bottom);
			d.cylinder(Mode.GLOW, BEAM, base, w, h, 32, 1, 0, 8, (float)(-t * 2), 1.3f, r(CYAN), g(CYAN), b(CYAN), 0.4f * after, 0.06f * after);
			d.cylinder(Mode.GLOW, BEAM, base, w * 0.35f, h, 16, 1, 0.5f, 8, (float)(-t * 3), 1.1f, 1f, 1f, 1f, 0.6f * after, 0.2f * after);
			d.billboard(Mode.GLOW, GLOW, base.x, base.y, base.z, 2f * w, 0, r(WHITE), g(WHITE), b(WHITE), 0.3f * after);
		}

		Vec3 ground = new Vec3(c.x, pos.getY() - levels + 1.04, c.z);
		shockRings(d, t, ground, core, 6f + levels * 2);

		// ---- 庇佑：效果範圍的光環
		if (t > 4.3) {
			float k = span(t, 4.3, 5.9);
			float rr = 1 + easeOut(k) * (float)range;
			float a = (1 - k) * life;
			d.flat(Mode.GLOW, RING, ground.add(0, 0.02, 0), rr, 0, r(CYAN), g(CYAN), b(CYAN), a);
			d.flat(Mode.GLOW, GRID, ground, rr, 0, r(CYAN), g(CYAN), b(CYAN), a * 0.2f);
			d.cylinder(Mode.GLOW, BEAM, ground.add(0, -0.3, 0), rr, 2.5f, 64, 16, (float)(t * 0.1), 1, 0, 1f, r(WHITE), g(WHITE), b(WHITE), a * 0.5f, 0);
		}
	}
}
