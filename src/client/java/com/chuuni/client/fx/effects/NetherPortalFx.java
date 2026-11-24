package com.chuuni.client.fx.effects;

import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Frame;
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
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 地獄門・冥界開門（點燃黑曜石框）。一生沒幾次，所以是完整的大招：衝擊後漫畫分格時停 + 四角大字。
 * <pre>
 * 鏡1 0.00 點火  貼地仰拍：火焰從點燃處沿著門框內緣往兩側一路燒上去，繞成一圈
 * 鏡2 1.30 漩渦  正面廣角：門中央浮現紫色漩渦陣，越轉越快、越來越大
 * 鏡3 2.40 吸氣  漩渦中心極近特寫
 * 衝擊 2.80     冥界之門開啟：紫紅衝擊幀 + 頓幀 + 漫畫分格 + 四角大字，漩渦炸開填滿整個門
 * 鏡4 2.80 英雄  超低角度仰望（慢動作）
 * 鏡5 4.30 異界  拉遠：紫色的餘燼被吸進門裡，地面殘留冥界的裂紋
 * </pre>
 */
public class NetherPortalFx extends AnimeFx {
	private static final int VOID = 0xB040FF;
	private static final int FIRE = 0xFF6A20;

	private final int width;
	private final int height;
	private final Vec3 center;
	private final Vector3f right;
	private final Vector3f normal;
	private final Frame pf;

	public NetherPortalFx(FxPayload p) {
		super(p);
		width = Math.max(2, p.power() / 100);
		height = Math.max(3, p.power() % 100);
		center = p.at();
		right = p.variant() == 0 ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
		var player = Minecraft.getInstance().player;
		Vector3f n = p.variant() == 0 ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0);

		if (player != null) {
			Vec3 toP = player.position().subtract(center);

			if (toP.x * n.x + toP.z * n.z < 0) {
				n.negate();
			}
		}

		normal = n;
		Vec3 bottom = center.add(0, -height / 2.0, 0);
		pf = new Frame(bottom, new Vec3(normal.z, 0, -normal.x), new Vec3(normal.x, 0, normal.z));
		name = "nether_portal";
		main = VOID;
		pale = 0xF0D8FF;
		impactMode = PostState.CRIMSON;
		manga = true;
		s3 = 2.4;
		impact = 2.8;
		end = 6.6;
		fadeColor = 0xF0D8FF;
		look("nether", 4.6);

		if (mine) {
			double h = height;
			Vec3 mid = pf.at(0, h / 2, 0);
			Cinematic.play(List.of(
				Shot.of(1.3, pf.at(width * 0.5 + 0.8, 0.3, 2.0), pf.at(width * 0.4 + 0.6, 0.4, 1.6), pf.at(0, 0.5, 0)).look(pf.at(0, 0.3, 0), pf.at(0, h * 0.6, 0))
					.roll(6, 9).fov(0.95f, 0.88f),
				Shot.of(s3 - 1.3, pf.at(-1.2, h * 0.55, h * 1.3 + 1.5), pf.at(-0.6, h * 0.5, h * 1.1 + 1.0), mid).roll(-3, -4),
				Shot.of(impact - s3, pf.at(0.12, h / 2 + 0.05, 0.9), pf.at(0.08, h / 2 + 0.03, 0.65), mid).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.3 - impact, pf.at(width * 0.5 + 0.5, 0.2, 1.8), pf.at(width * 0.5 + 0.9, 0.3, 2.4), pf.at(0, h * 0.8, 0))
					.look(pf.at(0, h * 0.9, 0), mid).roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(end - 4.3, pf.at(-2.5, h * 0.4, h * 1.4 + 2), pf.at(-3.2, h * 0.6, h * 1.8 + 3), mid).roll(-3, -1)
			));
		}
	}

	/** 門框內緣的周長參數 s（0~1）→ 位置；從底部中央開始往兩側燒 */
	private Vec3 rim(double s, int side) {
		double w = width / 2.0;
		double h = height / 2.0;
		double perim = 2 * w + 2 * h;
		double d = s * perim / 2;
		double u;
		double v;

		if (d < w) {
			u = d;
			v = -h;
		}
		else if (d < w + 2 * h) {
			u = w;
			v = -h + (d - w);
		}
		else {
			u = w - (d - w - 2 * h);
			v = h;
		}

		u *= side;
		return center.add(right.x * u, v, right.z * u);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "portal_ignite", c, 1.1f, 1f);
		}

		// 沿門框燒上去的火焰
		if (t < 1.4) {
			float k = easeOut(span(t, 0.05, 1.2));
			int n = rate(160, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = rim(k * Particles.RNG.nextDouble(), Particles.RNG.nextBoolean() ? 1 : -1);
				Particles.spawn(Kind.FLAME, at, new Vec3(0, Particles.rand(0.5f, 1.5f), 0), Particles.rand(0.3f, 0.6f)).size(Particles.rand(0.15f, 0.3f), 0.03f)
					.color(0xFFE0A0, FIRE).physics(-1f, 2f);
			}
		}

		// 被吸進漩渦的紫色微粒
		if (t > 1.2 && t < impact) {
			int n = rate(80, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = center.add(right.x * Particles.rand(-2.5f, 2.5f), Particles.rand(-2.5f, 2.5f), right.z * Particles.rand(-2.5f, 2.5f))
					.add(normal.x * 0.5, 0, normal.z * 0.5);
				Particles.spawn(Kind.GLOW, from, Vec3.ZERO, 0.6).size(0.06f, 0.01f).color(0xFFFFFF, VOID).physics(0, 0).attract(center, 12f);
			}
		}

		if (cue(impact - 0.5)) {
			Sfx.play(mine, "portal_open", c, 1.4f, 1f);
		}

		if (cue(impact)) {
			for (int i = 0; i < 120; i++) {
				Vec3 dir = new Vec3(right.x * Particles.rand(-1, 1) + normal.x * Particles.rand(0.5f, 2f), Particles.rand(-1, 1),
					right.z * Particles.rand(-1, 1) + normal.z * Particles.rand(0.5f, 2f));
				Particles.spawn(Kind.FLAME, center, dir.scale(Particles.rand(2f, 6f)), Particles.rand(0.5f, 1.1f)).size(Particles.rand(0.2f, 0.4f), 0.04f)
					.color(0xF0C0FF, VOID).physics(-0.5f, 2f);
			}
		}

		// 異界：紫色的餘燼往門裡飄
		if (t > impact) {
			int n = rate(30, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = center.add(Particles.randomDir(3)).add(normal.x * 1.5, 0, normal.z * 1.5);
				Particles.spawn(Kind.GLOW, from, Vec3.ZERO, 1.5).size(0.05f, 0.01f).color(0xFFD0FF, VOID).physics(0, 0.3f).attract(center, 3f).fadeIn(0.3f);
			}
		}

		beats(t, dt, center);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.5, end);
		float fire = 1 - span(t, 1.2, 2.2);
		float vortex = span(t, 1.2, impact);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.4) : 0;
		lights.add(new CinemaPost.Light(center.add(0, -height / 2.0 + 0.3, 0), 4f, 1f, 0.45f, 0.15f, 1.2f * fire, true));
		lights.add(new CinemaPost.Light(center, 3f + height, r(VOID), g(VOID), b(VOID), (vortex * 0.5f + burst * 0.8f + (t > impact ? 0.3f : 0)) * life, true));

		if (burst > 0.01f) {
			rays[0] = center;
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
		Vec3 front = center.add(normal.x * 0.12, 0, normal.z * 0.12);

		// ---- 門框內緣的火線（燒過去的地方留下發光的線）
		float k = easeOut(span(t, 0.05, 1.2));
		float rimA = (1 - span(t, impact + 0.5, end)) * life;

		if (rimA > 0.01f) {
			int segs = 40;

			for (int side = -1; side <= 1; side += 2) {
				for (int i = 0; i < segs; i++) {
					double s0 = (double)i / segs;
					double s1 = (double)(i + 1) / segs;

					if (s0 > k) {
						break;
					}

					Vec3 a = rim(s0, side).add(normal.x * 0.05, 0, normal.z * 0.05);
					Vec3 b = rim(Math.min(s1, k), side).add(normal.x * 0.05, 0, normal.z * 0.05);
					int col = t < impact ? FIRE : VOID;
					d.streak(Mode.GLOW, SPARK, a.x, a.y, a.z, b.x, b.y, b.z, 0.3f, r(col), g(col), b(col), 0.6f * rimA);
					d.streak(Mode.GLOW, SPARK, a.x, a.y, a.z, b.x, b.y, b.z, 0.1f, 1f, 0.9f, 0.7f, rimA);
				}
			}
		}

		// 火頭
		if (t < 1.3) {
			for (int side = -1; side <= 1; side += 2) {
				Vec3 h = rim(k, side);
				d.billboard(Mode.GLOW, GLOW, h.x, h.y, h.z, 0.9f, 0, 1f, 0.55f, 0.15f, 0.9f);
				d.billboard(Mode.GLOW, FLARE, h.x, h.y, h.z, 0.6f, (float)t * 3, 1f, 0.9f, 0.6f, 0.8f);
			}
		}

		// ---- 漩渦陣（在門的平面上）
		float grow = easeIn(span(t, 1.2, impact));
		float size = Math.min(width, height) * 0.5f;

		if (t < impact + 0.1 && grow > 0.01f) {
			d.disc(Mode.GLOW, JAG, front, right, up, size * (0.3f + grow * 0.7f), (float)(-t * (2 + grow * 8)), r(VOID), g(VOID), b(VOID), 0.9f * grow);
			d.disc(Mode.GLOW, GLOW, front.add(normal.x * 0.02, 0, normal.z * 0.02), right, up, size * 0.6f * grow, (float)(t * 4), 1f, 0.85f, 1f, 0.7f * grow);
			d.billboard(Mode.GLOW, GLOW, center.x, center.y, center.z, 0.5f + grow * 1.5f, 0, r(VOID), g(VOID), b(VOID), grow);
		}

		// ---- 衝擊：漩渦炸開、紫色雷光沿門框亂竄
		if (t > impact && t < impact + 1.2) {
			float w = span(t, impact, impact + 0.8);
			float a = (1 - w) * (1 - w);
			d.disc(Mode.GLOW, JAG, front, right, up, size + easeOut(w) * height * 1.5f, 0.3f, 1f, 1f, 1f, a);
			d.disc(Mode.GLOW, RING, front, right, up, size + easeOut(w) * height * 2.2f, 0, r(VOID), g(VOID), b(VOID), a);
			Random rng = new Random((long)Math.floor(t * 12) * 13);

			for (int i = 0; i < 4; i++) {
				bolt(d, rim(rng.nextDouble(), 1), rim(rng.nextDouble(), -1), rng, 0.05f, a, VOID);
			}
		}

		// ---- 開啟後：門面上緩慢旋轉的冥界陣
		if (t > impact) {
			float a = Math.min(span(t, impact, impact + 0.3), 1 - span(t, end - 1.2, end)) * 0.5f;
		}

		Vec3 ground = center.add(0, -height / 2.0 + 0.05, 0).add(normal.x * 1.0, 0, normal.z * 1.0);
		shockRings(d, t, ground, center, 7f);
	}
}
