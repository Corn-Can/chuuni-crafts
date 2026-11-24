package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 爆炸・爆裂破滅（TNT、苦力怕、床 / 重生錨、終界水晶、火球……；power = 爆炸半徑 ×10，規模跟著半徑走）。
 * 白熱的核心一閃 → 橘紅火球膨脹 → 立體衝擊波球殼 + 地面的鋸齒衝擊環 → 火焰與碎屑四散、餘燼飄落、黑煙往上捲。
 * 自己在附近時：頓幀、RGB 分離、FOV 衝擊、閃光與招式名。
 */
public class ExplosionFx extends MinorFx {
	private static final int HOT = 0xFFF0C0;
	private static final int ORANGE = 0xFF8a30;
	private static final int RED = 0xFF3818;

	private final Vec3 at;
	private final Vec3 ground;
	/** 爆炸半徑 */
	private final float rad;
	/** 規模倍率（TNT = 1） */
	private final float s;

	public ExplosionFx(FxPayload p) {
		super(p);
		at = p.at();
		rad = Math.max(0.5f, p.power() / 10f);
		s = Math.max(0.35f, Math.min(2f, rad / 4f));
		ground = new Vec3(at.x, Math.floor(at.y) + 0.05, at.z);
		end = 2.0 + s * 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			// 動漫素材：大爆炸「ドゴォン」、小爆炸「チュドーン」；合成的重低音留在底下補重量
			boolean anime = Sfx.anime(mine, s >= 0.7f ? "bomb" : "bomb_small", null, at, 1f, 1f);
			Sfx.play(mine, "blast_boom", at, (0.6f + 0.25f * s) * (anime ? 0.5f : 1f), 1.1f - 0.15f * s);
			shake(0.2f + 0.35f * s);

			if (near) {
				PostState.flash(HOT, 0.12f + 0.18f * s, 180, 0);
				PostState.tint(ORANGE, 150, 900);
			}

			if (mine) {
				FxClock.hitstop((int)(50 + 50 * s));
				PostState.split(0.6f + 0.8f * s, (float)(Particles.RNG.nextDouble() * 3), 300);
				PostState.shock(0.5f * s, 450);
				PostState.zoom(0.25f * s, 220);
				CameraShake.fovPunch(0.04f * s);
				smallName("explosion");
			}

			Particles.spawn(Kind.HIT, at, Vec3.ZERO, 0.32).size(1.4f * s, 2.4f * s).color(0xFFFFFF, HOT);

			for (int i = 0; i < (int)(70 * s); i++) {
				Particles.spawn(Kind.FLAME, at.add(Particles.randomDir(0.3 * s)), Particles.randomDir(Particles.rand(1.5f, 4.5f) * s).add(0, 1.2 * s, 0),
					Particles.rand(0.4f, 0.9f)).size(0.35f * s, 0.05f).color(0xFFE080, RED).physics(-0.5f, 3f).spin(3f);
			}

			for (int i = 0; i < (int)(90 * s); i++) {
				Particles.spawn(Kind.SPARK, at, Particles.randomDir(Particles.rand(4f, 12f) * s).add(0, 3 * s, 0), Particles.rand(0.5f, 1.2f))
					.size(0.05f, 0.01f).color(0xFFFFFF, i % 3 == 0 ? RED : ORANGE).physics(9f, 0.8f);
			}

			for (int i = 0; i < (int)(26 * s); i++) {
				Particles.spawn(Kind.SMOKE, at.add(Particles.randomDir(rad * 0.4)), Particles.randomDir(1.2 * s).add(0, Particles.rand(1f, 2.5f) * s, 0),
					Particles.rand(1.6f, 2.8f)).size(0.6f * s, 1.6f * s).color(0x504038, 0x201818).physics(-0.3f, 1.2f).spin(0.5f).fadeIn(0.15f);
			}
		}

		// 黑煙往上捲、餘燼飄落
		if (t > 0.1 && t < 0.9) {
			int n = rate(30 * s, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.SMOKE, at.add(Particles.randomDir(rad * 0.3)), new Vec3(0, Particles.rand(1.5f, 3f) * s, 0), Particles.rand(1.5f, 2.5f))
					.size(0.5f * s, 1.4f * s).color(0x403430, 0x181414).physics(-0.2f, 1f).spin(0.4f).fadeIn(0.2f);
			}

			int e = rate(40 * s, dt);

			for (int i = 0; i < e; i++) {
				Vec3 o = at.add(Particles.randomDir(rad * 0.9)).add(0, rad * 0.6, 0);
				Particles.spawn(Kind.FLARE, o, new Vec3(0, -0.6, 0).add(Particles.randomDir(0.4)), Particles.rand(1f, 1.8f)).size(0.05f, 0.01f)
					.color(0xFFE0A0, ORANGE).physics(0.6f, 1.2f).spin(3f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float k = 1 - span(t, 0, 0.9);

		if (k > 0.01f) {
			lights.add(new CinemaPost.Light(at, 4f + rad * 2f, r(ORANGE), g(ORANGE), b(ORANGE), (0.4f + 0.6f * k) * k, true));
		}

		if (t < 0.45 && rays[0] == null) {
			rays[0] = at;
			raysP[0] = r(HOT);
			raysP[1] = g(HOT);
			raysP[2] = b(HOT);
			raysP[3] = 0.35f * (1 - span(t, 0, 0.45));
		}
	}

	@Override
	protected void render(Draw d, double t) {
		// 白熱核心 → 火球
		float core = easeOut(span(t, 0, 0.08)) * (1 - span(t, 0.05, 0.35));
		glow(d, at, rad * 0.9f * (0.5f + core), HOT, core);
		d.billboard(Mode.GLOW, FLARE, at.x, at.y, at.z, rad * 1.6f * core, (float)t, 1f, 1f, 1f, core);

		float ball = easeOut(span(t, 0, 0.25));
		float ballA = 1 - span(t, 0.12, 0.8);
		glow(d, at, rad * (0.6f + ball * 0.9f), ORANGE, 0.9f * ballA);
		glow(d, at.add(0, rad * 0.3 * ball, 0), rad * (0.4f + ball * 1.1f), RED, 0.55f * ballA);

		// 立體衝擊波球殼
		float k = span(t, 0.02, 0.5);

		if (k > 0 && k < 1) {
			float a = (1 - k) * (1 - k);
			d.billboard(Mode.GLOW, RING, at.x, at.y, at.z, 0.5f + easeOut(k) * rad * 2.2f, 0, r(HOT), g(HOT), b(HOT), a);
			d.billboard(Mode.GLOW, JAG, at.x, at.y, at.z, 0.4f + easeOut(k) * rad * 1.6f, (float)(t * 2), r(ORANGE), g(ORANGE), b(ORANGE), a * 0.8f);
		}

		// 地面衝擊環
		groundRing(d, t, 0.02, 0.7, ground, 0.5f, rad * 2.6f, HOT, 0.9f, JAG);
		groundRing(d, t, 0.06, 0.95, ground.add(0, 0.01, 0), 0.5f, rad * 3.4f, ORANGE, 0.7f, RING);
		groundRing(d, t, 0.15, 1.2, ground.add(0, 0.02, 0), 0.4f, rad * 1.8f, RED, 0.5f, RING);

		// 焦土：燒紅的地面慢慢冷卻
		float scorch = (1 - span(t, 0.2, end)) * span(t, 0, 0.1);
		d.flat(Mode.GLOW, GLOW, ground.add(0, 0.03, 0), rad * 1.3f, 0, r(RED), g(RED), b(RED), 0.45f * scorch);
	}
}
