package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * 治療殭屍村民。variant 0 = 開始淨化（淨化儀式）：金光從頭頂罩下、紫色的虛弱之氣從身上被逼出來；
 * variant 1 = 變回村民（人性回歸）：腐綠的碎片整片剝落、一道金色光柱沖天、暖光爆開。
 */
public class CureFx extends MinorFx {
	private static final int HOLY = 0xFFE6A0;
	private static final int ROT = 0x60A060;
	private static final int WEAK = 0x8060A0;

	private final boolean done;

	public CureFx(FxPayload p) {
		super(p);
		done = p.variant() == 1;
		end = done ? 2.6 : 2.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, done ? "cure_finish" : "cure_start", c, 1f, 1f);
			tint(HOLY, 2.0);
			smallName(done ? "cure_end" : "cure_start");
		}

		if (!done && t > 0.3 && t < 1.8) {
			int n = rate(40, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.SMOKE, body().add(Particles.randomDir(0.3)), Particles.randomDir(1).add(0, 0.6, 0), 1.0).size(0.12f, 0.4f).color(WEAK, 0x302040)
					.alpha(0.5f).physics(-0.2f, 1.5f);
			}
		}

		if (done && cue(0.4)) {
			pop(HOLY, 0.6f);

			for (int i = 0; i < 40; i++) {
				Particles.spawn(Kind.SHARD, body().add(Particles.randomDir(0.4)), Particles.randomDir(Particles.rand(1f, 3f)).add(0, 1, 0), 1.0).size(0.07f, 0.04f)
					.color(ROT, 0x305030).physics(6f, 1f).spin(8f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(body().add(0, 1, 0), 3f, r(HOLY), g(HOLY), b(HOLY), 0.5f * (1 - span(time(), 1.2, end)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 feet = body().add(0, -bodyHeight() * 0.5, 0);
		float life = fade(t, 0.05, end - 0.5, end);

		if (!done) {
			// 金光從頭頂往下罩
			float k = easeOut(span(t, 0, 0.6));
			d.cylinder(Mode.GLOW, BEAM, feet.add(0, 3.5 - k * 3.5, 0), 0.6f, 3.5f * k + 0.1f, 20, 1, 0, 2, (float)(t * 2), 0.8f, r(HOLY), g(HOLY), b(HOLY), 0.35f * life, 0);
			return;
		}

		float pillar = span(t, 0.4, 0.5) * (1 - span(t, 0.6, 2.2));
		d.cylinder(Mode.GLOW, BEAM, feet, 0.5f, 20f, 20, 1, 0, 4, (float)(t * 3), 1.2f, r(HOLY), g(HOLY), b(HOLY), pillar * 0.6f, 0);
		groundRing(d, t, 0.4, 1.2, feet.add(0, 0.03, 0), 0.3f, 3f, HOLY, 0.9f, RING);
		glow(d, body(), 1.5f, HOLY, 0.6f * span(t, 0.4, 0.45) * (1 - span(t, 0.5, 1.5)));
	}
}
