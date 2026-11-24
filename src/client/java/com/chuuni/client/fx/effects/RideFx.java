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

/** 騎乘：坐騎腳下揚起一圈塵土，兩側掠過幾道速度線 */
public class RideFx extends MicroFx {
	private static final int DUST = 0xD8C8A8;

	public RideFx(FxPayload p) {
		super(p);
		end = 0.7;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("mount_ride", 1f);
			Vec3 feet = body().add(0, -bodyHeight() * 0.5 + 0.1, 0);

			for (int i = 0; i < 12; i++) {
				double a = i * Math.PI / 6;
				Particles.spawn(Kind.SMOKE, feet, new Vec3(Math.cos(a) * 1.4, 0.2, Math.sin(a) * 1.4), 0.6).size(0.12f, 0.35f).color(0xF0E8D8, DUST).alpha(0.45f)
					.physics(0, 3f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float a = 1 - span(t, 0.1, 0.5);
		Vec3 b = body();
		float w = bodyWidth();

		for (int s = -1; s <= 1; s += 2) {
			for (int i = 0; i < 2; i++) {
				Vec3 at = b.add(new Vec3(u.x, 0, u.z).scale(s * (w * 0.7 + 0.2))).add(0, -0.2 + i * 0.35, 0);
				float k = span(t, i * 0.05, 0.3 + i * 0.05);
				line(d, SPARK, at.add(nv.scale(0.6 - k)), at.add(nv.scale(-0.6 - k)), 0.03f, 0xFFFFFF, a * 0.6f);
			}
		}
	}
}
