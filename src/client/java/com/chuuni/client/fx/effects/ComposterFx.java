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
 * 堆肥桶・輪迴新生。variant 0 = 投入（power = 層數 1~7）、1 = 取出骨粉。
 * <ul>
 *   <li>投入：葉片碎片繞著桶口盤旋落入，桶口一圈七顆「輪迴之珠」依層數亮起；滿 7 層時出招式名</li>
 *   <li>取出：三股生命之光螺旋沖起，藤蔓狀的光線往外捲曲生長，骨粉的白色星光炸開</li>
 * </ul>
 */
public class ComposterFx extends MinorFx {
	private static final int LIFE = 0x78D850;
	private static final int BONE = 0xF4F0E0;

	private final boolean harvest;
	private final int level;
	private final Vec3 mouth;

	public ComposterFx(FxPayload p) {
		super(p);
		harvest = p.variant() == 1;
		level = Math.max(1, Math.min(8, p.power()));
		mouth = new Vec3(c.x, pos.getY() + 0.95, c.z);
		end = harvest ? 2.4 : 1.3;
	}

	@Override
	protected void update(double t, double dt) {
		if (!harvest) {
			if (cue(0.0)) {
				Sfx.play(mine, "compost_add", c, 0.8f, 0.85f + level * 0.04f);

				for (int i = 0; i < 18; i++) {
					double a = i * 0.9;
					Vec3 from = mouth.add(Math.cos(a) * 0.9, 0.5 + i * 0.03, Math.sin(a) * 0.9);
					Particles.spawn(Kind.SHARD, from, new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(1.5), 0.6).size(0.06f, 0.03f).color(0xA0F070, LIFE)
						.physics(0, 0).attract(mouth.add(0, -0.3, 0), 10f).spin(6f);
				}

				if (level >= 7) {
					tint(LIFE, 1.0);
					smallName("composter");
					pop(LIFE, 0.3f);
				}
			}

			return;
		}

		if (cue(0.0)) {
			Sfx.play(mine, "compost_harvest", c, 0.9f, 1f);
			tint(LIFE, 1.8);
			smallName("composter");
			pop(LIFE, 0.45f);

			for (int i = 0; i < 40; i++) {
				Particles.spawn(Kind.FLARE, mouth, Particles.randomDir(Particles.rand(1f, 2.5f)).add(0, 1.5, 0), Particles.rand(0.6f, 1.2f)).size(0.07f, 0.02f)
					.color(0xFFFFFF, BONE).physics(3f, 1.2f).spin(3f);
			}
		}

		// 三股生命之光螺旋上升
		if (t < 1.4) {
			int nn = rate(120, dt);

			for (int i = 0; i < nn; i++) {
				int strand = Particles.RNG.nextInt(3);
				double a = t * 7 + strand * Math.PI * 2 / 3;
				Vec3 at = mouth.add(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35);
				Particles.spawn(Kind.GLOW, at, new Vec3(-Math.sin(a) * 1.2, 2.2, Math.cos(a) * 1.2), 0.8).size(0.07f, 0.01f).color(0xFFFFFF, LIFE).physics(0, 0.4f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = harvest ? 0.55f * (1 - span(time(), 0.6, end)) : 0.25f * (1 - span(time(), 0, end));
		lights.add(new CinemaPost.Light(mouth, 2.5f, r(LIFE), g(LIFE), b(LIFE), k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.4, end);

		if (!harvest) {
			// 七顆輪迴之珠
			for (int i = 0; i < 7; i++) {
				double a = i * Math.PI * 2 / 7 + t * 0.6;
				Vec3 at = mouth.add(Math.cos(a) * 0.62, 0.1, Math.sin(a) * 0.62);
				boolean lit = i < level;
				boolean fresh = i == level - 1;
				float pulse = fresh ? 1 + (1 - span(t, 0.1, 0.5)) * 1.5f : 1;
				int col = lit ? LIFE : 0x405040;
				glow(d, at, 0.13f * pulse, col, (lit ? 0.9f : 0.4f) * life);
			}

			d.flat(Mode.GLOW, RING, mouth.add(0, 0.05, 0), 0.62f, 0, r(LIFE), g(LIFE), b(LIFE), 0.5f * life);
			groundRing(d, t, 0.1, 0.6, mouth, 0.1f, 0.9f, LIFE, 0.7f, RING);
			return;
		}

		// 藤蔓：六條往外捲曲生長的光線
		float grow = easeOut(span(t, 0.1, 0.9));

		for (int i = 0; i < 6; i++) {
			double a0 = i * Math.PI / 3;
			Vec3 prev = mouth;
			int segs = 14;

			for (int s = 1; s <= segs * grow; s++) {
				double k = (double)s / segs;
				double a = a0 + k * 2.2;
				double rr = 0.2 + k * 1.3;
				Vec3 at = mouth.add(Math.cos(a) * rr, k * 1.2 - k * k * 0.6, Math.sin(a) * rr);
				line(d, SPARK, prev, at, 0.05f * (float)(1.2 - k), LIFE, life * 0.9f);
				prev = at;
			}

			if (grow > 0.2) {
				glow(d, prev, 0.18f, BONE, life * 0.8f);
			}
		}

		groundRing(d, t, 0, 0.7, new Vec3(c.x, pos.getY() + 0.03, c.z), 0.3f, 2.8f, BONE, 0.8f, JAG);
	}
}
