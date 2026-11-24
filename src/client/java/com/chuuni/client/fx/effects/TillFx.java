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

/** 鋤地：土塊翻起散落，一道淡淡的金色田壟光在土面上劃過 */
public class TillFx extends MicroFx {
	private static final int SOIL = 0x5A3C24;
	private static final int FURROW = 0xE0C080;

	public TillFx(FxPayload p) {
		super(p);
		end = 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("soil_till", 1f);
			burst(Kind.SHARD, new Vec3(c.x, pos.getY() + 1.02, c.z), new Vec3(0, 1, 0), 10, 1.6f, 0.8f, 0x7A5A3A, SOIL, 0.06f, 8f, 0.6);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0, 0.2));
		float a = 1 - span(t, 0.2, 0.6);
		Vec3 top = new Vec3(c.x, pos.getY() + 0.96, c.z);
		Vec3 side = new Vec3(u.x, 0, u.z);

		for (int i = -1; i <= 1; i++) {
			Vec3 row = top.add(new Vec3(-side.z, 0, side.x).scale(i * 0.28));
			line(d, SPARK, row.add(side.scale(-0.45)), row.add(side.scale(-0.45 + 0.9 * k)), 0.03f, FURROW, a * 0.7f);
		}
	}
}
