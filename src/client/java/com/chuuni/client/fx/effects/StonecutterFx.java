package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 切石機・斷岩一閃（power = 件數）。
 * 鋸片越轉越快、鋸齒光環高速旋轉 → 一道橫向的刀光「一閃」劃過整個畫面寬度，石屑炸開 → 成品從鋸台上升起、轉一圈。
 */
public class StonecutterFx extends MinorFx {
	private static final int BLADE = 0xB8D0FF;
	private static final int STONE = 0xA0A0A8;
	private static final double FLASH = 0.45;

	private final ItemStack item;
	private final Vec3 saw;
	private final int count;

	public StonecutterFx(FxPayload p) {
		super(p);
		item = p.item();
		count = Math.max(1, p.power());
		saw = new Vec3(c.x, pos.getY() + 0.62, c.z);
		end = 2.2;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "stone_saw", c, 0.9f, count >= 16 ? 0.9f : 1f);
			tint(BLADE, 1.2);
			smallName("stonecutter");

			if (mine) {
				com.chuuni.client.fx.Cinematic.peek(1.5);
			}
		}

		if (t < FLASH) {
			int nn = rate(80, dt);

			for (int i = 0; i < nn; i++) {
				Particles.spawn(Kind.SPARK, saw, new Vec3(u.x, 0, u.z).scale(Particles.rand(-3f, 3f)).add(0, Particles.rand(0.5f, 2f), 0), 0.3).size(0.025f, 0.006f)
					.color(0xFFFFFF, BLADE).physics(5f, 1f);
			}
		}

		if (cue(FLASH)) {
			pop(BLADE, 0.6f + Math.min(0.3f, count / 64f));

			for (int i = 0; i < 30 + Math.min(30, count); i++) {
				Particles.spawn(Kind.SHARD, saw, Particles.randomDir(Particles.rand(1f, 3f)).add(0, 1.5, 0), Particles.rand(0.6f, 1.1f)).size(0.06f, 0.04f)
					.color(0xD0D0D8, STONE).physics(9f, 0.8f).spin(10f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = Math.max(span(time(), 0, FLASH) * 0.4f, time() > FLASH ? 1 - span(time(), FLASH, FLASH + 0.8) : 0);
		lights.add(new CinemaPost.Light(saw.add(0, 0.3, 0), 2.5f, r(BLADE), g(BLADE), b(BLADE), 0.5f * k, true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (item.isEmpty() || t < FLASH) {
			return;
		}

		float up = easeOut(span(t, FLASH, FLASH + 0.6));
		float s = 0.55f * (1 - span(t, end - 0.4, end));
		d.item(item, saw.add(0, 0.2 + up * 0.8, 0), s, (float)(t * 4), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.4, end);

		// 鋸齒光環：在鋸片平面上越轉越快
		float spin = span(t, 0, FLASH);
		float ring = (1 - span(t, FLASH, FLASH + 0.4)) * easeOut(span(t, 0, 0.15));
		var up = new org.joml.Vector3f(0, 1, 0);
		var side = new org.joml.Vector3f(n.x, 0, n.z);
		d.disc(Mode.GLOW, JAG, saw, side, up, 0.42f, (float)(t * t * 40), r(BLADE), g(BLADE), b(BLADE), ring * (0.4f + spin * 0.5f));
		d.disc(Mode.GLOW, RING, saw, side, up, 0.48f, 0, 1f, 1f, 1f, ring * 0.5f);

		// 一閃：橫向的長刀光
		float k = span(t, FLASH - 0.04, FLASH + 0.06);
		float a = 1 - span(t, FLASH + 0.06, FLASH + 0.5);

		if (t > FLASH - 0.04 && a > 0.01f) {
			Vec3 dirU = new Vec3(u.x, 0, u.z);
			Vec3 a0 = saw.add(dirU.scale(-2.5 * k));
			Vec3 a1 = saw.add(dirU.scale(2.5 * k));
			line(d, TRAIL, a0, a1, 0.5f * a, BLADE, a * 0.7f);
			line(d, SPARK, a0, a1, 0.07f, 0xFFFFFF, a);
		}

		d.flat(Mode.GLOW, GRID, new Vec3(c.x, pos.getY() + 0.57, c.z), 0.55f * easeOutBack(span(t, 0, 0.3)), 0, r(BLADE), g(BLADE), b(BLADE), 0.4f * life);
		groundRing(d, t, FLASH, FLASH + 0.6, new Vec3(c.x, pos.getY() + 0.02, c.z), 0.3f, 2.4f, BLADE, 0.8f, JAG);
	}
}
