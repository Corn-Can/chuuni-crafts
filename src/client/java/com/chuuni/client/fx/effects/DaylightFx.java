package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/** 陽光感測器切換：日間模式射出金色的放射光，夜間模式是一圈往下沉的藍光 */
public class DaylightFx extends MicroFx {
	private static final int SUN = 0xFFD060;
	private static final int MOON = 0x80A8FF;

	private final boolean night;

	public DaylightFx(FxPayload p) {
		super(p);
		night = p.power() == 1;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("daylight_flip", night ? 0.75f : 1.1f);

			if (night) {
				for (int i = 0; i < 8; i++) {
					Vec3 at = new Vec3(c.x, pos.getY() + 1.2, c.z).add(Particles.randomDir(0.4));
					Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.8, 0), 0.6).size(0.06f, 0.02f).color(0xFFFFFF, MOON).physics(0, 0.5f).spin(2f);
				}
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 top = boxTop().add(0, 0.02, 0);
		float a = 1 - span(t, 0.15, 0.7);

		if (night) {
			glow(d, top, 0.6f, MOON, a * 0.6f);
			return;
		}

		float k = easeOut(span(t, 0, 0.25));

		for (int i = 0; i < 8; i++) {
			double ang = i * Math.PI / 4 + 0.2;
			Vec3 dir = new Vec3(Math.cos(ang) * 0.6, 1, Math.sin(ang) * 0.6).normalize();
			line(d, SPARK, top.add(dir.scale(0.1)), top.add(dir.scale(0.1 + k * 0.7)), 0.04f, SUN, a);
		}

		glow(d, top, 0.5f, SUN, a * 0.6f);
	}
}
