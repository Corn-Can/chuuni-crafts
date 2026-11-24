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
 * 上蠟・蜜蠟封緘（variant 0 = 方塊、1 = 告示牌）。
 * 金色的蜜從方塊頂緣沿四面垂流 → 封蠟印從空中壓進點到的那一面「咚」地蓋上 → 一道琥珀光澤斜掃過表面，方塊的邊緣發亮。
 */
public class WaxFx extends MinorFx {
	private static final int HONEY = 0xFFAA20;
	private static final int PALE = 0xFFE6A0;
	private static final double STAMP = 0.45;

	private final boolean sign;
	private final double[] drips = new double[16];

	public WaxFx(FxPayload p) {
		super(p);
		sign = p.variant() == 1;
		end = 2.2;

		for (int i = 0; i < drips.length; i++) {
			drips[i] = Particles.RNG.nextDouble();
		}
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "wax_seal", c, 0.9f, sign ? 1.12f : 1f);
			tint(HONEY, 1.4);
			smallName("wax");
		}

		if (cue(STAMP)) {
			pop(HONEY, 0.4f);
			Vec3 at = sign ? c : face;

			for (int i = 0; i < 28; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 dir = new Vec3(u.x * Math.cos(a) + v.x * Math.sin(a), u.y * Math.cos(a) + v.y * Math.sin(a), u.z * Math.cos(a) + v.z * Math.sin(a));
				Particles.spawn(Kind.FLARE, at, dir.scale(Particles.rand(1.2f, 3f)).add(nv.scale(0.6)), 0.6).size(0.07f, 0.02f).color(0xFFFFFF, HONEY)
					.physics(1f, 3f).spin(3f);
			}

			for (int i = 0; i < 12; i++) {
				Vec3 at2 = c.add(Particles.rand(-0.5f, 0.5f), -0.5, Particles.rand(-0.5f, 0.5f));
				Particles.spawn(Kind.GLOW, at2, new Vec3(0, -0.4, 0), 0.9).size(0.06f, 0.03f).color(PALE, HONEY).physics(6f, 0.5f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float k = t < STAMP ? span(t, 0, STAMP) * 0.4f : 1 - span(t, STAMP, end);
		lights.add(new CinemaPost.Light(face.add(nv.scale(0.6)), 2.5f, r(HONEY), g(HONEY), b(HONEY), 0.3f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);
		double s = sign ? 0.5 : 1.0;

		// 蜜沿四面垂流（從頂緣往下長）
		if (!sign) {
			float grow = easeOut(span(t, 0, 0.6));
			float a = (1 - span(t, 1.4, end)) * life;

			for (int i = 0; i < drips.length; i++) {
				int side = i % 4;
				double k = drips[i] - 0.5;
				double sx = side == 0 ? k : side == 1 ? 0.505 : side == 2 ? -k : -0.505;
				double sz = side == 0 ? -0.505 : side == 1 ? k : side == 2 ? 0.505 : -k;
				double len = (0.3 + drips[(i + 5) % drips.length] * 0.6) * grow;
				Vec3 top = c.add(sx, 0.5, sz);
				line(d, TRAIL, top, top.add(0, -len, 0), 0.07f, HONEY, a * 0.8f);
				glow(d, top.add(0, -len, 0), 0.12f, PALE, a * 0.8f);
			}
		}

		// 封蠟印：從大往小壓下來
		Vec3 at = sign ? c : face;
		float in = easeIn(span(t, 0, STAMP));
		float after = span(t, STAMP, end);
		float flash = t > STAMP && t < STAMP + 0.15 ? 1 - span(t, STAMP, STAMP + 0.15) : 0;
		float radius = (float)(s * (t < STAMP ? 1.6 - in * 1.15 : 0.45 + after * 0.08));
		hexes(d, at, radius * 0.38f, HONEY, Math.min(1f, (0.3f + in * 0.7f) * (1 - after * after)) * life, Math.min(1f, (float)t * 2.5f));
		faceDisc(d, GLOW, at, radius * 1.2f, 0, PALE, flash * 0.35f);
		faceRing(d, t, STAMP, STAMP + 0.6, at, (float)(0.2 * s), (float)(1.4 * s), PALE, 0.9f);
		faceRing(d, t, STAMP, STAMP + 0.45, at, (float)(0.1 * s), (float)(0.9 * s), 0xFFFFFF, 0.7f);

		// 琥珀光澤：一道光從面的一角斜掃到另一角
		float sweep = span(t, STAMP + 0.1, STAMP + 0.7);

		if (sweep > 0 && sweep < 1 && !sign) {
			double o = -0.8 + sweep * 1.6;
			Vec3 a0 = on(o - 0.5, -0.5, 0.01);
			Vec3 a1 = on(o + 0.5, 0.5, 0.01);
			line(d, SPARK, a0, a1, 0.18f, PALE, (float)Math.sin(sweep * Math.PI) * 0.8f);
		}

		// 方塊邊緣發亮
		float edge = span(t, STAMP, STAMP + 0.1) * (1 - span(t, STAMP + 0.3, end)) * life;

		if (edge > 0.01f && !sign) {
			double x0 = pos.getX(), y0 = pos.getY(), z0 = pos.getZ();
			double[][] e = {
				{0, 0, 0, 1, 0, 0}, {0, 1, 0, 1, 1, 0}, {0, 0, 1, 1, 0, 1}, {0, 1, 1, 1, 1, 1},
				{0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1},
				{0, 0, 0, 0, 0, 1}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1}};

			for (double[] q : e) {
				d.streak(Mode.GLOW, SPARK, x0 + q[0], y0 + q[1], z0 + q[2], x0 + q[3], y0 + q[4], z0 + q[5], 0.05f, r(HONEY), g(HONEY), b(HONEY), edge);
			}
		}
	}
}
