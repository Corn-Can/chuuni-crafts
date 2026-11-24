package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 界伏盒・匣中異界（顏色跟著盒子的染色）。
 * 三個互相垂直的空間摺疊環繞著盒子收放旋轉（盒裡藏著另一個空間）→ 小方塊碎片慢慢往上飄浮（漂浮效果），
 * 盒蓋上方一團柔光。
 */
public class ShulkerFx extends MinorFx {
	private final int col;

	public ShulkerFx(FxPayload p) {
		super(p);
		col = mapColor(0xA070A0);
		end = 2.2;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "shulker_open", c, 0.9f, 1f);
			tint(col, 1.6);
			smallName("shulker");

			if (mine) {
				com.chuuni.client.fx.Cinematic.peek(1.4);
			}
		}

		if (cue(0.15)) {
			pop(col, 0.35f);
		}

		if (t > 0.1 && t < 1.6) {
			int nn = rate(30, dt);

			for (int i = 0; i < nn; i++) {
				Vec3 at = c.add(Particles.rand(-0.9f, 0.9f), -0.5, Particles.rand(-0.9f, 0.9f));
				Particles.spawn(Kind.SHARD, at, new Vec3(0, 0.5, 0), 1.4).size(0.06f, 0.03f).color(0xFFFFFF, col).physics(-0.4f, 0.8f).spin(2f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(c.add(0, 0.6, 0), 2.5f, r(col), g(col), b(col), 0.45f * (1 - span(time(), 1.2, end)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);
		float open = easeOutBack(span(t, 0, 0.35)) * life;
		float breathe = 1 + 0.12f * (float)Math.sin(t * 6);
		Vector3f[][] planes = {{Draw.X, Draw.Y}, {Draw.Y, Draw.Z}, {Draw.X, Draw.Z}};

		for (int i = 0; i < 3; i++) {
			float rr = (0.95f + i * 0.08f) * open * breathe;
			float rot = (float)(t * (i % 2 == 0 ? 1.4 : -1.1));
			d.disc(Mode.GLOW, RING, c, planes[i][0], planes[i][1], rr, rot, r(col), g(col), b(col), 0.6f * open);
		}

		glow(d, c.add(0, 0.65, 0), 1.0f, col, 0.5f * open);
		groundRing(d, t, 0.15, 0.8, new Vec3(c.x, pos.getY() + 0.03, c.z), 0.3f, 2.2f, col, 0.8f, JAG);
	}
}
