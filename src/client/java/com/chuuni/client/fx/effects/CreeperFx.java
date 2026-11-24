package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** 引爆倒數：苦力怕身上一閃一閃的綠白警告光，腳下紅綠交錯的警戒虛線環越轉越快（爆炸交給原版） */
public class CreeperFx extends MinorFx {
	private static final int GREEN = 0x60F060;
	private static final int RED = 0xFF3020;

	public CreeperFx(FxPayload p) {
		super(p);
		end = 1.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "creeper_hiss", c, 1f, 1f);
			tint(GREEN, 1.6);
			smallName("creeper");
		}

		if (t < end - 0.1) {
			int n = rate(50, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.SPARK, body().add(Particles.randomDir(0.4)), Particles.randomDir(1.5).add(0, 1, 0), 0.3).size(0.025f, 0.006f)
					.color(0xFFFFFF, i % 2 == 0 ? GREEN : RED).physics(3f, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float blink = (float)(Math.sin(t * (10 + t * 18)) * 0.5 + 0.5);
		glow(d, body(), 1.2f, GREEN, 0.25f + blink * 0.45f);
		Vec3 ground = new Vec3(body().x, Math.floor(body().y - bodyHeight() * 0.5) + 0.03, body().z);
		dashRing(d, ground, 2.8f * easeOut(span(t, 0, 0.3)), 20, (float)(t * (1 + t * 3)), RED, GREEN, 0.12f, 0.7f);
	}
}
