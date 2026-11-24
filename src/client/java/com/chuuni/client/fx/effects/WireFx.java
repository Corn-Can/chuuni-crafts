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

/** 紅石線切換：紅色電流從中心往四方爬一下，劈啪幾點火花 */
public class WireFx extends MicroFx {
	private static final int RED = 0xFF2A1A;

	public WireFx(FxPayload p) {
		super(p);
		end = 0.5;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("wire_toggle", 1f);
			burst(Kind.SPARK, c.add(0, -0.45, 0), new Vec3(0, 1, 0), 6, 1.2f, 0.8f, 0xFFFFFF, RED, 0.02f, 3f, 0.3);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 ctr = new Vec3(c.x, pos.getY() + 0.05, c.z);
		float k = easeOut(span(t, 0, 0.15));
		float a = 1 - span(t, 0.15, 0.45);

		for (int i = 0; i < 4; i++) {
			double ang = i * Math.PI / 2;
			line(d, SPARK, ctr, ctr.add(Math.cos(ang) * 0.5 * k, 0, Math.sin(ang) * 0.5 * k), 0.05f, RED, a);
		}

		glow(d, ctr, 0.35f, RED, a * 0.6f);
	}
}
