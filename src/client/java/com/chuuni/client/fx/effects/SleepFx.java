package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 睡到天亮・黎明降臨：溫暖的晨光從床上擴散開來，光點緩緩升起，腳下一圈慢慢轉的法陣。
 */
public class SleepFx extends MinorFx {
	private static final int DAWN = 0xFFD8A0;
	private static final int SKY = 0xFFF4E0;

	public SleepFx(FxPayload p) {
		super(p);
		end = 3.0;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = body();

		if (cue(0.0)) {
			Sfx.play(mine, "dream_dawn", at, 0.7f, 1f);
			smallName("sleep");
			tint(DAWN, 1.6);

			if (mine) {
				PostState.flash(SKY, 0.35f, 700, 0.05f);
			}
		}

		if (t < 2.2) {
			int n = rate(30, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = Particles.RNG.nextDouble() * 2.5;
				Vec3 o = at.add(Math.cos(a) * r, -0.2, Math.sin(a) * r);
				Particles.spawn(Kind.GLOW, o, new Vec3(0, Particles.rand(0.4f, 1f), 0), Particles.rand(1.2f, 2f)).size(0.07f, 0.01f).color(SKY, DAWN)
					.physics(-0.1f, 0.6f).fadeIn(0.3f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = body();
		float a = fade(t, 0.3, end - 1, end);
		d.flat(Mode.GLOW, SEAL, at.add(0, -0.15, 0), 1.6f, (float)(t * 0.3), r(DAWN), g(DAWN), b(DAWN), 0.5f * a);
		groundRing(d, t, 0, 2.2, at.add(0, -0.12, 0), 0.5f, 6f, DAWN, 0.7f, RING);
		glow(d, at, 2.5f, SKY, 0.45f * a);
	}
}
