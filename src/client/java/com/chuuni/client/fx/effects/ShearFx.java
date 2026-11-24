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

/**
 * 銀剪流舞：兩道銀色剪光交叉劃過身體 → 毛（羊毛顏色）/ 蘑菇 / 南瓜 / 花一塊塊蓬鬆地飄落。
 * variant：0 羊（power = 羊毛色）/ 1 哞菇 / 2 雪人 / 3 沼骸 / 4 銅魔像。
 */
public class ShearFx extends MinorFx {
	private static final int SILVER = 0xE0E8F0;

	private final int tuft;

	public ShearFx(FxPayload p) {
		super(p);
		tuft = switch (Math.floorMod(p.variant(), 5)) {
			case 0 -> p.power() == 0 ? 0xF0F0F0 : p.power();
			case 1 -> 0xE03030;
			case 2 -> 0xFFA030;
			case 3 -> 0x8A6A40;
			default -> 0xE02040;
		};
		end = 2.0;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "shear_snip", c, 0.9f, 1f);
			tint(SILVER, 1.2);
			smallName("shear");
		}

		if (cue(0.14)) {
			pop(SILVER, 0.35f);
			Vec3 b = body();
			float w = bodyWidth();

			for (int i = 0; i < 26; i++) {
				Vec3 at = b.add(Particles.rand(-w, w) * 0.6, Particles.rand(-0.3f, 0.4f), Particles.rand(-w, w) * 0.6);
				Particles.spawn(Kind.SHARD, at, Particles.randomDir(1.2).add(0, 1.4, 0), Particles.rand(1.2f, 1.8f)).size(0.12f, 0.1f).color(0xFFFFFF, tuft)
					.physics(1.5f, 2.5f).spin(4f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 b = body();
		float w = Math.max(0.6f, bodyWidth());
		Vec3 side = new Vec3(u.x, 0, u.z).scale(w);

		for (int k = 0; k < 2; k++) {
			double t0 = k * 0.1;
			float s = easeOut(span(t, t0, t0 + 0.1));
			float a = 1 - span(t, t0 + 0.1, t0 + 0.45);

			if (s <= 0 || a <= 0.01f) {
				continue;
			}

			double dy = (k == 0 ? 1 : -1) * 0.35;
			Vec3 a0 = b.add(side.scale(-1)).add(0, dy, 0).add(nv.scale(0.4));
			Vec3 a1 = b.add(side).add(0, -dy, 0).add(nv.scale(0.4));
			line(d, TRAIL, a0, lerp(a0, a1, s), 0.3f, SILVER, a * 0.8f);
			line(d, SPARK, a0, lerp(a0, a1, s), 0.05f, 0xFFFFFF, a);
		}
	}
}
