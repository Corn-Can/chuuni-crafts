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

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 磁石・磁極指引（羅盤綁定磁石）。
 * <pre>
 * 鏡1 0.00 磁化  低角度：羅盤浮在磁石上狂轉，紅藍兩極的磁力線從磁石一圈圈長出來
 * 鏡2 1.20 磁場  環繞：磁力線越轉越快、鐵屑沿著磁力線流動
 * 鏡3 2.30 吸氣  羅盤極近特寫
 * 衝擊 2.70     指針「喀」地咬合磁極：衝擊幀 + 毛筆直書招式名，磁力線瞬間繃直
 * 鏡4 2.70 英雄  低角度仰望
 * 鏡5 4.10 指引  一道光之絲線從磁石連到玩家身上（之後羅盤永遠指向這裡）
 * </pre>
 */
public class LodestoneFx extends AnimeFx {
	private static final int NORTH = 0xFF5050;
	private static final int SOUTH = 0x5090FF;
	private static final int STEEL = 0xE8F0FF;
	private static final double S5 = 4.1;

	public LodestoneFx(FxPayload p) {
		super(p);
		name = "lodestone";
		main = NORTH;
		pale = STEEL;
		impactMode = PostState.FROST;
		s3 = 2.3;
		impact = 2.7;
		end = 5.8;
		fadeColor = STEEL;
		look("magnet", S5 + 0.6);

		if (mine) {
			Vec3 compass = frame.at(0, 1.7, 0);
			Cinematic.play(List.of(
				Shot.of(1.2, frame.at(1.0, 0.6, 1.8), frame.at(0.75, 0.75, 1.4), frame.at(0, 1.3, 0)).roll(5, 8).fov(0.9f, 0.84f),
				Shot.of(s3 - 1.2, frame.at(-2.2, 2.2, 1.0), frame.at(-1.6, 2.6, 2.0), frame.at(0, 1.3, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(0.1, 1.85, 0.7), frame.at(0.06, 1.8, 0.5), compass).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(S5 - impact, frame.at(1.4, 0.35, 2.0), frame.at(1.75, 0.42, 2.5), frame.at(0, 2.2, 0)).look(frame.at(0, 2.4, 0), compass).roll(12, 9)
					.fov(1.12f, 1.06f),
				Shot.of(end - S5 - 0.1, frame.at(-1.8, 1.8, 0.6), frame.at(-2.2, 2.0, 1.4), frame.at(0, 1.3, 1.6)).roll(-3, -1)
			));
		}
	}

	private Vec3 compassPos(double t) {
		return new Vec3(c.x, pos.getY() + 1.15 + easeOut(span(t, 0.05, 0.7)) * 0.55 + Math.sin(t * 2) * 0.03, c.z);
	}

	private Vec3 playerChest() {
		var level = Minecraft.getInstance().level;
		Entity e = level == null ? null : level.getEntity(p.actor());
		return e == null ? frame.at(0, 1.2, 3) : e.position().add(0, 1.1, 0);
	}

	/** 第 i 條磁力線（繞 Y 軸的一個迴圈）上參數 s 的點 */
	private Vec3 fieldLine(int i, double s, double t, float tight) {
		double ang = i * Math.PI * 2 / 8 + t * (0.5 + span(t, 1.2, impact) * 3) * (t > impact ? 0.1 : 1);
		double r = (0.6 + 1.2 * Math.sin(s * Math.PI)) * tight;
		double y = pos.getY() + 0.5 + Math.cos(s * Math.PI) * 1.1;
		return new Vec3(c.x + Math.cos(ang) * r, y, c.z + Math.sin(ang) * r);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 compass = compassPos(t);

		if (cue(0.0)) {
			Sfx.play(mine, "magnet_hum", c, 1f, 1f);
		}

		// 鐵屑沿磁力線流動
		if (t > 0.8 && t < impact) {
			int n = rate(60, dt);

			for (int i = 0; i < n; i++) {
				int line = Particles.RNG.nextInt(8);
				double s = Particles.RNG.nextDouble();
				Vec3 a = fieldLine(line, s, t, 1);
				Vec3 b = fieldLine(line, Math.min(1, s + 0.05), t, 1);
				Particles.spawn(Kind.SPARK, a, b.subtract(a).scale(20), 0.25).size(0.025f, 0.006f).color(0xFFFFFF, s < 0.5 ? NORTH : SOUTH).physics(0, 0);
			}
		}

		if (cue(impact - 0.35)) {
			Sfx.play(mine, "magnet_lock", c, 1.3f, 1f);
		}

		if (t > S5) {
			Vec3 chest = playerChest();
			int n = rate(30, dt);

			for (int i = 0; i < n; i++) {
				double k = Particles.RNG.nextDouble();
				Vec3 at = lerp(compass, chest, k);
				Particles.spawn(Kind.GLOW, at, Vec3.ZERO, 0.6).size(0.05f, 0.01f).color(0xFFFFFF, NORTH).physics(0, 0.3f);
			}
		}

		beats(t, dt, compass);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 compass = compassPos(t);
		float life = 1 - span(t, end - 0.4, end);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.2) : 0;
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 1.6, c.z), 2.5f, r(NORTH), g(NORTH), b(NORTH), 0.5f * life, true));
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() - 0.4, c.z), 2.5f, r(SOUTH), g(SOUTH), b(SOUTH), 0.5f * life, false));

		if (burst > 0.01f) {
			lights.add(new CinemaPost.Light(compass, 4f, 1f, 1f, 1f, burst, true));
			rays[0] = compass;
			raysP[0] = r(STEEL);
			raysP[1] = g(STEEL);
			raysP[2] = b(STEEL);
			raysP[3] = burst * 0.5f;
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		// 狂轉 → 咬合時突然停住、微微回彈
		double spin = t < impact ? t * t * 6 : impact * impact * 6 + Math.sin((t - impact) * 25) * Math.exp(-(t - impact) * 6) * 0.3;
		d.item(p.item(), compassPos(t), 0.8f, (float)spin, (float)(Math.PI / 2) * 0.35f);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.15, end - 0.5, end);
		Vec3 compass = compassPos(t);

		// 磁力線：8 個繞著磁石的迴圈，上半紅（N）、下半藍（S）；衝擊時繃直變緊
		float grow = easeOut(span(t, 0.1, 1.0));
		float tight = t > impact ? 1 - 0.4f * easeOut(span(t, impact, impact + 0.2)) : 1;
		float a = (1 - span(t, S5, end - 0.3) * 0.7f) * life;
		int segs = 20;

		for (int i = 0; i < 8; i++) {
			for (int k = 0; k < segs; k++) {
				double s0 = (double)k / segs;
				double s1 = (double)(k + 1) / segs;

				if (s0 > grow) {
					break;
				}

				Vec3 p0 = fieldLine(i, s0, t, tight);
				Vec3 p1 = fieldLine(i, Math.min(s1, grow), t, tight);
				int col = s0 < 0.5 ? NORTH : SOUTH;
				d.streak(Mode.GLOW, SPARK, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, 0.04f, r(col), g(col), b(col), 0.7f * a);
			}
		}

		// 兩極的光
		d.billboard(Mode.GLOW, GLOW, c.x, pos.getY() + 1.6, c.z, 0.6f, 0, r(NORTH), g(NORTH), b(NORTH), 0.5f * life);
		d.billboard(Mode.GLOW, GLOW, c.x, pos.getY() - 0.6, c.z, 0.6f, 0, r(SOUTH), g(SOUTH), b(SOUTH), 0.5f * life);
		d.flat(Mode.GLOW, COMPASS, new Vec3(c.x, pos.getY() + 1.02, c.z), 1.1f * easeOutBack(span(t, 0, 0.5)), (float)(t < impact ? -t * 2 : -impact * 2), r(STEEL),
			g(STEEL), b(STEEL), 0.7f * life);

		// 指引：光之絲線連到玩家
		if (t > S5) {
			float k = easeOut(span(t, S5, S5 + 0.4));
			Vec3 chest = playerChest();
			Vec3 tip = lerp(compass, chest, k);
			float fa = (1 - span(t, end - 0.5, end));
			d.streak(Mode.GLOW, SPARK, compass.x, compass.y, compass.z, tip.x, tip.y, tip.z, 0.08f, r(NORTH), g(NORTH), b(NORTH), 0.8f * fa);
			d.streak(Mode.GLOW, SPARK, compass.x, compass.y, compass.z, tip.x, tip.y, tip.z, 0.03f, 1f, 1f, 1f, fa);
		}

		shockRings(d, t, new Vec3(c.x, pos.getY() + 0.05, c.z), compass, 4.5f);
	}
}
