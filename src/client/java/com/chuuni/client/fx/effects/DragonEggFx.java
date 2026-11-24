package com.chuuni.client.fx.effects;

import java.util.List;
import java.util.Random;

import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * 龍蛋・龍之遷躍（pos = 原位置，at = 瞬移後的位置）。短而快的一招：
 * <pre>
 * 鏡1 0.00 震顫  原位置極近：龍蛋劇烈顫抖、空間扭曲的紫環往內收
 * 塌陷 0.90     龍蛋被吸成一點 → 一道紫色的龍形光軌飛向新位置（廣角同時拍到兩處）
 * 衝擊 1.50     在新位置炸開重現：衝擊幀 + 毛筆直書招式名
 * 鏡3 1.50 英雄  新位置低角度仰望
 * 鏡4 3.20 殘響  光軌慢慢消散
 * </pre>
 */
public class DragonEggFx extends AnimeFx {
	private static final int DRAGON = 0xC060FF;
	private static final int DEEP = 0x401060;
	private static final ItemStack EGG = new ItemStack(Items.DRAGON_EGG);
	private static final double COLLAPSE = 0.9;

	private final Vec3 from;
	private final Vec3 to;

	public DragonEggFx(FxPayload p) {
		super(p);
		from = Vec3.atCenterOf(pos);
		to = p.at();
		name = "dragon_egg";
		main = DRAGON;
		pale = 0xF4E4FF;
		impactMode = PostState.VIOLET;
		s3 = 1.2;
		impact = 1.5;
		end = 4.6;
		look("dragon", 3.4);

		if (mine) {
			Vec3 mid = lerp(from, to, 0.5);
			Vec3 side = new Vec3(-(to.z - from.z), 0, to.x - from.x).normalize();
			double span = Math.max(4, from.distanceTo(to));
			Cinematic.play(List.of(
				Shot.of(COLLAPSE, from.add(frame.front().scale(1.3)).add(0, 0.2, 0), from.add(frame.front().scale(1.0)).add(0, 0.15, 0), from).roll(6, 10)
					.fov(0.8f, 0.7f),
				Shot.of(impact - COLLAPSE, mid.add(side.scale(span * 0.6)).add(0, span * 0.3, 0), mid.add(side.scale(span * 0.5)).add(0, span * 0.28, 0), mid.add(0, 1.2, 0)).roll(-3, -5),
				Shot.of(3.2 - impact, to.add(frame.front().scale(1.6)).add(0.5, -0.4, 0), to.add(frame.front().scale(2.0)).add(0.7, -0.3, 0), to.add(0, 0.6, 0))
					.roll(12, 9).fov(1.12f, 1.06f),
				Shot.of(end - 3.2, mid.add(side.scale(span * 0.7)).add(0, span * 0.5, 0), mid.add(side.scale(span * 0.9)).add(0, span * 0.6, 0), mid).roll(-2, -1)
			));
		}
	}

	/** 光軌的位置（0 = 原位置、1 = 新位置），中間往上拱成一道弧 */
	private Vec3 trail(double k) {
		return lerp(from, to, k).add(0, Math.sin(k * Math.PI) * Math.max(2, from.distanceTo(to) * 0.4), 0);
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "egg_warp", c, 1.2f, 1f);
		}

		if (t < COLLAPSE) {
			int n = rate(90, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = from.add(Particles.randomDir(1.4 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.GLOW, at, Vec3.ZERO, 0.5).size(0.06f, 0.01f).color(0xFFFFFF, DRAGON).physics(0, 0).attract(from, 24f);
			}
		}

		if (t > COLLAPSE && t < impact + 0.2) {
			float k = easeIn(span(t, COLLAPSE, impact));
			Vec3 head = trail(k);
			int n = rate(220, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.FLARE, head.add(Particles.randomDir(0.2)), Particles.randomDir(0.4), 1.0).size(0.12f, 0.02f).color(0xFFFFFF, DRAGON).physics(0, 1f);
			}
		}

		if (cue(impact - 0.1)) {
			Sfx.play(mine, "egg_appear", c, 1.2f, 1f);
		}

		beats(t, dt, to);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, end - 0.4, end);
		float charge = t < COLLAPSE ? span(t, 0, COLLAPSE) : 0;
		float burst = t > impact ? 1 - span(t, impact, impact + 1.4) : 0;
		lights.add(new CinemaPost.Light(from, 3f, r(DRAGON), g(DRAGON), b(DRAGON), 0.8f * charge, true));
		lights.add(new CinemaPost.Light(to, 3f + burst * 3, r(DRAGON), g(DRAGON), b(DRAGON), (0.2f + burst * 0.8f) * life, true));

		if (t > COLLAPSE && t < impact) {
			lights.add(new CinemaPost.Light(trail(easeIn(span(t, COLLAPSE, impact))), 3f, r(DRAGON), g(DRAGON), b(DRAGON), 0.9f, true));
		}
	}

	@Override
	protected void items(Draw d, double t) {
		// 原位置的「殘像」龍蛋：顫抖 → 被吸成一點
		if (t < COLLAPSE + 0.05) {
			float shrink = 1 - easeIn(span(t, COLLAPSE - 0.25, COLLAPSE));
			double jitter = 0.02 + span(t, 0, COLLAPSE) * 0.05;
			Vec3 at = from.add(Math.sin(t * 97) * jitter, -0.1 + Math.cos(t * 83) * jitter, Math.sin(t * 71) * jitter);
			d.item(EGG, at, 1.6f * shrink, (float)(t * t * 6), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.5, end);

		// 原位置：往內收的扭曲紫環
		if (t < COLLAPSE + 0.1) {
			for (int k = 0; k < 3; k++) {
				double ph = ((t * 1.6) + k / 3.0) % 1.0;
				float rr = (float)(1.8 * (1 - ph)) + 0.2f;
				d.billboard(Mode.GLOW, RING, from.x, from.y, from.z, rr, 0, r(DRAGON), g(DRAGON), b(DRAGON), (float)ph * 0.8f);
			}

		}

		// 新位置：重現前用暗紫的漩渦遮住（方塊其實已經在那裡了）
		if (t < impact) {
			float a = span(t, 0, 0.3);
			d.billboard(Mode.ALPHA, SMOKE, to.x, to.y, to.z, 1.4f, (float)(t * 4), r(DEEP), g(DEEP), b(DEEP), 0.9f * a);
			d.billboard(Mode.GLOW, JAG, to.x, to.y, to.z, 1.0f, (float)(t * 6), r(DRAGON), g(DRAGON), b(DRAGON), 0.5f * a);
		}

		// 龍形光軌：沿弧線畫出、往後拖長，抵達後慢慢消散
		if (t > COLLAPSE) {
			float head = easeIn(span(t, COLLAPSE, impact));
			float fadeA = (1 - span(t, impact + 0.4, end)) * life;
			int segs = 40;
			Random rng = new Random((long)Math.floor(t * 12));

			for (int i = 0; i < segs; i++) {
				double k0 = (double)i / segs * head;
				double k1 = (double)(i + 1) / segs * head;
				Vec3 a = trail(k0);
				Vec3 b = trail(k1);
				float w = 0.05f + 0.25f * (float)(i + 1) / segs;
				d.streak(Mode.GLOW, TRAIL, a.x, a.y, a.z, b.x, b.y, b.z, w * 4, r(DRAGON), g(DRAGON), b(DRAGON), 0.8f * fadeA);
				d.streak(Mode.GLOW, TRAIL, a.x, a.y, a.z, b.x, b.y, b.z, w * 0.6f, 1f, 1f, 1f, fadeA);
			}

			if (t < impact) {
				Vec3 h = trail(head);
				d.billboard(Mode.GLOW, FLARE, h.x, h.y, h.z, 2.2f, (float)t * 4, 1f, 1f, 1f, 1f);
				d.billboard(Mode.GLOW, GLOW, h.x, h.y, h.z, 2.5f, 0, r(DRAGON), g(DRAGON), b(DRAGON), 0.8f);
				bolt(d, h, h.add(Particles.randomDir(1.2)), rng, 0.04f, 0.8f, DRAGON);
			}
		}

		shockRings(d, t, new Vec3(to.x, Math.floor(to.y) + 0.05, to.z), to, 5f);
	}
}
