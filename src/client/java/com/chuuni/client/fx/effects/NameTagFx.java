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

/** 真名賜予：金色符文從四周捲進牠的頭頂，一道細光柱落下，名字刻在畫面下方（自己才看得到） */
public class NameTagFx extends MinorFx {
	private static final int GOLD = 0xFFE0A0;

	private final String given;

	public NameTagFx(FxPayload p) {
		super(p);
		end = 2.2;
		var name = p.item().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
		given = name == null ? "" : name.getString();
	}

	private Vec3 head() {
		return body().add(0, bodyHeight() * 0.5 + 0.2, 0);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "name_bestow", c, 0.9f, 1f);
			tint(GOLD, 1.6);
			smallName("name_tag");

			for (int i = 0; i < 24; i++) {
				Vec3 from = head().add(Particles.randomDir(1.4));
				Particles.spawn(Kind.GLYPH, from, Vec3.ZERO, 0.5).size(0.08f, 0.03f).color(0xFFFFFF, GOLD).physics(0, 0).attract(head(), 12f);
			}
		}

		if (cue(0.45)) {
			pop(GOLD, 0.35f);

			if (mine && !given.isEmpty()) {
				Hud.engrave(given);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float beam = span(t, 0.4, 0.5) * (1 - span(t, 0.6, 1.6));
		Vec3 top = head();
		d.cylinder(Mode.GLOW, BEAM, top.add(0, -bodyHeight() - 0.1, 0), 0.12f, 6f, 12, 1, 0, 3, (float)(t * 3), 1f, r(GOLD), g(GOLD), b(GOLD), beam * 0.7f, 0);
		glow(d, top, 0.6f, GOLD, beam * 0.8f);
	}
}
