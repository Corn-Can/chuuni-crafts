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
 * 爆擊・會心一擊：打中的那一點炸出十字星光與鋸齒環，金色火花四散。
 * 戰鬥中很頻繁 → 很短（0.8 秒）、頓幀只給自己。
 */
public class CritFx extends MinorFx {
	private static final int GOLD = 0xFFD040;

	public CritFx(FxPayload p) {
		super(p);
		end = 0.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Vec3 at = body();
			Sfx.anime(mine, "hit", null, at, 0.3f, 1.25f);
			Sfx.play(mine, "comb_cut", at, 0.4f, 1.1f);

			if (mine) {
				pop(GOLD, 0.35f);
				smallName("crit");
			}

			Particles.spawn(Kind.HIT, at, Vec3.ZERO, 0.25).size(0.8f, 1.3f).color(0xFFFFFF, GOLD);
			sparks(at, 22, 5f, GOLD, 0.4f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = body();
		float k = span(t, 0, 0.35);

		if (k < 1) {
			float a = (1 - k) * (1 - k);
			d.billboard(Mode.GLOW, FLARE, at.x, at.y, at.z, 1.6f * easeOut(Math.min(1, k * 3)) * (1 - k * 0.5f), 0.3f, 1f, 1f, 1f, a);
			d.billboard(Mode.GLOW, JAG, at.x, at.y, at.z, 0.3f + easeOut(k) * 1.4f, (float)(t * 3), r(GOLD), g(GOLD), b(GOLD), a);
		}

		glow(d, at, 0.9f, GOLD, 0.6f * (1 - span(t, 0, 0.5)));
	}
}
