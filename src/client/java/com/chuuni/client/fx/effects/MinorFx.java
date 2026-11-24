package com.chuuni.client.fx.effects;

import com.chuuni.client.fx.CameraShake;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * B 級短演出的共用骨架：不切鏡頭、3 秒內，但每個方塊都有自己的「一招」。
 * <ul>
 *   <li>點到的那一面：{@link #face} 中心、法線 {@link #n}、面內兩軸 {@link #u} / {@link #v}</li>
 *   <li>{@link #pop}：一記輕的打擊感（頓幀、RGB 分離、小震、FOV 衝擊、閃光）</li>
 *   <li>{@link #tint}：以主色淡淡染世界（只在附近）</li>
 *   <li>{@link #smallName}：右上角的小型毛筆招式名（同名 20 秒內不重複）</li>
 * </ul>
 */
public abstract class MinorFx extends AnimeFx {
	protected static final Identifier SHARD_T = com.chuuni.client.render.FxRender.tex("shard");

	/** 點到的面 */
	protected final Direction dir;
	/** 面中心（貼在方塊表面上，略浮起） */
	protected final Vec3 face;
	/** 實際點到的位置 */
	protected final Vec3 hit;
	protected final Vector3f n;
	protected final Vector3f u;
	protected final Vector3f v;
	protected final Vec3 nv;

	protected MinorFx(FxPayload p) {
		super(p);
		end = 2.4;
		var mc = Minecraft.getInstance();
		Direction d;

		if (p.face() >= 0 && p.face() < 6) {
			d = Direction.values()[p.face()];
		}
		else if (mc.player != null) {
			Vec3 to = mc.player.position().subtract(c);
			d = Direction.getApproximateNearest(to.x, 0, to.z);
		}
		else {
			d = Direction.NORTH;
		}

		dir = d;
		n = new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ());
		nv = new Vec3(n.x, n.y, n.z);

		if (d.getAxis() == Direction.Axis.Y) {
			u = new Vector3f(1, 0, 0);
			v = new Vector3f(0, 0, 1);
		}
		else {
			u = new Vector3f(-n.z, 0, n.x);
			v = new Vector3f(0, 1, 0);
		}

		face = c.add(nv.scale(0.52));
		hit = p.at();
	}

	/** 生物互動的目標（extra = 實體 id）；不存在時回傳 null */
	protected net.minecraft.world.entity.Entity target() {
		var level = Minecraft.getInstance().level;
		return level == null ? null : level.getEntity(p.extra());
	}

	/** 目標生物的身體中心（會跟著牠移動）；不在了就用伺服器給的位置 */
	protected Vec3 body() {
		var e = target();
		return e == null ? p.at() : e.position().add(0, e.getBbHeight() * 0.5, 0);
	}

	/** 目標生物的高度（不在了時用 1） */
	protected float bodyHeight() {
		var e = target();
		return e == null ? 1f : e.getBbHeight();
	}

	protected float bodyWidth() {
		var e = target();
		return e == null ? 0.8f : e.getBbWidth();
	}

	/** 觸發者（自己）的手的位置：眼睛往下一點、往前一點 */
	protected Vec3 hand() {
		var mc = Minecraft.getInstance();
		var level = mc.level;
		var e = level == null ? null : level.getEntity(p.actor());

		if (e == null) {
			return body().add(0, 0.5, 0);
		}

		return e.getEyePosition().add(e.getLookAngle().scale(0.6)).add(0, -0.45, 0);
	}

	/** 點到的方塊目前的狀態（客戶端世界） */
	protected BlockState state() {
		var level = Minecraft.getInstance().level;
		return level == null ? null : level.getBlockState(pos);
	}

	/** 方塊地圖色（染色蠟燭 / 界伏盒等用） */
	protected int mapColor(int fallback) {
		var level = Minecraft.getInstance().level;
		BlockState s = state();

		if (level == null || s == null) {
			return fallback;
		}

		int col = s.getMapColor(level, pos).col;
		return col == 0 ? fallback : col;
	}

	/** 面上的一點（a 沿 u、b 沿 v，再沿法線浮起 o） */
	protected Vec3 on(Vec3 origin, double a, double b, double o) {
		return origin.add(u.x * a + v.x * b + n.x * o, u.y * a + v.y * b + n.y * o, u.z * a + v.z * b + n.z * o);
	}

	protected Vec3 on(double a, double b, double o) {
		return on(face, a, b, o);
	}

	/** 在面上畫圓盤（水平面時用 flat） */
	protected void faceDisc(Draw d, Identifier tex, Vec3 at, float radius, float rot, int col, float a) {
		if (a <= 0.005f) {
			return;
		}

		if (dir.getAxis() == Direction.Axis.Y) {
			d.flat(Mode.GLOW, tex, at, radius, rot, r(col), g(col), b(col), a);
		}
		else {
			d.disc(Mode.GLOW, tex, at, u, v, radius, rot, r(col), g(col), b(col), a);
		}
	}

	protected void line(Draw d, Identifier tex, Vec3 a, Vec3 b, float w, int col, float alpha) {
		if (alpha > 0.005f) {
			d.streak(Mode.GLOW, tex, a.x, a.y, a.z, b.x, b.y, b.z, w, r(col), g(col), b(col), alpha);
		}
	}

	protected void glow(Draw d, Vec3 at, float size, int col, float a) {
		if (a > 0.005f) {
			d.billboard(Mode.GLOW, GLOW, at.x, at.y, at.z, size, 0, r(col), g(col), b(col), a);
		}
	}

	/** 輕打擊：頓幀 + RGB 分離 + 小震 + 一點點 FOV 衝擊 + 主色閃光 */
	protected void pop(int col, float strength) {
		FxClock.hitstop((int)(40 + 50 * strength));
		shake(0.1f + 0.25f * strength);

		if (mine) {
			PostState.split(0.4f + strength * 0.8f, (float)(Particles.RNG.nextDouble() * 3), 180);
			PostState.zoom(0.12f + strength * 0.2f, 160);
			PostState.flash(col, 0.12f + strength * 0.18f, 140, 0);
			CameraShake.fovPunch(0.02f + strength * 0.04f);

			if (strength >= 0.6f) {
				PostState.lines(0.45f * strength, false, 380, 0.45f);
			}
		}
	}

	protected void tint(int col, double seconds) {
		if (near) {
			PostState.tint(col, 200, (int)(seconds * 1000));
		}
	}

	protected void smallName(String key) {
		if (mine && Settings.titlesOn()) {
			Hud.smallName(key, 2.2);
		}
	}

	/** 大型毛筆招式名（不切鏡頭的大事件：不死圖騰、擊倒頭目、挑戰進度……） */
	protected void bigName(String key, double duration) {
		if (mine && Settings.titlesOn()) {
			Hud.brushName(key, duration);
		}
	}

	/** 觸發者（玩家）實體；不在了回傳 null */
	protected net.minecraft.world.entity.Entity actor() {
		var level = Minecraft.getInstance().level;
		return level == null ? null : level.getEntity(p.actor());
	}

	/** 往四面八方的火花 */
	protected static void sparks(Vec3 at, int count, float speed, int col, float life) {
		for (int i = 0; i < count; i++) {
			Particles.spawn(Kind.SPARK, at, Particles.randomDir(Particles.rand(speed * 0.4f, speed)), life * Particles.rand(0.6f, 1.2f)).size(0.03f, 0.008f)
				.color(0xFFFFFF, col).physics(3f, 2f);
		}
	}

	/** 在面上往外擴的環（法線方向） */
	protected void faceRing(Draw d, double t, double t0, double t1, Vec3 at, float r0, float r1, int col, float a) {
		float k = span(t, t0, t1);

		if (k > 0 && k < 1) {
			faceDisc(d, RING, at, r0 + (r1 - r0) * easeOut(k), 0, col, (1 - k) * a);
		}
	}

	/** 面上的蜂巢六角格：中央一格 + 周圍六格，grow 0→1 由內往外長出 */
	protected void hexes(Draw d, Vec3 center, float size, int col, float a, float grow) {
		if (a <= 0.01f) {
			return;
		}

		for (int k = -1; k < 6; k++) {
			float g = k < 0 ? Math.min(1, grow * 2) : clamp01((grow - 0.3f - k * 0.05f) * 3);

			if (g <= 0) {
				continue;
			}

			double ca = k * Math.PI / 3 + Math.PI / 6;
			double dist = k < 0 ? 0 : size * Math.sqrt(3);
			double cu = Math.cos(ca) * dist, cv = Math.sin(ca) * dist;

			for (int e = 0; e < 6; e++) {
				double a0 = e * Math.PI / 3, a1 = (e + 1) * Math.PI / 3;
				float rr = size * 0.92f * g;
				Vec3 p0 = on(center, cu + Math.cos(a0) * rr, cv + Math.sin(a0) * rr, 0.01);
				Vec3 p1 = on(center, cu + Math.cos(a1) * rr, cv + Math.sin(a1) * rr, 0.01);
				line(d, SPARK, p0, p1, 0.035f, col, a * g);
			}
		}
	}

	/** 地上的警戒虛線環（兩色交錯、旋轉） */
	protected static void dashRing(Draw d, Vec3 center, float radius, int n, float spin, int c0, int c1, float width, float a) {
		if (a <= 0.01f) {
			return;
		}

		for (int i = 0; i < n; i++) {
			double a0 = spin + i * Math.PI * 2 / n;
			double a1 = a0 + Math.PI * 2 / n * 0.6;
			int col = i % 2 == 0 ? c0 : c1;
			d.streak(Mode.GLOW, SPARK, center.x + Math.cos(a0) * radius, center.y, center.z + Math.sin(a0) * radius, center.x + Math.cos(a1) * radius, center.y,
				center.z + Math.sin(a1) * radius, width, r(col), g(col), b(col), a);
		}
	}

	/** 地面上往外擴的環 */
	protected static void groundRing(Draw d, double t, double t0, double t1, Vec3 at, float r0, float r1, int col, float a, Identifier tex) {
		float k = span(t, t0, t1);

		if (k > 0 && k < 1) {
			d.flat(Mode.GLOW, tex, at, r0 + (r1 - r0) * easeOut(k), 0, r(col), g(col), b(col), (1 - k) * (1 - k) * a);
		}
	}
}
