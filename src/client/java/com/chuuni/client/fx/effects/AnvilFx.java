package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Cinematic.Frame;
import com.chuuni.client.fx.Cinematic.Shot;
import com.chuuni.client.fx.Effect;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxMesh;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 千錘百鍊・魂鑄（鐵砧）——打架動漫分鏡。花費 15 級以上（或強度全開）是「重鍛」：第三擊後時間停止的漫畫分格 + 四角大字。
 * <pre>
 * 鏡1 0.00 點火  低角度近拍：魂鑄陣點燃，物品平躺在砧上被燒紅，靈魂之錘從火星中凝聚成形
 * 鏡2 1.10 鍛打  側面廣角：第一、二擊（頓幀 + RGB 錯開 + 火星噴泉），熱度一擊比一擊高
 * 鏡3 2.50 吸氣  仰拍高舉的錘頭、荷蘭角：畫面壓暗、黑色集中線
 * 衝擊 3.00     第三擊：衝擊幀 + 頓幀 + 折射衝擊波 →（重鍛：漫畫分格時停 + 四角大字）
 * 鏡4 3.00 英雄  超低角度仰望（慢動作）：白色集中線、橫向光芒、毛筆直書招式名
 * 鏡5 4.40 淬火  色調轉為冷鋼藍：蒸氣爆開，火紅退成冷光
 * 鏡6 5.30 寒光  刃身緩緩立起，一道冷光沿刃身掃過（改名時新名字燒進畫面）→ 直接回到世界
 * </pre>
 */
public class AnvilFx extends Effect {
	private static final Identifier CIRCLE = FxRender.tex("circle_forge");
	private static final Identifier BAND = FxRender.tex("rune_band");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");
	private static final Identifier BEAM = FxRender.tex("beam");

	private static final int ORANGE = 0xFF7A20;
	private static final int HOT = 0xFFD890;
	private static final int COLD = 0x8FD0FF;
	private static final int STEEL = 0xE4F2FF;

	private static final double S2 = 1.1;
	private static final double[] STRIKES = {1.45, 2.05, 3.0};
	private static final double S3 = 2.5;
	private static final double IMPACT = STRIKES[2];
	private static final double QUENCH = 4.4;
	private static final double S6 = 5.3;
	private static final double DURATION = 6.9;
	private static final int MANGA_MS = 1300;
	private static final float HANDLE = 1.69f;

	private final boolean heavy;
	private final Vec3 top;
	private final Vec3 pivot;
	private final float yaw;
	private final Vector3f side;
	private final Frame frame;
	private final boolean near;

	public AnvilFx(FxPayload p) {
		super(p);
		heavy = Settings.intensity >= 2 || p.power() >= 15;
		top = new Vec3(c.x, pos.getY() + 1.0, c.z);
		frame = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
		// 錘子放在「玩家視線的右側」，讓揮擊弧線完整入鏡
		side = new Vector3f((float)frame.right().x, 0, (float)frame.right().z);
		pivot = new Vec3(c.x + side.x * HANDLE, pos.getY() + 1.56, c.z + side.z * HANDLE);
		// 模型 +X 指向鐵砧（-side）
		yaw = (float)Math.atan2(side.z, -side.x);
		near = near();

		if (near) {
			PostState.look("anvil", 400, (int)(QUENCH * 1000) - 200);
		}

		if (mine) {
			Vec3 head = frame.at(1.15, 2.75, 0);
			Cinematic.play(List.of(
				Shot.of(S2, frame.at(-0.95, 0.75, 1.5), frame.at(-0.65, 0.85, 1.2), frame.at(0.25, 1.15, 0)).roll(4, 7).fov(0.88f, 0.82f),
				Shot.of(S3 - S2, frame.at(-0.35, 1.95, 2.5), frame.at(0.15, 1.85, 2.25), frame.at(0.75, 1.45, 0)).roll(-2, -3),
				Shot.of(IMPACT - S3, frame.at(-0.45, 1.15, 1.35), frame.at(-0.3, 1.1, 1.0), head).look(head, frame.at(1.0, 2.6, 0))
					.roll(-8, -13).fov(0.72f, 0.6f).accelerate(),
				Shot.of(QUENCH - IMPACT, frame.at(-0.95, 0.22, 1.7), frame.at(-1.25, 0.3, 2.1), frame.at(0.3, 1.7, 0))
					.look(frame.at(0.3, 1.7, 0), frame.at(0, 1.3, 0)).roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(S6 - QUENCH, frame.at(1.0, 1.65, 1.45), frame.at(0.75, 1.55, 1.15), frame.at(0, 1.1, 0)).roll(-4, -2),
				Shot.of(DURATION - S6, frame.at(0.25, 1.25, 2.5), frame.at(0.15, 1.6, 1.85), frame.at(0, 1.2, 0))
					.look(frame.at(0, 1.2, 0), frame.at(0, 1.95, 0)).roll(3, 0).fov(0.88f, 0.78f)
			), closeMenu(AnvilMenu.class));
		}
	}

	@Override
	public double duration() {
		return DURATION;
	}

	/** 錘柄角度（度）：90 = 敲在鐵砧上、負值 = 高舉往後 */
	private float swing(double t) {
		float angle = -25f;

		for (int i = 0; i < STRIKES.length; i++) {
			double s = STRIKES[i];
			double prev = i == 0 ? 0.5 : STRIKES[i - 1];

			if (t < prev) {
				break;
			}

			if (t >= s) {
				double k = t - s;
				angle = 90f - (float)(Math.max(0, Math.sin(Math.min(k, 0.25) / 0.25 * Math.PI)) * 9);
				continue;
			}

			boolean last = i == 2;
			float windup = easeOut(span(t, prev + 0.08, s - (last ? 0.12 : 0.1)));
			float slam = easeIn(span(t, s - (last ? 0.12 : 0.1), s));
			float raised = last ? -60f : -25f;
			float from = i == 0 ? -25f : 90f;
			angle = slam > 0 ? Mth.lerp(slam, raised, 90f) : Mth.lerp(windup, from, raised);
		}

		if (t > IMPACT + 0.35) {
			angle = Mth.lerp(easeOut(span(t, IMPACT + 0.35, IMPACT + 1.0)), 90f, 20f);
		}

		return angle;
	}

	/** 熱度 0~1（每敲一下升高，淬火後歸零） */
	private float heat(double t) {
		float h = 0.3f * span(t, 0.0, 0.8);

		for (int i = 0; i < STRIKES.length; i++) {
			if (t >= STRIKES[i]) {
				h = 0.5f + i * 0.25f;
			}
		}

		return h * (1 - span(t, QUENCH, QUENCH + 0.35));
	}

	/** 淬火後的冷光 0~1 */
	private float cold(double t) {
		return span(t, QUENCH, QUENCH + 0.3) * (1 - span(t, DURATION - 0.4, DURATION));
	}

	/** 物品：平躺在砧面上，被敲時彈起；寒光時緩緩立起 */
	private Vec3 itemPos(double t) {
		double bounce = 0;

		for (double s : STRIKES) {
			if (t >= s && t < s + 0.2) {
				bounce = Math.sin((t - s) / 0.2 * Math.PI) * 0.07;
			}
		}

		float rise = smooth(span(t, S6, S6 + 1.1));
		return top.add(0, 0.12 + bounce + rise * 0.82 + Math.sin(t * 2) * 0.02 * rise, 0);
	}

	private static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}

	private Vec3 hammerHead(double t) {
		Matrix4f m = new Matrix4f().rotateY(yaw).rotateZ((float)-Math.toRadians(swing(t)));
		Vector3f h = m.transformPosition(new Vector3f(0, 1.69f, 0));
		return pivot.add(h.x, h.y, h.z);
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 item = itemPos(t);

		// ---------------------------------------------------------- 鏡1：點火、錘子從火星中凝聚
		if (cue(0.0)) {
			Sfx.play(mine, "forge_ignite", c, 1f, heavy ? 0.92f : 1f);
		}

		if (t > 0.2 && t < 0.9) {
			Vec3 head = hammerHead(t);
			int n = rate(140, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = head.add(Particles.randomDir(1.4 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.GLOW, from, Vec3.ZERO, 0.4).size(0.06f, 0.02f).color(HOT, ORANGE).physics(0, 0).attract(head, 22f);
			}
		}

		// 鍛造中持續冒出的火星
		float h = heat(t);

		if (h > 0.1f) {
			int n = rate(40 * h, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = top.add(Particles.rand(-0.35f, 0.35f), 0.15, Particles.rand(-0.35f, 0.35f));
				Particles.spawn(Kind.GLOW, at, new Vec3(Particles.rand(-0.3f, 0.3f), Particles.rand(0.6f, 1.8f), Particles.rand(-0.3f, 0.3f)), 1.3)
					.size(0.05f, 0.01f).color(HOT, ORANGE).physics(-0.3f, 0.8f);
			}
		}

		// ---------------------------------------------------------- 鏡2：鍛打
		for (int i = 0; i < 2; i++) {
			if (cue(STRIKES[i] - 0.12)) {
				Sfx.play(mine, "whoosh", c, 0.6f, 0.75f + i * 0.1f);
			}

			if (cue(STRIKES[i])) {
				strike(i);
			}
		}

		// ---------------------------------------------------------- 鏡3：吸氣
		if (cue(S3)) {
			if (near) {
				PostState.darken(heavy ? 0.4f : 0.3f);
			}

			if (mine) {
				PostState.lines(0.8f, true, (int)((IMPACT - S3) * 1000) + 30, 0.32f);
			}

			Sfx.play(mine, "whoosh", c, 0.9f, 0.5f);
		}

		if (cue(IMPACT - 0.5)) {
			// forge_final 有 0.5 秒的吸入，重擊正好落在第三擊
			Sfx.play(mine, "forge_final", c, 1.3f, heavy ? 0.9f : 1f);
		}

		if (t > S3 && t < IMPACT) {
			// 錘頭吸入火光
			Vec3 head = hammerHead(t);
			int n = rate(120, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = head.add(Particles.randomDir(1.5 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.SPARK, from, head.subtract(from).scale(3), 0.3).size(0.035f, 0.01f).color(0xFFFFFF, ORANGE).physics(0, 0).attract(head, 30f);
			}
		}

		// ---------------------------------------------------------- 衝擊：第三擊
		if (cue(IMPACT)) {
			finalStrike(item);
		}

		if (heavy && cue(IMPACT + 0.07) && mine) {
			FxClock.hitstop(MANGA_MS);

			if (Settings.titlesOn()) {
				PostState.manga(MANGA_MS);
				PostState.lines(1f, true, MANGA_MS, 0.3f);
				Hud.mangaName("anvil", MANGA_MS / 1000.0);
			}
		}

		if (cue(IMPACT + 0.08)) {
			FxClock.slowmo(0.3f, 1200);

			if (mine) {
				PostState.lines(0.8f, false, 1100, 0.42f);
				PostState.streak(1.1f, 900);
				PostState.zoom(0.8f, 500);
			}
		}

		if (cue(IMPACT + 0.3) && mine && Settings.titlesOn()) {
			Hud.brushName("anvil", 3.0);
		}

		// 第三擊後的火星噴泉
		if (t > IMPACT && t < QUENCH - 0.2) {
			int n = rate(90 * (1 - span(t, IMPACT, QUENCH)), dt);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 v = new Vec3(Math.cos(a) * Particles.rand(1f, 4f), Particles.rand(3f, 7f), Math.sin(a) * Particles.rand(1f, 4f));
				Particles.spawn(Kind.SPARK, top.add(0, 0.1, 0), v, Particles.rand(0.6f, 1.2f)).size(0.05f, 0.02f).color(0xFFF6C0, 0xFF4A10).physics(9f, 0.4f);
			}
		}

		// ---------------------------------------------------------- 鏡5：淬火
		if (cue(QUENCH - 0.1)) {
			Sfx.play(mine, "forge_quench", c, 1.1f, heavy ? 0.9f : 1f);
		}

		if (cue(QUENCH)) {
			if (near) {
				PostState.look("quench", 450, (int)((DURATION - QUENCH + 0.4) * 1000));
				PostState.darken(0);
			}

			if (mine) {
				PostState.flash(COLD, 0.22f, 200, 0);
				PostState.split(0.6f, 1.6f, 300);
			}

			shake(0.3f);

			for (int i = 0; i < 70; i++) {
				Vec3 at = top.add(Particles.rand(-0.5f, 0.5f), 0.15, Particles.rand(-0.5f, 0.5f));
				Particles.spawn(Kind.SMOKE, at, new Vec3(Particles.rand(-1f, 1f), Particles.rand(1f, 3f), Particles.rand(-1f, 1f)), 2.2)
					.size(0.35f, 1.6f).color(0xB8C0C8, 0x707C88).physics(-0.5f, 1.4f).alpha(0.4f).spin(0.8f);
			}

			for (int i = 0; i < 10; i++) {
				Particles.spawn(Kind.FLARE, top.add(Particles.randomDir(0.6)), Particles.randomDir(1.0), 0.8).size(0.25f, 0f).color(0xFFFFFF, COLD);
			}
		}

		if (t > QUENCH && t < S6 + 0.5) {
			int n = rate(26 * (1 - span(t, QUENCH, S6 + 0.5)), dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = top.add(Particles.rand(-0.4f, 0.4f), 0.2, Particles.rand(-0.4f, 0.4f));
				Particles.spawn(Kind.SMOKE, at, new Vec3(Particles.rand(-0.2f, 0.2f), Particles.rand(0.6f, 1.4f), Particles.rand(-0.2f, 0.2f)), 2.0)
					.size(0.25f, 1.1f).color(0xB8C0C8, 0x7C8894).physics(-0.2f, 1f).alpha(0.3f).spin(0.5f);
			}
		}

		// ---------------------------------------------------------- 鏡6：寒光
		if (cue(S6)) {
			Sfx.play(mine, "blade_glint", c, 1f, heavy ? 0.94f : 1f);
		}

		if (cue(S6 + 0.6)) {
			// 寒光一閃
			if (mine) {
				PostState.streak(0.9f, 700);
				PostState.flash(STEEL, 0.25f, 160, 0);

				if (p.extra() == 1 && !p.item().isEmpty()) {
					Hud.engrave(p.item().getHoverName().getString());
				}
			}

			for (int i = 0; i < 30; i++) {
				Particles.spawn(Kind.SPARK, item, Particles.randomDir(Particles.rand(1.5f, 4f)), 0.6).size(0.03f, 0.005f).color(0xFFFFFF, COLD).physics(0, 2f);
			}
		}

		if (t > S6) {
			int n = rate(10, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = item.add(Particles.randomDir(0.8 + Particles.RNG.nextDouble() * 0.6));
				Particles.spawn(Kind.FLARE, at, new Vec3(0, -0.1, 0), 1.2).size(0.12f, 0f).color(0xFFFFFF, COLD).spin(1f).fadeIn(0.3f);
			}
		}

		if (cue(DURATION - 0.5) && mine) {
			Hud.whiteout(STEEL, 0.45, 0.9);
		}
	}

	private void strike(int i) {
		Sfx.play(mine, "forge_clang", c, 1f, 1f - i * 0.06f);
		FxClock.hitstop(90);
		shake(0.4f + i * 0.12f);

		if (mine) {
			PostState.zoom(0.35f + i * 0.1f, 160);
			PostState.split(0.6f + i * 0.4f, (float)(Math.PI * (0.2 + i * 0.55)), 220);
			PostState.flash(HOT, 0.25f + i * 0.1f, 120, 0);
		}

		sparks(70 + i * 30, 6f);

		for (int k = 0; k < 3; k++) {
			Particles.spawn(Kind.HIT, top.add(Particles.randomDir(0.25)), Vec3.ZERO, 0.28).size(0.6f + i * 0.2f, 0.8f).color(0xFFFFFF, HOT);
		}
	}

	private void finalStrike(Vec3 item) {
		FxClock.hitstop(170);
		shake(heavy ? 1.1f : 0.9f);

		if (near) {
			PostState.darken(0);
			PostState.flash(HOT, 0.55f, 160, mine ? 0.15f : 0.08f);
		}

		if (mine) {
			PostState.impact(PostState.EMBER, 160);
			PostState.shock(1.2f, 700);
			PostState.split(2.2f, 0.4f, 500);
			CameraShake.fovPunch(0.14f);
		}

		Sfx.play(mine, "shockwave", c, 1.1f, 0.85f);
		sparks(heavy ? 320 : 220, 10f);
		Particles.spawn(Kind.HIT, top.add(0, 0.1, 0), Vec3.ZERO, 0.4).size(1.6f, 2.2f).color(0xFFFFFF, HOT);

		for (int i = 0; i < 6; i++) {
			Particles.spawn(Kind.HIT, top.add(Particles.randomDir(0.9)), Vec3.ZERO, 0.35 + Particles.RNG.nextDouble() * 0.2).size(Particles.rand(0.9f, 1.6f), 1.8f)
				.color(0xFFFFFF, ORANGE);
		}

		// 鐵渣碎片
		for (int i = 0; i < 50; i++) {
			Particles.spawn(Kind.SHARD, top.add(0, 0.1, 0), Particles.randomDir(3 + Particles.RNG.nextDouble() * 5).add(0, 2, 0), 1.0 + Particles.RNG.nextDouble() * 0.6)
				.size(0.14f, 0.04f).color(HOT, 0xFF3A00).physics(10f, 1.2f).spin(10f);
		}

		for (int i = 0; i < 10; i++) {
			Particles.spawn(Kind.FLARE, top.add(Particles.randomDir(0.4)), Particles.randomDir(1.5), 0.5).size(0.6f, 0f).color(0xFFFFFF, ORANGE);
		}
	}

	private void sparks(int count, float speed) {
		for (int k = 0; k < count; k++) {
			double a = Particles.RNG.nextDouble() * Math.PI * 2;
			double up = Particles.rand(1.5f, 5.5f);
			double out = Particles.rand(2f, speed);
			Vec3 v = new Vec3(Math.cos(a) * out, up, Math.sin(a) * out);
			Particles.spawn(Kind.SPARK, top.add(0, 0.05, 0), v, Particles.rand(0.4f, 1.1f)).size(0.05f, 0.02f).color(0xFFF6C0, 0xFF5010).physics(12f, 0.6f);
		}
	}

	// ------------------------------------------------------------------ 光源（後製）

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float h = heat(t);
		float q = cold(t);
		Vec3 item = itemPos(t);
		float flash = t > IMPACT ? 1 - span(t, IMPACT, IMPACT + 0.9) : 0;
		lights.add(new CinemaPost.Light(top.add(0, 0.3, 0), 3.2f + flash * 3, 1f, 0.45f + h * 0.3f, 0.12f + h * 0.3f, (0.4f + h * 0.6f + flash * 0.6f) * span(t, 0, 0.4),
			true));
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 0.3, c.z), 3.5f, 1f, 0.5f, 0.15f, 0.6f * span(t, 0, 0.5) * (1 - span(t, QUENCH, QUENCH + 0.4)),
			false));

		if (t > 0.3 && t < IMPACT + 1.2) {
			Vec3 head = hammerHead(t);
			lights.add(new CinemaPost.Light(head, 2.2f, 1f, 0.55f, 0.2f, 0.5f * (1 - span(t, IMPACT + 0.6, IMPACT + 1.2)), true));
		}

		if (q > 0.01f) {
			lights.add(new CinemaPost.Light(item, 2.4f, r(COLD), g(COLD), b(COLD), 0.45f * q, true));
		}

		if (flash > 0.01f) {
			rays[0] = top.add(0, 0.15, 0);
			raysP[0] = 1f;
			raysP[1] = 0.75f;
			raysP[2] = 0.4f;
			raysP[3] = flash * 0.55f;
		}
		else if (t > S6 + 0.4) {
			float g0 = span(t, S6 + 0.4, S6 + 0.8) * (1 - span(t, DURATION - 0.6, DURATION));
			rays[0] = item;
			raysP[0] = r(STEEL);
			raysP[1] = g(STEEL);
			raysP[2] = b(STEEL);
			raysP[3] = g0 * 0.5f;
		}
	}

	// ------------------------------------------------------------------ 繪製

	@Override
	protected void items(Draw d, double t) {
		if (p.item().isEmpty()) {
			return;
		}

		float rise = smooth(span(t, S6, S6 + 1.1));
		float spin = rise > 0 ? (float)((t - S6) * 1.2 - (t - S6) * (t - S6) * 0.25) : 0;
		d.item(p.item(), itemPos(t), 0.75f + rise * 0.35f, yaw + spin, (float)Math.PI / 2 * (1 - rise));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.3, DURATION - 0.6, DURATION);
		float h = heat(t);
		float q = cold(t);
		Vec3 item = itemPos(t);

		// ---- 地面魂鑄陣（淬火後轉冷）
		float open = easeOutBack(span(t, 0, 0.5));
		float kick = t > IMPACT ? (1 - span(t, IMPACT, IMPACT + 0.5)) * 0.4f : 0;
		Vec3 floor = new Vec3(c.x, pos.getY() + 0.03, c.z);
		float fr = Mth.lerp(q, r(ORANGE), r(COLD));
		float fg = Mth.lerp(q, g(ORANGE), g(COLD));
		float fb = Mth.lerp(q, b(ORANGE), b(COLD));

		// ---- 背後直立的魂鑄陣
		Vector3f facing = new Vector3f((float)frame.front().x, 0, (float)frame.front().z);
		Vec3 back = c.add(facing.x * -1.7, 1.5, facing.z * -1.7);

		// ---- 每次敲擊的衝擊環
		for (int i = 0; i < STRIKES.length; i++) {
			double s = STRIKES[i];

			if (t > s && t < s + 0.8) {
				boolean last = i == 2;
				float k = span(t, s, s + (last ? 0.8 : 0.55));
				float a = (1 - k) * (1 - k);
				d.flat(Mode.GLOW, last ? JAG : RING, top.add(0, 0.02, 0), 0.3f + easeOut(k) * (last ? 6f : 2.4f), i * 0.7f, r(HOT), g(HOT), b(HOT), a);

				if (last) {
					d.flat(Mode.GLOW, RING, floor.add(0, 0.05, 0), 0.5f + easeOut(k) * 9f, 0, 1f, 0.6f, 0.25f, a * 0.8f);
					d.disc(Mode.GLOW, JAG, top.add(0, 0.2, 0), side, Draw.Y, 0.4f + easeOut(k) * 3f, 0.3f, 1f, 1f, 1f, a);
				}
			}
		}

		// ---- 第三擊：從砧面衝天的火柱
		float pillar = t < IMPACT ? 0 : 1 - span(t, IMPACT + 0.1, IMPACT + 1.1);

		if (pillar > 0.01f) {
			float w = easeOut(span(t, IMPACT, IMPACT + 0.06)) * (0.5f + 0.5f * pillar);
			float scroll = (float)(t * 3);
			d.cylinder(Mode.GLOW, BEAM, top, 0.7f * w, 24f, 28, 1, 0, 5, scroll, 1.2f, r(ORANGE), g(ORANGE), b(ORANGE), pillar * 0.5f, pillar * 0.1f);
			d.cylinder(Mode.GLOW, BEAM, top, 0.25f * w, 24f, 16, 1, 0.5f, 4, scroll * 1.6f, 1.1f, 1f, 0.95f, 0.8f, pillar * 0.6f, pillar * 0.25f);
		}

		// ---- 熱度光暈（橘 → 白熱）與淬火冷光
		Vec3 glowAt = top.add(0, 0.15, 0);
		d.billboard(Mode.GLOW, GLOW, glowAt.x, glowAt.y, glowAt.z, 0.6f + h * 0.8f, 0, 1f, 0.45f + h * 0.4f, 0.1f + h * 0.5f, (0.3f + h * 0.7f) * life);

		if (q > 0.01f) {
			float breathe = 0.85f + 0.15f * (float)Math.sin(t * 3);
			d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.1f * breathe, 0, r(COLD), g(COLD), b(COLD), 0.3f * q);
		}

		// ---- 寒光：一道冷光沿著立起的刃身由下往上掃過
		if (t > S6 + 0.4) {
			float k = span(t, S6 + 0.45, S6 + 0.85);

			if (k > 0 && k < 1) {
				Vec3 g0 = item.add(0, -0.4 + k * 0.8, 0);
				d.billboard(Mode.GLOW, FLARE, g0.x, g0.y, g0.z, 0.9f, (float)t, 1f, 1f, 1f, (float)Math.sin(k * Math.PI));
			}

			float ring = span(t, S6 + 0.85, S6 + 1.5);

			if (ring > 0 && ring < 1) {
				d.disc(Mode.GLOW, RING, item, Draw.X, Draw.Y, 0.3f + easeOut(ring) * 1.8f, 0, r(STEEL), g(STEEL), b(STEEL), (1 - ring) * 0.8f);
			}

			float star = span(t, S6 + 0.8, S6 + 1.0) * (1 - span(t, S6 + 1.0, S6 + 1.6));
			d.billboard(Mode.GLOW, FLARE, item.x, item.y + 0.4, item.z, 1.4f * star, 0.785f, r(STEEL), g(STEEL), b(STEEL), star);
		}

		// ---- 靈魂之錘
		float hammerA = Math.min(span(t, 0.45, 0.85), 1 - span(t, IMPACT + 0.9, IMPACT + 1.4));

		if (hammerA > 0.01f) {
			FxMesh mesh = FxMesh.get("hammer");
			float ang = (float)Math.toRadians(swing(t));
			float appear = easeOutBack(span(t, 0.45, 0.9));
			Matrix4f m = new Matrix4f().rotateY(yaw).rotateZ(-ang).scale(appear);
			d.mesh(Mode.SOLID, mesh, pivot, m, 1f, 0.92f, 0.85f, 0.95f * hammerA, true);
			Matrix4f shell = new Matrix4f(m).translate(0, 1.7f, 0).scale(1.08f).translate(0, -1.7f, 0);
			d.mesh(Mode.GLOW, mesh, pivot, shell, 1f, 0.5f + h * 0.3f, 0.15f, (0.35f + (t > S3 && t < IMPACT ? 0.3f : 0)) * hammerA, false);
			Vec3 head = hammerHead(t);
			d.billboard(Mode.GLOW, GLOW, head.x, head.y, head.z, 1.2f + (t > S3 && t < IMPACT ? span(t, S3, IMPACT) : 0), 0, 1f, 0.55f, 0.15f, 0.5f * hammerA);
		}
	}
}
