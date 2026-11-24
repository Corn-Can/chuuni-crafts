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

/** 拴繩：一道光沿著繩子從手（或柵欄）甩向生物 / 柵欄結，打結處一閃 */
public class LeashFx extends MicroFx {
	private static final int ROPE = 0xE8C890;

	private final boolean fence;

	public LeashFx(FxPayload p) {
		super(p);
		fence = p.variant() == 1;
		end = 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("leash_tie", 1f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 from = hand();
		Vec3 to = fence ? c.add(0, 0.2, 0) : body();
		float k = easeOut(span(t, 0, 0.18));
		float a = 1 - span(t, 0.18, 0.55);
		Vec3 prev = from;

		for (int i = 1; i <= 12; i++) {
			double f = (double)i / 12 * k;
			Vec3 at = lerp(from, to, f).add(0, -Math.sin(f * Math.PI) * 0.25, 0);
			line(d, SPARK, prev, at, 0.035f, ROPE, a);
			prev = at;
		}

		glow(d, to, 0.4f, ROPE, span(t, 0.15, 0.2) * a);
	}
}
