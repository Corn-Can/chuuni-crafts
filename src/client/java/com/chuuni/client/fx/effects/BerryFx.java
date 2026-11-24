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

/** 摘莓果（variant 0 甜莓紅、1 發光莓橙）：幾顆果實狀的光點彈出來、在地上彈一下 */
public class BerryFx extends MicroFx {
	private final int col;

	public BerryFx(FxPayload p) {
		super(p);
		col = p.variant() == 1 ? 0xFFB040 : 0xE02040;
		end = 0.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("berry_pop", p.variant() == 1 ? 1.1f : 1f);

			for (int i = 0; i < 5; i++) {
				Particles.spawn(Kind.GLOW, c, Particles.randomDir(1.2).add(0, 1.6, 0), 0.7).size(0.09f, 0.06f).color(0xFFFFFF, col).physics(6f, 0.5f);
			}

			Particles.spawn(Kind.FLARE, c, Vec3.ZERO, 0.3).size(0.4f, 0.1f).color(0xFFFFFF, col).spin(3f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		glow(d, c, 0.5f, col, 0.35f * (1 - span(t, 0, 0.5)));
	}
}
