package com.chuuni.client.fx.effects;

import java.util.List;

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

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 砂輪・魔力剝奪（power = 被剝掉的附魔數）。演完回到砂輪介面。
 * <pre>
 * 鏡1 0.00 附魔  附魔過的物品浮在砂輪上，一圈紫色的魔力符文繞著它轉
 * 鏡2 1.20 研磨  側面：砂輪（光之齒輪）狂轉、火花噴射，符文一片片被扯下、撕碎
 * 鏡3 2.30 吸氣  物品極近特寫
 * 衝擊 2.70     魔力剝奪：紫色的魔力外殼碎裂 + 經驗珠噴泉，毛筆直書招式名
 * 鏡4 2.70 英雄  低角度仰望
 * 鏡5 4.10 回收  經驗光點飛進玩家身體
 * </pre>
 */
public class GrindstoneFx extends AnimeFx {
	private static final int ARCANE = 0xB080FF;
	private static final int XP = 0xB8FF60;
	private static final int STONE = 0xE0D8C8;

	private final int enchants;

	public GrindstoneFx(FxPayload p) {
		super(p);
		enchants = Math.max(1, p.power());
		name = "grindstone";
		main = ARCANE;
		pale = 0xF0E8FF;
		impactMode = PostState.SPECTRAL;
		s3 = 2.3;
		impact = 2.7;
		end = 5.8;
		look("grindstone", 4.3);

		if (mine) {
			Vec3 item = frame.at(0, 1.5, 0);
			Cinematic.play(List.of(
				Shot.of(1.2, frame.at(0.8, 1.2, 1.4), frame.at(0.55, 1.3, 1.1), item).roll(5, 8).fov(0.88f, 0.82f),
				Shot.of(s3 - 1.2, frame.at(-2.0, 1.4, 0.3), frame.at(-1.8, 1.5, 0.9), frame.at(0, 1.2, 0)).roll(-3, -5),
				Shot.of(impact - s3, frame.at(0.1, 1.55, 0.7), frame.at(0.06, 1.52, 0.52), item).roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.1 - impact, frame.at(1.3, 0.35, 1.8), frame.at(1.6, 0.42, 2.3), frame.at(0, 2.0, 0)).look(frame.at(0, 2.2, 0), item).roll(12, 9)
					.fov(1.12f, 1.06f),
				Shot.of(end - 4.1 - 0.1, frame.at(-1.0, 1.6, 2.6), frame.at(-0.8, 1.5, 3.2), frame.at(0, 1.3, 1.2)).roll(-3, -1)
			));
		}
	}

	private Vec3 itemPos(double t) {
		float rise = easeOut(span(t, 0.1, 0.8));
		float lower = smooth(span(t, 1.2, 1.6)) * (1 - span(t, impact, impact + 0.3));
		return new Vec3(c.x, pos.getY() + 1.05 + rise * 0.5 - lower * 0.25, c.z);
	}

	private Vec3 playerChest() {
		var level = Minecraft.getInstance().level;
		Entity e = level == null ? null : level.getEntity(p.actor());
		return e == null ? frame.at(0, 1.2, 3) : e.position().add(0, 1.1, 0);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 item = itemPos(t);

		if (cue(0.0)) {
			Sfx.play(mine, "enchant_charge", c, 0.6f, 1.2f);
		}

		if (cue(1.0)) {
			Sfx.play(mine, "grind_spin", c, 1f, 1f);
		}

		// 研磨：火花朝鏡頭側噴出、符文被扯下撕碎
		if (t > 1.2 && t < impact) {
			Vec3 contact = new Vec3(c.x, pos.getY() + 1.0, c.z);
			Vec3 tangent = frame.front().add(0, 0.6, 0);
			int n = rate(140, dt);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.SPARK, contact, tangent.scale(Particles.rand(2f, 6f)).add(Particles.randomDir(1)), Particles.rand(0.3f, 0.7f)).size(0.04f, 0.01f)
					.color(0xFFF2C0, 0xFF9030).physics(9f, 0.5f);
			}

			int g = rate(14 + enchants * 6, dt);

			for (int i = 0; i < g; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = item.add(Math.cos(a) * 0.6, Particles.rand(-0.2f, 0.3f), Math.sin(a) * 0.6);
				Particles.spawn(Kind.GLYPH, at, new Vec3(Math.cos(a), 0.4, Math.sin(a)).scale(Particles.rand(1.5f, 3f)), 0.7).size(0.12f, 0.02f).color(0xFFFFFF, ARCANE)
					.physics(2f, 1.5f).spin(6f);
			}
		}

		if (cue(impact - 0.4)) {
			Sfx.play(mine, "grind_strip", c, 1.3f, 1f);
		}

		if (cue(impact)) {
			for (int i = 0; i < 30 + enchants * 15; i++) {
				Particles.spawn(Kind.SHARD, item, Particles.randomDir(Particles.rand(3f, 8f)), Particles.rand(0.6f, 1.2f)).size(0.18f, 0.04f).color(0xFFFFFF, ARCANE)
					.physics(3f, 2f).spin(10f);
			}

			// 經驗珠噴泉
			for (int i = 0; i < 40 + enchants * 15; i++) {
				Vec3 v = Particles.randomDir(Particles.rand(1f, 3f)).add(0, Particles.rand(3f, 6f), 0);
				Particles.spawn(Kind.FLARE, item, v, Particles.rand(1.0f, 1.6f)).size(0.14f, 0.05f).color(0xFFFFFF, XP).physics(6f, 0.6f).spin(2f);
			}
		}

		if (t > 4.1) {
			Vec3 chest = playerChest();
			int n = rate(30, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = item.add(Particles.randomDir(1.0));
				Particles.spawn(Kind.FLARE, from, Vec3.ZERO, 1.3).size(0.12f, 0.03f).color(0xFFFFFF, XP).physics(0, 0.2f).attract(chest, 9f);
			}
		}

		beats(t, dt, item);
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		Vec3 item = itemPos(t);
		float life = 1 - span(t, end - 0.4, end);
		float aura = (1 - span(t, impact, impact + 0.3)) * life;
		float burst = t > impact ? 1 - span(t, impact, impact + 1.2) : 0;
		lights.add(new CinemaPost.Light(item, 2.5f, r(ARCANE), g(ARCANE), b(ARCANE), 0.5f * aura, true));
		lights.add(new CinemaPost.Light(item, 3f, r(XP), g(XP), b(XP), 0.6f * burst * life, true));

		if (t > 1.2 && t < impact) {
			lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 1, c.z), 2.5f, 1f, 0.6f, 0.2f, 0.5f, false));
		}
	}

	@Override
	protected void items(Draw d, double t) {
		if (!p.item().isEmpty()) {
			d.item(p.item(), itemPos(t), 0.85f, (float)(t * (t > 1.2 && t < impact ? 6 : 1.5)), 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.2, end - 0.5, end);
		Vec3 item = itemPos(t);
		Vec3 wheelAt = new Vec3(c.x, pos.getY() + 0.65, c.z);
		float spin = (float)(t < 1.0 ? t * 2 : 2 + Math.pow(t - 1.0, 2) * 9);

		// 砂輪：光之齒輪（面向鏡頭的直立圓盤）
		float open = easeOutBack(span(t, 0, 0.4));
		d.disc(Mode.GLOW, JAG, wheelAt, frame.right().toVector3f(), Draw.Y, 0.75f * open, -spin, r(STONE), g(STONE), b(STONE), 0.6f * life);

		// 附魔外殼：環繞物品的符文帶（衝擊時碎掉）
		float aura = Math.min(span(t, 0.2, 0.6), 1 - span(t, impact, impact + 0.05)) * life;

		if (aura > 0.01f) {
			float flick = t > 1.2 ? 0.6f + 0.4f * (float)Math.sin(t * 40) : 1;
			d.cylinder(Mode.GLOW, BAND, item.add(0, -0.35, 0), 0.6f, 0.7f, 32, 3, (float)(t * 0.5), 1, 0, 1f, r(ARCANE), g(ARCANE), b(ARCANE), 0.35f * aura * flick,
				0.35f * aura * flick);
			d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.0f, 0, r(ARCANE), g(ARCANE), b(ARCANE), 0.4f * aura);
		}

		// 研磨接觸點的白熱光
		if (t > 1.2 && t < impact) {
			d.billboard(Mode.GLOW, FLARE, c.x, pos.getY() + 1.0, c.z, 0.7f, (float)t * 8, 1f, 0.8f, 0.5f, 0.9f * life);
		}

		shockRings(d, t, new Vec3(c.x, pos.getY() + 0.05, c.z), item, 4.5f);
	}
}
