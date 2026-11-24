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
 * 銅魔像雕像・銅像覺醒（power = 新姿勢，每個姿勢的光弧方向與顏色略有不同）。
 * 兩道銅色的動作光弧繞著雕像甩過（像擺出新姿勢的殘影）→「鏘」地定格：雙眼一閃、銅屑火花，
 * 腳下的齒輪陣轉過四分之一圈。
 */
public class CopperGolemFx extends MinorFx {
	private static final int COPPER = 0xE07A40;
	private static final int PATINA = 0x5FC8A8;
	private static final int EYE = 0xFFE6A0;
	private static final double LAND = 0.32;

	private final int pose;

	public CopperGolemFx(FxPayload p) {
		super(p);
		pose = Math.floorMod(p.power(), 4);
		end = 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "golem_pose", c, 0.9f, 0.95f + pose * 0.05f);
			tint(COPPER, 1.2);
			smallName("copper_golem");
		}

		if (cue(LAND)) {
			pop(COPPER, 0.4f);
			sparks(c.add(0, -0.3, 0), 20, 3f, COPPER, 0.4f);

			for (int i = 0; i < 10; i++) {
				Particles.spawn(Kind.SHARD, c, Particles.randomDir(1.5).add(0, 1, 0), 0.8).size(0.04f, 0.03f).color(PATINA, PATINA).physics(7f, 1f).spin(8f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(c, 2.5f, r(COPPER), g(COPPER), b(COPPER), 0.45f * (1 - span(time(), LAND, end)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.03, end - 0.4, end);

		// 動作光弧
		float swing = easeOut(span(t, 0, LAND));
		float sa = 1 - span(t, LAND, LAND + 0.4);
		double base = pose * Math.PI / 2;
		double dirSign = pose % 2 == 0 ? 1 : -1;

		for (int k = 0; k < 2; k++) {
			double y = c.y - 0.2 + k * 0.45;
			double rr = 0.75 - k * 0.12;
			Vec3 prev = null;
			int segs = 16;

			for (int s = 0; s <= segs; s++) {
				double f = (double)s / segs;

				if (f > swing) {
					break;
				}

				double a = base + dirSign * (f * Math.PI * 1.3 + k * 0.6);
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, y + f * 0.15, c.z + Math.sin(a) * rr);

				if (prev != null) {
					float w = (float)(0.04 + 0.2 * f);
					line(d, TRAIL, prev, at, w, k == 0 ? COPPER : PATINA, sa * 0.8f);
				}

				prev = at;
			}
		}

		// 雙眼一閃
		float eye = span(t, LAND, LAND + 0.05) * (1 - span(t, LAND + 0.1, 1.2));

		for (int i = -1; i <= 1; i += 2) {
			Vec3 e = on(c.add(nv.scale(0.3)), i * 0.1, 0.2, 0);
			d.billboard(Mode.GLOW, FLARE, e.x, e.y, e.z, 0.35f * eye, (float)t * 2, r(EYE), g(EYE), b(EYE), eye);
		}

		// 齒輪陣轉四分之一圈
		float turn = easeOutBack(span(t, LAND - 0.1, LAND + 0.3));
		groundRing(d, t, LAND, LAND + 0.6, new Vec3(c.x, pos.getY() + 0.03, c.z), 0.3f, 1.8f, COPPER, 0.8f, JAG);
	}
}
