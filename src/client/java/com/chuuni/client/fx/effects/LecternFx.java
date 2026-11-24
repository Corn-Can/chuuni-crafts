package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 講台・智典開示。variant 0 = 放上書（power = 頁碼）、1 = 翻頁（extra 1 = 往前翻）。
 * <ul>
 *   <li>放書：典籍「咚」地落下 → 書頁化成十二張光之頁環繞講台盤旋，符文帶沿圓柱上升，一道細光柱往天上射</li>
 *   <li>翻頁：一張光之頁沿弧線從書的一側翻到另一側，帶起幾個符文</li>
 * </ul>
 */
public class LecternFx extends MinorFx {
	private static final int ARCANE = 0xB090FF;
	private static final int GOLD = 0xFFD890;

	private final boolean page;
	private final Vec3 book;

	public LecternFx(FxPayload p) {
		super(p);
		page = p.variant() == 1;
		book = new Vec3(c.x, pos.getY() + 1.0, c.z);
		end = page ? 1.0 : 2.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (page) {
			if (cue(0.0)) {
				Sfx.play(mine, "page_turn", c, 0.8f, p.extra() == 1 ? 0.92f : 1.05f);
			}

			if (cue(0.25)) {
				for (int i = 0; i < 8; i++) {
					Particles.spawn(Kind.GLYPH, book.add(0, 0.2, 0), Particles.randomDir(0.8).add(0, 0.8, 0), 0.7).size(0.07f, 0.02f).color(0xFFFFFF, ARCANE)
						.physics(-0.2f, 1.5f);
				}
			}

			return;
		}

		if (cue(0.0)) {
			Sfx.play(mine, "book_place", c, 0.9f, 1f);
			tint(ARCANE, 1.8);
			smallName("lectern");
		}

		if (cue(0.08)) {
			pop(ARCANE, 0.4f);
		}

		if (t > 0.2 && t < 1.8) {
			int nn = rate(25, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Particles.spawn(Kind.GLYPH, book.add(Math.cos(a) * 0.9, -0.6, Math.sin(a) * 0.9), new Vec3(0, 1.2, 0), 1.2).size(0.08f, 0.03f)
					.color(0xFFFFFF, Particles.RNG.nextBoolean() ? ARCANE : GOLD).physics(0, 0.5f).attract(book.add(0, 1.2, 0), 1f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = page ? 0.3f * (1 - span(time(), 0, end)) : 0.5f * span(time(), 0, 0.3f) * (1 - span(time(), 1.6, end));
		lights.add(new CinemaPost.Light(book.add(0, 0.3, 0), 2.5f, r(ARCANE), g(ARCANE), b(ARCANE), k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		if (page) {
			// 翻頁：沿半圓弧從一側翻到另一側
			float k = easeOut(span(t, 0, 0.35));
			double dir = p.extra() == 1 ? -1 : 1;
			double ang = Math.PI * k;
			Vec3 du = new Vec3(u.x, 0, u.z).scale(dir);
			Vec3 tip = book.add(du.scale(-0.4 * Math.cos(ang))).add(0, 0.15 + 0.4 * Math.sin(ang), 0);
			float a = 1 - span(t, 0.35, end);
			line(d, TRAIL, book.add(0, 0.12, 0), tip, 0.35f, ARCANE, a * 0.7f);
			d.billboard(Mode.GLOW, FLARE, tip.x, tip.y, tip.z, 0.3f, (float)t * 3, 1f, 1f, 1f, a * 0.8f);
			groundRing(d, t, 0.2, 0.8, book.add(0, 0.05, 0), 0.1f, 0.9f, ARCANE, 0.6f, RING);
			return;
		}

		float life = fade(t, 0.05, end - 0.5, end);
		float open = easeOutBack(span(t, 0.05, 0.4)) * life;

		// 十二張光之頁環繞
		for (int i = 0; i < 12; i++) {
			double a = t * 1.6 + i * Math.PI / 6;
			double rr = 0.85 * open;
			Vec3 at = book.add(Math.cos(a) * rr, 0.25 + Math.sin(t * 3 + i) * 0.15 + span(t, 0.4, end) * 0.6, Math.sin(a) * rr);
			d.billboard(Mode.GLOW, SHARD_T, at.x, at.y, at.z, 0.16f * open, (float)(a + t), r(GOLD), g(GOLD), b(GOLD), 0.8f * open);
		}

		float beam = (1 - span(t, 0.3, 1.6)) * open;
		d.cylinder(Mode.GLOW, BEAM, book, 0.12f, 6f, 12, 1, 0, 3, (float)(t * 3), 1f, r(ARCANE), g(ARCANE), b(ARCANE), beam * 0.7f, 0);
		glow(d, book.add(0, 0.15, 0), 0.8f, GOLD, 0.6f * open);
	}
}
