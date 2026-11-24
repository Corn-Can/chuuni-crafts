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
 * 鞘翅起飛・天翼展開：背後張開兩片光之羽翼（各五根長羽），往後拖出光帶，自己有集中線。
 */
public class ElytraFx extends MinorFx {
	private static final int WING = 0xE0F0FF;
	private static final int TIP = 0x90C0FF;

	public ElytraFx(FxPayload p) {
		super(p);
		end = 1.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.anime(mine, "dash", "whoosh", body(), 0.6f, 1.05f);
			smallName("elytra");

			if (mine) {
				PostState.lines(0.5f, false, 700, 0.45f);
				CameraShake.fovPunch(0.05f);
			}
		}

		if (t < 0.9) {
			Vec3 at = body();
			int n = rate(40, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.FLARE, at.add(Particles.randomDir(0.8)), Vec3.ZERO, Particles.rand(0.3f, 0.6f)).size(0.05f, 0.005f).color(0xFFFFFF, WING)
					.physics(0, 0);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Entity e = target();

		if (e == null) {
			return;
		}

		float grow = easeOutBack(span(t, 0, 0.35));
		float a = 1 - span(t, 0.5, end);

		if (a <= 0.01f) {
			return;
		}

		Vec3 look = e.getLookAngle();
		Vec3 back = look.scale(-1);
		Vec3 right = look.cross(new Vec3(0, 1, 0));

		if (right.lengthSqr() < 1e-4) {
			right = new Vec3(1, 0, 0);
		}

		right = right.normalize();
		Vec3 up = right.cross(look).normalize();
		Vec3 shoulder = body().add(up.scale(bodyHeight() * 0.25)).add(back.scale(0.3));

		for (int s = -1; s <= 1; s += 2) {
			for (int i = 0; i < 5; i++) {
				double phi = -0.35 + i * 0.3;
				Vec3 dir = right.scale(s * Math.cos(phi)).add(up.scale(Math.sin(phi) * 0.9)).add(back.scale(0.45)).normalize();
				float len = (1.4f + (i == 2 ? 0.5f : 0) - Math.abs(i - 2) * 0.15f) * grow;
				Vec3 tip = shoulder.add(dir.scale(len));
				line(d, TRAIL, shoulder, tip, 0.35f, WING, 0.75f * a);
				line(d, SPARK, shoulder, tip, 0.08f, 0xFFFFFF, a);
				glow(d, tip, 0.25f, TIP, 0.7f * a);
			}
		}
	}
}
