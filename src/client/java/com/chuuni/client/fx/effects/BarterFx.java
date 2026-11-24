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

/** 黃金交易：金錠從手中飛進豬布林手裡，金色的光點像金幣一樣噴起來（牠盯著金子看的那幾秒一直發亮） */
public class BarterFx extends MinorFx {
	private static final int GOLD = 0xFFC830;
	private static final ItemStack INGOT = new ItemStack(Items.GOLD_INGOT);

	public BarterFx(FxPayload p) {
		super(p);
		end = 2.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "barter_gold", c, 0.9f, 1f);
			tint(GOLD, 1.8);
			smallName("barter");
		}

		if (cue(0.3)) {
			pop(GOLD, 0.35f);

			for (int i = 0; i < 30; i++) {
				Particles.spawn(Kind.FLARE, body().add(0, 0.3, 0), Particles.randomDir(Particles.rand(0.8f, 1.8f)).add(0, 2.5, 0), Particles.rand(0.8f, 1.2f))
					.size(0.08f, 0.03f).color(0xFFFFFF, GOLD).physics(7f, 0.5f).spin(4f);
			}
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (t > 0.32) {
			return;
		}

		float k = easeOut(span(t, 0, 0.3));
		d.item(INGOT, lerp(hand(), body(), k).add(0, Math.sin(k * Math.PI) * 0.4, 0), 0.5f, (float)(t * 12), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float a = span(t, 0.3, 0.4) * (1 - span(t, 1.6, end));
		glow(d, body(), 1.6f, GOLD, 0.55f * a * (0.8f + 0.2f * (float)Math.sin(t * 9)));
		groundRing(d, t, 0.3, 1.1, body().add(0, -bodyHeight() * 0.5 + 0.03, 0), 0.3f, 2.2f, GOLD, 0.8f, RING);
	}
}
