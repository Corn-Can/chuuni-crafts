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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 營火・營火祭禮（靈魂營火是藍色）。variant：0 點燃 / 1 熄滅 / 2 放上食材。
 * <ul>
 *   <li>點燃：火柱「轟」地沖天 → 地上燃起一圈火之祭壇，火焰沿著圓環竄起，火星螺旋上升</li>
 *   <li>熄滅：大團白色蒸氣往上翻湧，火光往內收、熄去</li>
 *   <li>放食材：食材在火上浮起轉一圈再落到網架上，油花劈啪</li>
 * </ul>
 */
public class CampfireFx extends MinorFx {
	private final int variant;
	private final int fire;
	private final int pale;
	private final Vec3 ground;
	private final ItemStack food;

	public CampfireFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 3);
		var s = state();
		boolean soul = s != null && s.is(Blocks.SOUL_CAMPFIRE);
		fire = soul ? 0x40D8FF : 0xFF7A20;
		pale = soul ? 0xD0FAFF : 0xFFE8B0;
		ground = new Vec3(c.x, pos.getY() + 0.02, c.z);
		food = p.item();
		end = variant == 0 ? 2.6 : variant == 1 ? 2.0 : 1.6;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 top = c.add(0, -0.1, 0);

		if (variant == 0) {
			if (cue(0.0)) {
				Sfx.play(mine, "camp_ignite", c, 1f, fire == 0x40D8FF ? 0.85f : 1f);
				tint(fire, 1.6);
				smallName("campfire");
				pop(fire, 0.55f);

				for (int i = 0; i < 50; i++) {
					Particles.spawn(Kind.FLAME, top, Particles.randomDir(1.2).add(0, Particles.rand(3f, 6f), 0), 0.6).size(0.3f, 0.05f).color(pale, fire)
						.physics(-1f, 2f);
				}
			}

			// 火焰沿著圓環竄起
			if (t > 0.15 && t < 1.9) {
				int nn = rate(70, dt);

				for (int i = 0; i < nn; i++) {
					double a = Particles.RNG.nextDouble() * Math.PI * 2;
					Vec3 at = ground.add(Math.cos(a) * 1.4, 0, Math.sin(a) * 1.4);
					Particles.spawn(Kind.FLAME, at, new Vec3(0, Particles.rand(1f, 2f), 0), 0.45).size(0.16f, 0.03f).color(pale, fire).physics(-0.5f, 2f);
				}

				int ne = rate(30, dt);

				for (int i = 0; i < ne; i++) {
					double a = t * 6 + i;
					Particles.spawn(Kind.SPARK, top.add(Math.cos(a) * 0.3, 0.3, Math.sin(a) * 0.3), new Vec3(-Math.sin(a) * 1.5, 2.5, Math.cos(a) * 1.5), 0.8)
						.size(0.03f, 0.006f).color(0xFFFFFF, fire).physics(-0.5f, 0.8f);
				}
			}
		}
		else if (variant == 1) {
			if (cue(0.0)) {
				Sfx.play(mine, "camp_douse", c, 0.9f, 1f);

				for (int i = 0; i < 24; i++) {
					Particles.spawn(Kind.SMOKE, top.add(Particles.randomDir(0.3)), Particles.randomDir(0.6).add(0, Particles.rand(1f, 2f), 0), Particles.rand(1.2f, 1.8f))
						.size(0.3f, 1.1f).color(0xF0F0F0, 0x909090).alpha(0.55f).physics(-0.3f, 1.5f);
				}
			}

			if (t < 0.8) {
				int nn = rate(40, dt);

				for (int i = 0; i < nn; i++) {
					Particles.spawn(Kind.SPARK, top.add(Particles.randomDir(0.4)), Particles.randomDir(1.2).add(0, 1, 0), 0.3).size(0.02f, 0.005f).color(0xFFFFFF, 0xA0A0A0)
						.physics(2f, 2f);
				}
			}
		}
		else {
			if (cue(0.0)) {
				Sfx.play(mine, "camp_place", c, 0.8f, 1f);
			}

			if (cue(0.7)) {
				sparks(top.add(0, 0.1, 0), 18, 2f, fire, 0.4f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float k = variant == 1 ? 1 - span(t, 0, 0.8) : (1 - span(t, end - 0.8, end)) * (variant == 2 ? 0.4f : 1);
		lights.add(new CinemaPost.Light(c.add(0, 0.3, 0), 4f, r(fire), g(fire), b(fire), 0.6f * k, true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (variant != 2 || food.isEmpty()) {
			return;
		}

		// 浮起、轉一圈、落到網架上
		float up = easeOut(span(t, 0, 0.35));
		float down = easeIn(span(t, 0.45, 0.7));
		double y = pos.getY() + 0.5 + up * 0.7 - down * 0.7;
		float s = 0.7f * (1 - span(t, 0.75, 0.95));

		if (s > 0.01f) {
			d.item(food, new Vec3(c.x, y, c.z), s, (float)(t * 8), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		if (variant == 0) {
			float pillar = 1 - span(t, 0.1, 0.9);

			if (pillar > 0.01f) {
				float w = easeOut(span(t, 0, 0.08));
				d.cylinder(Mode.GLOW, BEAM, c.add(0, -0.4, 0), 0.45f * w, 4f, 20, 1, 0, 3, (float)(t * 4), 0.4f, r(fire), g(fire), b(fire), pillar * 0.7f, 0);
			}

			// 火之祭壇
			float ring = easeOutBack(span(t, 0.05, 0.35)) * life;
			d.annulus(Mode.GLOW, GLOW, ground.add(0, 0.01, 0), 1.2f * ring, 1.6f * ring, 48, 1, 0, r(fire), g(fire), b(fire), 0f, 0.5f * ring);
			d.annulus(Mode.GLOW, GLOW, ground.add(0, 0.01, 0), 1.6f * ring, 1.9f * ring, 48, 1, 0, r(fire), g(fire), b(fire), 0.5f * ring, 0f);
			groundRing(d, t, 0, 0.7, ground, 0.3f, 3.5f, pale, 0.9f, JAG);
		}
		else if (variant == 1) {
			// 火光往內收
			for (int k = 0; k < 3; k++) {
				float ph = span(t, k * 0.12, 0.6 + k * 0.12);

				if (ph > 0 && ph < 1) {
					d.flat(Mode.GLOW, RING, ground, 1.6f * (1 - ph) + 0.1f, 0, r(fire), g(fire), b(fire), ph * 0.6f);
				}
			}

			glow(d, c, 1.0f * (1 - span(t, 0, 0.7)), fire, 0.3f * (1 - span(t, 0, 0.7)));
		}
		else {
			float a = span(t, 0.7, 0.8) * (1 - span(t, 0.8, end));
			groundRing(d, t, 0.7, 1.3, c.add(0, -0.05, 0), 0.2f, 1.2f, fire, 0.7f, RING);
			glow(d, c, 1f, fire, a * 0.5f);
		}
	}
}
