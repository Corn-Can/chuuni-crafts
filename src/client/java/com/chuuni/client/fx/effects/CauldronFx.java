package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 鍋釜・魔女之釜。variant：0 注水 / 1 熔岩 / 2 細雪 / 3 舀出 / 4 洗掉染色；power = 之後的水位。
 * <ul>
 *   <li>注入：液面浮現旋轉的魔女法陣，液柱從釜中螺旋沖起 → 在頂點「啪」地炸開回落</li>
 *   <li>熔岩：火焰液柱 + 熔岩飛濺；細雪：冰晶從空中盤旋落入、結出一圈冰花</li>
 *   <li>舀出 / 洗色：釜中的漩渦把光點吸進去，最後吐出一團（洗色時是彩色的顏料被洗出來）</li>
 * </ul>
 */
public class CauldronFx extends MinorFx {
	private static final int[] COL = {0x3A8CFF, 0xFF6A1A, 0xBFE8FF, 0x5AA8FF, 0x9A70FF};
	private static final int WITCH = 0x9A60FF;
	private static final int[] RAINBOW = {0xFF5A5A, 0xFFB040, 0xFFF060, 0x60E070, 0x50A0FF, 0xB070FF};

	private final int variant;
	private final int col;
	private final boolean fill;
	private final Vec3 surface;

	public CauldronFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 5);
		int src = variant >= 3 ? p.extra() : variant;
		col = variant == 4 ? COL[4] : COL[Math.min(2, src)];
		fill = variant < 3;
		int lvl = Math.max(0, Math.min(3, p.power()));
		surface = new Vec3(c.x, pos.getY() + (lvl == 0 ? 0.3 : (6 + 3 * lvl) / 16.0) + 0.02, c.z);
		end = fill ? 2.4 : 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			String s = switch (variant) {
				case 0 -> "cauldron_fill";
				case 1 -> "cauldron_lava";
				case 2 -> "cauldron_frost";
				default -> "cauldron_swirl";
			};
			Sfx.play(mine, s, c, 0.9f, variant == 4 ? 1.15f : 1f);
			tint(fill ? col : WITCH, 1.3);
			smallName("cauldron");

			if (variant == 2) {
				// 冰晶從空中盤旋落入
				for (int i = 0; i < 40; i++) {
					double a = i * 0.7;
					Vec3 from = surface.add(Math.cos(a) * 1.4, 1.6 + i * 0.02, Math.sin(a) * 1.4);
					Particles.spawn(Kind.FLARE, from, Vec3.ZERO, 0.8).size(0.07f, 0.03f).color(0xFFFFFF, col).physics(0, 0).attract(surface, 6f).spin(4f);
				}
			}
		}

		if (fill && t < 0.75) {
			// 螺旋液柱
			int nn = rate(variant == 1 ? 120 : 160, dt);

			for (int i = 0; i < nn; i++) {
				double a = t * 14 + Particles.RNG.nextDouble() * 0.6;
				Vec3 at = surface.add(Math.cos(a) * 0.25, 0, Math.sin(a) * 0.25);
				Vec3 vel = new Vec3(-Math.sin(a) * 1.4, Particles.rand(2.5f, 4f), Math.cos(a) * 1.4);

				if (variant == 1) {
					Particles.spawn(Kind.FLAME, at, vel, 0.45).size(0.16f, 0.04f).color(0xFFE0A0, col).physics(2f, 1.5f);
				}
				else {
					Particles.spawn(Kind.GLOW, at, vel, 0.5).size(0.08f, 0.02f).color(0xFFFFFF, col).physics(4f, 1f);
				}
			}
		}

		if (fill && cue(0.6)) {
			pop(col, 0.45f);
			Vec3 top = surface.add(0, 1.2, 0);

			for (int i = 0; i < 46; i++) {
				Vec3 vel = Particles.randomDir(Particles.rand(1f, 2.6f)).add(0, 1.2, 0);

				if (variant == 2) {
					Particles.spawn(Kind.SHARD, top, vel, Particles.rand(0.8f, 1.3f)).size(0.07f, 0.04f).color(0xFFFFFF, col).physics(5f, 1f).spin(6f);
				}
				else {
					Particles.spawn(Kind.SPARK, top, vel, Particles.rand(0.6f, 1.1f)).size(0.05f, 0.015f).color(0xFFFFFF, col).physics(9f, 0.6f);
				}
			}

			if (variant == 1) {
				for (int i = 0; i < 10; i++) {
					Particles.spawn(Kind.SMOKE, top, Particles.randomDir(0.6).add(0, 0.8, 0), 1.4).size(0.4f, 0.9f).color(0x402010, 0x100804).alpha(0.5f)
						.physics(-0.5f, 1f);
				}
			}
		}

		if (!fill && t < 0.9) {
			// 漩渦：光點從外圈被吸進釜中
			int nn = rate(90, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = surface.add(Math.cos(a) * 1.1, 0.4 + Particles.RNG.nextDouble() * 0.5, Math.sin(a) * 1.1);
				int k = variant == 4 ? RAINBOW[Particles.RNG.nextInt(RAINBOW.length)] : col;
				Particles.spawn(Kind.GLOW, at, new Vec3(-Math.sin(a) * 2, 0, Math.cos(a) * 2), 0.5).size(0.06f, 0.01f).color(0xFFFFFF, k).physics(0, 0)
					.attract(surface, 18f);
			}
		}

		if (!fill && cue(0.95)) {
			pop(variant == 4 ? WITCH : col, 0.35f);

			for (int i = 0; i < 30; i++) {
				int k = variant == 4 ? RAINBOW[i % RAINBOW.length] : col;
				Particles.spawn(variant == 4 ? Kind.GLYPH : Kind.SPARK, surface, Particles.randomDir(Particles.rand(0.8f, 2f)).add(0, 1.5, 0), 0.9)
					.size(0.07f, 0.02f).color(0xFFFFFF, k).physics(variant == 4 ? -0.5f : 6f, 1.2f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.6, end);
		lights.add(new CinemaPost.Light(surface.add(0, 0.4, 0), 3f, r(col), g(col), b(col), (variant == 1 ? 0.6f : 0.35f) * life, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.6, end);
		float open = easeOutBack(span(t, 0, 0.3));

		// 液面的漣漪：一圈圈往釜壁擴散
		for (int k = 0; k < 4; k++) {
			double ph = (t * 1.4 + k / 4.0) % 1.0;
			d.flat(Mode.GLOW, RING, surface, (float)(0.06 + 0.38 * ph) * open, 0, r(col), g(col), b(col), (float)(1 - ph) * 0.7f * life);
		}

		if (fill) {
			// 螺旋液柱
			float rise = easeOut(span(t, 0, 0.55));
			float a = (1 - span(t, 0.55, 1.1)) * life;

			if (a > 0.01f) {
				d.cylinder(Mode.GLOW, BEAM, surface, 0.28f, 1.3f * rise, 16, 2, (float)(t * 3), 2, (float)(-t * 4), 0.6f, r(col), g(col), b(col), a * 0.7f, 0);
				d.cylinder(Mode.GLOW, BEAM, surface, 0.4f, 0.7f * rise, 24, 3, (float)(t * 1.5), 2, (float)(-t * 6), 0.7f, r(col), g(col), b(col), a * 0.5f, 0);
			}

			groundRing(d, t, 0.6, 1.3, surface, 0.3f, 1.6f, col, 0.9f, RING);
			groundRing(d, t, 0.6, 1.0, surface.add(0, 0.01, 0), 0.2f, 1.1f, 0xFFFFFF, 0.8f, JAG);

			if (variant == 2) {
				// 冰花
				float bloom = easeOutBack(span(t, 0.6, 0.9)) * (1 - span(t, 1.6, end));
				d.flat(Mode.GLOW, JAG, surface.add(0, 0.02, 0), 0.55f * bloom, 0.3f, 0.9f, 0.97f, 1f, 0.7f * bloom);
			}
		}
		else {
			// 漩渦：往內收的環
			for (int k = 0; k < 3; k++) {
				double ph = (t * 1.8 + k / 3.0) % 1.0;
				float a = (float)ph * (1 - span(t, 0.9, 1.2));
				d.flat(Mode.GLOW, RING, surface.add(0, 0.01 * k, 0), (float)(1.2 * (1 - ph)) + 0.1f, 0, r(col), g(col), b(col), a * 0.8f);
			}

			groundRing(d, t, 0.95, 1.5, surface, 0.1f, 1.2f, variant == 4 ? WITCH : col, 0.9f, JAG);
		}

		glow(d, surface.add(0, 0.15, 0), 0.9f, col, 0.35f * life);
	}
}
