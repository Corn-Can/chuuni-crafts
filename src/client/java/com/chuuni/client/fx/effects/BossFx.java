package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
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
 * 擊倒頭目：終界龍（屠龍之刻）、凋零怪（凋零殞落）、伏守者（深淵鎮魂）、遠古守衛者（古神沉眠）。
 * 整個戰場都看得到：貫天光柱、巨大的地面衝擊環、旋轉的法陣、滿天光屑；擊倒的人有慢動作與毛筆大字。
 */
public class BossFx extends MinorFx {
	private static final String[] NAMES = {"boss_dragon", "boss_wither", "boss_warden", "boss_elder"};

	private final Vec3 at;
	private final Vec3 ground;
	private final int col;
	private final int col2;

	public BossFx(FxPayload p) {
		super(p);
		end = 5.0;
		at = p.at();
		ground = new Vec3(at.x, Math.floor(at.y - (p.variant() == 0 ? 4 : 1)) + 0.05, at.z);

		switch (p.variant()) {
			case 0 -> {
				col = 0xC070FF;
				col2 = 0xFFE0FF;
			}
			case 1 -> {
				col = 0x7050A0;
				col2 = 0xFFFFFF;
			}
			case 2 -> {
				col = 0x20D0C0;
				col2 = 0xA0FFF0;
			}
			default -> {
				col = 0x80FFE0;
				col2 = 0xFFFFFF;
			}
		}
	}

	/** 大場面：80 格內都算在附近（染色、閃光） */
	@Override
	protected boolean near() {
		var player = net.minecraft.client.Minecraft.getInstance().player;
		return player != null && player.position().distanceTo(p.at()) < 80;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "anchor_blast", at, 1.2f, 0.8f);
			Sfx.play(mine, "end_open", at, 1f, 1f);
			shake(0.9f);

			if (near) {
				PostState.flash(col2, 0.55f, 420, 0.1f);
				PostState.tint(col, 300, 2500);
			}

			if (mine) {
				FxClock.slowmo(0.3f, 1500);
				PostState.lines(1f, false, 1600, 0.3f);
				PostState.split(1.4f, 0.6f, 700);
				PostState.shock(1f, 900);
				CameraShake.fovPunch(0.12f);
				bigName(NAMES[Math.clamp(p.variant(), 0, 3)], 4.0);
			}

			Particles.spawn(Kind.HIT, at, Vec3.ZERO, 0.4).size(2.5f, 5f).color(0xFFFFFF, col2);

			for (int i = 0; i < 160; i++) {
				Particles.spawn(Kind.SPARK, at, Particles.randomDir(Particles.rand(6f, 18f)), Particles.rand(0.8f, 1.8f)).size(0.08f, 0.02f)
					.color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(4f, 0.8f);
			}
		}

		if (cue(0.7)) {
			Sfx.anime(mine, "holy", null, at, 0.9f, 0.9f);
		}

		if (t > 0.2 && t < 3.5) {
			int n = rate(90, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = Particles.RNG.nextDouble() * 10;
				Vec3 o = ground.add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
				Particles.spawn(i % 2 == 0 ? Kind.GLYPH : Kind.FLARE, o, new Vec3(0, Particles.rand(2f, 6f), 0), Particles.rand(1.2f, 2.2f)).size(0.12f, 0.02f)
					.color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(-0.4f, 0.8f).spin(2f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = 1 - span(time(), 0.3, end);
		lights.add(new CinemaPost.Light(at, 24f, r(col), g(col), b(col), 0.8f * k, true));

		if (time() < 1.2 && rays[0] == null) {
			rays[0] = at;
			raysP[0] = r(col2);
			raysP[1] = g(col2);
			raysP[2] = b(col2);
			raysP[3] = 0.5f * (1 - span(time(), 0, 1.2));
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float a = fade(t, 0.05, end - 1.2, end);

		// 貫天光柱
		float beam = a * (1 - span(t, 1.5, 4));
		d.cylinder(Mode.GLOW, BEAM, ground, 2.5f * easeOut(span(t, 0, 0.3)), 120, 32, 1, 0, 12, (float)(-t * 4), 1.6f, r(col), g(col), b(col), 0.6f * beam, 0f);
		d.cylinder(Mode.GLOW, BEAM, ground, 0.9f, 120, 16, 1, 0.5f, 12, (float)(-t * 6), 1.2f, 1f, 1f, 1f, 0.35f * beam, 0f);

		// 地面法陣與衝擊環
		d.flat(Mode.GLOW, SEAL, ground, 9f * easeOutBack(span(t, 0, 0.6)), (float)(t * 0.4), r(col), g(col), b(col), 0.7f * a);
		d.flat(Mode.GLOW, CIRCLE, ground.add(0, 0.02, 0), 6f, (float)(-t * 0.6), r(col2), g(col2), b(col2), 0.5f * a);
		groundRing(d, t, 0.0, 1.2, ground.add(0, 0.03, 0), 1f, 30f, col2, 1f, JAG);
		groundRing(d, t, 0.1, 1.6, ground.add(0, 0.04, 0), 1f, 40f, col, 0.8f, RING);

		// 立體衝擊波
		float k = span(t, 0, 0.8);

		if (k < 1) {
			d.billboard(Mode.GLOW, RING, at.x, at.y, at.z, 1 + easeOut(k) * 25, 0, r(col2), g(col2), b(col2), (1 - k) * (1 - k));
		}

		glow(d, at, 6f, col, 0.6f * a * (1 - 0.6f * span(t, 0.3, 1.5)));
	}
}
