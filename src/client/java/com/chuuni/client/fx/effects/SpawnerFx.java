package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 生怪磚・魂籠召喚（variant 1 = 試煉生怪磚，橘色）。
 * 生怪蛋從手中飛入籠中、被壓成一點 → 四周的魂魄被吸進籠子，籠子的十二條邊一條條燒亮 →「喀」地封存：
 * 衝擊閃光 + 籠下的召喚陣定格。
 */
public class SpawnerFx extends MinorFx {
	private static final double LOCK = 1.0;

	private final ItemStack egg;
	private final int soul;
	private final int pale;

	public SpawnerFx(FxPayload p) {
		super(p);
		egg = p.item();
		boolean trial = p.variant() == 1;
		soul = trial ? 0xFF9A30 : 0x60A0FF;
		pale = trial ? 0xFFE6C0 : 0xD8E8FF;
		end = 2.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "spawner_bind", c, 0.9f, p.variant() == 1 ? 1.08f : 1f);
			tint(soul, 2.0);
			smallName("spawner");
		}

		if (t > 0.1 && t < LOCK) {
			int nn = rate(70, dt);

			for (int i = 0; i < nn; i++) {
				Vec3 from = c.add(Particles.randomDir(1.8 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.GLOW, from, Vec3.ZERO, 0.6).size(0.08f, 0.02f).color(0xFFFFFF, soul).physics(0, 0).attract(c, 14f);
			}
		}

		if (cue(LOCK)) {
			pop(soul, 0.6f);
			Particles.spawn(Kind.HIT, c, Vec3.ZERO, 0.3).size(1.0f, 1.4f).color(0xFFFFFF, soul);
			sparks(c, 30, 4f, soul, 0.5f);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float k = Math.max(span(t, 0, LOCK) * 0.5f, t > LOCK ? 1 - span(t, LOCK, end) : 0);
		lights.add(new CinemaPost.Light(c, 3f, r(soul), g(soul), b(soul), 0.6f * k, true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (egg.isEmpty() || t > 0.6) {
			return;
		}

		float k = easeIn(span(t, 0, 0.55));
		d.item(egg, lerp(hit, c, k), 0.5f * (1 - k * 0.9f), (float)(t * 12), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		// 籠子的十二條邊一條條燒亮
		double x0 = pos.getX(), y0 = pos.getY(), z0 = pos.getZ();
		double[][] e = {
			{0, 0, 0, 1, 0, 0}, {1, 0, 0, 1, 0, 1}, {1, 0, 1, 0, 0, 1}, {0, 0, 1, 0, 0, 0},
			{0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {1, 0, 1, 1, 1, 1}, {0, 0, 1, 0, 1, 1},
			{0, 1, 0, 1, 1, 0}, {1, 1, 0, 1, 1, 1}, {1, 1, 1, 0, 1, 1}, {0, 1, 1, 0, 1, 0}};

		for (int i = 0; i < e.length; i++) {
			double[] q = e[i];
			float k = span(t, 0.15 + i * 0.06, 0.25 + i * 0.06);
			float flash = t > LOCK ? 1 - span(t, LOCK, LOCK + 0.4) : 0;
			float a = Math.min(1f, k * 0.7f + flash) * (1 - span(t, LOCK + 0.3, end));

			if (a > 0.01f) {
				double ex = x0 + q[0] + (q[3] - q[0]) * k, ey = y0 + q[1] + (q[4] - q[1]) * k, ez = z0 + q[2] + (q[5] - q[2]) * k;
				d.streak(Mode.GLOW, SPARK, x0 + q[0], y0 + q[1], z0 + q[2], ex, ey, ez, 0.07f, r(soul), g(soul), b(soul), a);
			}
		}

		// 籠心的魂
		float core = span(t, 0.2, LOCK) * (1 - span(t, LOCK + 0.2, end));
		glow(d, c, 0.4f + core * 0.8f, soul, core * 0.8f);
		d.billboard(Mode.GLOW, RING, c.x, c.y, c.z, 1.6f - core, (float)t, r(pale), g(pale), b(pale), core * 0.5f);

		// 召喚陣
		Vec3 ground = new Vec3(c.x, pos.getY() + 0.02, c.z);
		float circle = easeOutBack(span(t, 0, 0.4)) * life;
		d.flat(Mode.GLOW, SEAL, ground, 1.7f * circle, (float)(t < LOCK ? t * 2 : LOCK * 2 + (t - LOCK) * 0.2), r(soul), g(soul), b(soul), 0.6f * circle);
		groundRing(d, t, LOCK, LOCK + 0.7, ground, 0.4f, 3f, pale, 0.9f, JAG);
	}
}
