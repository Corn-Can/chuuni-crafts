package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 床・夢境結界。睡覺是安靜的事，所以這段不用衝擊幀與黑色集中線，改成柔和的夢境：
 * <pre>
 * 鏡1 0.00 入夢  從正上方慢慢降下，看著睡著的自己；粉彩光點飄起
 * 鏡2 1.60 升空  鏡頭緩緩升上夜空，床上方展開巨大的夢之陣、星光往上流
 * 結界 3.00     夢之陣化成一座星空穹頂罩住床（柔光一閃 + 毛筆直書招式名）
 * 鏡3 3.00 守護  低角度仰望穹頂
 * 鏡4 4.90 天明  溫暖的晨光白光淡出
 * </pre>
 * 在地獄 / 終界爆炸（variant 1）：無鏡頭——紅色閃光 + 負片衝擊幀 + 大震動。
 */
public class BedFx extends AnimeFx {
	private static final int DREAM = 0xB8A8FF;
	private static final int PINK = 0xFFC0E8;
	private static final double BARRIER = 3.0;
	private static final double DAWN = 4.9;

	private final boolean explode;
	private final Vec3 bed;

	public BedFx(FxPayload p) {
		super(p);
		explode = p.variant() == 1;
		bed = new Vec3(c.x, pos.getY() + 0.6, c.z);
		main = DREAM;
		pale = 0xF4F0FF;

		if (explode) {
			end = 1.6;
			return;
		}

		end = 6.6;
		look("dream", end);

		if (mine) {
			Cinematic.play(List.of(
				Shot.of(1.6, frame.at(0.3, 4.2, 0.4), frame.at(0.2, 2.6, 0.3), bed).roll(0, 15),
				Shot.of(BARRIER - 1.6, frame.at(1.6, 2.2, 2.2), frame.at(2.4, 5.0, 3.2), frame.at(0, 3.0, 0)).roll(-2, -4),
				Shot.of(DAWN - BARRIER, frame.at(2.4, 0.4, 3.0), frame.at(2.8, 0.5, 3.6), frame.at(0, 3.5, 0)).look(frame.at(0, 4.5, 0), frame.at(0, 2.5, 0))
					.roll(8, 5).fov(1.15f, 1.1f),
				Shot.of(end - DAWN, frame.at(-1.6, 1.8, 2.6), frame.at(-1.2, 1.6, 2.0), bed).roll(-2, 0).fov(0.95f, 0.9f)
			));
			Cinematic.showSelf();
		}
	}

	@Override
	protected void update(double t, double dt) {
		if (explode) {
			if (cue(0.0)) {
				Sfx.play(mine, "anchor_blast", c, 1.4f, 1.1f);
				shake(1.3f);

				if (near) {
					PostState.impact(PostState.NEGATIVE, 160);
					PostState.flash(0xFF5030, 0.7f, 220, 0.2f);
					PostState.shock(1.3f, 700);
					PostState.split(2.2f, 0.3f, 600);
					CameraShake.fovPunch(0.15f);
				}

				for (int i = 0; i < 140; i++) {
					Particles.spawn(Kind.FLAME, bed, Particles.randomDir(Particles.rand(3f, 8f)), Particles.rand(0.4f, 1f)).size(Particles.rand(0.25f, 0.45f), 0.05f)
						.color(0xFFF0C0, 0xFF3010).physics(-0.5f, 2.5f);
				}

				// 羽毛四散
				for (int i = 0; i < 40; i++) {
					Particles.spawn(Kind.SHARD, bed, Particles.randomDir(Particles.rand(2f, 5f)).add(0, 2, 0), 2.0).size(0.08f, 0.06f).color(0xFFFFFF, 0xE0E0E0)
						.physics(1.5f, 2.5f).spin(5f);
				}
			}

			return;
		}

		if (cue(0.0)) {
			Sfx.play(mine, "dream_fall", c, 1f, 1f);
		}

		// 粉彩光點從床上飄起
		int n = rate(t < DAWN ? 24 : 8, dt);

		for (int i = 0; i < n; i++) {
			Vec3 at = bed.add(Particles.rand(-0.8f, 0.8f), Particles.rand(0f, 0.4f), Particles.rand(-0.8f, 0.8f));
			Particles.spawn(Kind.FLARE, at, new Vec3(Particles.rand(-0.1f, 0.1f), Particles.rand(0.3f, 0.8f), Particles.rand(-0.1f, 0.1f)), 2.5).size(0.09f, 0.02f)
				.color(0xFFFFFF, Particles.RNG.nextBoolean() ? DREAM : PINK).physics(0, 0.2f).fadeIn(0.5f).spin(1f);
		}

		// 升空時星光往上流
		if (t > 1.6 && t < DAWN) {
			int k = rate(40, dt);

			for (int i = 0; i < k; i++) {
				Vec3 at = bed.add(Particles.randomDir(2.5)).add(0, 1, 0);
				Particles.spawn(Kind.GLOW, at, new Vec3(0, Particles.rand(1f, 3f), 0), 1.5).size(0.05f, 0.01f).color(0xFFFFFF, DREAM).physics(0, 0);
			}
		}

		if (cue(BARRIER)) {
			Sfx.play(mine, "name_hit", c, 0.6f, 1.2f);

			if (mine) {
				PostState.flash(pale, 0.3f, 300, 0.1f);
				PostState.shock(0.4f, 900);
				PostState.streak(0.5f, 900);

				if (Settings.titlesOn()) {
					Hud.brushName("bed", 3.2);
				}
			}
		}

		if (cue(DAWN)) {
			Sfx.play(mine, "dream_dawn", c, 1f, 1f);

			if (near) {
				PostState.look("holy", 1200, (int)((end - DAWN + 0.5) * 1000));
			}
		}

		if (cue(end - 0.8) && mine) {
			Hud.whiteout(0xFFF2D8, 0.6, 1.0);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();

		if (explode) {
			lights.add(new CinemaPost.Light(bed, 6f, 1f, 0.4f, 0.15f, 1.5f * (1 - span(t, 0, 1.2)), true));
			return;
		}

		float life = 1 - span(t, end - 0.5, end);
		float dome = span(t, BARRIER - 0.3, BARRIER + 0.4);
		float dawn = span(t, DAWN, end);
		lights.add(new CinemaPost.Light(bed.add(0, 1, 0), 3f + dome * 3, r(DREAM), g(DREAM), b(DREAM), (0.3f + dome * 0.3f) * life * (1 - dawn), true));
		lights.add(new CinemaPost.Light(bed.add(0, 2, 0), 5f, 1f, 0.85f, 0.6f, 0.6f * dawn, true));
	}

	@Override
	protected void render(Draw d, double t) {
		if (explode) {
			float k = span(t, 0, 1.2);
			d.flat(Mode.GLOW, JAG, new Vec3(c.x, pos.getY() + 0.05, c.z), 0.5f + easeOut(k) * 8f, 0, 1f, 0.5f, 0.3f, (1 - k) * (1 - k));
			return;
		}

		float life = fade(t, 0.3, end - 0.6, end);
		float night = 1 - span(t, DAWN, end);
		Vec3 floor = new Vec3(c.x, pos.getY() + 0.58, c.z);

		// 床上方的夢之陣
		float open = easeOutBack(span(t, 1.4, 2.4));
		Vec3 sky = bed.add(0, 3.2, 0);

		// 床面的柔光
		d.flat(Mode.GLOW, GLOW, floor, 1.5f, 0, r(DREAM), g(DREAM), b(DREAM), 0.3f * life * night);

		// 結界：星空穹頂
		float dome = easeOut(span(t, BARRIER - 0.2, BARRIER + 0.5)) * life * night;

		if (dome > 0.01f) {
			float rr = 2.8f * dome;
			d.cylinder(Mode.GLOW, STARS, new Vec3(c.x, pos.getY(), c.z), rr, 3.4f, 40, 2, (float)(t * 0.02), 1, 0, 0.5f, r(pale), g(pale), b(pale), dome * 0.35f,
				dome * 0.7f);
			d.flat(Mode.GLOW, STARS, new Vec3(c.x, pos.getY() + 3.4, c.z), rr * 0.5f, (float)(t * 0.05), r(pale), g(pale), b(pale), dome * 0.6f);
		}

		float k = span(t, BARRIER, BARRIER + 1.2);

		if (k > 0 && k < 1) {
			d.flat(Mode.GLOW, RING, new Vec3(c.x, pos.getY() + 0.05, c.z), 0.5f + easeOut(k) * 6, 0, r(pale), g(pale), b(pale), (1 - k) * (1 - k) * 0.7f);
		}
	}
}
