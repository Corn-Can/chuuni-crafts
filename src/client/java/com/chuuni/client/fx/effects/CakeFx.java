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
 * 蛋糕・甘味祝宴（power = 已吃幾口，7 = 吃完）。
 * 咬下去的瞬間一圈粉色的光環「啵」地彈開，彩色紙花噴起來慢慢飄落，奶油色的星光閃爍；
 * 吃掉最後一口時紙花加倍、多一道金色光環。
 */
public class CakeFx extends MinorFx {
	private static final int PINK = 0xFF8AB8;
	private static final int CREAM = 0xFFF0D0;
	private static final int[] CONFETTI = {0xFF6A9A, 0xFFD050, 0x70D8FF, 0x90F080, 0xD090FF, 0xFFFFFF};

	private final boolean last;
	private final Vec3 top;

	public CakeFx(FxPayload p) {
		super(p);
		last = p.power() >= 7;
		top = new Vec3(c.x, pos.getY() + 0.55, c.z);
		end = last ? 2.4 : 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "cake_bite", c, 0.9f, last ? 0.9f : 1f + p.power() * 0.02f);
			tint(PINK, 1.2);
			smallName("cake");
			pop(PINK, last ? 0.5f : 0.25f);

			int n = last ? 70 : 34;

			for (int i = 0; i < n; i++) {
				Vec3 vel = Particles.randomDir(Particles.rand(0.6f, 1.6f)).add(0, Particles.rand(2f, 3.5f), 0);
				Particles.spawn(Kind.SHARD, top, vel, Particles.rand(1.2f, 1.8f)).size(0.06f, 0.05f).color(CONFETTI[i % CONFETTI.length], CONFETTI[(i + 2) % CONFETTI.length])
					.physics(2.5f, 2.2f).spin(12f);
			}

			for (int i = 0; i < 16; i++) {
				Particles.spawn(Kind.FLARE, top.add(Particles.randomDir(0.4)), Particles.randomDir(0.6).add(0, 0.6, 0), 0.8).size(0.08f, 0.02f).color(0xFFFFFF, CREAM)
					.physics(0, 1f).spin(3f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(top, 2f, r(PINK), g(PINK), b(PINK), 0.35f * (1 - span(time(), 0, 1.2)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 ground = new Vec3(c.x, pos.getY() + 0.02, c.z);
		groundRing(d, t, 0, 0.5, top, 0.2f, 1.2f, PINK, 0.9f, RING);
		groundRing(d, t, 0.05, 0.6, top, 0.1f, 0.8f, CREAM, 0.8f, JAG);
		d.billboard(Mode.GLOW, RING, top.x, top.y + 0.2, top.z, 0.3f + easeOut(span(t, 0, 0.4)) * 0.8f, 0, r(PINK), g(PINK), b(PINK), 1 - span(t, 0, 0.4));

		if (last) {
			groundRing(d, t, 0.15, 1.0, ground, 0.4f, 3.2f, 0xFFD070, 0.9f, RING);
		}

		float life = fade(t, 0.05, end - 0.5, end);
		glow(d, top, 0.8f, CREAM, 0.4f * (1 - span(t, 0, 0.8)));
	}
}
