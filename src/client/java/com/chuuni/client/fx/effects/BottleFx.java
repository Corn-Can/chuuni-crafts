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

/** 裝水瓶：水面捲起一小股螺旋水流往上收進瓶子，冒幾顆泡泡 */
public class BottleFx extends MicroFx {
	private static final int WATER = 0x50A8FF;

	public BottleFx(FxPayload p) {
		super(p);
		end = 0.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("bottle_fill", 1f);
		}

		if (t < 0.45) {
			int n = rate(70, dt);

			for (int i = 0; i < n; i++) {
				double a = t * 18 + i;
				Vec3 at = hit.add(Math.cos(a) * 0.25 * (1 - t * 2), t * 0.8, Math.sin(a) * 0.25 * (1 - t * 2));
				Particles.spawn(Kind.GLOW, at, new Vec3(0, 0.6, 0), 0.3).size(0.04f, 0.01f).color(0xFFFFFF, WATER).physics(0, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		groundRing(d, t, 0, 0.5, hit.add(0, 0.02, 0), 0.05f, 0.5f, WATER, 0.6f, RING);
	}
}
