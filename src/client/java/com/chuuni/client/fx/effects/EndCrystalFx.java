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

/** 終界水晶：從天上落下一道紫粉色的光柱 → 水晶「喀」地落座，碎晶繞著它轉 */
public class EndCrystalFx extends MinorFx {
	private static final int PINK = 0xE080FF;

	private final Vec3 seat;

	public EndCrystalFx(FxPayload p) {
		super(p);
		seat = p.at().add(0, 1.0, 0);
		end = 2.0;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "crystal_place", c, 0.9f, 1f);
			tint(PINK, 1.5);
			smallName("end_crystal");
		}

		if (cue(0.25)) {
			pop(PINK, 0.4f);

			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI / 8;
				Particles.spawn(Kind.SHARD, seat.add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8), new Vec3(-Math.sin(a) * 2, 0.4, Math.cos(a) * 2), 1.0).size(0.07f, 0.03f)
					.color(0xFFFFFF, PINK).physics(0, 1f).attract(seat, 3f).spin(6f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float drop = easeIn(span(t, 0, 0.25));
		float a = 1 - span(t, 0.3, 1.2);
		double top = 12 - drop * 12;
		d.cylinder(Mode.GLOW, BEAM, seat.add(0, top - 0.5, 0), 0.25f, 14f, 16, 1, 0, 4, (float)(t * 4), 1f, r(PINK), g(PINK), b(PINK), a * 0.7f, 0);
		glow(d, seat, 1.0f, PINK, 0.6f * span(t, 0.25, 0.3) * (1 - span(t, 0.5, end)));
	}
}
