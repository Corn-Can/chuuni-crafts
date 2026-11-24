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

/** 水桶：舀起時液面捲起往上收（BUCKET 0）；倒出時往四周濺開（1）。水藍 / 熔岩橘 / 細雪白 */
public class BucketFx extends MicroFx {
	private final boolean fill;
	private final int col;

	public BucketFx(FxPayload p) {
		super(p);
		fill = p.variant() == 0;
		col = switch (Math.floorMod(p.extra(), 3)) {
			case 1 -> 0xFF7020;
			case 2 -> 0xE8F4FF;
			default -> 0x40A0FF;
		};
		end = 0.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx(fill ? "bucket_fill" : "bucket_pour", p.extra() == 1 ? 0.8f : 1f);

			if (!fill) {
				for (int i = 0; i < 16; i++) {
					Particles.spawn(p.extra() == 1 ? Kind.FLAME : Kind.GLOW, hit, Particles.randomDir(Particles.rand(1f, 2f)).add(0, 1.2, 0), 0.5).size(0.06f, 0.02f)
						.color(0xFFFFFF, col).physics(6f, 1f);
				}
			}
		}

		if (fill && t < 0.4) {
			int n = rate(90, dt);

			for (int i = 0; i < n; i++) {
				double a = t * 16 + i;
				Vec3 at = hit.add(Math.cos(a) * 0.5 * (1 - t * 2), t * 1.2, Math.sin(a) * 0.5 * (1 - t * 2));
				Particles.spawn(p.extra() == 1 ? Kind.FLAME : Kind.GLOW, at, new Vec3(0, 0.8, 0), 0.3).size(0.05f, 0.01f).color(0xFFFFFF, col).physics(0, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		groundRing(d, t, 0, 0.6, hit.add(0, 0.02, 0), fill ? 0.8f : 0.1f, fill ? 0.1f : 1.2f, col, 0.6f, RING);
	}
}
