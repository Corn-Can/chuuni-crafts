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

/** 鱗甲拂落：刷子三下刷過甲殼，一片鱗甲旋轉著彈出來，揚起細沙 */
public class ArmadilloFx extends MinorFx {
	private static final int SHELL = 0xC09080;
	private static final ItemStack SCUTE = new ItemStack(Items.ARMADILLO_SCUTE);

	public ArmadilloFx(FxPayload p) {
		super(p);
		end = 1.4;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "scute_brush", c, 0.9f, 1f);
			tint(SHELL, 1.0);
			smallName("armadillo");
		}

		if (cue(0.36)) {
			pop(SHELL, 0.3f);
			Particles.spawn(Kind.SMOKE, body(), new Vec3(0, 0.5, 0), 0.8).size(0.2f, 0.6f).color(0xE8D8C0, 0xA08870).alpha(0.4f).physics(0, 2f);
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (t < 0.36) {
			return;
		}

		float k = span(t, 0.36, 1.1);
		Vec3 at = body().add(nv.scale(k * 0.8)).add(0, 0.2 + Math.sin(k * Math.PI) * 0.6, 0);
		d.item(SCUTE, at, 0.4f * (1 - span(t, 1.1, end)), (float)(t * 12), (float)(t * 7));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 b = body().add(0, 0.15, 0);
		Vec3 side = new Vec3(u.x, 0, u.z).scale(0.4);

		for (int i = 0; i < 3; i++) {
			double t0 = i * 0.11;
			float k = easeOut(span(t, t0, t0 + 0.09));
			float a = 1 - span(t, t0 + 0.09, t0 + 0.3);

			if (k > 0 && a > 0.01f) {
				Vec3 a0 = b.add(side.scale(-1)).add(nv.scale(0.3));
				line(d, TRAIL, a0, lerp(a0, b.add(side).add(nv.scale(0.3)), k), 0.2f, 0xFFE0D0, a);
				glow(d, b, 0.8f, SHELL, a * 0.4f);
			}
		}
	}
}
