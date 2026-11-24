package com.chuuni.client.fx.effects;

import java.util.ArrayList;
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
 * 終界門框・終焉之門。
 * <ul>
 *   <li>嵌入一顆眼（variant 0，power = 附近已嵌入的眼數）：無鏡頭 1.4 秒——眼「喀」地嵌入，虛空光柱衝上去，
 *       之後這道光柱就常駐在眼上（{@link com.chuuni.client.fx.Ambient}），越多顆音越高</li>
 *   <li>12 顆滿、門開啟（variant 1）：
 * <pre>
 * 鏡1 0.00 點眼  低角度環繞：12 顆眼一顆一顆依序點亮，各自升起光柱
 * 鏡2 1.60 共鳴  高空俯瞰：12 道光柱沿螺旋轉圈往中心收攏，地上拖出的光軌畫出「十二法陣」
 * 鏡3 2.60 吸氣  合成中央一道大光柱，十二法陣越轉越快、往內收緊
 * 衝擊 3.00     十二法陣炸開 → 終焉之門開啟：負片衝擊幀 + 漫畫分格時停 + 四角大字，星海從門中湧出
 * 鏡4 3.00 英雄  門內往上仰望（慢動作）
 * 鏡5 4.50 虛空  拉上高空俯瞰：星海沉入門中、虛空光柱沖天 → 回到世界（大光柱之後常駐在門上）
 * </pre></li>
 * </ul>
 */
public class EndPortalFx extends AnimeFx {
	private static final int VOIDC = 0x60FFD0;
	private static final int DEEP = 0x205050;

	private final boolean open;
	private final int eyes;
	private final List<Vec3> frames = new ArrayList<>();
	/** 收攏開始 / 合一 */
	private static final double GATHER = 1.6;
	private static final double MERGE = 2.6;
	private boolean released;

	public EndPortalFx(FxPayload p) {
		super(p);
		open = p.variant() == 1;
		eyes = Math.max(1, p.power());
		main = VOIDC;
		pale = 0xE8FFF6;

		if (!open) {
			end = 1.4;
			com.chuuni.client.fx.Ambient.rescan();
			return;
		}

		// 演出中先藏起這座門的常駐光，演完再淡入
		com.chuuni.client.fx.Ambient.hold(Vec3.atCenterOf(pos).add(0, 0.26, 0));

		// 12 個門框：3×3 門的四邊各三格（離中心 2 格）
		Vec3 ctr = Vec3.atCenterOf(pos);

		for (int side = 0; side < 4; side++) {
			for (int k = -1; k <= 1; k++) {
				double x = side == 0 ? k : side == 1 ? 2 : side == 2 ? -k : -2;
				double z = side == 0 ? -2 : side == 1 ? k : side == 2 ? 2 : -k;
				frames.add(ctr.add(x, 0.45, z));
			}
		}

		name = "end_portal";
		impactMode = PostState.NEGATIVE;
		manga = true;
		s3 = 2.6;
		impact = 3.0;
		end = 7.0;
		fadeColor = 0xE8FFF6;
		look("end", 5.0);

		if (mine) {
			Vec3 mid = frame.at(0, 0.5, 0);
			Cinematic.play(List.of(
				Shot.of(1.6, frame.at(3.2, 2.0, 2.6), frame.at(1.6, 2.2, 3.6), mid).look(frame.at(0, 0.3, 0), mid).roll(5, 8).fov(0.95f, 0.9f),
				Shot.of(s3 - 1.6, frame.at(-2.5, 6.5, 3.0), frame.at(-1.8, 5.5, 2.4), frame.at(0, 0.3, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(0.2, 1.9, 1.6), frame.at(0.12, 1.6, 1.25), frame.at(0, 0.4, 0)).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.5 - impact, frame.at(2.6, 0.3, 2.6), frame.at(3.1, 0.4, 3.1), frame.at(0, 6, 0)).look(frame.at(0, 7, 0), frame.at(0, 3, 0)).roll(10, 6)
					.fov(1.15f, 1.08f),
				Shot.of(end - 4.5, frame.at(0.8, 7, 6), frame.at(1.2, 15, 10), frame.at(0, 0, 0)).roll(0, -4).fov(1f, 1.1f)
			));
		}
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 ctr = Vec3.atCenterOf(pos);

		if (!open) {
			if (cue(0.0)) {
				Sfx.play(mine, "endeye_insert", c, 1f, 0.8f + eyes * 0.035f);
				Sfx.play(mine, "beam_rise", c, 0.6f, 0.85f + eyes * 0.03f);
				// 「きらーん」眼睛亮起
				Sfx.anime(mine, "eye_shine", null, c, 0.8f, 0.9f + eyes * 0.02f);
				shake(0.12f + eyes * 0.01f);

				if (mine) {
					PostState.split(0.4f, 1f, 160);
				}

				for (int i = 0; i < 30; i++) {
					Particles.spawn(Kind.GLOW, ctr.add(0, 0.4, 0), Particles.randomDir(Particles.rand(0.5f, 2f)).add(0, 1, 0), 0.8).size(0.06f, 0.01f)
						.color(0xFFFFFF, VOIDC).physics(1f, 1.5f);
				}
			}

			return;
		}

		if (cue(0.0)) {
			Sfx.play(mine, "endeye_insert", c, 1f, 0.9f);
		}

		for (int i = 0; i < 12; i++) {
			if (cue(0.1 + i * 0.11)) {
				Vec3 f = frames.get(i);
				Sfx.play(mine, "combo_ping", c, 0.4f, 0.7f + i * 0.04f);

				for (int k = 0; k < 8; k++) {
					Particles.spawn(Kind.SPARK, f, Particles.randomDir(2).add(0, 2, 0), 0.4).size(0.03f, 0.008f).color(0xFFFFFF, VOIDC).physics(2f, 2f);
				}
			}
		}

		if (cue(impact - 0.8)) {
			Sfx.play(mine, "end_open", c, 1.4f, 1f);
		}

		if (cue(GATHER)) {
			Sfx.play(mine, "portal_spiral", c, 0.8f, 1f);
			Sfx.anime(mine, "circle", null, c, 1f, 1f);
		}

		if (cue(MERGE)) {
			Sfx.anime(mine, "aura", "plasma_charge", c, 0.9f, 1f);
		}

		// 收攏中：光柱底部沿螺旋噴出火花
		if (t > GATHER && t < MERGE) {
			for (int i = 0; i < 12; i++) {
				if (Particles.RNG.nextDouble() < dt * 20) {
					Vec3 b = spiral(i, gather(t));
					Particles.spawn(Kind.SPARK, b, Particles.randomDir(1).add(0, 1.5, 0), 0.4).size(0.03f, 0.006f).color(0xFFFFFF, VOIDC).physics(2f, 2f);
				}
			}
		}

		if (cue(impact)) {
			Sfx.play(mine, "circle_burst", c, 1f, 1f);
			// 十二法陣炸開：十二個節點往外射出碎片
			for (int i = 0; i < 12; i++) {
				double a = angle(i);
				Vec3 dir = new Vec3(Math.cos(a), 0.25, Math.sin(a));

				for (int k = 0; k < 10; k++) {
					Particles.spawn(Kind.SHARD, ctr.add(dir.scale(2.4)), dir.scale(Particles.rand(4f, 9f)).add(Particles.randomDir(1.2)),
						Particles.rand(0.6f, 1.2f)).size(0.09f, 0.02f).color(0xE8FFF6, VOIDC).physics(5f, 1f).spin(12f);
				}
			}

			for (int i = 0; i < 160; i++) {
				Vec3 v = Particles.randomDir(Particles.rand(1f, 4f)).add(0, Particles.rand(2f, 7f), 0);
				Particles.spawn(Kind.FLARE, ctr, v, Particles.rand(0.8f, 1.6f)).size(0.12f, 0.02f).color(0xFFFFFF, VOIDC).physics(-0.5f, 1f).spin(2f);
			}
		}

		// 星海沉入門中
		if (t > 4.5) {
			int n = rate(50, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = ctr.add(Particles.randomDir(4 + Particles.RNG.nextDouble() * 3)).add(0, 2, 0);
				Particles.spawn(Kind.FLARE, from, Vec3.ZERO, 1.5).size(0.08f, 0.01f).color(0xFFFFFF, VOIDC).physics(0, 0).attract(ctr, 5f).fadeIn(0.3f);
			}
		}

		beats(t, dt, ctr.add(0, 0.4, 0));

		if (!released && t > end - 0.9) {
			released = true;
			com.chuuni.client.fx.Ambient.release(ctr.add(0, 0.26, 0));
		}
	}

	/** 第 i 顆眼相對中心的方位角 */
	private double angle(int i) {
		Vec3 f = frames.get(i).subtract(Vec3.atCenterOf(pos));
		return Math.atan2(f.z, f.x);
	}

	/** 收攏的進度 0 → 1（慢起慢收） */
	private float gather(double t) {
		return smooth(span(t, GATHER, MERGE));
	}

	/** 第 i 道光柱在螺旋上的底部位置（k = 收攏進度：轉 1.5 圈、半徑縮到 0） */
	private Vec3 spiral(int i, float k) {
		Vec3 ctr = Vec3.atCenterOf(pos);
		Vec3 f = frames.get(i);
		double r0 = Math.sqrt((f.x - ctr.x) * (f.x - ctr.x) + (f.z - ctr.z) * (f.z - ctr.z));
		double a = angle(i) + k * Math.PI * 1.5;
		double rr = r0 * (1 - k);
		return new Vec3(ctr.x + Math.cos(a) * rr, f.y, ctr.z + Math.sin(a) * rr);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 ctr = Vec3.atCenterOf(pos);
		float life = 1 - span(t, end - 0.5, end);

		if (!open) {
			lights.add(new CinemaPost.Light(ctr.add(0, 0.5, 0), 2.5f, r(VOIDC), g(VOIDC), b(VOIDC), 0.6f * (1 - span(t, 0.2, 1.2)), true));
			return;
		}

		float charge = span(t, 1.4, impact);
		float burst = t > impact ? 1 - span(t, impact, impact + 1.6) : 0;
		lights.add(new CinemaPost.Light(ctr.add(0, 0.6, 0), 4f + burst * 4, r(VOIDC), g(VOIDC), b(VOIDC), (0.25f + charge * 0.25f + burst * 0.35f) * life, true));

		if (burst > 0.01f) {
			segs.add(new CinemaPost.Seg(ctr, ctr.add(0, 50, 0), 8, r(VOIDC), g(VOIDC), b(VOIDC), burst * 0.3f, 0.35f));
			rays[0] = ctr.add(0, 0.5, 0);
			raysP[0] = r(pale);
			raysP[1] = g(pale);
			raysP[2] = b(pale);
			raysP[3] = burst * 0.25f;
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 ctr = Vec3.atCenterOf(pos);

		if (!open) {
			float a = 1 - span(t, 0.1, 1.3);
			d.cylinder(Mode.GLOW, BEAM, ctr.add(0, 0.3, 0), 0.25f + 0.2f * a, 9f * easeOut(span(t, 0, 0.2)), 16, 1, 0, 3, (float)(t * 4), 1.2f, r(VOIDC), g(VOIDC),
				b(VOIDC), a * 0.8f, 0);
			d.flat(Mode.GLOW, RING, ctr.add(0, 0.33, 0), 0.3f + easeOut(span(t, 0, 0.8)) * (1.2f + eyes * 0.08f), 0, r(VOIDC), g(VOIDC), b(VOIDC), a);
			return;
		}

		float life = fade(t, 0.1, end - 0.5, end);

		Vec3 plate = ctr.add(0, 0.42, 0);
		float k = gather(t);
		float merged = span(t, MERGE - 0.15, MERGE);
		float burst = easeOut(span(t, impact, impact + 0.55));

		// 12 顆眼依序亮起，各自升起光柱
		for (int i = 0; i < 12; i++) {
			Vec3 f = frames.get(i);
			double on0 = 0.1 + i * 0.11;
			float on = easeOutBack(span(t, on0, on0 + 0.2)) * life;
			d.billboard(Mode.GLOW, GLOW, f.x, f.y, f.z, 0.45f * on, 0, r(VOIDC), g(VOIDC), b(VOIDC), on);
			d.billboard(Mode.GLOW, FLARE, f.x, f.y, f.z, 0.55f * on, (float)t, 1f, 1f, 1f, on * 0.6f);

			// 光柱：升起 → 沿螺旋收攏 → 合成中央大光柱後消失
			float h = 6f * easeOut(span(t, on0, on0 + 0.35)) + k * 4f;
			float ba = Math.min(span(t, on0, on0 + 0.1), 1 - merged) * life;

			if (ba > 0.01f) {
				Vec3 base = spiral(i, k);
				d.cylinder(Mode.GLOW, BEAM, base, 0.17f + k * 0.08f, h, 12, 1, 0, 2, (float)(-t * 1.2), 1.4f, r(VOIDC), g(VOIDC), b(VOIDC), 0.45f * ba, 0);
				d.cylinder(Mode.GLOW, BEAM, base, 0.05f, h * 1.05f, 8, 1, 0.5f, 2, (float)(-t * 2), 1f, 1f, 1f, 1f, 0.6f * ba, 0);
				d.billboard(Mode.GLOW, GLOW, base.x, base.y, base.z, 0.4f, 0, r(VOIDC), g(VOIDC), b(VOIDC), ba);
			}

			// 收攏時地上拖出的螺旋光軌 = 法陣的十二道旋臂
			if (k > 0.01f) {
				float ta = Math.min(1, k * 3) * (1 - burst) * life;
				Vec3 prev = null;
				int seg = 14;

				for (int si = 0; si <= seg; si++) {
					Vec3 sp = spiral(i, k * si / seg);
					Vec3 cur = new Vec3(sp.x, plate.y, sp.z);

					if (prev != null) {
						d.streak(Mode.GLOW, SPARK, prev.x, prev.y, prev.z, cur.x, cur.y, cur.z, 0.07f, r(VOIDC), g(VOIDC), b(VOIDC), ta * (0.4f + 0.6f * si / seg));
					}

					prev = cur;
				}
			}
		}

		// 十二法陣：收攏時浮現，越轉越快；吸氣時往內收緊；衝擊時炸開
		float circle = span(t, GATHER, GATHER + 0.5) * (1 - burst) * life;

		if (circle > 0.01f) {
			double tt = Math.max(0, t - GATHER);
			float spin = (float)(tt * tt * 1.1);
			float squeeze = 1 - 0.15f * span(t, s3, impact);
			float scale = squeeze * (1 + burst * 3);
			float ca = circle * (0.6f + 0.4f * span(t, s3, impact));
			d.flat(Mode.GLOW, RING, plate.add(0, 0.01, 0), 3.1f * scale, 0, r(VOIDC), g(VOIDC), b(VOIDC), ca);
			d.flat(Mode.GLOW, RING, plate.add(0, 0.01, 0), 2.7f * scale, 0, 1f, 1f, 1f, ca * 0.5f);
			d.flat(Mode.GLOW, SEAL, plate.add(0, 0.02, 0), 2.6f * scale, spin, r(VOIDC), g(VOIDC), b(VOIDC), ca * 0.8f);
			d.flat(Mode.GLOW, CIRCLE, plate.add(0, 0.03, 0), 1.4f * scale, -spin * 1.6f, r(pale), g(pale), b(pale), ca * 0.7f);
			com.chuuni.client.fx.Ambient.twelveStar(d, plate.add(0, 0.04, 0), 2.45f * scale, spin * 0.5f, VOIDC, ca);
		}

		// 炸開的衝擊環
		if (burst > 0 && burst < 1) {
			d.flat(Mode.GLOW, JAG, plate.add(0, 0.05, 0), 3f + burst * 9f, 0, 1f, 1f, 1f, (1 - burst) * life);
		}

		// 合成的中央大光柱（衝擊前）
		float big = merged * (1 - span(t, impact, impact + 0.15)) * life;

		if (big > 0.01f) {
			float grow = span(t, MERGE - 0.15, impact);
			d.cylinder(Mode.GLOW, BEAM, plate, 0.5f + grow * 0.5f, 14f + grow * 10, 24, 1, 0, 4, (float)(-t * 3), 1.2f, r(VOIDC), g(VOIDC), b(VOIDC), 0.6f * big, 0);
			d.cylinder(Mode.GLOW, BEAM, plate, 0.18f + grow * 0.12f, 16f + grow * 10, 12, 1, 0.5f, 4, (float)(-t * 4), 1f, 1f, 1f, 1f, 0.8f * big, 0);
		}

		// 中心的星空陣
		float sky = easeOutBack(span(t, 1.8, 2.6));

		if (sky > 0.01f) {
			float after = t > impact ? 1 + easeOut(span(t, impact, impact + 0.6)) * 3 : 1;
			float a = (1 - span(t, 4.5, end)) * life;
			d.flat(Mode.GLOW, STARS, plate, 1.5f * sky * after, (float)(t * 0.2), r(pale), g(pale), b(pale), 0.6f * a);
		}

		// 衝擊後：虛空光柱 + 星空穹頂
		float pillar = t < impact ? 0 : Math.min(1, 1.3f - span(t, impact, end));

		if (pillar > 0.01f) {
			float w = easeOut(span(t, impact, impact + 0.1));
			d.cylinder(Mode.GLOW, BEAM, plate, 1.5f * w, 40f, 32, 1, 0, 6, (float)(t * 2), 1.2f, r(VOIDC), g(VOIDC), b(VOIDC), pillar * 0.3f, pillar * 0.03f);
			d.cylinder(Mode.GLOW, BEAM, plate, 0.4f * w, 40f, 16, 1, 0.5f, 5, (float)(t * 3), 1f, 1f, 1f, 1f, pillar * 0.35f, pillar * 0.06f);
			float dome = Math.min(span(t, impact, impact + 0.6), 1 - span(t, 4.8, end)) * 0.8f;
			d.cylinder(Mode.GLOW, STARS, ctr.add(0, -1.5, 0), 10f, 10f, 48, 2, (float)(t * 0.01), 1, 0, 0.6f, r(DEEP) + 0.4f, g(DEEP) + 0.4f, b(DEEP) + 0.4f,
				dome * 0.2f, dome * 0.5f);
		}

		shockRings(d, t, ctr.add(0, -0.45, 0), plate, 8f);
	}
}
