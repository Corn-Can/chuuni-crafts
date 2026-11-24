package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.phys.Vec3;

/**
 * 投擲物：雪球（冰華連彈）、雞蛋（創生之卵）、藥水（祕藥飛散）、經驗瓶（智慧結晶）、箭（流星一矢）、三叉戟（海神之槍）、
 * 風彈（風神裂破）、煙火（星火繚亂）、釣竿（天釣一線）。
 * 每一種都是「出手 → 拖著光帶飛 → 命中那一刻炸開」，顏色、粒子與命中的招式各不相同。
 */
public class ThrowFx extends FlightFx {
	private final FxPayload.Kind kind;
	private final Vec3 from;
	private int col;
	private int col2;
	private float width = 0.16f;
	/** 滿弓的箭 / 附魔三叉戟：更粗更亮，命中時出招式名 */
	private boolean strong;
	private boolean lingering;
	private Vec3 lastPos;
	private double lastMove;

	public ThrowFx(FxPayload p) {
		super(p);
		kind = p.kind();
		from = p.at();
		lastPos = from;
		trailLife = 0.35;
		after = 1.0;

		switch (kind) {
			case SNOWBALL -> {
				col = 0xA0E8FF;
				col2 = 0xFFFFFF;
			}
			case EGG -> {
				col = 0xFFE070;
				col2 = 0xFFF8E0;
			}
			case POTION -> {
				col = 0x9060FF;
				col2 = 0xFFFFFF;
				width = 0.2f;
				after = 1.6;
			}
			case XP_BOTTLE -> {
				col = 0x90FF60;
				col2 = 0xFFE060;
			}
			case ARROW -> {
				col = 0xFFF0C0;
				col2 = 0xFFFFFF;
				width = 0.1f;
				trailLife = 0.25;
				after = 0.7;
			}
			case TRIDENT -> {
				col = 0x40E0FF;
				col2 = 0xC0FFFF;
				width = 0.24f;
			}
			case WIND_CHARGE -> {
				col = 0xD8F4FF;
				col2 = 0x90C8FF;
				width = 0.22f;
			}
			case FIREWORK -> {
				col = 0xFF60C0;
				col2 = 0xFFE060;
				width = 0.14f;
				after = 1.4;
			}
			default -> {
				// 釣竿
				col = 0x60F0E0;
				col2 = 0xFFFFFF;
				width = 0.07f;
				trailLife = 0.5;
			}
		}
	}

	/** 跟著實體更新的資訊（藥水顏色、箭是否滿弓……實體資料可能比實體本身晚一點到） */
	private void sync(Entity e) {
		if (e instanceof AbstractThrownPotion potion) {
			var contents = potion.getItem().get(DataComponents.POTION_CONTENTS);

			if (contents != null) {
				col = brighten(contents.getColorOr(col) & 0xFFFFFF);
			}

			lingering = potion instanceof ThrownLingeringPotion;
		}
		else if (e instanceof AbstractArrow arrow && !(e instanceof ThrownTrident)) {
			if (arrow.isCritArrow() && !strong) {
				strong = true;
				width = 0.16f;
				col = 0xFFD040;
			}
		}
		else if (e instanceof ThrownTrident trident) {
			if (trident.isFoil()) {
				strong = true;
				col2 = 0xD0A0FF;
			}
		}
		else if (e instanceof FireworkRocketEntity rocket) {
			var fw = rocket.getItem().get(DataComponents.FIREWORKS);

			if (fw != null && !fw.explosions().isEmpty() && !fw.explosions().getFirst().colors().isEmpty()) {
				col = fw.explosions().getFirst().colors().getInt(0) & 0xFFFFFF;
				var fade = fw.explosions().getFirst().fadeColors();
				col2 = fade.isEmpty() ? 0xFFFFFF : fade.getInt(0) & 0xFFFFFF;
			}
		}
	}

	@Override
	protected boolean stopped(double t, Vec3 now) {
		if (now.distanceToSqr(lastPos) > 0.0004) {
			lastMove = t;
		}

		lastPos = now;
		Entity e = target();

		// 釣竿的浮標：碰到水或地面就算落下
		if (kind == FxPayload.Kind.FISHING) {
			return t > 0.15 && e != null && (e.isInWater() || e.onGround());
		}

		// 箭和三叉戟插住了（一段時間沒動）
		return (kind == FxPayload.Kind.ARROW || kind == FxPayload.Kind.TRIDENT) && t > 0.15 && t - lastMove > 0.12;
	}

	@Override
	protected void fly(double t, double dt, Vec3 at) {
		Entity e = target();

		if (e != null) {
			sync(e);
		}

		if (cue(0.0)) {
			switch (kind) {
				case ARROW -> Sfx.play(mine, "whoosh", from, 0.35f, 1.3f);
				case TRIDENT -> Sfx.play(mine, "smoke_swirl", from, 0.7f, 1.1f);
				case WIND_CHARGE -> Sfx.play(mine, "whoosh", from, 0.5f, 0.9f);
				case FIREWORK -> Sfx.play(mine, "whoosh", from, 0.4f, 1.4f);
				case FISHING -> Sfx.play(mine, "compass_spin", from, 0.35f, 1.2f);
				default -> Sfx.play(mine, "whoosh", from, 0.45f, 1.15f);
			}

			if (!mine) {
				sparks(from, 8, 2f, col, 0.3f);
			}
		}

		// 滿弓的箭 / 附魔三叉戟：出手時一閃
		if (strong && cue(0.05)) {
			Sfx.anime(mine, "eye_shine2", "sparkle", at, 0.45f, 1.1f);
		}

		int n = rate(strong ? 70 : 40, dt);

		for (int i = 0; i < n; i++) {
			Vec3 o = at.add(Particles.randomDir(0.1));

			switch (kind) {
				case SNOWBALL -> Particles.spawn(Kind.FLARE, o, Particles.randomDir(0.3).add(0, -0.3, 0), Particles.rand(0.4f, 0.8f)).size(0.05f, 0.01f)
					.color(0xFFFFFF, col).physics(1f, 1.5f).spin(3f);
				case POTION -> Particles.spawn(Kind.GLOW, o, Particles.randomDir(0.3), Particles.rand(0.4f, 0.7f)).size(0.08f, 0.01f).color(0xFFFFFF, col)
					.physics(-0.4f, 2f);
				case XP_BOTTLE -> Particles.spawn(i % 2 == 0 ? Kind.GLYPH : Kind.FLARE, o, Particles.randomDir(0.3), Particles.rand(0.4f, 0.7f)).size(0.06f, 0.01f)
					.color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(-0.5f, 2f).spin(3f);
				case WIND_CHARGE -> {
					double a = t * 25 + i;
					Vec3 sp = new Vec3(Math.cos(a) * 0.25, Math.sin(a * 0.7) * 0.25, Math.sin(a) * 0.25);
					Particles.spawn(Kind.SMOKE, at.add(sp), sp.scale(2), 0.35).size(0.15f, 0.35f).color(0xFFFFFF, col2).alpha(0.35f).physics(0, 2f);
				}
				case FIREWORK -> Particles.spawn(Kind.SPARK, o, Particles.randomDir(0.8).add(0, -1.5, 0), Particles.rand(0.3f, 0.6f)).size(0.04f, 0.005f)
					.color(0xFFFFFF, i % 2 == 0 ? col : col2).physics(4f, 1f);
				case FISHING -> {
					if (i % 3 == 0) {
						Particles.spawn(Kind.FLARE, o, Vec3.ZERO, 0.4).size(0.04f, 0.005f).color(0xFFFFFF, col).physics(0, 0);
					}
				}
				default -> Particles.spawn(Kind.FLARE, o, Particles.randomDir(0.4), Particles.rand(0.3f, 0.6f)).size(strong ? 0.06f : 0.04f, 0.005f)
					.color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(0, 2f).spin(4f);
			}
		}
	}

	@Override
	protected void land(Vec3 at) {
		Entity e = target();

		if (e != null) {
			sync(e);
		}

		switch (kind) {
			case SNOWBALL -> {
				Sfx.anime(mine, "magic_ice3", null, at, 0.5f, 1.2f);

				for (int i = 0; i < 18; i++) {
					Particles.spawn(Kind.SHARD, at, Particles.randomDir(Particles.rand(1.5f, 3.5f)), Particles.rand(0.4f, 0.8f)).size(0.08f, 0.02f)
						.color(0xFFFFFF, col).physics(6f, 1f).spin(8f);
				}

				smallName("snowball");
			}
			case EGG -> {
				Sfx.anime(mine, "pa1", null, at, 0.5f, 1f);
				sparks(at, 24, 4f, col, 0.5f);
				smallName("egg");
			}
			case POTION -> {
				Sfx.anime(mine, "glass_water1", null, at, 0.6f, 1.1f);
				Sfx.anime(mine, "magic_cure2", null, at, 0.5f, 1f);
				pop(col, 0.3f);
				tint(col, 0.8);

				for (int i = 0; i < 50; i++) {
					Vec3 v = Particles.randomDir(Particles.rand(1.5f, 4f));
					Particles.spawn(i % 3 == 0 ? Kind.GLYPH : Kind.GLOW, at, new Vec3(v.x, Math.abs(v.y) * 0.8, v.z), Particles.rand(0.6f, 1.2f)).size(0.1f, 0.02f)
						.color(0xFFFFFF, col).physics(3f, 2f).spin(2f);
				}

				smallName("potion");
			}
			case XP_BOTTLE -> {
				Sfx.anime(mine, "shine3", "sparkle", at, 0.5f, 1.1f);

				for (int i = 0; i < 30; i++) {
					Particles.spawn(i % 2 == 0 ? Kind.GLYPH : Kind.FLARE, at, Particles.randomDir(Particles.rand(0.5f, 2f)).add(0, 1.5, 0), Particles.rand(0.8f, 1.4f))
						.size(0.08f, 0.01f).color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(-0.8f, 1.5f).spin(3f);
				}

				smallName("xp_bottle");
			}
			case ARROW -> {
				Sfx.play(mine, "comb_cut", at, strong ? 0.6f : 0.35f, 1.2f);
				sparks(at, strong ? 26 : 10, strong ? 5f : 3f, col, 0.35f);
				Particles.spawn(Kind.HIT, at, Vec3.ZERO, 0.22).size(strong ? 0.7f : 0.4f, strong ? 1.1f : 0.6f).color(0xFFFFFF, col);

				if (strong) {
					pop(col, 0.35f);
					smallName("arrow");
				}
			}
			case TRIDENT -> {
				Sfx.play(mine, "forge_clang", at, 0.6f, 1.2f);
				Sfx.play(mine, "bucket_catch", at, 0.5f, 1f);
				pop(col, 0.5f);

				for (int i = 0; i < 40; i++) {
					Vec3 v = Particles.randomDir(Particles.rand(2f, 5f));
					Particles.spawn(Kind.GLOW, at, new Vec3(v.x, Math.abs(v.y) + 1, v.z), Particles.rand(0.5f, 1f)).size(0.08f, 0.02f)
						.color(0xFFFFFF, i % 3 == 0 ? col2 : col).physics(7f, 1f);
				}

				smallName("trident");
			}
			case WIND_CHARGE -> {
				Sfx.play(mine, "smoke_swirl", at, 0.7f, 1.2f);
				pop(col2, 0.3f);

				for (int i = 0; i < 30; i++) {
					Vec3 v = Particles.randomDir(Particles.rand(3f, 6f));
					Particles.spawn(Kind.SMOKE, at, v, Particles.rand(0.4f, 0.7f)).size(0.3f, 0.9f).color(0xFFFFFF, col2).alpha(0.4f).physics(0, 4f).spin(2f);
				}

				smallName("wind_charge");
			}
			case FIREWORK -> {
				Sfx.play(mine, "bloom_burst", at, 0.6f, 1.1f);

				for (int i = 0; i < 60; i++) {
					Particles.spawn(Kind.FLARE, at, Particles.randomDir(Particles.rand(4f, 9f)), Particles.rand(0.8f, 1.4f)).size(0.08f, 0.01f)
						.color(0xFFFFFF, i % 2 == 0 ? col : col2).physics(2.5f, 1.5f).spin(4f);
				}

				smallName("firework");
			}
			default -> {
				// 釣竿：浮標落水
				Sfx.play(mine, "bucket_catch", at, 0.45f, 1.3f);

				for (int i = 0; i < 16; i++) {
					Vec3 v = Particles.randomDir(Particles.rand(1f, 2f));
					Particles.spawn(Kind.GLOW, at, new Vec3(v.x, Math.abs(v.y) * 2 + 1, v.z), Particles.rand(0.4f, 0.7f)).size(0.06f, 0.01f).color(0xFFFFFF, col)
						.physics(8f, 0.5f);
				}

				smallName("fishing");
			}
		}
	}

	@Override
	protected void after(double t, double dt) {
		// 滯留藥水：地上留一圈慢慢旋轉的法陣，飄著同色的光點
		if (landT >= 0 && kind == FxPayload.Kind.POTION && lingering && t - landT < after - 0.3) {
			int n = rate(20, dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				double r = Particles.RNG.nextDouble() * 2.5;
				Vec3 o = head.add(Math.cos(a) * r, -0.3, Math.sin(a) * r);
				Particles.spawn(Kind.GLOW, o, new Vec3(0, Particles.rand(0.3f, 0.8f), 0), Particles.rand(0.8f, 1.4f)).size(0.07f, 0.01f).color(0xFFFFFF, col)
					.physics(-0.2f, 1f);
			}
		}

		// 煙火：第二層花瓣
		if (landT >= 0 && kind == FxPayload.Kind.FIREWORK && cue(landT + 0.25)) {
			for (int i = 0; i < 40; i++) {
				Particles.spawn(Kind.GLYPH, head, Particles.randomDir(Particles.rand(2f, 4f)), Particles.rand(0.8f, 1.2f)).size(0.08f, 0.02f)
					.color(0xFFFFFF, i % 2 == 0 ? col2 : col).physics(1.5f, 1.5f).spin(3f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		// 自己丟的：剛出手時光源就在鏡頭上，整個畫面會被染色 → 晚一點亮
		headLight(lights, col, (strong ? 0.4f : 0.2f) * (mine ? span(time(), 0.15, 0.4) : 1));

		if (landT >= 0) {
			float k = 1 - span(time(), landT, landT + Math.min(after, 0.8));
			float big = kind == FxPayload.Kind.FIREWORK || kind == FxPayload.Kind.POTION || kind == FxPayload.Kind.TRIDENT ? 5f : 3f;

			if (k > 0.01f) {
				lights.add(new CinemaPost.Light(head, big, r(col), g(col), b(col), 0.6f * k, true));
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		// 自己丟的：剛出手時光帶就貼在鏡頭前，先淡淡的
		float near0 = mine ? span(t, 0.1, 0.3) : 1;
		ribbon(d, t, width, col, (kind == FxPayload.Kind.FISHING ? 0.5f : 0.85f) * near0);

		if (flying()) {
			// 剛出手時離鏡頭太近，先淡淡的
			float closeIn = mine ? span(t, 0.06, 0.2) : 1;
			glow(d, head, width * 3.5f, col, 0.7f * closeIn);

			if (strong || kind == FxPayload.Kind.FIREWORK) {
				d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, width * 3f, (float)(t * 6), 1f, 1f, 1f, 0.7f * closeIn);
			}

			// 風彈：繞著自己轉的風環
			if (kind == FxPayload.Kind.WIND_CHARGE) {
				d.billboard(Mode.GLOW, JAG, head.x, head.y, head.z, 0.6f, (float)(t * 12), r(col2), g(col2), b(col2), 0.5f * closeIn);
			}

			return;
		}

		double lt = t - landT;
		float k = span(lt, 0, kind == FxPayload.Kind.ARROW ? 0.35 : 0.6);

		if (k < 1) {
			float fade = (1 - k) * (1 - k);

			switch (kind) {
				case POTION -> {
					// 飛濺的法陣（滯留藥水更大、留更久）
					float rad = lingering ? 3f : 2f;
					Vec3 g0 = new Vec3(head.x, Math.floor(head.y) + 0.06, head.z);
					d.flat(Mode.GLOW, RING, g0, 0.3f + easeOut(k) * rad * 1.4f, 0, r(col), g(col), b(col), fade);
					d.billboard(Mode.GLOW, JAG, head.x, head.y, head.z, 0.4f + easeOut(k) * 2f, (float)lt, r(col), g(col), b(col), fade * 0.7f);
				}
				case WIND_CHARGE -> {
					for (int i = 0; i < 3; i++) {
						float kk = span(lt, i * 0.06, 0.5 + i * 0.06);

						if (kk > 0 && kk < 1) {
							d.billboard(Mode.GLOW, RING, head.x, head.y, head.z, 0.3f + easeOut(kk) * (2.5f + i), 0, r(col2), g(col2), b(col2), (1 - kk) * (1 - kk) * 0.8f);
						}
					}
				}
				case FIREWORK -> {
					d.billboard(Mode.GLOW, RING, head.x, head.y, head.z, 0.5f + easeOut(k) * 5f, 0, r(col), g(col), b(col), fade);
					d.billboard(Mode.GLOW, JAG, head.x, head.y, head.z, 0.4f + easeOut(k) * 3.5f, (float)lt * 2, r(col2), g(col2), b(col2), fade * 0.8f);
					d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, 3f * (1 - k), (float)lt, 1f, 1f, 1f, 1 - k);
				}
				case ARROW -> d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, (strong ? 1.4f : 0.7f) * (1 - k), (float)lt, r(col), g(col), b(col), 1 - k);
				default -> {
					d.billboard(Mode.GLOW, RING, head.x, head.y, head.z, 0.3f + easeOut(k) * 1.6f, 0, r(col), g(col), b(col), fade);

					if (kind == FxPayload.Kind.TRIDENT || kind == FxPayload.Kind.FISHING) {
						Vec3 g0 = new Vec3(head.x, kind == FxPayload.Kind.FISHING ? head.y - 0.15 : Math.floor(head.y) + 0.06, head.z);
						d.flat(Mode.GLOW, RING, g0, 0.3f + easeOut(k) * (kind == FxPayload.Kind.TRIDENT ? 3f : 1.2f), 0, r(col), g(col), b(col), fade);
					}
				}
			}
		}

		// 滯留藥水的地面法陣
		if (kind == FxPayload.Kind.POTION && lingering) {
			float a = fade(lt, 0.3, after - 0.5, after) * 0.5f;
			Vec3 g0 = new Vec3(head.x, Math.floor(head.y) + 0.05, head.z);
			d.flat(Mode.GLOW, SEAL, g0, 2.6f, (float)(lt * 0.6), r(col), g(col), b(col), a);
		}

		glow(d, head, width * 6f, col, 0.6f * (1 - span(lt, 0, 0.5)));
	}

	/** 讓色環稍微偏亮（藥水顏色有時很暗） */
	private static int brighten(int rgb) {
		return (Math.min(255, (rgb >> 16 & 255) + 40) << 16) | (Math.min(255, (rgb >> 8 & 255) + 40) << 8) | Math.min(255, (rgb & 255) + 40);
	}
}
