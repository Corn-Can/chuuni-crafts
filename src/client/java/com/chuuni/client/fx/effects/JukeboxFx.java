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
 * 唱片機・天籟之音。
 * 唱片從手中飛出、在唱片機上方化成一張巨大的光之唱盤旋轉 → 一圈圈彩虹色的聲波往外擴散，
 * 符文像音符一樣沿著正弦波往上飄。
 */
public class JukeboxFx extends MinorFx {
	private static final int[] HUE = {0xFF6A9A, 0xFFB050, 0xFFF070, 0x70E8A0, 0x60B0FF, 0xB080FF};
	private static final int VIOLET = 0xB080FF;

	private final ItemStack disc;
	private final Vec3 top;

	public JukeboxFx(FxPayload p) {
		super(p);
		disc = p.item();
		top = new Vec3(c.x, pos.getY() + 1.35, c.z);
		end = 3.0;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "disc_drop", c, 0.9f, 1f);
			tint(VIOLET, 2.4);
			smallName("jukebox");
		}

		if (cue(0.5)) {
			pop(VIOLET, 0.35f);
		}

		if (t > 0.4 && t < end - 0.5) {
			int nn = rate(14, dt);

			for (int i = 0; i < nn; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = top.add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6);
				Particles.spawn(Kind.GLYPH, at, new Vec3(Math.cos(a) * 0.4, 0.8, Math.sin(a) * 0.4), 1.8).size(0.1f, 0.05f).color(0xFFFFFF, HUE[Particles.RNG.nextInt(HUE.length)])
					.physics(-0.1f, 0.3f).spin(1.5f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		int h = HUE[(int)(time() * 4) % HUE.length];
		lights.add(new CinemaPost.Light(top, 3f, r(h), g(h), b(h), 0.35f * (1 - span(time(), end - 0.6, end)) * span(time(), 0.3, 0.6), true));
	}

	@Override
	protected void items(Draw d, double t) {
		if (disc.isEmpty() || t > 0.45) {
			return;
		}

		// 唱片從點擊處飛向唱片機上方
		Vec3 from = hit.add(0, 0.2, 0);
		float k = easeOut(span(t, 0, 0.4));
		d.item(disc, lerp(from, top, k), 0.6f * (1 - span(t, 0.35, 0.45) * 0.8f), (float)(t * 18), (float)(Math.PI / 2 * k));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);
		float open = easeOutBack(span(t, 0.35, 0.6)) * life;

		// 光之唱盤
		for (int k = 0; k < 6; k++) {
			float rr = (0.3f + k * 0.09f) * open;
			d.flat(Mode.GLOW, RING, top.add(0, 0.004 * k, 0), rr, 0, r(VIOLET), g(VIOLET), b(VIOLET), (k == 5 ? 0.8f : 0.3f) * open);
		}

		d.flat(Mode.GLOW, GLOW, top.add(0, 0.03, 0), 0.22f * open, 0, 1f, 0.8f, 0.9f, 0.8f * open);

		// 唱片的反光：一道光沿著溝紋轉
		double sa = -t * 5;
		line(d, SPARK, top.add(Math.cos(sa) * 0.3 * open, 0.03, Math.sin(sa) * 0.3 * open), top.add(Math.cos(sa) * 0.75 * open, 0.03, Math.sin(sa) * 0.75 * open),
			0.06f, 0xFFFFFF, 0.6f * open);

		// 聲波：每 0.35 秒一圈，換一個顏色
		for (int k = 0; k < 8; k++) {
			double t0 = 0.5 + k * 0.32;
			float ph = span(t, t0, t0 + 1.0);

			if (ph > 0 && ph < 1) {
				int col = HUE[k % HUE.length];
				d.flat(Mode.GLOW, RING, top.add(0, -0.2 - ph * 0.2, 0), 0.8f + easeOut(ph) * 2.6f, 0, r(col), g(col), b(col), (1 - ph) * 0.7f * life);
			}
		}

		glow(d, top, 1.2f, VIOLET, 0.3f * open);
	}
}
