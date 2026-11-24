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

/** 白乳恩賜：乳白的光滴從身下一滴滴落進桶裡，落點濺起一圈柔光（山羊奶偏米色、燉湯是褐紅色） */
public class MilkFx extends MinorFx {
	private final int col;

	public MilkFx(FxPayload p) {
		super(p);
		col = switch (Math.floorMod(p.variant(), 3)) {
			case 1 -> 0xFFF0D8;
			case 2 -> 0xC07050;
			default -> 0xFFFFFF;
		};
		end = 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "milk_pour", c, 0.9f, p.variant() == 2 ? 0.85f : 1f);
			tint(0xF0F0E8, 1.2);
			smallName("milk");
		}

		if (t < 0.8) {
			int n = rate(30, dt);
			Vec3 udder = body().add(0, -bodyHeight() * 0.25, 0);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.GLOW, udder.add(Particles.randomDir(0.15)), new Vec3(0, -1.5, 0), 0.5).size(0.06f, 0.04f).color(0xFFFFFF, col).physics(6f, 0.2f);
			}
		}

		if (cue(0.5)) {
			pop(col, 0.25f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 ground = new Vec3(body().x, Math.floor(body().y - bodyHeight() * 0.5) + 0.03, body().z);
		groundRing(d, t, 0.4, 1.2, ground, 0.2f, 1.4f, col, 0.8f, RING);
		glow(d, ground.add(0, 0.2, 0), 0.6f, col, 0.4f * span(t, 0.2, 0.4) * (1 - span(t, 0.8, end)));
	}
}
