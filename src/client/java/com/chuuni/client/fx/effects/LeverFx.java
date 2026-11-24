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

/** 拉桿：沿著扳動方向劃出一道弧光，底座噴出紅石火花（關掉時是暗色、往回劃） */
public class LeverFx extends MicroFx {
	private static final int RED = 0xFF3020;
	private static final int OFF = 0x806060;

	private final boolean on;

	public LeverFx(FxPayload p) {
		super(p);
		on = p.power() == 1;
		end = 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("lever_click", on ? 1f : 0.88f);

			if (on) {
				burst(Kind.SPARK, boxCenter(), new Vec3(0, 1, 0), 8, 1.5f, 0.8f, 0xFFFFFF, RED, 0.025f, 4f, 0.35);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0, 0.12));
		float a = 1 - span(t, 0.12, 0.5);
		Vec3 side = vec(facing());
		Vec3 ctr = boxCenter();
		Vec3 prev = null;

		for (int i = 0; i <= 10; i++) {
			double f = (double)i / 10 * k;
			double ang = on ? Math.PI * 0.75 - f * Math.PI * 0.5 : Math.PI * 0.25 + f * Math.PI * 0.5;
			Vec3 at = ctr.add(side.scale(Math.cos(ang) * 0.35)).add(0, Math.sin(ang) * 0.35, 0);

			if (prev != null) {
				line(d, TRAIL, prev, at, 0.05f + 0.12f * (float)i / 10, on ? RED : OFF, a * 0.8f);
			}

			prev = at;
		}

		glow(d, ctr, 0.4f, on ? RED : OFF, a * 0.6f);
	}
}
