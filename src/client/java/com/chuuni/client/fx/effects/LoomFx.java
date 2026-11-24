package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 織布機・天衣織紋。
 * 織機上方，彩色的經線與緯線一條條射入、交錯成一張光之布 → 中央浮現金色紋章「啪」地蓋印 → 旗幟從光布中升起、緩緩轉身。
 */
public class LoomFx extends MinorFx {
	private static final int GOLD = 0xFFD080;
	private static final int[] THREAD = {0xFF6A6A, 0xFFD050, 0x6AA8FF, 0x70E080, 0xD080FF, 0xFFFFFF};
	private static final double STAMP = 1.1;
	private static final int LINES = 6;

	private final ItemStack item;
	private final Vec3 loom;

	public LoomFx(FxPayload p) {
		super(p);
		item = p.item();
		loom = new Vec3(c.x, pos.getY() + 1.12, c.z);
		end = 2.6;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "loom_weave", c, 0.9f, 1f);
			tint(GOLD, 1.8);
			smallName("loom");

			if (mine) {
				com.chuuni.client.fx.Cinematic.peek(2.2);
			}
		}

		if (cue(STAMP)) {
			pop(GOLD, 0.45f);

			for (int i = 0; i < 30; i++) {
				Particles.spawn(Kind.GLYPH, loom, Particles.randomDir(Particles.rand(0.6f, 1.8f)).add(0, 0.6, 0), 1.0).size(0.07f, 0.02f)
					.color(0xFFFFFF, THREAD[i % THREAD.length]).physics(-0.3f, 1.5f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		float k = span(time(), 0.2, STAMP) * (1 - span(time(), STAMP + 0.6, end));
		lights.add(new CinemaPost.Light(loom, 2.5f, r(GOLD), g(GOLD), b(GOLD), 0.45f * k, true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (item.isEmpty() || t < STAMP) {
			return;
		}

		float up = easeOut(span(t, STAMP, STAMP + 0.8));
		float s = 0.8f * (1 - span(t, end - 0.4, end));
		d.item(item, loom.add(0, 0.2 + up * 0.7, 0), s, (float)(t * 1.6), 0);
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.4, end);
		float clothA = (1 - span(t, STAMP + 0.4, STAMP + 1.2)) * life;
		Vec3 du = new Vec3(u.x, 0, u.z);
		Vec3 dn = new Vec3(n.x, 0, n.z);

		// 經線（沿法線方向）與緯線（沿 u 方向）一條條射入
		for (int i = 0; i < LINES; i++) {
			double off = (i - (LINES - 1) / 2.0) * 0.16;
			float warp = easeOut(span(t, 0.05 + i * 0.07, 0.3 + i * 0.07));
			float weft = easeOut(span(t, 0.5 + i * 0.07, 0.75 + i * 0.07));
			int cw = THREAD[i % THREAD.length];
			int cf = THREAD[(i + 3) % THREAD.length];
			double wave = Math.sin(t * 9 + i) * 0.015;

			if (warp > 0) {
				Vec3 a0 = loom.add(du.scale(off)).add(dn.scale(-0.5)).add(0, wave, 0);
				line(d, SPARK, a0, a0.add(dn.scale(warp)), 0.09f, cw, clothA);
			}

			if (weft > 0) {
				Vec3 b0 = loom.add(dn.scale(off)).add(du.scale(-0.5)).add(0, -wave, 0);
				line(d, SPARK, b0, b0.add(du.scale(weft)), 0.09f, cf, clothA);
			}
		}

		// 光之布 + 紋章
		float weave = span(t, 0.6, STAMP);
		d.flat(Mode.GLOW, GRID, loom.add(0, -0.01, 0), 0.6f * easeOut(weave), 0, r(GOLD), g(GOLD), b(GOLD), 0.35f * clothA);
		float crest = easeOutBack(span(t, STAMP - 0.15, STAMP)) * (1 - span(t, STAMP + 0.5, end));
		groundRing(d, t, STAMP, STAMP + 0.6, loom, 0.2f, 1.6f, GOLD, 0.8f, JAG);
		glow(d, loom, 1.0f, GOLD, 0.3f * crest);
	}
}
