package com.chuuni.client.fx.effects;

import java.util.List;

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

import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 寶庫・祕寶解封（variant 1 = 不祥寶庫：凶寶解封，青綠 + 血紅、漫畫分格時停）。
 * <pre>
 * 鏡1 0.00 插鑰  鑰匙孔極近：鑰匙發光，寶庫的接縫一條條透出光
 * 鏡2 1.10 解鎖  正面：三重鎖環浮在寶庫前，一環一環「喀」地轉開
 * 鏡3 2.40 吸氣  寶庫正面極近特寫
 * 衝擊 2.80     寶光噴發：金色光柱 + 寶光噴泉，毛筆直書招式名
 * 鏡4 2.80 英雄  低角度仰望
 * 鏡5 4.20 祝福  金色光點如雨灑落（寶庫本身會照常吐出寶物）
 * </pre>
 */
public class VaultFx extends AnimeFx {
	private static final double[] LOCKS = {1.3, 1.7, 2.1};

	private final boolean ominous;
	private final int gold;
	private final int accent;
	private final Vector3f n;
	private final Vector3f u;
	private final Vec3 face;

	public VaultFx(FxPayload p) {
		super(p);
		ominous = p.variant() == 1;
		gold = ominous ? 0x40F0D0 : 0xFFC850;
		accent = ominous ? 0xFF3050 : 0xFFF0B0;
		Vec3 f = frame.front();
		n = new Vector3f((float)f.x, 0, (float)f.z);
		u = new Vector3f(-n.z, 0, n.x);
		face = c.add(f.scale(0.52));
		name = ominous ? "vault_ominous" : "vault";
		main = gold;
		pale = ominous ? 0xE0FFF8 : 0xFFF6E0;
		impactMode = ominous ? PostState.CRIMSON : PostState.EMBER;
		manga = ominous;
		s3 = 2.4;
		impact = 2.8;
		end = 6.0;
		fadeColor = pale;
		look(ominous ? "vault_ominous" : "vault", 4.4);

		if (mine) {
			Cinematic.play(List.of(
				Shot.of(1.1, frame.at(0.7, 0.75, 2.0), frame.at(0.5, 0.7, 1.65), frame.at(0, 0.5, 0.5)).roll(5, 8).fov(0.85f, 0.78f),
				Shot.of(s3 - 1.1, frame.at(-0.8, 1.0, 3.4), frame.at(-0.4, 0.9, 3.0), frame.at(0, 0.6, 0.8)).roll(-2, -4),
				Shot.of(impact - s3, frame.at(0.15, 0.6, 2.0), frame.at(0.1, 0.55, 1.65), frame.at(0, 0.5, 0.5)).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.2 - impact, frame.at(1.4, 0.15, 2.2), frame.at(1.7, 0.2, 2.8), frame.at(0, 2.5, 0)).look(frame.at(0, 3, 0), frame.at(0, 1.2, 0))
					.roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(end - 4.2, frame.at(-1.6, 2.2, 3.6), frame.at(-2.0, 2.6, 4.4), frame.at(0, 1.0, 0)).roll(-3, -1)
			));
		}
	}

	private Vec3 on(double a, double b, double out) {
		return face.add(u.x * a + n.x * out, b - 0.5 + 0.5, u.z * a + n.z * out);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "endeye_insert", c, 0.7f, 1.3f);
		}

		if (cue(0.35)) {
			Sfx.play(mine, "vault_unlock", c, 1f, ominous ? 0.85f : 1f);
		}

		for (int i = 0; i < LOCKS.length; i++) {
			if (cue(LOCKS[i])) {
				FxClock.hitstop(50);
				shake(0.15f + i * 0.08f);

				if (mine) {
					PostState.split(0.4f + i * 0.25f, i * 1.3f, 160);
					PostState.zoom(0.2f + i * 0.08f, 140);
				}

				Vec3 at = on(0, 0, 0.3 + i * 0.25);

				for (int k = 0; k < 20; k++) {
					Particles.spawn(Kind.SPARK, at, Particles.randomDir(Particles.rand(1.5f, 3.5f)), 0.35).size(0.035f, 0.008f).color(0xFFFFFF, gold).physics(3f, 2f);
				}
			}
		}

		if (cue(impact - 0.05)) {
			Sfx.play(mine, "vault_burst", c, 1.3f, ominous ? 0.85f : 1f);
		}

		if (cue(impact)) {
			Vec3 top = c.add(0, 0.6, 0);

			for (int i = 0; i < 140; i++) {
				Vec3 v = Particles.randomDir(Particles.rand(1f, 3.5f)).add(0, Particles.rand(3f, 8f), 0);
				Particles.spawn(Kind.FLARE, top, v, Particles.rand(1f, 1.8f)).size(0.13f, 0.04f).color(0xFFFFFF, i % 4 == 0 ? accent : gold).physics(7f, 0.5f).spin(3f);
			}
		}

		if (t > impact + 0.4 && t < end - 0.5) {
			int nn = rate(30, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double rr = Math.sqrt(Particles.RNG.nextDouble()) * 3;
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, pos.getY() + 4 + Particles.RNG.nextDouble() * 2, c.z + Math.sin(a) * rr);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -1.5, 0), 2.5).size(0.08f, 0.02f).color(0xFFFFFF, gold).physics(0, 0).fadeIn(0.2f).spin(2f);
			}
		}

		beats(t, dt, on(0, 0, 0.3));
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.5, end);
		float seep = span(t, 0.3, impact);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.4) : 0;
		lights.add(new CinemaPost.Light(face.add(n.x * 0.3, 0, n.z * 0.3), 2.5f + burst * 3, r(gold), g(gold), b(gold), (0.25f + seep * 0.25f + burst * 0.5f) * life,
			true));

		if (burst > 0.01f) {
			rays[0] = c.add(0, 0.5, 0);
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst * 0.5f;
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.1, end - 0.5, end);
		Vector3f up = new Vector3f(0, 1, 0);

		// 鑰匙孔的光
		float key = Math.min(span(t, 0, 0.3), 1 - span(t, impact, impact + 0.3)) * life;
		Vec3 hole = on(0, 0, 0.03);
		d.billboard(Mode.GLOW, GLOW, hole.x, hole.y, hole.z, 0.35f + span(t, 0, impact) * 0.25f, 0, r(gold), g(gold), b(gold), 0.5f * key);
		d.billboard(Mode.GLOW, FLARE, hole.x, hole.y, hole.z, 0.35f, (float)t * 2, 1f, 1f, 1f, key * 0.4f);

		// 接縫透出的光：方塊的 12 條邊
		float seam = span(t, 0.3, 1.1) * (1 - span(t, impact + 0.3, 4.6)) * life;

		if (seam > 0.01f) {
			double x0 = pos.getX(), y0 = pos.getY(), z0 = pos.getZ();
			double[][] e = {
				{0, 0, 0, 1, 0, 0}, {0, 1, 0, 1, 1, 0}, {0, 0, 1, 1, 0, 1}, {0, 1, 1, 1, 1, 1},
				{0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1},
				{0, 0, 0, 0, 0, 1}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1}};
			float flick = 0.75f + 0.25f * (float)Math.sin(t * 23);

			for (double[] s : e) {
				d.streak(Mode.GLOW, SPARK, x0 + s[0], y0 + s[1], z0 + s[2], x0 + s[3], y0 + s[4], z0 + s[5], 0.06f, r(gold), g(gold), b(gold), seam * flick);
			}
		}

		// 三重鎖環：浮在寶庫前方，一環一環轉開
		for (int i = 0; i < LOCKS.length; i++) {
			float appear = easeOutBack(span(t, 0.9 + i * 0.08, 1.15 + i * 0.08));
			float unlock = span(t, LOCKS[i], LOCKS[i] + 0.3);
			float a = appear * (1 - unlock) * life;

			if (a <= 0.01f) {
				continue;
			}

			Vec3 at = on(0, 0, 0.3 + i * 0.25);
			float rot = (float)(i % 2 == 0 ? t : -t) * 0.8f + (float)(easeOut(unlock) * Math.PI * 0.5);
			d.disc(Mode.GLOW, COMPASS, at, u, up, (0.5f + i * 0.18f) * (1 + unlock * 0.6f), rot, r(gold), g(gold), b(gold), a);
		}

		// 寶光噴發：光柱
		float pillar = t < impact ? 0 : 1 - span(t, impact + 0.1, impact + 1.4);

		if (pillar > 0.01f) {
			float w = easeOut(span(t, impact, impact + 0.06));
			Vec3 base = c.add(0, 0.3, 0);
			d.cylinder(Mode.GLOW, BEAM, base, 0.8f * w, 24f, 24, 1, 0, 5, (float)(t * 3), 1.3f, r(gold), g(gold), b(gold), pillar * 0.6f, pillar * 0.1f);
			d.cylinder(Mode.GLOW, BEAM, base, 0.25f * w, 24f, 12, 1, 0.5f, 4, (float)(t * 5), 1f, r(accent), g(accent), b(accent), pillar * 0.8f, pillar * 0.2f);
		}

		shockRings(d, t, new Vec3(c.x, pos.getY() + 0.05, c.z), c.add(0, 0.5, 0), 5f);
	}
}
