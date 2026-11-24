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

/** 展示框：放入時框的四邊亮一下、物品「彈」出一點光；旋轉時沿著框畫一小段弧 */
public class ItemFrameFx extends MicroFx {
	private static final int GOLD = 0xFFE0A0;

	private final boolean rotate;

	public ItemFrameFx(FxPayload p) {
		super(p);
		rotate = p.variant() == 1;
		end = 0.6;
	}

	private net.minecraft.core.Direction frameDir() {
		return target() instanceof net.minecraft.world.entity.decoration.ItemFrame f ? f.getDirection() : dir;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx(rotate ? "frame_rotate" : "frame_place", rotate ? 0.9f + (p.power() % 8) * 0.03f : 1f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		var fd = frameDir();
		Vec3 n = vec(fd);
		Vec3 a = across(fd);
		Vec3 b = fd.getAxis() == net.minecraft.core.Direction.Axis.Y ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
		var e = target();
		Vec3 ctr = (e == null ? p.at() : e.position()).add(n.scale(0.02));
		float fade = 1 - span(t, 0.1, 0.55);

		if (!rotate) {
			double r = 0.38;
			Vec3[] q = {ctr.add(a.scale(-r)).add(b.scale(-r)), ctr.add(a.scale(r)).add(b.scale(-r)), ctr.add(a.scale(r)).add(b.scale(r)), ctr.add(a.scale(-r)).add(b.scale(r))};

			for (int i = 0; i < 4; i++) {
				line(d, SPARK, q[i], q[(i + 1) % 4], 0.04f, GOLD, fade);
			}

			glow(d, ctr, 0.5f, GOLD, fade * 0.5f);
			return;
		}

		// 旋轉：沿著框畫 45 度的弧
		double start = p.power() * Math.PI / 4;
		float k = easeOut(span(t, 0, 0.2));
		Vec3 prev = null;

		for (int i = 0; i <= 8; i++) {
			double ang = start - (double)i / 8 * k * Math.PI / 4;
			Vec3 at = ctr.add(a.scale(Math.cos(ang) * 0.42)).add(b.scale(Math.sin(ang) * 0.42));

			if (prev != null) {
				line(d, TRAIL, prev, at, 0.06f, GOLD, fade);
			}

			prev = at;
		}
	}
}
