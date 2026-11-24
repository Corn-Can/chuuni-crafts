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
 * 升級・境界突破：金色光環從腳下一圈圈升起、符文往上飄；每 10 級是大突破（法陣 + 光柱 + 毛筆大字）。
 * 自己升級時畫面中央刻出「Lv.N」。
 */
public class LevelUpFx extends MinorFx {
	private static final int GOLD = 0xFFE060;
	private static final int LIME = 0xB0FF60;

	private final boolean big;

	public LevelUpFx(FxPayload p) {
		super(p);
		big = p.power() > 0 && p.power() % 10 == 0;
		end = big ? 2.6 : 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = body();

		if (cue(0.0)) {
			if (big) {
				Sfx.anime(mine, "rise", "beacon_rise", at, 0.8f, 1f);
			}
			else {
				Sfx.play(mine, "beacon_rise", at, 0.55f, 1.15f);
			}

			if (mine) {
				Hud.engrave("Lv." + p.power());

				if (big) {
					pop(GOLD, 0.6f);
					bigName("level_up", 3.0);
				}
				else {
					smallName("level_up");
				}
			}
		}

		if (t < end - 0.6) {
			int n = rate(big ? 60 : 30, dt);

			for (int i = 0; i < n; i++) {
				Vec3 o = at.add(Particles.randomDir(0.7)).add(0, -bodyHeight() * 0.4, 0);
				Particles.spawn(i % 2 == 0 ? Kind.GLYPH : Kind.FLARE, o, new Vec3(0, Particles.rand(1f, 2.5f), 0), Particles.rand(0.6f, 1.1f)).size(0.06f, 0.01f)
					.color(0xFFFFFF, i % 3 == 0 ? LIME : GOLD).physics(-0.3f, 1f).spin(2f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = body();
		float h = bodyHeight();
		Vec3 feet = at.add(0, -h * 0.5 + 0.05, 0);
		float a = fade(t, 0.1, end - 0.6, end);

		for (int i = 0; i < 3; i++) {
			float k = span(t, i * 0.18, i * 0.18 + 0.9);

			if (k > 0 && k < 1) {
				d.flat(Mode.GLOW, RING, feet.add(0, easeOut(k) * (h + 0.4), 0), 0.9f - k * 0.3f, 0, r(GOLD), g(GOLD), b(GOLD), (float)Math.sin(k * Math.PI) * a);
			}
		}

		if (big) {
			d.flat(Mode.GLOW, CIRCLE, feet, 1.5f * easeOutBack(span(t, 0, 0.4)), (float)(t * 0.7), r(GOLD), g(GOLD), b(GOLD), 0.7f * a);
			d.cylinder(Mode.GLOW, BEAM, feet, 0.5f, h + 4f, 16, 1, 0, 4, (float)(-t * 3), 1.3f, r(GOLD), g(GOLD), b(GOLD), 0.45f * a * (1 - span(t, 0.4, 1.8)),
				0f);
		}

		glow(d, at, 1.2f, GOLD, 0.35f * a);
	}
}
