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

/** 骨粉：一小股綠色的生命光點螺旋往上冒，幾片新葉彈開 */
public class BonemealFx extends MicroFx {
	private static final int LIFE = 0x80E060;

	public BonemealFx(FxPayload p) {
		super(p);
		end = 0.9;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("bloom_burst", 0.95f + Particles.RNG.nextFloat() * 0.1f);
			burst(Kind.SHARD, hit.add(0, 0.1, 0), new Vec3(0, 1, 0), 10, 1.8f, 0.6f, 0xB0F090, LIFE, 0.06f, 4f, 0.7);
		}

		if (t < 0.5) {
			int n = rate(110, dt);

			for (int i = 0; i < n; i++) {
				double ang = t * 14 + i;
				Vec3 at = hit.add(Math.cos(ang) * 0.4, 0.05 + t * 1.2, Math.sin(ang) * 0.4);
				Particles.spawn(Kind.GLOW, at, new Vec3(0, 0.8, 0), 0.5).size(0.06f, 0.015f).color(0xFFFFFF, LIFE).physics(0, 1f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		glow(d, hit.add(0, 0.3, 0), 0.9f, LIFE, 0.5f * (1 - span(t, 0, 0.7)));
	}
}
