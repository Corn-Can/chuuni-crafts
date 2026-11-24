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

/**
 * 終界之眼・終焉指引。
 * 眼睛升空時兩顆星點繞著它螺旋；往要塞的方向射出一道淡淡的「指引之光」，光上的刻度一節一節往前流；
 * 最後眼睛在空中碎裂（或落下）時炸出綠色的玻璃碎片與一圈光環。
 */
public class EyeFx extends FlightFx {
	private static final int VOIDC = 0x60FFD0;
	private static final int GOLD = 0xD8FF70;

	/** 最後一次知道的水平飛行方向（指引的方向） */
	private Vec3 heading = Vec3.ZERO;
	private Vec3 prev;

	public EyeFx(FxPayload p) {
		super(p);
		trailLife = 0.6;
		after = 1.0;
		prev = p.at();
	}

	@Override
	protected void fly(double t, double dt, Vec3 at) {
		if (cue(0.0)) {
			Sfx.anime(mine, "rise", "eye_launch", at, 0.9f, 1f);
			smallName("eye");
		}

		Vec3 mv = at.subtract(prev);
		prev = at;
		Vec3 flat = new Vec3(mv.x, 0, mv.z);

		if (flat.lengthSqr() > 1e-5) {
			heading = heading.scale(0.85).add(flat.normalize().scale(0.15));
		}

		int n = rate(40, dt);

		for (int i = 0; i < n; i++) {
			Particles.spawn(Kind.FLARE, at.add(Particles.randomDir(0.15)), new Vec3(0, -0.3, 0), Particles.rand(0.5f, 1f)).size(0.04f, 0.005f)
				.color(0xFFFFFF, i % 2 == 0 ? VOIDC : GOLD).physics(0.5f, 1.5f).spin(3f);
		}
	}

	@Override
	protected void land(Vec3 at) {
		Sfx.play(mine, "eye_shatter", at, 0.8f, 1f);
		shake(0.08f);

		for (int i = 0; i < 26; i++) {
			Particles.spawn(Kind.SHARD, at, Particles.randomDir(Particles.rand(1.5f, 4f)).add(0, 1, 0), Particles.rand(0.6f, 1.1f)).size(0.07f, 0.02f)
				.color(0xE0FFF4, VOIDC).physics(6f, 1f).spin(10f);
		}

		sparks(at, 24, 4f, VOIDC, 0.5f);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		headLight(lights, VOIDC, 0.3f);
	}

	@Override
	protected void render(Draw d, double t) {
		ribbon(d, t, 0.14f, VOIDC, 0.75f);

		if (flying()) {
			// 兩顆繞著眼睛螺旋的星點
			for (int k = 0; k < 2; k++) {
				double a = t * 9 + k * Math.PI;
				Vec3 o = head.add(Math.cos(a) * 0.45, Math.sin(t * 4 + k) * 0.15, Math.sin(a) * 0.45);
				d.billboard(Mode.GLOW, FLARE, o.x, o.y, o.z, 0.18f, (float)t * 5, 1f, 1f, 1f, 0.9f);
				glow(d, o, 0.3f, k == 0 ? VOIDC : GOLD, 0.7f);
			}

			glow(d, head, 0.6f, VOIDC, 0.6f);

			// 指引之光：往要塞方向延伸、越遠越淡，上面的刻度往前流動
			float on = span(t, 0.3, 0.9);

			if (on > 0 && heading.lengthSqr() > 0.01) {
				Vec3 dir = heading.normalize();
				Vec3 far = head.add(dir.scale(24));
				line(d, TRAIL, head, far, 0.08f, VOIDC, 0.35f * on);

				for (int i = 0; i < 8; i++) {
					double s = (i + (t * 1.5) % 1) * 3;
					Vec3 a = head.add(dir.scale(s));
					Vec3 b = head.add(dir.scale(s + 0.8));
					line(d, SPARK, a, b, 0.09f, GOLD, on * 0.8f * (1 - (float)s / 24));
				}
			}

			return;
		}

		float k = span(t, landT, landT + 0.7);

		if (k < 1) {
			d.billboard(Mode.GLOW, RING, head.x, head.y, head.z, 0.3f + easeOut(k) * 1.8f, 0, r(VOIDC), g(VOIDC), b(VOIDC), (1 - k) * (1 - k));
			glow(d, head, 1.2f, VOIDC, (1 - k) * 0.7f);
		}
	}
}
