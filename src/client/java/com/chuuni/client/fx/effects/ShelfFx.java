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

/** 架子：一道光沿著架面從左掃到右，三個擺放位各閃一下 */
public class ShelfFx extends MicroFx {
	private static final int WOOD = 0xFFD8A0;

	public ShelfFx(FxPayload p) {
		super(p);
		end = 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("shelf_place", 1f);
		}

		for (int i = 0; i < 3; i++) {
			if (cue(0.05 + i * 0.07)) {
				Vec3 at = on(-0.33 + i * 0.33, hit.y - face.y, 0.05);
				Particles.spawn(Kind.FLARE, at, new Vec3(0, 0.3, 0), 0.35).size(0.12f, 0.03f).color(0xFFFFFF, WOOD).spin(3f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0, 0.25));
		float a = 1 - span(t, 0.25, 0.6);
		double y = hit.y - face.y;
		line(d, SPARK, on(-0.48, y, 0.03), on(-0.48 + 0.96 * k, y, 0.03), 0.04f, WOOD, a);
	}
}
