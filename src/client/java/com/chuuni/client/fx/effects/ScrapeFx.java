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
 * 斧頭刮銅・返璞歸真（variant 0 = 刮除銅鏽、1 = 除蠟）。
 * 斧刃一刀斜劈過點到的面 → 銅鏽（青綠）/ 蜜蠟（金黃）碎片整片剝落飛散 → 一道新銅的橙光從刀痕擴散整面，四角閃光。
 */
public class ScrapeFx extends MinorFx {
	private static final int COPPER = 0xFF8A50;
	private static final int PATINA = 0x4FC0A0;
	private static final int WAX = 0xFFC040;

	private final int old;

	public ScrapeFx(FxPayload p) {
		super(p);
		old = p.variant() == 1 ? WAX : PATINA;
		end = 2.0;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "copper_scrape", c, 0.9f, p.variant() == 1 ? 1.1f : 1f);
			tint(COPPER, 1.2);
			smallName("scrape");
		}

		if (cue(0.15)) {
			pop(old, 0.4f);

			for (int i = 0; i < 36; i++) {
				Vec3 at = on(Particles.rand(-0.45f, 0.45f), Particles.rand(-0.45f, 0.45f), 0.03);
				Vec3 vel = nv.scale(Particles.rand(0.8f, 2.4f)).add(Particles.randomDir(1.2)).add(0, 1, 0);
				Particles.spawn(Kind.SHARD, at, vel, Particles.rand(0.8f, 1.4f)).size(0.06f, 0.04f).color(old, old).physics(7f, 1f).spin(10f);
			}
		}

		if (cue(0.55)) {
			for (int k = 0; k < 4; k++) {
				Vec3 corner = on(k % 2 == 0 ? -0.5 : 0.5, k < 2 ? -0.5 : 0.5, 0.03);
				sparks(corner, 6, 2f, COPPER, 0.4f);
				Particles.spawn(Kind.FLARE, corner, Vec3.ZERO, 0.5).size(0.3f, 0.05f).color(0xFFFFFF, COPPER).spin(2f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = span(time(), 0.3, 0.6) * (1 - span(time(), 0.9, end));
		lights.add(new CinemaPost.Light(face.add(nv.scale(0.5)), 2.5f, r(COPPER), g(COPPER), b(COPPER), 0.5f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		// 斧刃斜劈：刀光頭從一角劃到另一角
		float slash = easeOut(span(t, 0, 0.18));
		float sa = 1 - span(t, 0.18, 0.5);

		if (sa > 0.01f) {
			Vec3 a0 = on(-0.7, 0.6, 0.06);
			Vec3 a1 = lerp(a0, on(0.7, -0.6, 0.06), slash);
			line(d, TRAIL, a0, a1, 0.35f, old, sa * 0.8f);
			line(d, SPARK, a0, a1, 0.08f, 0xFFFFFF, sa);
		}

		// 新銅的光從刀痕往外擴散
		float spread = easeOut(span(t, 0.3, 0.8));
		float a = spread * (1 - span(t, 0.9, end));
		faceDisc(d, GLOW, face, 0.3f + spread * 0.9f, 0, COPPER, a * 0.5f);
		faceRing(d, t, 0.3, 0.9, face, 0.1f, 1.3f, 0xFFE0C0, 0.8f);

		// 舊鏽 / 舊蠟：一層斑駁的色光，從刀痕兩側褪掉
		float rest = 1 - span(t, 0.15, 0.6);
		faceDisc(d, GLOW, on(-0.25, 0.2, 0), 0.45f, 0, old, rest * 0.4f);
		faceDisc(d, GLOW, on(0.25, -0.2, 0), 0.45f, 0, old, rest * 0.4f);

		// 光澤掃過
		float sweep = span(t, 0.6, 1.2);

		if (sweep > 0 && sweep < 1) {
			double o = -0.9 + sweep * 1.8;
			line(d, SPARK, on(o - 0.4, -0.5, 0.02), on(o + 0.4, 0.5, 0.02), 0.14f, 0xFFF0E0, (float)Math.sin(sweep * Math.PI) * 0.8f);
		}
	}
}
