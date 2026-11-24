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

/** 按鈕：往內一縮的閃光，四周彈出幾點紅石粉 */
public class ButtonFx extends MicroFx {
	private static final int RED = 0xFF3828;

	public ButtonFx(FxPayload p) {
		super(p);
		end = 0.5;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("button_tick", 1f);
			burst(Kind.SPARK, boxCenter(), Vec3.ZERO, 7, 1f, 1f, 0xFFFFFF, RED, 0.02f, 3f, 0.3);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = span(t, 0, 0.15);
		Vec3 c = boxCenter();
		glow(d, c, 0.4f * (1 - k * 0.6f), RED, (1 - span(t, 0.05, 0.4)) * 0.7f);
		d.billboard(Mode.GLOW, FLARE, c.x, c.y, c.z, 0.25f * (1 - k), (float)t * 5, 1f, 1f, 1f, 1 - k);
	}
}
