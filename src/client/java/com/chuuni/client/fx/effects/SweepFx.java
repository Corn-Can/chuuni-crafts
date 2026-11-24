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
 * 橫掃・橫掃千軍：玩家身前畫出一道 160° 的弧形刀光，掃過的地方留下殘光。
 */
public class SweepFx extends MinorFx {
	private static final int EDGE = 0xC0E8FF;

	private final Vec3 center;
	private final double yaw;

	public SweepFx(FxPayload p) {
		super(p);
		end = 0.9;
		Entity a = actor();
		center = a == null ? p.at() : a.position().add(0, a.getBbHeight() * 0.55, 0);
		yaw = Math.toRadians(a == null ? 0 : a.getYRot());
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "plasma_cut", center, 0.5f, 1.1f);

			if (mine) {
				smallName("sweep");
				CameraShake.fovPunch(0.03f);
			}
		}
	}

	/** 弧上角度 th（0 = 正前方）的點 */
	private Vec3 arc(double th, double radius) {
		double a = yaw + th;
		return center.add(-Math.sin(a) * radius, 0, Math.cos(a) * radius);
	}

	@Override
	protected void render(Draw d, double t) {
		float grow = easeOut(span(t, 0, 0.12));
		float a = 1 - span(t, 0.12, 0.7);

		if (a <= 0.01f) {
			return;
		}

		double half = Math.toRadians(80);
		int seg = 28;
		double from = half;
		double to = half - 2 * half * grow;

		for (int i = 0; i < seg; i++) {
			double a0 = from + (to - from) * i / seg;
			// 每段往後多畫一點，接縫才不會斷開
			double a1 = from + (to - from) * (i + 1.8) / seg;
			// 刀光越靠近刀尖越亮
			float k = (i + 1f) / seg;
			line(d, TRAIL, arc(a0, 2.2), arc(a1, 2.2), 0.5f * (0.4f + 0.6f * k), EDGE, a * (0.3f + 0.7f * k));
			line(d, SPARK, arc(a0, 2.2), arc(a1, 2.2), 0.12f, 0xFFFFFF, a * k);
		}

		Vec3 tip = arc(to, 2.2);
		glow(d, tip, 0.6f, EDGE, a);
	}
}
