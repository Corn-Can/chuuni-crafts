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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 合成器・機巧自律（紅石自動觸發，同一台 1 秒內只演一次）。
 * 四面浮出齒輪陣交錯咬合轉動、頂面的 3×3 格一格一格亮起 → 活塞「嘶」地噴出蒸氣 → 成品沿出口方向射出，拖著一道光軌。
 */
public class CrafterFx extends MinorFx {
	private static final int MECH = 0x80D0FF;
	private static final int HEAT = 0xFF9A40;
	private static final double EJECT = 0.6;

	private final ItemStack item;
	private final Vec3 out;

	public CrafterFx(FxPayload p) {
		super(p);
		item = p.item();
		var s = state();
		Direction front = s != null && s.hasProperty(BlockStateProperties.ORIENTATION) ? s.getValue(BlockStateProperties.ORIENTATION).front() : Direction.UP;
		out = new Vec3(front.getStepX(), front.getStepY(), front.getStepZ());
		end = 1.8;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "crafter_pulse", c, 0.8f, 1f);

			if (mine) {
				tint(MECH, 1.0);
				smallName("crafter");
			}
		}

		if (cue(EJECT - 0.1)) {
			for (int i = 0; i < 10; i++) {
				Particles.spawn(Kind.SMOKE, c.add(0, 0.5, 0), Particles.randomDir(0.5).add(0, 0.8, 0), 0.9).size(0.15f, 0.5f).color(0xFFFFFF, 0xB0C0D0).alpha(0.45f)
					.physics(-0.4f, 1.5f);
			}
		}

		if (cue(EJECT)) {
			pop(MECH, 0.3f);
			sparks(c.add(out.scale(0.55)), 16, 2.5f, HEAT, 0.35f);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = 1 - span(time(), EJECT, end);
		lights.add(new CinemaPost.Light(c, 2.5f, r(MECH), g(MECH), b(MECH), 0.35f * k, true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (item.isEmpty() || t < EJECT) {
			return;
		}

		float k = easeOut(span(t, EJECT, EJECT + 0.5));
		float s = 0.45f * (1 - span(t, end - 0.3, end));
		d.item(item, c.add(out.scale(0.5 + k * 0.9)), s, (float)(t * 6), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.3, end);
		float gear = easeOutBack(span(t, 0, 0.2)) * (1 - span(t, EJECT + 0.3, end));

		// 四面的齒輪陣，相鄰的反向轉
		Vector3f up = new Vector3f(0, 1, 0);

		for (int i = 0; i < 4; i++) {
			Direction side = Direction.from2DDataValue(i);
			Vector3f sn = new Vector3f(side.getStepX(), 0, side.getStepZ());
			Vector3f su = new Vector3f(-sn.z, 0, sn.x);
			Vec3 at = c.add(sn.x * 0.53, 0, sn.z * 0.53);
			// 三條橫向光縫依序推出，像機關的活塞一節節運轉
			for (int k = 0; k < 3; k++) {
				float push = (float)Math.max(0, Math.sin((t * 9 - k * 0.9)));
				Vec3 mid = at.add(0, (k - 1) * 0.28, 0).add(sn.x * push * 0.06, 0, sn.z * push * 0.06);
				Vec3 l0 = mid.add(su.x * -0.38, 0, su.z * -0.38);
				Vec3 l1 = mid.add(su.x * 0.38, 0, su.z * 0.38);
				line(d, SPARK, l0, l1, 0.05f + push * 0.03f, k == 1 ? HEAT : MECH, (0.35f + push * 0.5f) * gear);
			}
		}

		// 頂面 3×3 格一格一格亮
		Vec3 top = new Vec3(c.x, pos.getY() + 1.02, c.z);

		for (int i = 0; i < 9; i++) {
			float on = span(t, 0.05 + i * 0.05, 0.12 + i * 0.05) * (1 - span(t, EJECT + 0.2, end));
			Vec3 cell = top.add((i % 3 - 1) * 0.3, 0, (i / 3 - 1) * 0.3);
			d.flat(Mode.GLOW, GLOW, cell, 0.16f, 0, r(i == 4 ? HEAT : MECH), g(i == 4 ? HEAT : MECH), b(i == 4 ? HEAT : MECH), on * 0.8f);
		}

		// 出口的光軌
		float trail = span(t, EJECT, EJECT + 0.5);

		if (trail > 0 && trail < 1) {
			line(d, TRAIL, c.add(out.scale(0.5)), c.add(out.scale(0.5 + easeOut(trail) * 0.9)), 0.3f, HEAT, (1 - trail) * life);
		}

		glow(d, c.add(out.scale(0.55)), 0.8f, HEAT, (1 - span(t, EJECT, EJECT + 0.6)) * span(t, EJECT - 0.1, EJECT) * 0.8f);
	}
}
