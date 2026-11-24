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

/** 水牢捕獲：一道水的漩渦把牠捲成一點、收進桶裡，泡泡往外一炸（魚橘、美西螈粉、蝌蚪褐） */
public class BucketMobFx extends MinorFx {
	private static final int WATER = 0x40A0FF;

	private final int col;

	public BucketMobFx(FxPayload p) {
		super(p);
		col = switch (Math.floorMod(p.variant(), 3)) {
			case 1 -> 0xFF90C0;
			case 2 -> 0x806040;
			default -> 0xFFA040;
		};
		end = 1.6;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = p.at();

		if (cue(0.0)) {
			Sfx.play(mine, "bucket_catch", c, 0.9f, 1f);
			tint(WATER, 1.2);
			smallName("bucket_mob");

			for (int i = 0; i < 30; i++) {
				double a = i * 0.7;
				Vec3 from = at.add(Math.cos(a) * 1.2, Particles.rand(-0.3f, 0.6f), Math.sin(a) * 1.2);
				Particles.spawn(Kind.GLOW, from, new Vec3(-Math.sin(a) * 2, 0, Math.cos(a) * 2), 0.5).size(0.07f, 0.02f).color(0xFFFFFF, i % 4 == 0 ? col : WATER)
					.physics(0, 0).attract(at, 16f);
			}
		}

		if (cue(0.5)) {
			pop(WATER, 0.3f);

			for (int i = 0; i < 20; i++) {
				Particles.spawn(Kind.GLOW, at, Particles.randomDir(Particles.rand(1f, 2f)).add(0, 0.8, 0), 0.6).size(0.05f, 0.02f).color(0xFFFFFF, WATER).physics(4f, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = p.at();

		for (int k = 0; k < 3; k++) {
			double ph = (t * 2 + k / 3.0) % 1.0;
			float a = (float)ph * (1 - span(t, 0.5, 0.7));
			d.billboard(Mode.GLOW, RING, at.x, at.y, at.z, (float)(1.2 * (1 - ph)) + 0.1f, (float)t * 3, r(WATER), g(WATER), b(WATER), a * 0.7f);
		}
	}
}
