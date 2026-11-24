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

/** 斧頭剝樹皮：順著木紋劃一刀，樹皮一片片捲起剝落 */
public class StripFx extends MicroFx {
	private static final int BARK = 0x6A4A30;
	private static final int WOOD = 0xE8C890;

	public StripFx(FxPayload p) {
		super(p);
		end = 0.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("bark_peel", 1f);
		}

		if (cue(0.06)) {
			burst(Kind.SHARD, face, nv.add(0, 0.6, 0), 12, 1.8f, 0.7f, 0x8A6448, BARK, 0.06f, 7f, 0.7);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0, 0.12));
		float a = 1 - span(t, 0.12, 0.5);
		Vec3 a0 = on(0.1, 0.5, 0.02);
		line(d, TRAIL, a0, lerp(a0, on(-0.1, -0.5, 0.02), k), 0.18f, WOOD, a * 0.7f);
		line(d, SPARK, a0, lerp(a0, on(-0.1, -0.5, 0.02), k), 0.04f, 0xFFFFFF, a);
	}
}
