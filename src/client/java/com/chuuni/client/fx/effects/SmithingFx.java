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
import com.chuuni.client.render.FxMesh;
import com.chuuni.net.FxPayload;

import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 鍛造台・神匠鍛冶（升級獄髓 / 盔甲紋飾）。
 * <pre>
 * 鏡1 0.00 顯紋  桌面浮現金色鍛紋陣，成品浮起，漆黑的獄髓霧氣纏上來
 * 鏡2 1.20 鍛打  俯角：神匠之錘從正上方垂直砸下兩次（頓幀 + 金色火花）
 * 鏡3 2.30 吸氣  成品極近特寫
 * 衝擊 2.70     第三擊・融合：黑金色的內爆 + 衝擊幀，毛筆直書招式名
 * 鏡4 2.70 英雄  超低角度仰望
 * 鏡5 4.10 降臨  成品帶著金光緩緩降回桌面 → 變成只有自己能撿的發光掉落物，直接回到世界
 * </pre>
 */
public class SmithingFx extends AnimeFx {
	private static final int GOLD = 0xFFC860;
	private static final int DARK = 0x3A2848;
	private static final double[] STRIKES = {1.45, 1.95, 2.7};
	private static final double S5 = 4.1;

	private final float spin0;

	public SmithingFx(FxPayload p) {
		super(p);
		name = "smithing";
		main = GOLD;
		pale = 0xFFF0D0;
		impactMode = PostState.EMBER;
		s3 = 2.3;
		impact = STRIKES[2];
		end = 6.9;
		fadeColor = 0xFFF4E0;
		spin0 = (float)(frame.front().x * 3);
		look("smithing", S5 + 0.4);

		if (mine) {
			Vec3 item = frame.at(0, 1.55, 0);
			Cinematic.play(List.of(
				Shot.of(1.2, frame.at(0.9, 1.05, 1.5), frame.at(0.6, 1.15, 1.15), item).roll(5, 8).fov(0.88f, 0.82f),
				Shot.of(s3 - 1.2, frame.at(-2.1, 1.8, 1.3), frame.at(-1.8, 2.0, 1.6), frame.at(0, 2.0, 0)).roll(-4, -6),
				Shot.of(impact - s3, frame.at(0.1, 1.65, 0.75), frame.at(0.06, 1.6, 0.55), item).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(S5 - impact, frame.at(1.4, 0.4, 2.0), frame.at(1.75, 0.48, 2.5), frame.at(0, 2.0, 0)).look(frame.at(0, 2.2, 0), item).roll(12, 9)
					.fov(1.12f, 1.06f),
				Shot.of(end - S5, frame.at(1.2, 1.25, 2.6), frame.at(0.8, 1.3, 2.0), item).look(item, frame.at(0, 1.1, 0)).roll(4, 1).fov(0.9f, 0.82f)
			), claimAndClose(SmithingMenu.class));
		}
	}

	private Vec3 itemPos(double t) {
		float rise = easeOut(span(t, 0.1, 0.9));
		double y = pos.getY() + 1.1 + rise * 0.45;

		if (t > S5) {
			float d = smooth(span(t, S5, end - 0.35));
			y = pos.getY() + 1.55 - d * 0.42 + Math.sin(t * 2.2) * 0.02 * (1 - d);
		}

		double bounce = 0;

		for (double s : STRIKES) {
			if (t >= s && t < s + 0.15) {
				bounce = -Math.sin((t - s) / 0.15 * Math.PI) * 0.05;
			}
		}

		return new Vec3(c.x, y + bounce, c.z);
	}

	/** 錘頭高度（相對物品）：高舉 → 砸下 → 反彈 */
	private double hammerLift(double t) {
		double lift = 1.4;

		for (int i = 0; i < STRIKES.length; i++) {
			double s = STRIKES[i];
			double prev = i == 0 ? 1.0 : STRIKES[i - 1];

			if (t < prev) {
				break;
			}

			if (t >= s) {
				lift = Math.sin(Math.min(t - s, 0.2) / 0.2 * Math.PI) * 0.15;
				continue;
			}

			double raise = i == 2 ? 2.0 : 1.4;
			float up = easeOut(span(t, prev + 0.05, s - 0.1));
			float slam = easeIn(span(t, s - 0.1, s));
			lift = slam > 0 ? raise * (1 - slam) : raise * up;
		}

		return lift;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 item = itemPos(t);

		if (cue(0.0)) {
			Sfx.play(mine, "forge_ignite", c, 0.8f, 1.2f);
		}

		// 纏繞上來的獄髓霧氣
		if (t > 0.3 && t < impact) {
			int n = rate(20, dt);

			for (int i = 0; i < n; i++) {
				double a = t * 2 + Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = item.add(Math.cos(a) * 0.9, -0.4, Math.sin(a) * 0.9);
				Particles.spawn(Kind.SMOKE, at, new Vec3(-Math.sin(a), 0.6, Math.cos(a)).scale(0.8), 1.2).size(0.18f, 0.5f).color(0x504060, 0x181020).alpha(0.6f)
					.physics(0, 1f).attract(item, 2f).spin(1f);
			}
		}

		for (int i = 0; i < 2; i++) {
			if (cue(STRIKES[i])) {
				Sfx.play(mine, "smith_hammer", c, 1f, 1f + i * 0.06f);
				FxClock.hitstop(80);
				shake(0.35f + i * 0.1f);

				if (mine) {
					PostState.split(0.6f + i * 0.4f, 1.2f + i, 200);
					PostState.zoom(0.3f, 150);
				}

				for (int k = 0; k < 70; k++) {
					double a = Particles.RNG.nextDouble() * Math.PI * 2;
					Particles.spawn(Kind.SPARK, item, new Vec3(Math.cos(a) * Particles.rand(2f, 6f), Particles.rand(0.5f, 3f), Math.sin(a) * Particles.rand(2f, 6f)),
						Particles.rand(0.4f, 0.9f)).size(0.05f, 0.015f).color(0xFFF6C0, GOLD).physics(10f, 0.6f);
				}

				Particles.spawn(Kind.HIT, item.add(0, 0.2, 0), Vec3.ZERO, 0.25).size(0.7f, 0.9f).color(0xFFFFFF, GOLD);
			}
		}

		if (cue(impact - 0.5)) {
			Sfx.play(mine, "smith_fuse", c, 1.3f, 1f);
		}

		if (cue(impact)) {
			// 融合：黑金色的碎片往外、獄髓霧被一口吸進去
			for (int i = 0; i < 50; i++) {
				Particles.spawn(Kind.SHARD, item, Particles.randomDir(Particles.rand(3f, 7f)), Particles.rand(0.8f, 1.3f)).size(0.16f, 0.04f).color(GOLD, DARK)
					.physics(4f, 2f).spin(9f);
			}
		}

		if (cue(S5 - 0.2)) {
			Sfx.play(mine, "holy_descend", c, 0.9f, 0.84f);

			if (near) {
				PostState.look("holy", 1200, (int)((end - S5 + 0.4) * 1000));
			}
		}

		if (t > S5 - 0.2) {
			int n = rate(18, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = item.add(Particles.randomDir(0.6 + Particles.RNG.nextDouble() * 0.6)).add(0, 0.6, 0);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.3, 0), 1.8).size(0.09f, 0.02f).color(0xFFFFFF, GOLD).physics(0, 0).fadeIn(0.3f).spin(1f);
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
		lights.add(new CinemaPost.Light(item, 2.5f + burst * 3, r(GOLD), g(GOLD), b(GOLD), (0.4f + burst * 0.7f) * life, true));

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
		if (!p.item().isEmpty()) {
			float spin = (float)(Math.min(t, S5) * 1.6 + Math.max(0, t - S5) * 0.6);
			d.item(p.item(), itemPos(t), 0.9f, spin0 + spin, 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, end - 0.5, end);
		Vec3 item = itemPos(t);
		Vec3 table = new Vec3(c.x, pos.getY() + 1.02, c.z);

		// 桌面的金色鍛紋陣 + 地面的大陣
		float open = easeOutBack(span(t, 0, 0.5));

		// 神匠之錘：從正上方垂直砸下
		float hammerA = Math.min(span(t, 1.0, 1.3), 1 - span(t, impact + 0.6, impact + 1.1));

		if (hammerA > 0.01f) {
			double lift = hammerLift(t);
			Vec3 pivot = item.add(0, 0.25 + 1.69 + lift, 0);
			Matrix4f m = new Matrix4f().rotateY(spin0 + 0.6f).rotateZ((float)Math.PI);
			FxMesh mesh = FxMesh.get("hammer");
			d.mesh(Mode.SOLID, mesh, pivot, m, 0.55f, 0.48f, 0.42f, 0.95f * hammerA, true);
			d.mesh(Mode.GLOW, mesh, pivot, new Matrix4f(m).scale(1.06f), r(GOLD), g(GOLD), b(GOLD), 0.35f * hammerA, false);
		}

		// 每次敲擊的金環
		for (double s : STRIKES) {
			if (t > s && t < s + 0.6) {
				float k = span(t, s, s + 0.55);
				d.flat(Mode.GLOW, RING, item, 0.3f + easeOut(k) * (s == impact ? 3f : 1.6f), 0, r(GOLD), g(GOLD), b(GOLD), (1 - k) * (1 - k));
			}
		}

		// 融合前：物品周圍纏繞的黑金螺旋
		if (t > 0.6 && t < impact) {
			float a = span(t, 0.6, 1.0) * life;
			float tight = 1 - 0.7f * span(t, s3, impact);

			for (int k = 0; k < 3; k++) {
				double ang = t * 3 + k * Math.PI * 2 / 3;
				Vec3 a0 = item.add(Math.cos(ang) * 0.7 * tight, -0.4, Math.sin(ang) * 0.7 * tight);
				Vec3 a1 = item.add(Math.cos(ang + 1.6) * 0.7 * tight, 0.4, Math.sin(ang + 1.6) * 0.7 * tight);
				d.streak(Mode.GLOW, TRAIL, a0.x, a0.y, a0.z, a1.x, a1.y, a1.z, 0.18f, r(GOLD), g(GOLD), b(GOLD), 0.6f * a);
			}
		}

		// 成品的光芒
		float glow = (t > impact ? 1 : 0.4f) * life;
		d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.1f, 0, r(GOLD), g(GOLD), b(GOLD), 0.35f * glow);

		if (t > S5) {
			float holy = span(t, S5, S5 + 0.6) * (1 - span(t, end - 0.3, end));
			d.cylinder(Mode.GLOW, BEAM, table, 0.5f, 10f, 20, 1, 0.3f, 2, (float)(-t * 0.5), 1f, 1f, 0.92f, 0.75f, 0.12f * holy, 0);
			d.flat(Mode.GLOW, RING, item.add(0, 0.5, 0), 0.3f, (float)t * 0.5f, r(GOLD), g(GOLD), b(GOLD), 0.7f * holy);
		}

		shockRings(d, t, new Vec3(c.x, pos.getY() + 0.05, c.z), item, 5f);
	}
}
