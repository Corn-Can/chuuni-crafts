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

import net.minecraft.world.phys.Vec3;

/**
 * 考古刷子・古代遺物。刷出的寶物由伺服器先保管，演出最後在光中降下時才變成只有自己能撿的發光掉落物。
 * <pre>
 * 鏡1 0.00 拂沙  貼地近拍：一刷一刷，沙塵捲起、地面浮現古代的符文陣
 * 鏡2 1.20 出土  側面：遺物被沙之漩渦捧著從方塊裡升起
 * 鏡3 2.30 吸氣  遺物極近特寫
 * 衝擊 2.70     重見天日：沙塵往外炸開 + 金色衝擊幀，毛筆直書招式名
 * 鏡4 2.70 英雄  低角度仰望
 * 鏡5 4.10 降臨  遺物帶著金光緩緩降回方塊上 → 專屬掉落物
 * </pre>
 */
public class BrushFx extends AnimeFx {
	private static final int SAND = 0xE8C890;
	private static final int ANCIENT = 0xFFC870;
	private static final double S5 = 4.1;

	public BrushFx(FxPayload p) {
		super(p);
		name = "brush";
		main = ANCIENT;
		pale = 0xFFF4E0;
		impactMode = PostState.EMBER;
		s3 = 2.3;
		impact = 2.7;
		end = 6.6;
		fadeColor = 0xFFF4E0;
		look("relic", S5 + 0.4);

		if (mine) {
			Vec3 item = frame.at(0, 1.6, 0);
			Cinematic.play(List.of(
				Shot.of(1.2, frame.at(0.8, 1.15, 1.2), frame.at(0.5, 1.2, 0.95), frame.at(0, 1.0, 0)).roll(6, 9).fov(0.85f, 0.8f),
				Shot.of(s3 - 1.2, frame.at(-1.9, 1.3, 1.2), frame.at(-1.6, 1.6, 1.6), frame.at(0, 1.4, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(0.1, 1.7, 0.75), frame.at(0.06, 1.65, 0.55), item).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(S5 - impact, frame.at(1.4, 0.6, 2.0), frame.at(1.75, 0.7, 2.5), frame.at(0, 2.2, 0)).look(frame.at(0, 2.4, 0), item).roll(12, 9)
					.fov(1.12f, 1.06f),
				Shot.of(end - S5, frame.at(1.1, 1.5, 2.4), frame.at(0.75, 1.5, 1.85), item).look(item, frame.at(0, 1.2, 0)).roll(4, 1).fov(0.9f, 0.82f)
			), claimAndClose(Void.class));
		}
	}

	private Vec3 itemPos(double t) {
		float rise = easeOut(span(t, 1.2, 2.3));
		double y = pos.getY() + 0.6 + rise * 1.0;

		if (t > S5) {
			float d = smooth(span(t, S5, end - 0.35));
			y = pos.getY() + 1.6 - d * 0.45 + Math.sin(t * 2.2) * 0.02 * (1 - d);
		}

		return new Vec3(c.x, y, c.z);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 item = itemPos(t);
		Vec3 top = new Vec3(c.x, pos.getY() + 1.02, c.z);

		if (cue(0.0)) {
			Sfx.play(mine, "brush_sweep", c, 1f, 1f);
		}

		if (cue(1.1)) {
			Sfx.play(mine, "relic_reveal", c, 1.2f, 1f);
		}

		// 拂沙：每刷一下捲起一陣沙
		for (int k = 0; k < 4; k++) {
			if (cue(0.05 + k * 0.3)) {
				Vec3 dir = frame.right().scale(k % 2 == 0 ? 1 : -1);

				for (int i = 0; i < 30; i++) {
					Particles.spawn(Kind.SMOKE, top.add(Particles.rand(-0.4f, 0.4f), 0, Particles.rand(-0.4f, 0.4f)),
						dir.scale(Particles.rand(1f, 2.5f)).add(0, Particles.rand(0.3f, 1f), 0), Particles.rand(0.6f, 1.2f)).size(0.12f, 0.4f).color(SAND, 0xA08860)
						.alpha(0.6f).physics(1f, 2f).spin(2f);
				}
			}
		}

		// 出土：沙之漩渦
		if (t > 1.2 && t < impact) {
			int n = rate(70, dt);

			for (int i = 0; i < n; i++) {
				double a = t * 6 + Particles.RNG.nextDouble() * Math.PI * 2;
				double rr = 0.5 + Particles.RNG.nextDouble() * 0.4;
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, top.y, c.z + Math.sin(a) * rr);
				Particles.spawn(Kind.SMOKE, at, new Vec3(-Math.sin(a) * 2, Particles.rand(1f, 2f), Math.cos(a) * 2), 0.8).size(0.08f, 0.3f).color(SAND, 0xA08860)
					.alpha(0.5f).physics(0, 1f).spin(3f);
			}
		}

		if (cue(impact)) {
			for (int i = 0; i < 70; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 v = new Vec3(Math.cos(a), Particles.rand(0f, 0.4f), Math.sin(a)).scale(Particles.rand(2.5f, 6f));
				Particles.spawn(Kind.SMOKE, item, v, Particles.rand(1f, 1.8f)).size(0.3f, 1.2f).color(SAND, 0x907850).alpha(0.55f).physics(-0.2f, 2.5f).spin(1f);
			}

			for (int i = 0; i < 30; i++) {
				Particles.spawn(Kind.GLYPH, item, Particles.randomDir(Particles.rand(2f, 4f)), 1.0).size(0.15f, 0.08f).color(0xFFFFFF, ANCIENT).physics(0, 1.5f);
			}
		}

		if (cue(S5 - 0.2)) {
			Sfx.play(mine, "holy_descend", c, 0.9f, 0.9f);

			if (near) {
				PostState.look("holy", 1200, (int)((end - S5 + 0.4) * 1000));
			}
		}

		if (t > S5 - 0.2) {
			int n = rate(16, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = item.add(Particles.randomDir(0.6 + Particles.RNG.nextDouble() * 0.6)).add(0, 0.6, 0);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.3, 0), 1.8).size(0.09f, 0.02f).color(0xFFFFFF, ANCIENT).physics(0, 0).fadeIn(0.3f).spin(1f);
			}
		}

		beats(t, dt, item);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 item = itemPos(t);
		float life = 1 - span(t, end - 0.4, end);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.2) : 0;
		lights.add(new CinemaPost.Light(item, 2.5f + burst * 3, r(ANCIENT), g(ANCIENT), b(ANCIENT), (0.3f + span(t, 1.2, impact) * 0.3f + burst * 0.7f) * life, true));

		if (burst > 0.01f) {
			rays[0] = item;
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst * 0.5f;
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (!p.item().isEmpty() && t > 1.1) {
			float grow = easeOut(span(t, 1.1, 1.6));
			d.item(p.item(), itemPos(t), 0.9f * grow, (float)(Math.min(t, S5) * 1.4 + Math.max(0, t - S5) * 0.5), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, end - 0.5, end);
		Vec3 item = itemPos(t);
		Vec3 ground = new Vec3(c.x, pos.getY() + 1.03, c.z);

		// 古代符文陣（一圈一圈浮現）
		float open = easeOutBack(span(t, 0.2, 0.9));

		// 遺物的光芒
		if (t > 1.1) {
			float a = span(t, 1.1, 1.6) * life;
			d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.1f, 0, r(ANCIENT), g(ANCIENT), b(ANCIENT), 0.35f * a);
		}

		if (t > S5) {
			float holy = span(t, S5, S5 + 0.6) * (1 - span(t, end - 0.3, end));
			d.cylinder(Mode.GLOW, BEAM, ground, 0.5f, 10f, 20, 1, 0.3f, 2, (float)(-t * 0.5), 1f, 1f, 0.92f, 0.75f, 0.12f * holy, 0);
			d.flat(Mode.GLOW, RING, item.add(0, 0.5, 0), 0.3f, (float)t * 0.5f, r(ANCIENT), g(ANCIENT), b(ANCIENT), 0.7f * holy);
		}

		shockRings(d, t, ground, item, 5f);
	}
}
