package com.chuuni.client.fx.effects;

import java.util.List;

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
 * 鐘・晨鐘十二響（power = 第幾響，同一口鐘 2.5 秒內連敲往上數）：
 * <pre>
 * 一響破邪  二響撼心  三響震天  四響懾靈  五響伏衆  六響斷魔
 * 七響裂空  八響鎮魂  九響焚獄  十響逆命  十一響滅界  十二響歸墟
 * </pre>
 * 每一響：被敲的那一側閃出衝擊 → 金色聲波一圈圈擴散 → 破邪光芒四射、光塵飄落；
 * 鐘的四周浮著十二顆「響珠」，敲到第幾響就點亮幾顆。響數越高聲波越多、光芒越長，
 * 六響起地上浮現法陣、九響起升起光柱；第十二響：十二顆響珠一齊射出光束、法陣炸開、天降光柱。
 */
public class BellFx extends MinorFx {
	private static final int GOLD = 0xFFD070;
	private static final int HOLY = 0xFFF6E0;
	private static final int DARK = 0xFF6040;

	private final Vec3 ground;
	private final int toll;
	private final boolean last;
	/** 這一響的強度 0（一響）→ 1（十二響） */
	private final float lv;
	private final double[] waves;

	public BellFx(FxPayload p) {
		super(p);
		toll = Math.max(1, Math.min(12, p.power()));
		last = toll == 12;
		lv = (toll - 1) / 11f;
		ground = new Vec3(c.x, Math.floor(c.y) + 0.03, c.z);
		int n = 1 + toll / 3;
		waves = new double[n];

		for (int i = 0; i < n; i++) {
			waves[i] = i * (0.42 - lv * 0.12);
		}

		end = last ? 4.2 : 2.6 + lv * 0.6;
		// 第十二響借用 A 級的衝擊環（impact = 法陣炸開的時間）
		impact = 0.9;
		main = GOLD;
		pale = HOLY;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			// 一～五響：晨鐘；六～十一響：更低更厚的巨鐘（越後面越低沉）
			// 動漫素材「お寺の鐘」越敲越低沉；沒有素材時用合成的晨鐘 / 巨鐘
			if (!Sfx.anime(mine, "temple_bell", null, c, 1f, 1.15f - lv * 0.4f)) {
				Sfx.play(mine, toll <= 5 ? "bell_toll" : "bell_deep", c, 1f, (toll <= 5 ? 1.05f : 1.15f) - lv * 0.3f);
			}
			tint(last ? HOLY : GOLD, 1.6 + lv * 1.5);
			smallName("bell" + toll);
		}

		// 十二響歸墟：倒吸之後三口巨鐘齊鳴，重擊正好落在法陣炸開（0.9 秒）
		if (last && cue(0.2)) {
			Sfx.play(mine, "bell_final", c, Sfx.has("anime_holy") ? 0.7f : 1.2f, 1f);
		}

		// 「聖魔法」高峰在 0.35 秒，提早播讓它對上法陣炸開
		if (last && cue(0.55)) {
			// 「聖魔法」裁きの光：正好在法陣炸開前湧起
			Sfx.anime(mine, "holy", null, c, 1f, 1f);
		}

		if (cue(0.1)) {
			pop(GOLD, 0.35f + lv * 0.45f);
			Vec3 side = c.add(nv.scale(-0.35));
			Particles.spawn(Kind.HIT, side, Vec3.ZERO, 0.25).size(0.8f + lv, 1.2f + lv * 1.5f).color(0xFFFFFF, GOLD);
			sparks(side, 20 + toll * 4, 4f + lv * 3, GOLD, 0.4f);
		}

		for (double w : waves) {
			if (w > 0 && cue(w + 0.1)) {
				shake(0.06f + lv * 0.08f);
			}
		}

		if (last) {
			if (cue(0.9)) {
				Sfx.play(mine, "shockwave", c, 1f, 0.75f);
				shake(0.7f);

				if (mine) {
					PostState.flash(HOLY, 0.4f, 220, 0.1f);
					PostState.shock(1f, 600);
					PostState.lines(0.9f, false, 900, 0.4f);
				}

				for (int i = 0; i < 120; i++) {
					Particles.spawn(Kind.FLARE, c, Particles.randomDir(Particles.rand(2f, 7f)).add(0, 1.5, 0), Particles.rand(0.8f, 1.6f)).size(0.1f, 0.02f)
						.color(0xFFFFFF, i % 4 == 0 ? DARK : GOLD).physics(1.5f, 1.2f).spin(3f);
				}
			}
		}

		if (t > 0.3 && t < end - 0.6) {
			int nn = rate(20 + toll * 6, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double rr = Math.sqrt(Particles.RNG.nextDouble()) * (3 + lv * 2);
				Vec3 at = new Vec3(c.x + Math.cos(a) * rr, c.y + 2 + Particles.RNG.nextDouble() * (1 + lv * 2), c.z + Math.sin(a) * rr);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.8, 0), 2.0).size(0.06f, 0.02f).color(0xFFFFFF, GOLD).physics(0, 0).fadeIn(0.2f).spin(2f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float k = 1 - span(t, 0.1, end);
		lights.add(new CinemaPost.Light(c, 4f + lv * 3, r(GOLD), g(GOLD), b(GOLD), (0.5f + lv * 0.3f) * k, true));

		if (toll >= 9) {
			float p = pillar(t);
			segs.add(new CinemaPost.Seg(c, c.add(0, 30, 0), 5, r(HOLY), g(HOLY), b(HOLY), p * 0.25f, 0.3f));
		}

		if (t < 1.2) {
			rays[0] = c;
			raysP[0] = r(HOLY);
			raysP[1] = g(HOLY);
			raysP[2] = b(HOLY);
			raysP[3] = (0.25f + lv * 0.2f) * (1 - span(t, 0.1, 1.2));
		}
	}

	private float pillar(double t) {
		return Math.min(easeOut(span(t, 0.15, 0.45)), 1 - span(t, end - 1.2, end));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		// 聲波
		for (int i = 0; i < waves.length; i++) {
			double w0 = waves[i] + 0.1;
			float k = span(t, w0, w0 + 1.3);

			if (k <= 0 || k >= 1) {
				continue;
			}

			float a = (1 - k) * (1 - k) * life;
			float rr = 0.6f + easeOut(k) * (7f + lv * 5 - i * 0.8f);
			int col = i == 0 ? HOLY : GOLD;
			d.flat(Mode.GLOW, i == 0 ? JAG : RING, ground, rr, 0, r(col), g(col), b(col), a);
			d.billboard(Mode.GLOW, RING, c.x, c.y, c.z, 0.5f + easeOut(k) * (3.5f + lv * 2 - i * 0.4f), 0, r(GOLD), g(GOLD), b(GOLD), a * 0.7f);
		}

		// 破邪光芒（越後面越長）
		float ray = easeOut(span(t, 0.1, 0.5));
		float ra = (1 - span(t, 0.4, 1.4 + lv * 0.6)) * life;

		if (ra > 0.01f) {
			for (int i = 0; i < 12; i++) {
				double a = i * Math.PI / 6 + 0.13;
				Vec3 dir = new Vec3(Math.cos(a), 0.15 * Math.sin(i * 2.1), Math.sin(a));
				line(d, SPARK, c.add(dir.scale(0.5)), c.add(dir.scale(0.5 + ray * (3.2 + lv * 4))), 0.08f + lv * 0.06f, i % 2 == 0 ? HOLY : GOLD, ra);
			}
		}

		// 十二顆響珠：敲到第幾響亮幾顆（最新的那顆亮一下）
		float orbs = fade(t, 0.15, end - 0.6, end);
		float spin = (float)(t * 0.4);

		for (int i = 0; i < 12; i++) {
			double a = spin + i * Math.PI / 6 - Math.PI / 2;
			Vec3 o = c.add(Math.cos(a) * 1.5, 0.6, Math.sin(a) * 1.5);
			boolean lit = i < toll;
			float fresh = i == toll - 1 ? 1 - span(t, 0.1, 0.6) : 0;
			float sz = lit ? 0.22f + fresh * 0.35f : 0.1f;
			int col = lit ? (i == 11 ? HOLY : GOLD) : 0x806040;
			d.billboard(Mode.GLOW, GLOW, o.x, o.y, o.z, sz, 0, r(col), g(col), b(col), orbs * (lit ? 0.9f : 0.35f));

			if (lit) {
				d.billboard(Mode.GLOW, FLARE, o.x, o.y, o.z, sz * 1.1f, (float)t * 2, 1f, 1f, 1f, orbs * 0.6f);
			}

			// 十二響：每顆響珠往天上射出一道光
			if (last) {
				float up = Math.min(span(t, 0.5 + i * 0.02, 0.8 + i * 0.02), 1 - span(t, 1.0, 1.4));

				if (up > 0.01f) {
					line(d, SPARK, o, o.add(0, 12 * up, 0), 0.1f, HOLY, up * life);
				}
			}
		}

		// 六響起：地上的法陣（第十二響在 0.9 秒炸開）
		if (toll >= 6) {
			float open = easeOutBack(span(t, 0.05, 0.45));
			float burst = last ? easeOut(span(t, 0.9, 1.5)) : 0;
			float a = (0.25f + lv * 0.45f) * life * (1 - burst);
			d.flat(Mode.GLOW, SEAL, ground.add(0, 0.02, 0), (1.8f + lv * 1.2f) * open * (1 + burst * 2.5f), (float)(t * (0.4 + lv)), r(GOLD), g(GOLD), b(GOLD), a);

			if (toll >= 10) {
				com.chuuni.client.fx.Ambient.twelveStar(d, ground.add(0, 0.04, 0), (2.4f + lv) * open * (1 + burst * 3), (float)(-t * 0.6), GOLD, a * 0.8f);
			}
		}

		// 九響起：光柱
		if (toll >= 9) {
			float p = pillar(t) * life;
			float w = last ? 1.4f : 0.5f + (toll - 9) * 0.2f;
			d.cylinder(Mode.GLOW, BEAM, ground, w, 30f, 24, 1, 0, 4, (float)(-t * 2), 1.1f, r(GOLD), g(GOLD), b(GOLD), p * 0.35f, 0);
			d.cylinder(Mode.GLOW, BEAM, ground, w * 0.3f, 32f, 12, 1, 0.5f, 4, (float)(-t * 3), 1f, 1f, 1f, 1f, p * 0.5f, 0);
		}

		if (last) {
			shockRings(d, t, ground, c, 9f);
		}

		glow(d, c, 1.6f + lv * 1.4f, GOLD, (0.5f + lv * 0.3f) * (1 - span(t, 0.1, 1.5)));
	}
}
