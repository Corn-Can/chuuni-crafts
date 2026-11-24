package com.chuuni.client.fx.effects;

import java.util.ArrayList;
import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * TNT・爆炎序曲：點燃後的 3 秒「倒數」（爆炸本身交給原版）。
 * 地上展開爆炸範圍的紅色警戒陣，警戒帶旋轉；心跳般的紅色脈衝一下比一下急，引信火花不斷往上噴；
 * 最後半秒畫面四周壓暗、黑色集中線收緊——留給原版的爆炸去「砰」。
 */
public class TntFx extends MinorFx {
	private static final int RED = 0xFF2A20;
	private static final int WARN = 0xFFD020;

	private final List<Double> beats = new ArrayList<>();
	private final Vec3 ground;

	public TntFx(FxPayload p) {
		super(p);
		end = 3.0;
		ground = new Vec3(c.x, pos.getY() + 0.03, c.z);
		double tt = 0.15;

		for (int i = 0; tt < end - 0.2; i++) {
			beats.add(tt);
			tt += Math.max(0.16, 0.5 - i * 0.05);
		}
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "tnt_fuse", c, 1f, 1f);
			tint(RED, 3.2);
			smallName("tnt");
			pop(RED, 0.4f);
		}

		for (int i = 0; i < beats.size(); i++) {
			if (cue(beats.get(i))) {
				shake(0.04f + i * 0.012f);

				if (mine && i % 2 == 0) {
					PostState.split(0.2f + i * 0.04f, 0.5f, 120);
				}
			}
		}

		if (cue(end - 0.55) && mine) {
			PostState.darken(0.25f);
			PostState.lines(0.7f, true, 600, 0.35f);
		}

		if (cue(end - 0.05) && near) {
			PostState.darken(0);
		}

		// 引信火花
		int nn = rate(70, dt);
		Vec3 top = c.add(0, 0.55, 0);

		for (int i = 0; i < nn; i++) {
			Particles.spawn(Kind.SPARK, top, Particles.randomDir(1.5).add(0, Particles.rand(1.5f, 3f), 0), 0.5).size(0.03f, 0.006f)
				.color(0xFFFFFF, i % 3 == 0 ? RED : WARN).physics(4f, 1f);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(c.add(0, 0.6, 0), 3.5f, r(RED), g(RED), b(RED), 0.25f + 0.35f * pulse(time()), true));
	}

	/** 最近一次心跳的亮度 */
	private float pulse(double t) {
		float best = 0;

		for (double b : beats) {
			if (t >= b) {
				best = Math.max(best, 1 - span(t, b, b + 0.2));
			}
		}

		return best;
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.1, end - 0.15, end);
		float open = easeOutBack(span(t, 0, 0.4)) * life;
		float pl = pulse(t);

		// 警戒陣（大約是爆炸半徑）+ 旋轉的警戒帶
		dashRing(d, ground.add(0, 0.01, 0), 3.9f * open, 28, (float)(t * (0.3 + t * 0.4)), RED, WARN, 0.14f, (0.6f + pl * 0.4f) * open);
		d.annulus(Mode.GLOW, GLOW, ground, 0.3f, 3.7f * open, 48, 1, 0, r(RED), g(RED), b(RED), 0.05f * open, (0.12f + pl * 0.25f) * open);
		d.flat(Mode.GLOW, RING, ground.add(0, 0.02, 0), 4.0f * open, 0, r(RED), g(RED), b(RED), 0.5f * open);

		// 心跳脈衝：每一下從 TNT 往外推一圈
		for (double b : beats) {
			float k = span(t, b, b + 0.5);

			if (k > 0 && k < 1) {
				d.flat(Mode.GLOW, RING, ground.add(0, 0.03, 0), 0.6f + easeOut(k) * 3.4f, 0, r(RED), g(RED), b(RED), (1 - k) * 0.8f * life);
				d.billboard(Mode.GLOW, RING, c.x, c.y, c.z, 0.7f + k * 1.2f, 0, r(WARN), g(WARN), b(WARN), (1 - k) * 0.5f * life);
			}
		}

		glow(d, c, 1.5f + pl * 0.8f, RED, (0.3f + pl * 0.5f) * life);
	}
}
