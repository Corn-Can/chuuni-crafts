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
 * 激流・激流迴天：三股水流沿著衝刺方向螺旋纏繞全身，衝出去時炸開水花。
 */
public class RiptideFx extends MinorFx {
	private static final int WATER = 0x40C0FF;
	private static final int FOAM = 0xD0F8FF;

	public RiptideFx(FxPayload p) {
		super(p);
		end = 1.3;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = body();

		if (cue(0.0)) {
			Sfx.play(mine, "smoke_swirl", at, 0.8f, 1f);
			Sfx.play(mine, "bucket_catch", at, 0.6f, 0.9f);
			smallName("riptide");

			if (mine) {
				pop(WATER, 0.4f);
				PostState.lines(0.6f, false, 800, 0.4f);
			}

			for (int i = 0; i < 40; i++) {
				Particles.spawn(Kind.GLOW, at, Particles.randomDir(Particles.rand(2f, 5f)), Particles.rand(0.4f, 0.8f)).size(0.08f, 0.01f)
					.color(0xFFFFFF, i % 2 == 0 ? WATER : FOAM).physics(6f, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Entity e = target();

		if (e == null) {
			return;
		}

		float a = fade(t, 0.08, 0.7, end);

		if (a <= 0.01f) {
			return;
		}

		Vec3 axis = e.getLookAngle().normalize();
		Vec3 u = axis.cross(new Vec3(0, 1, 0));

		if (u.lengthSqr() < 1e-4) {
			u = new Vec3(1, 0, 0);
		}

		u = u.normalize();
		Vec3 v = u.cross(axis).normalize();
		Vec3 center = body();

		for (int s = 0; s < 3; s++) {
			Vec3 prev = null;

			for (int i = 0; i <= 24; i++) {
				double z = -1.6 + i * 0.12;
				double th = z * 5 + t * 22 + s * Math.PI * 2 / 3;
				double r = 0.75 - Math.abs(z) * 0.15;
				Vec3 pt = center.add(axis.scale(z)).add(u.scale(Math.cos(th) * r)).add(v.scale(Math.sin(th) * r));

				if (prev != null) {
					float k = i / 24f;
					line(d, TRAIL, prev, pt, 0.22f, WATER, a * (0.3f + 0.7f * k));
					line(d, SPARK, prev, pt, 0.05f, FOAM, a * k);
				}

				prev = pt;
			}
		}

		glow(d, center, 1.2f, WATER, 0.4f * a);
	}
}
