package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 雕刻南瓜・南瓜鬼面（face = 雕出臉的那一面）。
 * 三刀鋸齒狀的刀光在南瓜表面刻出臉 → 雙眼與嘴「轟」地亮起鬼火般的橘光，三道光束從臉上往外射出，
 * 南瓜籽四濺，紫色的鬼魂煙霧在周圍繚繞升起。
 */
public class PumpkinFx extends MinorFx {
	private static final int ORANGE = 0xFF8A20;
	private static final int GHOST = 0x8A50FF;
	private static final double LIGHT = 0.35;

	public PumpkinFx(FxPayload p) {
		super(p);
		end = 2.4;
	}

	private Vec3[] features() {
		return new Vec3[]{on(-0.2, 0.14, 0.02), on(0.2, 0.14, 0.02), on(0, -0.18, 0.02)};
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "pumpkin_carve", c, 0.9f, 1f);
			tint(ORANGE, 1.8);
			smallName("pumpkin");
		}

		if (cue(0.1)) {
			for (int i = 0; i < 24; i++) {
				Particles.spawn(Kind.SHARD, face, nv.scale(Particles.rand(1f, 2.5f)).add(Particles.randomDir(1.2)).add(0, 1.2, 0), Particles.rand(0.7f, 1.2f))
					.size(0.04f, 0.03f).color(0xFFF0B0, 0xF0D080).physics(8f, 0.8f).spin(10f);
			}
		}

		if (cue(LIGHT)) {
			pop(ORANGE, 0.55f);

			for (Vec3 f : features()) {
				sparks(f, 8, 2.5f, ORANGE, 0.35f);
			}
		}

		if (t > 0.3 && t < 1.9) {
			int nn = rate(18, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = c.add(Math.cos(a) * 0.9, -0.4, Math.sin(a) * 0.9);
				Particles.spawn(Kind.SMOKE, at, new Vec3(-Math.sin(a) * 0.4, 0.7, Math.cos(a) * 0.4), 1.4).size(0.2f, 0.6f).color(GHOST, 0x301050).alpha(0.4f)
					.physics(-0.2f, 0.6f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float flick = 0.8f + 0.2f * (float)Math.sin(t * 31) * (float)Math.sin(t * 13);
		float k = span(t, LIGHT, LIGHT + 0.05) * (1 - span(t, 1.4, end)) * flick;
		lights.add(new CinemaPost.Light(face.add(nv.scale(0.6)), 3f, r(ORANGE), g(ORANGE), b(ORANGE), 0.6f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		// 三刀鋸齒刀光
		for (int i = 0; i < 3; i++) {
			double t0 = i * 0.09;
			float k = easeOut(span(t, t0, t0 + 0.08));
			float a = 1 - span(t, t0 + 0.08, t0 + 0.35);

			if (k <= 0 || a <= 0.01f) {
				continue;
			}

			double y = 0.3 - i * 0.25;
			Vec3 a0 = on(-0.45, y + 0.1, 0.05);
			Vec3 a1 = on(0.45, y - 0.1, 0.05);
			line(d, TRAIL, a0, lerp(a0, a1, k), 0.22f, ORANGE, a * 0.8f);
			line(d, SPARK, a0, lerp(a0, a1, k), 0.05f, 0xFFFFFF, a);
		}

		// 雙眼與嘴的鬼火
		float flick = 0.75f + 0.25f * (float)Math.sin(t * 29);
		float on = span(t, LIGHT, LIGHT + 0.05) * life;
		Vec3[] f = features();

		for (int i = 0; i < f.length; i++) {
			float size = i == 2 ? 0.32f : 0.22f;
			glow(d, f[i], size * 1.6f, ORANGE, on * flick * 0.8f);
			d.billboard(Mode.GLOW, FLARE, f[i].x, f[i].y, f[i].z, size, (float)t * 2, 1f, 0.9f, 0.6f, on * 0.7f);

			// 光束往外射
			float beam = easeOut(span(t, LIGHT, LIGHT + 0.25));
			float ba = (1 - span(t, LIGHT + 0.3, 1.6)) * life;
			line(d, BEAM, f[i], f[i].add(nv.scale(0.2 + beam * 2.5)).add(0, beam * (i == 2 ? -0.2 : 0.3), 0), 0.25f + beam * 0.25f, ORANGE, ba * 0.5f);
		}

		faceRing(d, t, LIGHT, LIGHT + 0.6, face, 0.2f, 1.6f, ORANGE, 0.8f);
	}
}
