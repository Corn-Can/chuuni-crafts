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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 用南瓜頭完成魔像。variant：0 鐵巨人（A 級・鐵壁守護）/ 1 雪人（B 級・霜雪之靈）/ 2 銅魔像（B 級・銅偶誕生）。
 * <pre>
 * 鐵巨人（A 級）：
 * 鏡1 0.00 甦醒  頭部極近：鐵塊的接縫一條條透出熔鐵般的橘光，雙眼還是暗的
 * 鏡2 1.20 蓄力  低角度全身：肩膀噴出蒸氣，胸口的核心越來越亮
 * 鏡3 2.40 吸氣  胸口極近特寫
 * 衝擊 2.80     雙眼點燃 + 一記踏地：地面裂開的衝擊波、碎石、漫畫分格時停 + 四角大字
 * 鏡4 2.80 英雄  腳邊仰望
 * 鏡5 4.30 守護  拉遠：巨人站定，接縫的光慢慢冷卻
 * </pre>
 * 雪人：冷風捲起雪花繞著身體旋轉 → 南瓜頭「噗」地點亮；銅魔像：銅屑火花 + 雙眼一閃（都不切鏡頭）。
 */
public class GolemFx extends MinorFx {
	private static final int MOLTEN = 0xFF8A30;
	private static final int EYE = 0xFF3020;
	private static final int FROST = 0xC8F0FF;
	private static final int PUMPKIN = 0xFFA030;
	private static final int COPPER = 0xE08050;

	private final int variant;
	private final Vec3 feet;

	public GolemFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 3);
		feet = new Vec3(p.at().x, Math.floor(p.at().y - 0.5) + 0.02, p.at().z);

		if (variant != 0) {
			end = 2.4;
			return;
		}

		name = "iron_golem";
		main = MOLTEN;
		pale = 0xFFF0DC;
		impactMode = PostState.EMBER;
		manga = true;
		s3 = 2.4;
		impact = 2.8;
		end = 6.0;
		fadeColor = -1;
		look("golem", 4.6);

		if (mine) {
			Vec3 base = feet;
			Cinematic.play(List.of(
				Shot.of(1.2, base.add(frame.right().scale(0.6)).add(frame.front().scale(2.8)).add(0, 2.6, 0), base.add(frame.right().scale(0.4)).add(frame.front().scale(2.4)).add(0, 2.5, 0),
					base.add(0, 2.3, 0)).roll(5, 8).fov(0.85f, 0.78f),
				Shot.of(s3 - 1.2, base.add(frame.right().scale(-2.4)).add(frame.front().scale(3.6)).add(0, 0.4, 0),
					base.add(frame.right().scale(-2.0)).add(frame.front().scale(3.1)).add(0, 0.5, 0), base.add(0, 1.6, 0)).roll(-3, -5),
				Shot.of(impact - s3, base.add(frame.front().scale(2.8)).add(0, 1.7, 0), base.add(frame.front().scale(2.3)).add(0, 1.7, 0), base.add(0, 1.7, 0))
					.roll(-9, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(4.3 - impact, base.add(frame.right().scale(1.4)).add(frame.front().scale(2.4)).add(0, 0.25, 0),
					base.add(frame.right().scale(1.8)).add(frame.front().scale(3.0)).add(0, 0.3, 0), base.add(0, 2.4, 0)).look(base.add(0, 2.8, 0), base.add(0, 2.2, 0))
					.roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(end - 4.3, base.add(frame.right().scale(-1.8)).add(frame.front().scale(4.2)).add(0, 2.2, 0),
					base.add(frame.right().scale(-2.3)).add(frame.front().scale(5.2)).add(0, 2.8, 0), base.add(0, 1.4, 0)).roll(-3, -1)
			));
		}
	}

	@Override
	public double duration() {
		return end;
	}

	/** 魔像正面（依身體朝向）上的一點 */
	private Vec3 front(double side, double y, double out) {
		Entity e = target();
		double yaw = Math.toRadians(e == null ? 0 : e.getYRot());
		Vec3 f = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 r = new Vec3(-f.z, 0, f.x);
		Vec3 base = e == null ? feet : e.position();
		return base.add(f.scale(out)).add(r.scale(side)).add(0, y, 0);
	}

	@Override
	protected void update(double t, double dt) {
		switch (variant) {
			case 0 -> iron(t, dt);
			case 1 -> snow(t, dt);
			default -> copper(t, dt);
		}
	}

	private void iron(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "golem_awaken", c, 1.1f, 1f);
		}

		if (cue(impact - 0.4)) {
			Sfx.play(mine, "golem_stomp", c, 1.3f, 1f);
		}

		// 肩膀噴蒸氣
		if (t > 1.0 && t < impact) {
			int n = rate(40, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = front(Particles.RNG.nextBoolean() ? -0.7 : 0.7, 2.1, -0.1);
				Particles.spawn(Kind.SMOKE, at, Particles.randomDir(0.3).add(0, 1.2, 0), 1.0).size(0.15f, 0.6f).color(0xFFFFFF, 0xA0A0A0).alpha(0.5f).physics(-0.3f, 1f);
			}
		}

		if (cue(impact)) {
			for (int i = 0; i < 50; i++) {
				Particles.spawn(Kind.SHARD, feet, Particles.randomDir(Particles.rand(2f, 5f)).add(0, 3, 0), Particles.rand(0.8f, 1.4f)).size(0.08f, 0.05f)
					.color(0xB0A8A0, 0x605850).physics(10f, 0.5f).spin(10f);
			}

			for (int i = 0; i < 24; i++) {
				double a = i * Math.PI / 12;
				Particles.spawn(Kind.SMOKE, feet.add(0, 0.2, 0), new Vec3(Math.cos(a) * 3, 0.3, Math.sin(a) * 3), 1.2).size(0.3f, 1.0f).color(0xD0C8C0, 0x706860)
					.alpha(0.5f).physics(0, 2.5f);
			}
		}

		beats(t, dt, front(0, 1.7, 0.4));
	}

	private void snow(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "snow_spirit", c, 0.9f, 1f);
			tint(FROST, 1.6);
			smallName("snow_golem");
		}

		if (t < 1.2) {
			int n = rate(90, dt);
			Vec3 b = body();

			for (int i = 0; i < n; i++) {
				double a = t * 8 + Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = b.add(Math.cos(a) * 1.0, Particles.rand(-0.9f, 0.9f), Math.sin(a) * 1.0);
				Particles.spawn(Kind.FLARE, at, new Vec3(-Math.sin(a) * 2, 0.4, Math.cos(a) * 2), 0.6).size(0.06f, 0.02f).color(0xFFFFFF, FROST).physics(0, 1f)
					.attract(b, 3f).spin(4f);
			}
		}

		if (cue(0.9)) {
			pop(PUMPKIN, 0.4f);
			sparks(front(0, 1.6, 0.3), 16, 2.5f, PUMPKIN, 0.4f);
		}
	}

	private void copper(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "copper_birth", c, 0.9f, 1f);
			tint(COPPER, 1.2);
			smallName("copper_birth");
		}

		if (cue(0.5)) {
			pop(COPPER, 0.35f);
			sparks(body(), 24, 3f, COPPER, 0.45f);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();

		if (variant == 0) {
			float core = span(t, 0.8, impact) * (1 - span(t, impact + 0.6, end));
			lights.add(new CinemaPost.Light(front(0, 1.7, 0.4), 3.5f, r(MOLTEN), g(MOLTEN), b(MOLTEN), 0.2f + 0.5f * core, true));
			return;
		}

		int col = variant == 1 ? PUMPKIN : COPPER;
		lights.add(new CinemaPost.Light(body(), 3f, r(col), g(col), b(col), 0.4f * (1 - span(t, 1.2, end)), true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.1, end - 0.5, end);

		if (variant == 1) {
			float on = span(t, 0.9, 1.0) * life;
			glow(d, front(0, 1.6, 0.3), 0.7f, PUMPKIN, on * 0.7f);
			groundRing(d, t, 0.9, 1.6, feet, 0.3f, 2.2f, FROST, 0.8f, JAG);
			return;
		}

		if (variant == 2) {
			float eye = span(t, 0.5, 0.55) * (1 - span(t, 0.7, 1.6));

			for (int s = -1; s <= 1; s += 2) {
				Vec3 e = front(s * 0.09, 0.62, 0.25);
				d.billboard(Mode.GLOW, FLARE, e.x, e.y, e.z, 0.3f * eye, (float)t * 3, 1f, 0.9f, 0.6f, eye);
			}

			groundRing(d, t, 0.5, 1.1, feet, 0.2f, 1.6f, COPPER, 0.8f, JAG);
			return;
		}

		// 鐵巨人：接縫透出熔鐵光（身體四條縱向接縫 + 肩 / 腰兩道橫縫）
		float seam = span(t, 0.1, 1.2) * (1 - span(t, impact + 0.8, end)) * life;
		float flick = 0.8f + 0.2f * (float)Math.sin(t * 19);

		if (seam > 0.01f) {
			for (int s = -1; s <= 1; s += 2) {
				line(d, SPARK, front(s * 0.45, 0.9, 0.36), front(s * 0.45, 2.2, 0.36), 0.05f, MOLTEN, seam * flick);
				line(d, SPARK, front(s * 0.15, 1.0, 0.4), front(s * 0.15, 1.9, 0.4), 0.04f, MOLTEN, seam * 0.7f);
			}

			line(d, SPARK, front(-0.65, 2.15, 0.35), front(0.65, 2.15, 0.35), 0.05f, MOLTEN, seam * flick);
			line(d, SPARK, front(-0.4, 1.0, 0.36), front(0.4, 1.0, 0.36), 0.05f, MOLTEN, seam);
		}

		// 胸口核心
		float core = span(t, 0.8, impact) * (1 - span(t, impact + 1.0, end)) * life;
		Vec3 chest = front(0, 1.65, 0.42);
		glow(d, chest, 0.3f + core * 0.8f, MOLTEN, core * 0.8f);
		d.billboard(Mode.GLOW, FLARE, chest.x, chest.y, chest.z, 0.4f + core * 0.6f, (float)t, 1f, 0.9f, 0.7f, core * 0.6f);

		// 雙眼在衝擊時點燃
		float eye = span(t, impact, impact + 0.05) * life;

		for (int s = -1; s <= 1; s += 2) {
			Vec3 e = front(s * 0.13, 2.45, 0.38);
			glow(d, e, 0.25f, EYE, eye);
			d.billboard(Mode.GLOW, FLARE, e.x, e.y, e.z, 0.35f * eye, (float)t * 2, 1f, 0.6f, 0.5f, eye * 0.8f);
		}

		// 踏地的裂痕：從腳下往外輻射的光縫
		float crack = easeOut(span(t, impact, impact + 0.25)) * (1 - span(t, impact + 0.6, impact + 2.2)) * life;

		if (crack > 0.01f) {
			for (int i = 0; i < 9; i++) {
				double a = i * Math.PI * 2 / 9 + 0.3;
				double len = (1.4 + (i % 3) * 0.5) * crack;
				Vec3 a0 = feet.add(Math.cos(a) * 0.5, 0.01, Math.sin(a) * 0.5);
				Vec3 a1 = feet.add(Math.cos(a + 0.15) * (0.5 + len), 0.01, Math.sin(a + 0.15) * (0.5 + len));
				line(d, SPARK, a0, a1, 0.07f, MOLTEN, crack);
			}
		}

		shockRings(d, t, feet, front(0, 0.3, 0.4), 6f);
	}
}
