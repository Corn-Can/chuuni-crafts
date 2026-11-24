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
 * 蠟燭・燭火點靈。variant：0 點燃（power = 根數）/ 1 熄滅 / 2 點燃蠟燭蛋糕。
 * <ul>
 *   <li>點燃：火星從點擊處射向燭芯 →「噗」地立起一朵火之精靈，幾顆燭靈（蠟燭本身的顏色）繞著火苗盤旋上升，地上暖光一圈圈擴散</li>
 *   <li>熄滅：一縷煙螺旋升起，精靈的光點跟著飄走、散去</li>
 * </ul>
 */
public class CandleFx extends MinorFx {
	private static final int FLAME = 0xFFA040;

	private final int variant;
	private final int count;
	private final int wax;
	private final Vec3 wick;

	public CandleFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 3);
		count = Math.max(1, Math.min(4, p.power()));
		wax = variant == 2 ? 0xFF90B0 : mapColor(0xF0E0C0);
		wick = new Vec3(c.x, pos.getY() + (variant == 2 ? 0.95 : 0.5), c.z);
		end = variant == 1 ? 1.6 : 2.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (variant == 1) {
			if (cue(0.0)) {
				Sfx.play(mine, "candle_snuff", c, 0.8f, 1f + count * 0.03f);
			}

			if (t < 1.0) {
				int nn = rate(40, dt);

				for (int i = 0; i < nn; i++) {
					double a = t * 9;
					Vec3 at = wick.add(Math.cos(a) * 0.08, t * 0.5, Math.sin(a) * 0.08);
					Particles.spawn(Kind.SMOKE, at, new Vec3(Math.cos(a) * 0.2, 0.6, Math.sin(a) * 0.2), 1.2).size(0.08f, 0.3f).color(0xC8C0B8, 0x605850)
						.alpha(0.5f).physics(-0.2f, 1f);
				}
			}

			return;
		}

		if (cue(0.0)) {
			Sfx.play(mine, "candle_light", c, 0.9f, 1.1f - count * 0.04f);
			tint(FLAME, 1.4);
			smallName("candle");
		}

		if (cue(0.15)) {
			pop(FLAME, 0.3f);

			for (int i = 0; i < 18; i++) {
				Particles.spawn(Kind.FLAME, wick, Particles.randomDir(0.5).add(0, Particles.rand(1f, 2.2f), 0), 0.5).size(0.12f, 0.02f).color(0xFFF0C0, FLAME)
					.physics(-1f, 2f);
			}

			sparks(wick, 14, 2.5f, FLAME, 0.4f);
		}

		// 燭靈盤旋上升
		if (t > 0.2 && t < 1.8) {
			int nn = rate(20 + count * 10, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = wick.add(Math.cos(a) * 0.45, Particles.rand(-0.1f, 0.2f), Math.sin(a) * 0.45);
				Particles.spawn(Kind.GLOW, at, new Vec3(-Math.sin(a) * 0.8, 0.6, Math.cos(a) * 0.8), 1.0).size(0.07f, 0.01f).color(0xFFFFFF, wax)
					.physics(-0.3f, 0.5f).attract(wick.add(0, 1.2, 0), 1.5f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();

		if (variant == 1) {
			lights.add(new CinemaPost.Light(wick, 2f, r(FLAME), g(FLAME), b(FLAME), 0.4f * (1 - span(t, 0, 0.4)), true));
			return;
		}

		float k = span(t, 0.1, 0.3) * (1 - span(t, 1.2, end));
		lights.add(new CinemaPost.Light(wick, 2.5f + count * 0.4f, r(FLAME), g(FLAME), b(FLAME), 0.4f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		if (variant == 1) {
			float a = 1 - span(t, 0, 0.5);
			glow(d, wick, 0.6f * a, FLAME, a);

			for (int k = 0; k < 2; k++) {
				double ph = span(t, k * 0.15, 0.5 + k * 0.15);

				if (ph > 0 && ph < 1) {
					d.flat(Mode.GLOW, RING, new Vec3(c.x, pos.getY() + 0.02, c.z), (float)(1.2 * (1 - ph)) + 0.1f, 0, r(FLAME), g(FLAME), b(FLAME),
						(float)ph * 0.5f);
				}
			}

			return;
		}

		float life = fade(t, 0.05, end - 0.5, end);

		// 火星射向燭芯
		float fly = easeIn(span(t, 0, 0.15));

		if (t < 0.25) {
			Vec3 head = lerp(hit, wick, fly);
			line(d, SPARK, lerp(hit, wick, Math.max(0, fly - 0.3)), head, 0.06f, FLAME, 1 - span(t, 0.15, 0.25));
		}

		// 火之精靈：火苗上方的光暈與光芒
		float spirit = easeOutBack(span(t, 0.15, 0.4)) * life;
		glow(d, wick.add(0, 0.12, 0), 0.55f * spirit, FLAME, 0.45f * spirit);
		d.billboard(Mode.GLOW, FLARE, wick.x, wick.y + 0.12, wick.z, 0.45f * spirit, (float)(t * 0.8), 1f, 0.95f, 0.8f, 0.4f * spirit);
		d.billboard(Mode.GLOW, RING, wick.x, wick.y + 0.12, wick.z, 0.5f * spirit, 0, r(wax), g(wax), b(wax), 0.5f * spirit);

		// 地上的暖光環
		Vec3 ground = new Vec3(c.x, pos.getY() + 0.02, c.z);

		for (int k = 0; k < count; k++) {
			groundRing(d, t, 0.15 + k * 0.18, 1.1 + k * 0.18, ground, 0.2f, 1.6f + count * 0.3f, k % 2 == 0 ? FLAME : wax, 0.7f, RING);
		}

		glow(d, ground, (0.8f + count * 0.15f) * spirit, FLAME, 0.25f * spirit);
	}
}
