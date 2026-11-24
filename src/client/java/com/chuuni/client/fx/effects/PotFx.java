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
 * 罐子與花盆。variant 0 = 裝飾陶罐放入物品、1 = 花盆種入、2 = 花盆取出。
 * <ul>
 *   <li>陶罐：物品縮小掉進罐口，罐身兩側晃出兩道弧線，罐口冒一小撮陶土粉</li>
 *   <li>種入：葉片從盆口往上彈開；取出：幾塊泥土翻出來</li>
 * </ul>
 */
public class PotFx extends MicroFx {
	private static final int CLAY = 0xD89070;
	private static final int LEAF = 0x70D060;
	private static final int SOIL = 0x6A4A30;

	private final int variant;
	private final ItemStack item;
	private final Vec3 mouth;

	public PotFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 3);
		item = p.item();
		mouth = boxTop();
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx(variant == 0 ? "pot_drop" : "plant_settle", variant == 2 ? 0.85f : 1f);
		}

		if (variant == 0 && cue(0.25)) {
			burst(Kind.SMOKE, mouth, new Vec3(0, 1, 0), 5, 0.5f, 0.3f, 0xF0D0C0, CLAY, 0.1f, -0.2f, 0.6);
		}

		if (variant == 1 && cue(0.05)) {
			burst(Kind.SHARD, mouth, new Vec3(0, 1, 0), 10, 1.6f, 0.6f, 0xA0F080, LEAF, 0.05f, 4f, 0.7);
		}

		if (variant == 2 && cue(0.05)) {
			burst(Kind.SHARD, mouth, new Vec3(0, 1, 0), 8, 1.4f, 0.6f, 0x8A6A48, SOIL, 0.05f, 7f, 0.6);
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (variant != 0 || item.isEmpty() || t > 0.3) {
			return;
		}

		float k = easeIn(span(t, 0, 0.25));
		d.item(item, mouth.add(0, 0.5 * (1 - k), 0), 0.4f * (1 - k * 0.8f), (float)(t * 10), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		if (variant != 0) {
			glow(d, mouth, 0.5f, variant == 1 ? LEAF : SOIL, 0.35f * (1 - span(t, 0, 0.5)));
			return;
		}

		// 罐身晃動的弧線（左右各一）
		float a = span(t, 0.22, 0.27) * (1 - span(t, 0.3, 0.75));
		glow(d, mouth, 0.5f, CLAY, a * 0.6f);
		Vec3 side = new Vec3(u.x, 0, u.z);

		for (int s = -1; s <= 1; s += 2) {
			Vec3 prev = null;

			for (int i = 0; i <= 8; i++) {
				double ang = -0.6 + 1.2 * i / 8;
				Vec3 at = c.add(side.scale(s * (0.58 + 0.08 * Math.cos(ang)))).add(nv.x * 0.2, Math.sin(ang) * 0.4, nv.z * 0.2);

				if (prev != null) {
					line(d, SPARK, prev, at, 0.06f, 0xFFE0C8, a);
				}

				prev = at;
			}
		}
	}
}
