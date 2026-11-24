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
 * 終界箱・虛空寶庫。
 * 箱蓋上方撕開一個星空漩渦（通往全世界共享的虛空倉庫）→ 終界之眼在漩渦中央睜開、閃一下 →
 * 青綠的虛空粒子螺旋湧出、紫色的粒子被反吸回去。
 */
public class EnderChestFx extends MinorFx {
	private static final int VOIDC = 0x40E0B0;
	private static final int PURPLE = 0x9050D0;

	private final Vec3 rift;

	public EnderChestFx(FxPayload p) {
		super(p);
		rift = new Vec3(c.x, pos.getY() + 1.15, c.z);
		end = 2.2;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "ender_open", c, 0.9f, 1f);
			tint(VOIDC, 1.8);
			smallName("ender_chest");

			if (mine) {
				com.chuuni.client.fx.Cinematic.peek(1.4);
			}
		}

		if (cue(0.35)) {
			pop(VOIDC, 0.4f);
		}

		if (t > 0.2 && t < 1.6) {
			int nn = rate(70, dt);

			for (int i = 0; i < nn; i++) {
				double a = t * 6 + Particles.RNG.nextDouble() * Math.PI * 2;
				Particles.spawn(Kind.GLOW, rift, new Vec3(Math.cos(a) * 1.4, Particles.rand(0.3f, 1.2f), Math.sin(a) * 1.4), 0.9).size(0.06f, 0.01f)
					.color(0xFFFFFF, VOIDC).physics(0, 1f);
			}

			int np = rate(40, dt);

			for (int i = 0; i < np; i++) {
				Vec3 from = rift.add(Particles.randomDir(1.4));
				Particles.spawn(Kind.GLOW, from, Vec3.ZERO, 0.5).size(0.05f, 0.01f).color(0xFFFFFF, PURPLE).physics(0, 0).attract(rift, 12f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = span(time(), 0, 0.35) * (1 - span(time(), 1.4, end));
		lights.add(new CinemaPost.Light(rift, 3f, r(VOIDC), g(VOIDC), b(VOIDC), 0.5f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);
		float open = easeOutBack(span(t, 0, 0.35)) * life;

		// 星空漩渦
		d.flat(Mode.GLOW, STARS, rift, 0.55f * open, (float)(t * 0.6), 0.8f, 1f, 0.95f, 0.6f * open);
		d.flat(Mode.GLOW, RING, rift.add(0, 0.02, 0), 0.75f * open, 0, r(PURPLE), g(PURPLE), b(PURPLE), 0.8f * open);

		// 眼睜開
		float eye = span(t, 0.3, 0.45) * (1 - span(t, 1.2, 1.8));
		d.billboard(Mode.GLOW, FLARE, rift.x, rift.y + 0.25, rift.z, 0.7f * eye, (float)t, 0.8f, 1f, 0.9f, eye);
		glow(d, rift.add(0, 0.25, 0), 0.6f, PURPLE, eye * 0.9f);

		// 虛空細柱
		float beam = (1 - span(t, 0.4, 1.6)) * open;
		d.cylinder(Mode.GLOW, BEAM, rift, 0.18f, 3f, 12, 1, 0, 3, (float)(t * 3), 0.3f, r(VOIDC), g(VOIDC), b(VOIDC), beam * 0.6f, 0);
		groundRing(d, t, 0.35, 1.0, rift, 0.3f, 2f, VOIDC, 0.8f, JAG);
	}
}
