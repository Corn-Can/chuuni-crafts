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

/** 音階盒調音：一個符文音符依音高的顏色往上飄，兩側各一道聲波弧；音高越高飄越高 */
public class NoteFx extends MicroFx {
	private final int note;
	private final int col;

	public NoteFx(FxPayload p) {
		super(p);
		note = Math.max(0, Math.min(24, p.power()));
		// 原版音符粒子的配色
		double f = note / 24.0;
		float r = (float)Math.max(0, Math.sin(f * Math.PI * 2) * 0.65 + 0.35);
		float g = (float)Math.max(0, Math.sin((f + 1 / 3.0) * Math.PI * 2) * 0.65 + 0.35);
		float b = (float)Math.max(0, Math.sin((f + 2 / 3.0) * Math.PI * 2) * 0.65 + 0.35);
		col = (int)(r * 255) << 16 | (int)(g * 255) << 8 | (int)(b * 255);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			sfx("note_glow", (float)Math.pow(2, (note - 12) / 12.0));
			Particles.spawn(Kind.GLYPH, c.add(0, 0.6, 0), new Vec3(0, 0.8 + note * 0.03, 0), 0.8).size(0.14f, 0.06f).color(0xFFFFFF, col).physics(0, 1.5f);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float k = easeOut(span(t, 0, 0.5));
		float a = 1 - span(t, 0.1, 0.6);
		Vec3 side = new Vec3(u.x, 0, u.z);
		Vec3 top = c.add(0, 0.55, 0);

		for (int s = -1; s <= 1; s += 2) {
			Vec3 prev = null;

			for (int i = 0; i <= 6; i++) {
				double ang = -0.7 + 1.4 * i / 6;
				double rr = 0.35 + 0.4 * k;
				Vec3 at = top.add(side.scale(s * Math.cos(ang) * rr)).add(0, Math.sin(ang) * rr, 0);

				if (prev != null) {
					line(d, SPARK, prev, at, 0.03f, col, a);
				}

				prev = at;
			}
		}
	}
}
