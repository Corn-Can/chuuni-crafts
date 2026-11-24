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

/** 點亮紅石礦：礦石裡的紅色結晶「劈啪」爆出火花，從點到的面往外脈動一下 */
public class RedstoneOreFx extends MicroFx {
	private static final int RED = 0xFF3020;

	public RedstoneOreFx(FxPayload p) {
		super(p);
		end = 0.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("ore_glow", 1f);
			burst(Kind.SPARK, face, nv, 14, 2f, 0.9f, 0xFFFFFF, RED, 0.025f, 4f, 0.35);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float a = 1 - span(t, 0.05, 0.6);
		faceDisc(d, GLOW, face, 0.6f, 0, RED, a * 0.6f);
		faceRing(d, t, 0, 0.5, face, 0.1f, 0.9f, RED, 0.7f);
	}
}
