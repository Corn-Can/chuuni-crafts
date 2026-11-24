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
 * 進度達成：頭頂綻開一顆星（一般 = 金、目標 = 青、挑戰 = 紫），進度圖示浮在上面轉一圈；
 * 挑戰進度是「傳說刻名」—— 打擊感 + 毛筆大字。
 */
public class AdvancementFx extends MinorFx {
	private final int col;
	private final boolean challenge;

	public AdvancementFx(FxPayload p) {
		super(p);
		challenge = p.variant() == 1;
		col = challenge ? 0xC080FF : p.variant() == 2 ? 0x60E0FF : 0xFFE070;
		end = challenge ? 3.0 : 2.4;
	}

	private Vec3 top() {
		return body().add(0, bodyHeight() * 0.5 + 0.9, 0);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = top();

		if (cue(0.0)) {
			switch (p.variant()) {
				case 1 -> {
					Sfx.play(mine, "name_bestow", at, 0.8f, 1f);
					Sfx.play(mine, "title_slam", at, 0.5f, 1f);
				}
				case 2 -> Sfx.play(mine, "disc_drop", at, 0.7f, 1f);
				default -> Sfx.play(mine, "vault_burst", at, 0.55f, 1.1f);
			}

			if (mine && challenge) {
				pop(col, 0.6f);
				bigName("advancement_challenge", 3.2);
			}
			else {
				smallName("advancement");
			}

			for (int i = 0; i < (challenge ? 60 : 30); i++) {
				Particles.spawn(Kind.FLARE, at, Particles.randomDir(Particles.rand(1.5f, 4f)), Particles.rand(0.6f, 1.2f)).size(0.07f, 0.01f)
					.color(0xFFFFFF, col).physics(1f, 1.5f).spin(4f);
			}
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		float s = easeOutBack(span(t, 0.05, 0.4)) * (1 - span(t, end - 0.4, end));

		if (s > 0.01f) {
			d.item(p.item(), top().add(0, 0.1, 0), 0.6f * s, (float)(t * 4), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = top();
		float a = fade(t, 0.05, end - 0.5, end);
		float k = span(t, 0, 0.6);

		d.billboard(Mode.GLOW, FLARE, at.x, at.y, at.z, 2.2f * easeOut(Math.min(1, k * 2)) * (1 - k * 0.4f), (float)(t * 0.8), r(col), g(col), b(col), a);

		if (k < 1) {
			d.billboard(Mode.GLOW, RING, at.x, at.y, at.z, 0.3f + easeOut(k) * 2.5f, 0, r(col), g(col), b(col), (1 - k) * (1 - k));
		}

		if (challenge) {
			d.billboard(Mode.GLOW, SEAL, at.x, at.y, at.z, 1.3f * easeOutBack(span(t, 0, 0.5)), (float)(t * 1.2), r(col), g(col), b(col), 0.6f * a);
		}

		glow(d, at, 1.3f, col, 0.5f * a);
	}
}
