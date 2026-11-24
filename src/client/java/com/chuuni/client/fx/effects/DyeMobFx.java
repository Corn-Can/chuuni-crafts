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

/** 七彩染身：顏料「啪」地潑上去，一道新顏色的光從頭到尾掃過身體，色點四濺 */
public class DyeMobFx extends MinorFx {
	private final int col;

	public DyeMobFx(FxPayload p) {
		super(p);
		col = p.power() == 0 ? 0xFFFFFF : p.power();
		end = 1.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "mob_dye", c, 0.9f, 1f);
			tint(col, 1.0);
			smallName("dye_mob");
			pop(col, 0.3f);

			for (int i = 0; i < 24; i++) {
				Particles.spawn(Kind.SHARD, body(), Particles.randomDir(Particles.rand(1f, 2.5f)).add(0, 1, 0), 0.9).size(0.06f, 0.04f).color(col, col).physics(7f, 1f).spin(8f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0.05, 0.5));
		float a = 1 - span(t, 0.5, 1.2);
		Vec3 b = body();
		float h = bodyHeight();
		Vec3 side = new Vec3(u.x, 0, u.z).scale(bodyWidth() * 0.8);
		Vec3 band = b.add(0, h * 0.6 - k * h * 1.2, 0);
		line(d, GLOW, band.add(side.scale(-1.3)), band.add(side.scale(1.3)), 0.5f, col, a);
		line(d, SPARK, band.add(side.scale(-1.3)), band.add(side.scale(1.3)), 0.08f, 0xFFFFFF, a);
		glow(d, b, 1.4f * h, col, 0.55f * a);
	}
}
