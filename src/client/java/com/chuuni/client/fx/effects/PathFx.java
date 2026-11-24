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

/** 鏟出小徑：一陣塵土往四周低低地散開，地面被「壓實」的光往外推一下 */
public class PathFx extends MicroFx {
	private static final int DUST = 0xC8A878;

	public PathFx(FxPayload p) {
		super(p);
		end = 0.7;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("path_press", 1f);
			Vec3 top = new Vec3(c.x, pos.getY() + 0.95, c.z);

			for (int i = 0; i < 10; i++) {
				double ang = i * Math.PI / 5;
				Particles.spawn(Kind.SMOKE, top, new Vec3(Math.cos(ang) * 1.2, 0.2, Math.sin(ang) * 1.2), 0.7).size(0.12f, 0.35f).color(0xE8D8B8, DUST).alpha(0.5f)
					.physics(0, 3f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 top = new Vec3(c.x, pos.getY() + 0.94, c.z);
		float k = span(t, 0, 0.35);

		// 四邊往外推的一道光（方形，跟著方塊的形狀）
		double r = 0.5 + k * 0.4;
		float a = (1 - k) * 0.7f;
		Vec3[] q = {top.add(-r, 0, -r), top.add(r, 0, -r), top.add(r, 0, r), top.add(-r, 0, r)};

		for (int i = 0; i < 4; i++) {
			line(d, SPARK, q[i], q[(i + 1) % 4], 0.03f, DUST, a);
		}
	}
}
