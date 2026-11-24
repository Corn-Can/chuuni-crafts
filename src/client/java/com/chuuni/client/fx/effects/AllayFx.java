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

/** 悅靈之約：藍色的光點繞著悅靈轉一圈，物品從手中被輕輕「接」過去（拿回時反過來飛向手） */
public class AllayFx extends MinorFx {
	private static final int SKY = 0x80D8FF;

	private final ItemStack held;
	private final boolean back;

	public AllayFx(FxPayload p) {
		super(p);
		held = p.item();
		back = p.variant() == 1;
		end = 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "allay_chime", c, 0.9f, back ? 0.9f : 1f);
			tint(SKY, 1.3);
			smallName("allay");
		}

		if (t < 1.2) {
			int n = rate(50, dt);
			Vec3 b = body();

			for (int i = 0; i < n; i++) {
				double a = t * 9 + i * 2.1;
				Particles.spawn(Kind.FLARE, b.add(Math.cos(a) * 0.5, Math.sin(t * 5) * 0.2, Math.sin(a) * 0.5), new Vec3(0, 0.3, 0), 0.5).size(0.05f, 0.01f)
					.color(0xFFFFFF, SKY).physics(0, 1f).spin(3f);
			}
		}

		if (cue(0.4)) {
			pop(SKY, 0.25f);
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (held.isEmpty() || t > 0.45) {
			return;
		}

		float k = easeOut(span(t, 0, 0.4));
		Vec3 from = back ? body() : hand();
		Vec3 to = back ? hand() : body();
		d.item(held, lerp(from, to, k).add(0, Math.sin(k * Math.PI) * 0.3, 0), 0.4f, (float)(t * 10), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		glow(d, body(), 0.8f, SKY, 0.4f * (1 - span(t, 0.4, end)));
	}
}
