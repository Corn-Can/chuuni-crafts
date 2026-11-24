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
 * 不死圖騰・不死涅槃：金綠色的光環一圈圈往上升、羽毛般的光點飛舞，腳下展開法陣，一道光柱貫穿全身。
 * 自己用到時：慢動作、閃光、毛筆大字。
 */
public class TotemFx extends MinorFx {
	private static final int GOLD = 0xFFE060;
	private static final int GREEN = 0x80FF90;

	public TotemFx(FxPayload p) {
		super(p);
		end = 3.0;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = body();

		if (cue(0.0)) {
			Sfx.anime(mine, "holy", null, at, 0.9f, 1f);
			Sfx.play(mine, "holy_descend", at, 0.6f, 1f);
			shake(0.3f);
			tint(GOLD, 1.5);

			if (mine) {
				FxClock.slowmo(0.35f, 700);
				PostState.flash(GOLD, 0.5f, 320, 0.08f);
				PostState.lines(0.7f, false, 900, 0.4f);
				CameraShake.fovPunch(0.06f);
				bigName("totem", 3.2);
			}

			sparks(at, 40, 5f, GOLD, 0.6f);
		}

		if (t < 1.6) {
			int n = rate(60, dt);

			for (int i = 0; i < n; i++) {
				Vec3 o = at.add(Particles.randomDir(0.6)).add(0, -0.6, 0);
				Particles.spawn(i % 3 == 0 ? Kind.GLYPH : Kind.FLARE, o, Particles.randomDir(0.8).add(0, Particles.rand(1.5f, 3f), 0), Particles.rand(0.8f, 1.4f))
					.size(0.07f, 0.01f).color(0xFFFFFF, i % 2 == 0 ? GOLD : GREEN).physics(-0.3f, 1.2f).spin(3f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = 1 - span(time(), 0.2, end);
		lights.add(new CinemaPost.Light(body(), 6f, r(GOLD), g(GOLD), b(GOLD), 0.8f * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = body();
		float h = bodyHeight();
		Vec3 feet = at.add(0, -h * 0.5 + 0.05, 0);
		float a = fade(t, 0.1, end - 0.8, end);

		d.flat(Mode.GLOW, SEAL, feet, 1.6f * easeOutBack(span(t, 0, 0.4)), (float)(t * 0.8), r(GOLD), g(GOLD), b(GOLD), 0.8f * a);

		for (int i = 0; i < 4; i++) {
			double k = (t * 0.9 + i * 0.25) % 1.0;
			Vec3 ring = feet.add(0, k * (h + 0.8), 0);
			int col = i % 2 == 0 ? GOLD : GREEN;
			d.flat(Mode.GLOW, RING, ring, 0.9f + (float)k * 0.3f, 0, r(col), g(col), b(col), a * (float)Math.sin(k * Math.PI) * 0.8f);
		}

		float beam = a * (1 - span(t, 0.3, 1.6));
		d.cylinder(Mode.GLOW, BEAM, feet, 0.45f, h + 3f, 16, 1, 0, 4, (float)(-t * 3), 1.4f, r(GOLD), g(GOLD), b(GOLD), 0.5f * beam, 0f);
		glow(d, at, 1.8f, GOLD, 0.5f * a);
	}
}
