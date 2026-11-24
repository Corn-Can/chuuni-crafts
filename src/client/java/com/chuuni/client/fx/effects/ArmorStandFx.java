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

/** 盔甲架換裝：一道金屬光由下往上掃過盔甲架，換裝的位置閃一下 */
public class ArmorStandFx extends MicroFx {
	private static final int METAL = 0xD0D8E8;

	public ArmorStandFx(FxPayload p) {
		super(p);
		end = 0.7;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("stand_equip", 1f);
			sparks(p.at().add(0, p.power() / 10.0 - 1, 0), 8, 1.5f, METAL, 0.3f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		var e = target();
		Vec3 feet = e == null ? p.at().add(0, -1, 0) : e.position();
		float k = easeOut(span(t, 0, 0.35));
		float a = 1 - span(t, 0.35, 0.7);
		Vec3 y = feet.add(0, k * 2.0, 0);
		Vec3 side = new Vec3(u.x, 0, u.z).scale(0.4);
		line(d, SPARK, y.add(side.scale(-1)), y.add(side), 0.05f, METAL, a);
	}
}
