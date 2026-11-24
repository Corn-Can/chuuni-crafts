package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * 蜂巢 / 蜂箱・蜂王蜜宴。variant 0 = 裝蜂蜜、1 = 剪蜂巢片。
 * <ul>
 *   <li>裝蜂蜜：點到的面浮出金色蜂巢紋章，一道濃稠的金色蜜流從巢口螺旋流向玩家手中</li>
 *   <li>剪蜂巢：剪刀一刀「喀嚓」，蜂巢碎片金光四濺；被驚動的光之蜂群繞著巢亂飛</li>
 * </ul>
 */
public class BeehiveFx extends MinorFx {
	private static final int HONEY = 0xFFB020;
	private static final int BEE = 0xFFE040;
	private static final int BEES = 9;

	private final boolean shears;
	private final Vec3 hand;

	public BeehiveFx(FxPayload p) {
		super(p);
		shears = p.variant() == 1;
		var pl = Minecraft.getInstance().player;
		hand = pl != null && mine ? pl.getEyePosition().add(0, -0.5, 0) : face.add(nv.scale(1.5));
		end = 2.2;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, shears ? "comb_cut" : "honey_drip", c, 0.9f, 1f);
			tint(HONEY, 1.6);
			smallName("beehive");
		}

		if (shears && cue(0.08)) {
			pop(HONEY, 0.45f);

			for (int i = 0; i < 30; i++) {
				Particles.spawn(Kind.SHARD, face, nv.scale(Particles.rand(1f, 2.5f)).add(Particles.randomDir(1.3)).add(0, 1, 0), Particles.rand(0.8f, 1.3f))
					.size(0.07f, 0.05f).color(0xFFE080, HONEY).physics(7f, 1f).spin(8f);
			}
		}

		if (!shears && cue(0.2)) {
			pop(HONEY, 0.3f);
		}

		if (!shears && t > 0.2 && t < 1.2) {
			int nn = rate(60, dt);

			for (int i = 0; i < nn; i++) {
				Particles.spawn(Kind.GLOW, face.add(Particles.randomDir(0.15)), nv.scale(0.8).add(0, 0.5, 0), 0.9).size(0.08f, 0.03f).color(0xFFF0B0, HONEY)
					.physics(0, 1f).attract(hand, 7f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(face.add(nv.scale(0.4)), 2.5f, r(HONEY), g(HONEY), b(HONEY), 0.4f * (1 - span(time(), 0.8, end)), true));
	}

	/** 光之蜂：以固定種子的亂數繞巢飛行 */
	private Vec3 bee(int i, double t) {
		double a = t * (2.5 + i * 0.3) + i * 1.7;
		double rr = 0.9 + 0.3 * Math.sin(t * 3 + i);
		return c.add(Math.cos(a) * rr, 0.2 + Math.sin(t * 4.3 + i * 2.1) * 0.5, Math.sin(a) * rr);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		if (shears) {
			// 剪刀的一刀
			float k = easeOut(span(t, 0, 0.1));
			float a = 1 - span(t, 0.1, 0.4);

			if (a > 0.01f) {
				Vec3 a0 = on(-0.6, 0.25, 0.05);
				Vec3 a1 = lerp(a0, on(0.6, -0.25, 0.05), k);
				line(d, TRAIL, a0, a1, 0.28f, HONEY, a * 0.8f);
				line(d, SPARK, a0, a1, 0.06f, 0xFFFFFF, a);
			}

			// 被驚動的蜂群
			float swarm = span(t, 0.1, 0.3) * life;

			for (int i = 0; i < BEES; i++) {
				Vec3 b0 = bee(i, t);
				Vec3 b1 = bee(i, t - 0.06);
				line(d, SPARK, b1, b0, 0.05f, BEE, swarm * 0.8f);
				glow(d, b0, 0.12f, BEE, swarm);
			}
		}
		else {
			// 蜜流：巢口 → 手
			float flow = easeOut(span(t, 0.1, 0.5));
			float fa = (1 - span(t, 1.0, 1.5)) * life;
			Vec3 prev = face;
			int segs = 18;

			for (int s = 1; s <= segs; s++) {
				double k = (double)s / segs * flow;
				Vec3 at = lerp(face, hand, k).add(0, Math.sin(k * Math.PI) * 0.5, 0)
					.add(new Vec3(u.x, 0, u.z).scale(Math.sin(k * 12 + t * 6) * 0.08 * (1 - k)));
				line(d, TRAIL, prev, at, 0.14f, HONEY, fa * 0.8f);
				prev = at;
			}
		}

		// 蜂巢紋章
		float crest = easeOutBack(span(t, 0, 0.25)) * life;
		faceDisc(d, GLOW, face, 0.6f * crest, 0, HONEY, 0.35f * crest);
		faceRing(d, t, 0.05, 0.6, face, 0.1f, 1.2f, BEE, 0.8f);
	}
}
