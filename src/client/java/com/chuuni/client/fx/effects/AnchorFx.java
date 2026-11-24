package com.chuuni.client.fx.effects;

import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxMesh;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 重生錨・魂錨契約。
 * <ul>
 *   <li>充能（variant 0）：無鏡頭 1.4 秒——螢石碎光螺旋吸入，錨頂的四盞燈依充能數亮起，紫色魂火一噴；第 4 格充滿時多一圈衝擊</li>
 *   <li>契約（variant 1，設定重生點）：
 * <pre>
 * 鏡1 0.00 共鳴  低角度：四盞魂燈脈動、紫色魂火從錨裡冒出
 * 鏡2 1.20 鎖鏈  側面廣角：四條靈魂鎖鏈從錨射向玩家，地面浮現契約陣
 * 鏡3 2.30 吸氣  錨頂極近特寫
 * 衝擊 2.70     鎖鏈「喀」地扣上：衝擊幀 + 魂柱沖天，毛筆直書招式名
 * 鏡5 4.20 烙印  魂火化成光點飄進玩家身體 → 回到世界
 * </pre></li>
 *   <li>爆炸（variant 2）：無鏡頭——吸入一瞬，紅紫閃光 + 負片衝擊幀 + 大震動</li>
 * </ul>
 */
public class AnchorFx extends AnimeFx {
	private static final int SOUL = 0xA050FF;
	private static final int GLOW_C = 0xFFB040;

	private final int kind;
	private final int charge;

	public AnchorFx(FxPayload p) {
		super(p);
		kind = p.variant();
		charge = Math.max(1, Math.min(4, p.power()));
		main = SOUL;
		pale = 0xE8D8FF;

		if (kind == 1) {
			name = "anchor";
			impactMode = PostState.VIOLET;
			s3 = 2.3;
			impact = 2.7;
			end = 5.8;
			fadeColor = pale;
			look("anchor", 4.3);

			if (mine) {
				Vec3 top = frame.at(0, 1.1, 0);
				Cinematic.play(List.of(
					Shot.of(1.2, frame.at(0.9, 0.35, 1.6), frame.at(0.65, 0.42, 1.25), top).roll(5, 8).fov(0.88f, 0.82f),
					Shot.of(s3 - 1.2, frame.at(-2.4, 1.6, 0.6), frame.at(-2.2, 1.8, 1.6), frame.at(0, 1.0, 1.4)).roll(-3, -4),
					Shot.of(impact - s3, frame.at(0.15, 1.6, 0.8), frame.at(0.1, 1.5, 0.6), top).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
					Shot.of(4.2 - impact, frame.at(1.4, 0.25, 1.8), frame.at(1.7, 0.32, 2.3), frame.at(0, 2.2, 0)).look(frame.at(0, 2.4, 0), frame.at(0, 1.6, 0))
						.roll(12, 9).fov(1.12f, 1.06f),
					Shot.of(end - 4.2, frame.at(-1.2, 1.8, 3.6), frame.at(-0.9, 1.7, 4.4), frame.at(0, 1.2, 1.6)).roll(-3, -1)
				));
			}
		}
		else {
			end = kind == 0 ? 1.4 : 1.6;
		}
	}

	private Vec3 playerChest() {
		var level = Minecraft.getInstance().level;
		Entity e = level == null ? null : level.getEntity(p.actor());
		return e == null ? frame.at(0, 1.2, 3) : e.position().add(0, 1.1, 0);
	}

	/** 錨頂四個角落的燈 */
	private Vec3 lamp(int i) {
		double a = Math.PI / 4 + i * Math.PI / 2;
		return new Vec3(c.x + Math.cos(a) * 0.36, pos.getY() + 1.02, c.z + Math.sin(a) * 0.36);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 top = new Vec3(c.x, pos.getY() + 1.05, c.z);

		if (kind == 0) {
			if (cue(0.0)) {
				Sfx.play(mine, "anchor_charge", c, 1f, 0.85f + charge * 0.08f);
			}

			if (t < 0.5) {
				int n = rate(120, dt);

				for (int i = 0; i < n; i++) {
					Vec3 from = top.add(Particles.randomDir(1.2 + Particles.RNG.nextDouble()));
					Particles.spawn(Kind.SPARK, from, Vec3.ZERO, 0.4).size(0.04f, 0.01f).color(0xFFFFFF, GLOW_C).physics(0, 0).attract(top, 26f);
				}
			}

			if (cue(0.45)) {
				shake(0.15f + charge * 0.06f);

				if (mine) {
					PostState.split(0.4f + charge * 0.15f, 0.8f, 180);
					PostState.zoom(0.15f + charge * 0.05f, 150);
				}

				for (int i = 0; i < 20 + charge * 10; i++) {
					Particles.spawn(Kind.FLAME, top, Particles.randomDir(Particles.rand(0.5f, 2f)).add(0, 1.5, 0), Particles.rand(0.4f, 0.8f))
						.size(Particles.rand(0.12f, 0.22f), 0.02f).color(0xE0C0FF, SOUL).physics(-1f, 2f);
				}

				if (charge == 4) {
					Particles.spawn(Kind.HIT, top, Vec3.ZERO, 0.3).size(1.2f, 1.6f).color(0xFFFFFF, SOUL);

					if (mine) {
						PostState.flash(SOUL, 0.3f, 150, 0);
					}
				}
			}

			return;
		}

		if (kind == 2) {
			if (cue(0.0)) {
				Sfx.play(mine, "anchor_blast", c, 1.4f, 1f);

				if (near) {
					PostState.darken(0.5f);
				}
			}

			if (cue(0.3)) {
				shake(1.4f);

				if (near) {
					PostState.darken(0);
					PostState.impact(PostState.NEGATIVE, 160);
					PostState.flash(0xFF4060, 0.7f, 220, 0.2f);
					PostState.shock(1.4f, 700);
					PostState.split(2.5f, 0.3f, 600);
					CameraShake.fovPunch(0.16f);
				}

				for (int i = 0; i < 160; i++) {
					Particles.spawn(Kind.FLAME, top, Particles.randomDir(Particles.rand(3f, 9f)), Particles.rand(0.4f, 1f)).size(Particles.rand(0.25f, 0.45f), 0.05f)
						.color(0xFFC0E0, SOUL).physics(-0.5f, 2.5f);
				}
			}

			return;
		}

		// ---------------------------------------------------------- 契約
		if (cue(0.0)) {
			Sfx.play(mine, "anchor_charge", c, 1f, 0.8f);
		}

		if (cue(impact - 0.6)) {
			// anchor_pact 有 0.6 秒的吸入，鎖鏈扣上的瞬間正好落在 impact
			Sfx.play(mine, "anchor_pact", c, 1.3f, 1f);
		}

		// 魂火持續從錨頂冒出
		if (t < 4.4) {
			int n = rate(t < impact ? 30 : 60 * (1 - span(t, impact, 4.4)), dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = top.add(Particles.rand(-0.35f, 0.35f), 0, Particles.rand(-0.35f, 0.35f));
				Particles.spawn(Kind.FLAME, at, new Vec3(0, Particles.rand(0.8f, 2f), 0), Particles.rand(0.5f, 0.9f)).size(Particles.rand(0.12f, 0.22f), 0.02f)
					.color(0xE0C0FF, SOUL).physics(-1f, 2f);
			}
		}

		// 烙印：光點飄進玩家身體
		if (t > 4.2) {
			Vec3 chest = playerChest();
			int n = rate(40, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = top.add(Particles.randomDir(0.8));
				Particles.spawn(Kind.FLARE, from, chest.subtract(from).scale(0.6), 1.4).size(0.1f, 0.02f).color(0xFFFFFF, SOUL).physics(0, 0.3f).attract(chest, 6f);
			}
		}

		beats(t, dt, top);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 top = new Vec3(c.x, pos.getY() + 1.2, c.z);
		float life = 1 - span(t, end - 0.5, end);
		float burst = kind == 1 && t > impact ? 1 - span(t, impact, impact + 1.4) : 0;
		lights.add(new CinemaPost.Light(top, 3f + burst * 3, r(SOUL), g(SOUL), b(SOUL), (0.5f + burst * 0.7f) * life, true));

		if (burst > 0.01f) {
			rays[0] = top;
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst * 0.5f;
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.15, end - 0.5, end);
		Vec3 floor = new Vec3(c.x, pos.getY() + 0.03, c.z);
		Vec3 top = new Vec3(c.x, pos.getY() + 1.05, c.z);
		float pulse = 0.75f + 0.25f * (float)Math.sin(t * 9);

		// 四盞魂燈
		int lit = kind == 1 ? 4 : charge;

		for (int i = 0; i < 4; i++) {
			Vec3 l = lamp(i);
			boolean on = i < lit;
			float a = on ? (kind == 0 && i == lit - 1 ? easeOut(span(t, 0.4, 0.55)) : 1) * life : 0.15f * life;
			d.billboard(Mode.GLOW, GLOW, l.x, l.y, l.z, 0.35f * pulse, 0, r(GLOW_C), g(GLOW_C), b(GLOW_C), a);
			d.billboard(Mode.GLOW, FLARE, l.x, l.y, l.z, 0.3f, (float)t, 1f, 1f, 1f, a * 0.6f);
		}

		if (kind == 0) {
			float k = span(t, 0.45, 1.1);

			if (k > 0 && k < 1) {
				d.flat(Mode.GLOW, RING, top, 0.3f + easeOut(k) * (1f + charge * 0.4f), 0, r(SOUL), g(SOUL), b(SOUL), (1 - k) * (1 - k));
			}

			return;
		}

		if (kind == 2) {
			float k = span(t, 0.3, 1.4);

			if (k > 0) {
				d.flat(Mode.GLOW, JAG, floor, 0.5f + easeOut(k) * 9f, 0, 1f, 0.4f, 0.6f, (1 - k) * (1 - k));
				d.billboard(Mode.GLOW, GLOW, top.x, top.y, top.z, 2f + k * 4, 0, 1f, 0.3f, 0.6f, (1 - k) * 0.8f);
			}
			else {
				float suck = span(t, 0, 0.3);
				d.billboard(Mode.GLOW, GLOW, top.x, top.y, top.z, 1.5f - suck, 0, r(SOUL), g(SOUL), b(SOUL), suck);
			}

			return;
		}

		// ---- 契約陣（地面，延伸到玩家腳下）
		float open = easeOutBack(span(t, 1.1, 1.6));
		Vec3 chest = playerChest();
		Vec3 mid = lerp(floor, new Vec3(chest.x, floor.y, chest.z), 0.5);
		d.flat(Mode.GLOW, SEAL, floor, 2.4f * easeOutBack(span(t, 0, 0.5)), (float)(t * 0.4), r(SOUL), g(SOUL), b(SOUL), 0.85f * life);

		// ---- 四條靈魂鎖鏈：從錨頂射向玩家（衝擊前在飛，衝擊時扣上）
		float shoot = easeOut(span(t, 1.2, 2.2));
		float chainA = Math.min(span(t, 1.2, 1.4), 1 - span(t, 4.0, 4.8)) * life;

		if (chainA > 0.01f) {
			Random rng = new Random((long)Math.floor(t * 12) * 7 + pos.asLong());

			for (int i = 0; i < 4; i++) {
				Vec3 from = lamp(i).add(0, 0.05, 0);
				double ang = i * Math.PI / 2 + t * 0.6;
				Vec3 to = chest.add(Math.cos(ang) * 0.35, Math.sin(ang * 2) * 0.25, Math.sin(ang) * 0.35);
				Vec3 tip = lerp(from, to, shoot);
				int links = 14;

				for (int k = 0; k < links; k++) {
					double f0 = (double)k / links;
					double f1 = f0 + 0.55 / links;
					Vec3 a = lerp(from, tip, f0);
					Vec3 b = lerp(from, tip, f1);
					double sag = Math.sin(f0 * Math.PI) * 0.25 * (1 - span(t, impact, impact + 0.2));
					a = a.add(0, -sag, 0);
					b = b.add(0, -Math.sin(f1 * Math.PI) * 0.25 * (1 - span(t, impact, impact + 0.2)), 0);
					d.streak(Mode.GLOW, SPARK, a.x, a.y, a.z, b.x, b.y, b.z, 0.12f, r(SOUL), g(SOUL), b(SOUL), chainA);
				}

				if (t > impact && t < impact + 0.5) {
					bolt(d, from, to, rng, 0.04f, 1 - span(t, impact, impact + 0.5), SOUL);
				}
			}
		}

		// ---- 魂柱沖天
		float pillar = t < impact ? 0 : 1 - span(t, impact + 0.1, impact + 1.4);

		if (pillar > 0.01f) {
			float w = easeOut(span(t, impact, impact + 0.06)) * (0.5f + 0.5f * pillar);
			d.cylinder(Mode.GLOW, BEAM, top, 0.6f * w, 30f, 24, 1, 0, 5, (float)(t * 3), 1.2f, r(SOUL), g(SOUL), b(SOUL), pillar * 0.6f, pillar * 0.1f);
			d.cylinder(Mode.GLOW, BEAM, top, 0.2f * w, 30f, 12, 1, 0.5f, 4, (float)(t * 5), 1f, 1f, 1f, 1f, pillar * 0.8f, pillar * 0.2f);
		}

		// 錨頂的魂火核心
		FxMesh spirit = FxMesh.get("spirit");
		float core = Math.min(span(t, 0, 0.5), 1 - span(t, 4.4, 5.2));
		Matrix4f m = new Matrix4f().rotateY((float)(t * 3)).scale(0.6f, 0.75f + 0.08f * (float)Math.sin(t * 12), 0.6f);
		d.mesh(Mode.GLOW, spirit, top.add(0, 0.2, 0), m, r(SOUL), g(SOUL), b(SOUL), 0.8f * core, false);
		d.billboard(Mode.GLOW, GLOW, top.x, top.y + 0.3, top.z, 1.2f * pulse, 0, r(SOUL), g(SOUL), b(SOUL), 0.5f * core);

		// 烙印：玩家胸口的契約印記
		float brand = span(t, 4.3, 4.7) * (1 - span(t, end - 0.4, end));

		if (brand > 0.01f) {
			d.billboard(Mode.GLOW, SEAL, chest.x, chest.y, chest.z, 0.6f, (float)t, r(SOUL), g(SOUL), b(SOUL), brand);
		}

		shockRings(d, t, floor.add(0, 0.04, 0), top, 6f);
	}
}
