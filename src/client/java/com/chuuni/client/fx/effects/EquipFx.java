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

/** 鞍甲加身：裝備從手中飛向牠的背、「喀」地扣上，扣上的位置閃出金屬光、火花四濺 */
public class EquipFx extends MinorFx {
	private static final int METAL = 0xD8E0F0;

	private final ItemStack gear;

	public EquipFx(FxPayload p) {
		super(p);
		gear = p.item();
		end = 1.6;
	}

	private Vec3 back() {
		return body().add(0, bodyHeight() * 0.3, 0);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "equip_armor", c, 0.9f, 1f);
			tint(METAL, 1.0);
			smallName("equip");
		}

		if (cue(0.3)) {
			pop(METAL, 0.35f);
			sparks(back(), 20, 3f, METAL, 0.4f);
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (gear.isEmpty() || t > 0.32) {
			return;
		}

		float k = easeIn(span(t, 0, 0.3));
		d.item(gear, lerp(hand(), back(), k).add(0, Math.sin(k * Math.PI) * 0.4, 0), 0.6f, (float)(t * 14), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float a = span(t, 0.3, 0.35) * (1 - span(t, 0.5, 1.4));
		Vec3 at = back();
		glow(d, at, 1.4f, METAL, a * 0.8f);
		groundRing(d, t, 0.3, 1.0, body().add(0, -bodyHeight() * 0.5 + 0.03, 0), 0.3f, 1.8f, METAL, 0.8f, JAG);
		d.billboard(Mode.GLOW, FLARE, at.x, at.y, at.z, 0.8f * a, (float)t * 3, 1f, 1f, 1f, a);
		line(d, SPARK, at.add(-bodyWidth(), 0, 0), at.add(bodyWidth(), 0, 0), 0.09f, METAL, a);
	}
}
