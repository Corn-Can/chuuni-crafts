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

/** 泥巴：水滲進泥土，褐色的泥點濺起、表面暗下去一圈 */
public class MudFx extends MicroFx {
	private static final int MUD = 0x4A3A30;

	public MudFx(FxPayload p) {
		super(p);
		end = 0.7;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("mud_make", 1f);
			burst(Kind.SHARD, hit, new Vec3(0, 1, 0), 10, 1.4f, 0.7f, 0x6A5444, MUD, 0.05f, 7f, 0.6);
			burst(Kind.GLOW, hit, new Vec3(0, 1, 0), 6, 1.2f, 0.6f, 0xFFFFFF, 0x6090C0, 0.04f, 6f, 0.4);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		groundRing(d, t, 0, 0.5, hit.add(0, 0.02, 0), 0.1f, 0.7f, 0x80A0C0, 0.5f, RING);
	}
}
