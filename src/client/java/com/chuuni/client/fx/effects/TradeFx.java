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

/** 商談成立（關掉交易介面時）：綠寶石光點像錢幣一樣噴起，最後一筆的成品在村民頭上轉一圈，印章「咚」一閃；多筆時顯示筆數 */
public class TradeFx extends MinorFx {
	private static final int EMERALD = 0x40E080;

	private final ItemStack result;
	private final int deals;

	public TradeFx(FxPayload p) {
		super(p);
		result = p.item();
		deals = Math.max(1, p.power());
		end = 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "trade_seal", c, 0.9f, 1f);
			tint(EMERALD, 1.4);
			smallName("trade");
			pop(EMERALD, 0.35f);

			if (mine && deals > 1) {
				Hud.combo(deals, EMERALD, "DEALS");
			}

			for (int i = 0; i < 20 + Math.min(40, deals * 4); i++) {
				Particles.spawn(Kind.FLARE, body().add(0, 0.4, 0), Particles.randomDir(Particles.rand(0.6f, 1.6f)).add(0, 2.2, 0), Particles.rand(0.8f, 1.2f))
					.size(0.07f, 0.03f).color(0xFFFFFF, EMERALD).physics(6f, 0.5f).spin(4f);
			}
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (result.isEmpty()) {
			return;
		}

		float up = easeOut(span(t, 0, 0.4));
		Vec3 at = body().add(0, bodyHeight() * 0.5 + 0.3 + up * 0.4, 0);
		d.item(result, at, 0.45f * (1 - span(t, end - 0.4, end)), (float)(t * 4), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		glow(d, body().add(0, bodyHeight() * 0.5 + 0.6, 0), 0.6f, EMERALD, 0.5f * (1 - span(t, 0.3, end)));
		glow(d, body(), 1.4f, EMERALD, 0.45f * (1 - span(t, 0.2, 1.2)));
		Vec3 feet = body().add(0, -bodyHeight() * 0.5 + 0.03, 0);
		groundRing(d, t, 0, 0.8, feet, 0.3f, 2.4f, EMERALD, 0.9f, RING);
		groundRing(d, t, 0.05, 0.7, feet, 0.2f, 1.6f, 0xFFFFFF, 0.7f, JAG);
	}
}
