package com.chuuni.client.fx.effects;

import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 凋零・凋零降世（A 級）。最後一顆頭骨放上去的那一刻：
 * <pre>
 * 鏡1 0.00 凝聚  遠景仰望：天色瞬間壓暗，黑色的凋零之風從四面八方捲向靈魂沙上的身軀
 * 鏡2 1.30 三首  三顆頭骨極近、依序掃過：眼窩一個一個亮起紫光
 * 鏡3 3.00 吸氣  正面特寫，黑風被吸進胸口
 * 衝擊 3.40     紫黑雷霆從天劈落三顆頭 → 負片衝擊幀 + 漫畫分格時停 + 四角大字
 * 鏡4 3.40 英雄  腳下仰望（慢動作），雷光在背後閃
 * 鏡5 5.00 降世  拉遠：風暴往外散開，凋零留在中央（原版的蓄力爆炸接在後面）
 * </pre>
 */
public class WitherFx extends MinorFx {
	private static final int VOID = 0x9050E0;
	private static final int ASH = 0x201828;
	private static final double[] EYES = {1.6, 2.0, 2.4};

	private final Vec3 base;

	public WitherFx(FxPayload p) {
		super(p);
		base = new Vec3(p.at().x, Math.floor(p.at().y - 1.5) + 0.02, p.at().z);
		name = "wither";
		main = VOID;
		pale = 0xF0E6FF;
		impactMode = PostState.NEGATIVE;
		manga = true;
		s3 = 3.0;
		impact = 3.4;
		end = 6.6;
		look("wither", 5.6);

		if (mine) {
			Vec3 f = frame.front();
			Vec3 r = frame.right();
			Cinematic.play(List.of(
				Shot.of(1.3, base.add(f.scale(5)).add(0, 0.8, 0), base.add(f.scale(4.4)).add(0, 1.0, 0), base.add(0, 2.2, 0)).roll(-2, -4).fov(1.05f, 1f),
				Shot.of(s3 - 1.3, base.add(r.scale(-1.6)).add(f.scale(1.6)).add(0, 2.9, 0), base.add(r.scale(1.6)).add(f.scale(1.6)).add(0, 2.9, 0),
					base.add(0, 2.8, 0)).roll(6, -6).fov(0.85f, 0.8f),
				Shot.of(impact - s3, base.add(f.scale(2.2)).add(0, 2.4, 0), base.add(f.scale(1.7)).add(0, 2.4, 0), base.add(0, 2.3, 0)).roll(-9, -13).fov(0.72f, 0.6f)
					.accelerate(),
				Shot.of(5.0 - impact, base.add(r.scale(1.5)).add(f.scale(2.8)).add(0, 0.2, 0), base.add(r.scale(1.9)).add(f.scale(3.4)).add(0, 0.3, 0),
					base.add(0, 3.2, 0)).look(base.add(0, 3.6, 0), base.add(0, 2.6, 0)).roll(12, 9).fov(1.18f, 1.1f),
				Shot.of(end - 5.0, base.add(r.scale(-3)).add(f.scale(8)).add(0, 4, 0), base.add(r.scale(-3.6)).add(f.scale(10)).add(0, 5.5, 0), base.add(0, 2, 0))
					.roll(-3, -1)
			));
		}
	}

	/** 三顆頭骨（0 = 中、1 = 左、2 = 右），跟著凋零移動 */
	private Vec3 skull(int i) {
		var level = net.minecraft.client.Minecraft.getInstance().level;
		Entity e = level == null ? null : level.getEntity(p.extra());
		Vec3 at = e == null ? base.add(0, 0.02, 0) : e.position();
		double yaw = Math.toRadians(e == null ? 0 : e.getYRot());
		Vec3 r = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		return i == 0 ? at.add(0, 3.0, 0) : at.add(r.scale(i == 1 ? -1.3 : 1.3)).add(0, 2.7, 0);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "wither_storm", c, 1.2f, 1f);

			if (near) {
				PostState.darken(0.12f);
			}
		}

		if (cue(impact - 0.1)) {
			Sfx.play(mine, "wither_roar", c, 1.4f, 1f);
		}

		for (int i = 0; i < 3; i++) {
			if (cue(EYES[i])) {
				Sfx.play(mine, "combo_ping", c, 0.6f, 0.55f + i * 0.05f);
				shake(0.1f);
				sparks(skull(i), 12, 2f, VOID, 0.4f);
			}
		}

		// 凋零之風：黑色的煙從四周捲向身軀
		if (t < impact + 0.2) {
			int n = rate(140, dt);
			Vec3 core = base.add(0, 1.8, 0);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double rr = 5 + Particles.RNG.nextDouble() * 3;
				Vec3 from = core.add(Math.cos(a) * rr, Particles.rand(-1.5f, 3f), Math.sin(a) * rr);
				Vec3 swirl = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(4);
				Particles.spawn(Kind.SMOKE, from, swirl, 1.4).size(0.4f, 0.15f).color(ASH, 0x08040C).alpha(0.7f).physics(0, 0.5f).attract(core, 6f).fadeIn(0.3f);
			}
		}

		if (cue(impact)) {
			FxClock.hitstop(120);

			for (int i = 0; i < 40; i++) {
				double a = i * Math.PI / 20;
				Particles.spawn(Kind.SMOKE, base.add(0, 1.5, 0), new Vec3(Math.cos(a) * 6, Particles.rand(-0.5f, 1.5f), Math.sin(a) * 6), 1.6).size(0.6f, 1.8f)
					.color(ASH, 0x060408).alpha(0.7f).physics(0, 2f);
			}
		}

		if (cue(end - 0.4) && near) {
			PostState.darken(0);
		}

		beats(t, dt, skull(0).add(0, -0.6, 0));
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.6, end);

		for (int i = 0; i < 3; i++) {
			float on = span(t, EYES[i], EYES[i] + 0.1) * life;
			lights.add(new CinemaPost.Light(skull(i), 3f, r(VOID), g(VOID), b(VOID), 0.6f * on, true));
		}

		lights.add(new CinemaPost.Light(base.add(0, 1.5, 0), 6f, r(VOID), g(VOID), b(VOID), 0.5f * life * span(t, 0, 0.6), true));

		float strike = t > impact ? 1 - span(t, impact, impact + 0.8) : 0;

		if (strike > 0.01f) {
			for (int i = 0; i < 3; i++) {
				segs.add(new CinemaPost.Seg(skull(i), skull(i).add(0, 30, 0), 5, r(VOID), g(VOID), b(VOID), strike * 0.5f, 0.2f));
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.1, end - 0.5, end);

		// 三顆頭骨的眼窩依序亮起
		for (int i = 0; i < 3; i++) {
			float on = span(t, EYES[i], EYES[i] + 0.1) * life;
			Vec3 s = skull(i);
			glow(d, s, 0.7f, VOID, on * 0.7f);
			d.billboard(Mode.GLOW, FLARE, s.x, s.y, s.z, 0.5f * on, (float)t * 2, 1f, 0.9f, 1f, on * 0.6f);
		}

		// 雷霆：衝擊瞬間從天劈落三顆頭，之後零星閃
		Random rng = new Random((long)Math.floor(t * 14));

		for (int i = 0; i < 3; i++) {
			boolean main = t > impact && t < impact + 0.35;
			boolean flicker = t > impact + 0.4 && t < 5.2 && rng.nextFloat() < 0.12f;

			if (main || flicker) {
				Vec3 s = skull(i);
				bolt(d, s.add(rng.nextGaussian() * 2, 14, rng.nextGaussian() * 2), s, rng, main ? 0.12f : 0.06f, main ? 1f : 0.6f, VOID);
			}
		}

		// 腳下靈魂沙冒出的黑色裂縫光
		float crack = span(t, 0.2, 1.2) * life;

		for (int i = 0; i < 7; i++) {
			double a = i * Math.PI * 2 / 7 + 0.4;
			double len = (1.2 + (i % 3) * 0.6) * crack;
			line(d, SPARK, base.add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6), base.add(Math.cos(a + 0.2) * (0.6 + len), 0, Math.sin(a + 0.2) * (0.6 + len)), 0.06f,
				VOID, crack * 0.7f);
		}

		shockRings(d, t, base, skull(0).add(0, -1, 0), 9f);
	}
}
