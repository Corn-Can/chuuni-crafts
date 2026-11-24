package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

/**
 * 釀造台・煉金祕藥（power = 一次取出的瓶數）。藥水的顏色決定整段演出的主色。演完回到釀造台介面。
 * <pre>
 * 鏡1 0.00 沸騰  低角度：藥水瓶一瓶瓶浮起，瓶中液體冒泡
 * 鏡2 1.20 煉成  環繞：瓶子繞圈，三道液體緞帶螺旋往上纏成漩渦
 * 鏡3 2.30 吸氣  漩渦中心極近特寫
 * 衝擊 2.70     質變：藥水色的閃光 + 衝擊幀，毛筆直書招式名
 * 鏡4 2.70 英雄  低角度仰望
 * 鏡5 4.10 沉澱  瓶子帶著光暈緩緩降回釀造台
 * </pre>
 */
public class BrewingFx extends AnimeFx {
	private final int bottles;
	private final int liquid;
	private final Vec3 vortex;

	public BrewingFx(FxPayload p) {
		super(p);
		bottles = Math.max(1, Math.min(3, p.power()));
		PotionContents pc = p.item().get(DataComponents.POTION_CONTENTS);
		int col = pc == null ? 0xFF60D0 : pc.getColor() & 0xFFFFFF;
		liquid = col == 0 ? 0xFF60D0 : col;
		vortex = new Vec3(c.x, pos.getY() + 2.0, c.z);
		name = "brewing";
		main = liquid;
		pale = 0xFFF4FC;
		impactMode = PostState.SPECTRAL;
		s3 = 2.3;
		impact = 2.7;
		end = 5.8;
		look("brewing", 4.3);

		if (mine) {
			Cinematic.play(List.of(
				Shot.of(1.2, frame.at(0.9, 0.6, 1.6), frame.at(0.65, 0.75, 1.25), frame.at(0, 1.2, 0)).roll(5, 8).fov(0.9f, 0.84f),
				Shot.of(s3 - 1.2, frame.at(-2.2, 1.8, 1.6), frame.at(-1.4, 2.4, 2.2), frame.at(0, 1.7, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(0.15, 2.1, 1.4), frame.at(0.1, 2.05, 1.1), frame.at(0, 2.0, 0)).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.1 - impact, frame.at(1.4, 0.4, 2.0), frame.at(1.75, 0.48, 2.5), frame.at(0, 2.4, 0)).look(frame.at(0, 2.6, 0), frame.at(0, 1.7, 0))
					.roll(12, 9).fov(1.12f, 1.06f),
				Shot.of(end - 4.1 - 0.1, frame.at(-1.0, 1.4, 2.2), frame.at(-0.75, 1.3, 1.8), frame.at(0, 1.1, 0)).roll(-3, -1).fov(0.9f, 0.84f)
			));
		}
	}

	/** 第 i 瓶的位置 */
	private Vec3 bottle(int i, double t) {
		float rise = easeOut(span(t, 0.1 + i * 0.15, 0.8 + i * 0.15));
		float settle = smooth(span(t, 4.1, end - 0.3));
		double ang = i * Math.PI * 2 / 3 + t * (1.2 + span(t, 1.2, impact) * 3) * (1 - settle);
		double r = 0.55 + rise * 0.35 - span(t, s3, impact) * 0.3 + (t > impact ? 0.3 * (1 - settle) : 0);
		double y = pos.getY() + 0.5 + rise * 1.1 - settle * 1.1 + Math.sin(t * 3 + i) * 0.05;
		r = r * (1 - settle) + 0.3 * settle;
		return new Vec3(c.x + Math.cos(ang) * r, y, c.z + Math.sin(ang) * r);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "brew_bubble", c, 1f, 1f);
		}

		// 泡泡
		if (t < end - 0.6) {
			int n = rate(25, dt);

			for (int i = 0; i < n; i++) {
				Vec3 b = bottle(Particles.RNG.nextInt(bottles), t);
				Particles.spawn(Kind.GLOW, b.add(Particles.randomDir(0.1)), new Vec3(0, Particles.rand(0.5f, 1.2f), 0), 0.8).size(0.05f, 0.08f).color(0xFFFFFF, liquid)
					.physics(-0.5f, 1f);
			}
		}

		if (cue(impact - 0.4)) {
			Sfx.play(mine, "brew_transmute", c, 1.3f, 1f);
		}

		if (cue(impact)) {
			for (int i = 0; i < 80; i++) {
				Particles.spawn(Kind.GLOW, vortex, Particles.randomDir(Particles.rand(2f, 6f)), Particles.rand(0.6f, 1.2f)).size(0.1f, 0.02f).color(0xFFFFFF, liquid)
					.physics(3f, 1.5f);
			}
		}

		if (cue(4.0)) {
			Sfx.play(mine, "sparkle", c, 0.6f, 0.9f);
		}

		beats(t, dt, vortex);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.4, end);
		float charge = span(t, 1.2, impact);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.2) : 0;
		lights.add(new CinemaPost.Light(vortex, 3f, r(liquid), g(liquid), b(liquid), (0.25f + charge * 0.25f + burst * 0.4f) * life, true));
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 0.6, c.z), 2.5f, r(liquid), g(liquid), b(liquid), 0.4f * life, false));

		if (burst > 0.01f) {
			rays[0] = vortex;
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst * 0.5f;
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		for (int i = 0; i < bottles; i++) {
			d.item(p.item(), bottle(i, t), 0.55f, (float)(t * 1.5 + i), (float)Math.sin(t * 2 + i) * 0.2f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, end - 0.5, end);
		Vec3 floor = new Vec3(c.x, pos.getY() + 0.03, c.z);

		// 三道液體緞帶：從瓶子螺旋往上纏到漩渦
		float ribbon = Math.min(span(t, 1.1, 1.5), 1 - span(t, impact, impact + 0.15)) * life;

		if (ribbon > 0.01f) {
			for (int i = 0; i < bottles; i++) {
				Vec3 prev = null;
				int segs = 14;

				for (int k = 0; k <= segs; k++) {
					double f = (double)k / segs;
					Vec3 b = bottle(i, t - f * 0.5);
					Vec3 at = lerp(b, vortex, f * f).add(0, Math.sin(f * Math.PI) * 0.3, 0);

					if (prev != null) {
						d.streak(Mode.GLOW, TRAIL, prev.x, prev.y, prev.z, at.x, at.y, at.z, 0.14f * (1 - (float)f * 0.6f), r(liquid), g(liquid), b(liquid), 0.8f * ribbon);
					}

					prev = at;
				}
			}

			float sw = span(t, 1.2, impact);
			d.billboard(Mode.GLOW, GLOW, vortex.x, vortex.y, vortex.z, 0.3f + sw * 0.6f, 0, r(liquid), g(liquid), b(liquid), 0.45f * sw * ribbon);
			d.flat(Mode.GLOW, RING, vortex, 0.4f + sw * 0.7f, (float)(t * 5), r(liquid), g(liquid), b(liquid), sw * ribbon);
		}

		// 衝擊後：漩渦中心化成一顆旋轉的煉金陣
		if (t > impact) {
			float a = (1 - span(t, impact + 0.8, 4.4)) * life;
		}

		// 沉澱：瓶子底下的光暈
		if (t > 4.0) {
			float s = span(t, 4.0, 4.6) * (1 - span(t, end - 0.4, end));

			for (int i = 0; i < bottles; i++) {
				Vec3 b = bottle(i, t);
				d.billboard(Mode.GLOW, GLOW, b.x, b.y, b.z, 0.45f, 0, r(liquid), g(liquid), b(liquid), 0.6f * s);
			}
		}

		shockRings(d, t, floor.add(0, 0.02, 0), vortex, 4.5f);
	}
}
