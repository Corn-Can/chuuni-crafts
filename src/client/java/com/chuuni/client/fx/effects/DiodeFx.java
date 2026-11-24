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

/**
 * 中繼器 / 比較器。variant 0 = 中繼器（power = 延遲 1~4）：沿著訊號方向的四顆刻度依序亮到目前的延遲；
 * variant 1 = 比較器（power = 1 減法）：前端火把閃一下（比較 = 紅、減法 = 白）。
 */
public class DiodeFx extends MicroFx {
	private static final int RED = 0xFF3020;

	private final boolean comparator;
	private final int value;
	private final Direction front;

	public DiodeFx(FxPayload p) {
		super(p);
		comparator = p.variant() == 1;
		value = p.power();
		front = facing();
		end = 0.7;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("diode_tick", comparator ? (value == 1 ? 1.15f : 0.95f) : 0.9f + value * 0.06f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 f = vec(front);
		Vec3 top = boxTop().add(0, 0.03, 0);

		if (comparator) {
			Vec3 torch = top.add(f.scale(-0.31)).add(0, 0.1, 0);
			float k = span(t, 0, 0.05) * (1 - span(t, 0.1, 0.6));
			int col = value == 1 ? 0xFFF0E0 : RED;
			glow(d, torch, 0.4f, col, k);
			d.billboard(Mode.GLOW, FLARE, torch.x, torch.y, torch.z, 0.3f * k, (float)t * 4, 1f, 1f, 1f, k);
			return;
		}

		// 訊號沿著中繼器跑一趟
		float run = span(t, 0, 0.3);

		if (run > 0 && run < 1) {
			Vec3 head = top.add(f.scale(0.45 - run * 0.9));
			line(d, TRAIL, top.add(f.scale(0.45)), head, 0.12f, RED, (1 - run) * 0.8f);
		}

		// 四格刻度（從輸入端到輸出端）
		for (int i = 0; i < 4; i++) {
			Vec3 at = top.add(f.scale(0.3 - i * 0.2));
			boolean lit = i < value;
			float k = lit ? span(t, i * 0.06, i * 0.06 + 0.05) * (1 - span(t, 0.35, 0.7)) : 0.15f * (1 - span(t, 0.2, 0.5));
			glow(d, at, 0.32f, lit ? RED : 0x502020, k);
			d.billboard(Mode.GLOW, FLARE, at.x, at.y, at.z, 0.16f * k, 0, 1f, 0.6f, 0.5f, lit ? k : 0);
		}
	}
}
