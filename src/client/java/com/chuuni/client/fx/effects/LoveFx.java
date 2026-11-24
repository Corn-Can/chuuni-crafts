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

/** 餵食發情：粉色的柔光往上冒一下，幾顆光點像心跳一樣跳兩下（原版的愛心粒子照常） */
public class LoveFx extends MicroFx {
	private static final int PINK = 0xFF8AB0;

	public LoveFx(FxPayload p) {
		super(p);
		end = 0.9;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("love_heart", 1f);
		}

		for (double beat : new double[]{0.0, 0.18}) {
			if (cue(beat)) {
				for (int i = 0; i < 5; i++) {
					Particles.spawn(Kind.GLOW, body().add(Particles.randomDir(0.3)), new Vec3(0, 1.2, 0), 0.6).size(0.07f, 0.02f).color(0xFFFFFF, PINK).physics(0, 1.5f);
				}
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		glow(d, body(), 0.8f * bodyHeight(), PINK, 0.35f * (1 - span(t, 0, 0.7)));
	}
}
