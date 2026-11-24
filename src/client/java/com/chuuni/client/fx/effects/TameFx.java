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

/** 主從之契：一條金色的光之絲從玩家手中牽向牠 → 兩下心跳般的脈動 → 牠身上暖光一閃，光點往上飄 */
public class TameFx extends MinorFx {
	private static final int GOLD = 0xFFD080;
	private static final int WARM = 0xFFA0A0;

	public TameFx(FxPayload p) {
		super(p);
		end = 2.2;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "tame_bond", c, 0.9f, 1f);
			tint(GOLD, 1.6);
			smallName("tame");
		}

		for (double beat : new double[]{0.35, 0.6}) {
			if (cue(beat)) {
				pop(WARM, 0.25f);
				sparks(body(), 10, 2f, WARM, 0.4f);
			}
		}

		if (t > 0.6 && t < 1.8) {
			int n = rate(30, dt);
			Vec3 b = body();
			float w = bodyWidth();

			for (int i = 0; i < n; i++) {
				Vec3 at = b.add(Particles.rand(-w, w), Particles.rand(-0.4f, 0.4f), Particles.rand(-w, w));
				Particles.spawn(Kind.GLOW, at, new Vec3(0, 0.8, 0), 1.0).size(0.06f, 0.02f).color(0xFFFFFF, Particles.RNG.nextBoolean() ? GOLD : WARM).physics(0, 0.5f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(body(), 2.5f, r(GOLD), g(GOLD), b(GOLD), 0.4f * (1 - span(time(), 0.8, end)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 from = hand();
		Vec3 to = body();
		float k = easeOut(span(t, 0, 0.3));
		float a = 1 - span(t, 0.7, 1.4);
		float pulse = 1 + Math.max(0, 1 - span(t, 0.35, 0.5) * 1) * 0 + (t > 0.35 && t < 0.75 ? (float)Math.abs(Math.sin((t - 0.35) * Math.PI / 0.25)) : 0);
		Vec3 prev = from;

		for (int i = 1; i <= 16; i++) {
			double f = (double)i / 16 * k;
			Vec3 at = lerp(from, to, f).add(0, Math.sin(f * Math.PI) * 0.3, 0);
			line(d, TRAIL, prev, at, 0.06f * pulse, GOLD, a * 0.8f);
			prev = at;
		}

		glow(d, to, 0.8f * bodyHeight(), WARM, 0.4f * span(t, 0.3, 0.4) * (1 - span(t, 0.8, 1.8)));
	}
}
