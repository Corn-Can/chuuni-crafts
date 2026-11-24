package com.chuuni.client.fx.effects;

import com.chuuni.client.fx.Effect;
import com.chuuni.client.fx.FxClock;
import com.chuuni.client.fx.Hud;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Settings;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 封印解除！／結界再封（門、活板門、柵欄門）。每天會開上百次，所以是 1 秒內的精簡版，但每次都有完整的「動作」：
 * <ul>
 *   <li>封印陣出現在玩家實際點到的那一面（伺服器送來點擊位置與面）</li>
 *   <li>開：封印浮現 → 碎裂，符紙碎片朝玩家飛散、中央裂出一道光縫往外撐開</li>
 *   <li>關：四條符鏈從四角射向中心 → 封印「砰」地蓋上、印記一閃</li>
 *   <li>門：直立封印 + 上下兩張符紙；活板門：水平封印 + 外圈符文環；柵欄門：左右兩枚小封印</li>
 *   <li>連段越高越大越亮、顏色往金色偏；5 連段毛筆招式名，10 / 20 / 30… 連段漫畫分格 + 四角大字</li>
 * </ul>
 */
public class DoorFx extends Effect {
	private static final Identifier SEAL = FxRender.tex("circle_seal");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier BAND = FxRender.tex("rune_band");
	private static final Identifier TRAIL = FxRender.tex("slash_trail");
	private static final Identifier SPARK = FxRender.tex("spark");
	private static final Identifier GLOW = FxRender.tex("glow");

	private static final int OPEN = 0x7CFFB0;
	private static final int CLOSE = 0x9A90FF;
	private static final int GOLD = 0xFFD070;
	private static final int PAPER = 0xF6EBD0;

	private final boolean open;
	private final int combo;
	private final int color;
	private final int kind;
	/** 封印陣中心（貼在點到的那一面上） */
	private final Vec3 center;
	/** 面的法線（朝向玩家） */
	private final Vector3f normal;
	/** 面內的兩個軸：u 水平、v 垂直（水平面時 v 也是水平） */
	private final Vector3f u;
	private final Vector3f v;
	private final boolean flat;
	private final float size;
	private final float boost;
	private final float spin0;

	public DoorFx(FxPayload p) {
		super(p);
		open = p.extra() == 1;
		combo = Math.max(1, p.power());
		kind = Math.floorMod(p.variant(), 3);
		float gold = Math.min(1f, (combo - 1) / 12f);
		color = lerpColor(open ? OPEN : CLOSE, GOLD, gold);
		boost = Math.min(2.0f, 1f + (combo - 1) * 0.07f) * power;
		spin0 = (float)(Math.floorMod(p.pos().asLong() * 31 + combo * 7L, 628) / 100.0);

		BlockState state = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockState(pos);
		BlockPos base = pos;

		if (state != null && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
			base = pos.below();
		}

		Vec3 mid = switch (kind) {
			case 0 -> Vec3.atBottomCenterOf(base).add(0, 1.0, 0);
			case 2 -> Vec3.atCenterOf(pos).add(0, 0.1, 0);
			default -> Vec3.atCenterOf(pos);
		};

		Direction face = p.face() >= 0 && p.face() < 6 ? Direction.values()[p.face()] : towardPlayer(mid);
		normal = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		flat = face.getAxis() == Direction.Axis.Y;

		// 法線方向的座標取點擊位置（剛好貼在點到的那個面上），另外兩軸置中
		Vec3 hit = p.at();
		double off = 0.03;
		center = switch (face.getAxis()) {
			case X -> new Vec3(hit.x + face.getStepX() * off, mid.y, mid.z);
			case Y -> new Vec3(mid.x, hit.y + face.getStepY() * off, mid.z);
			case Z -> new Vec3(mid.x, mid.y, hit.z + face.getStepZ() * off);
		};

		if (flat) {
			u = new Vector3f(1, 0, 0);
			v = new Vector3f(0, 0, 1);
		}
		else {
			u = new Vector3f(-normal.z, 0, normal.x);
			v = new Vector3f(0, 1, 0);
		}

		size = kind == 0 ? 0.48f : kind == 1 ? 0.55f : 0.36f;
	}

	private Direction towardPlayer(Vec3 mid) {
		var player = Minecraft.getInstance().player;

		if (player == null) {
			return Direction.NORTH;
		}

		Vec3 d = player.position().subtract(mid);
		return Direction.getApproximateNearest(d.x, 0, d.z);
	}

	private static int lerpColor(int a, int b, float t) {
		int r = (int)Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
		int g = (int)Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
		int bl = (int)Mth.lerp(t, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}

	@Override
	public double duration() {
		return 1.0;
	}

	/** 面上的一點（a 沿 u、b 沿 v，再沿法線浮起 n） */
	private Vec3 on(double a, double b, double n) {
		return center.add(u.x * a + v.x * b + normal.x * n, u.y * a + v.y * b + normal.y * n, u.z * a + v.z * b + normal.z * n);
	}

	/** 這個變種的所有封印中心（柵欄門左右兩枚） */
	private Vec3[] seals() {
		return kind == 2 ? new Vec3[]{on(-0.25, 0, 0), on(0.25, 0, 0)} : new Vec3[]{center};
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			float pitch = (kind == 1 ? 1.12f : kind == 2 ? 1.06f : 1f) + Math.min(combo, 12) * 0.02f;
			Sfx.play(mine, open ? "seal_open" : "seal_close", center, 0.75f, pitch);

			if (!open) {
				// 符鏈從四角射入時的符文
				for (int i = 0; i < (int)(10 * boost); i++) {
					Vec3 from = on(Particles.rand(-0.9f, 0.9f), Particles.rand(-1f, 1f), 0.2);
					Particles.spawn(Kind.GLYPH, from, Vec3.ZERO, 0.4).size(0.08f * boost, 0.02f).color(0xFFFFFF, color).physics(0, 0).attract(center, 25f);
				}
			}

			milestone();
		}

		if (open && cue(0.1)) {
			shatter();
		}

		if (!open && cue(0.25)) {
			stamp();
		}
	}

	/** 開：封印碎裂 */
	private void shatter() {
		Vec3 n = new Vec3(normal.x, normal.y, normal.z);

		for (Vec3 s : seals()) {
			// 符紙碎片朝玩家飛散、慢慢飄落
			for (int i = 0; i < (int)(14 * boost); i++) {
				Vec3 at = s.add(Particles.randomDir(size * 0.6));
				Vec3 vel = n.scale(Particles.rand(1f, 2.6f)).add(Particles.randomDir(1.4));
				Particles.spawn(Kind.SHARD, at, vel, Particles.rand(0.7f, 1.2f)).size(0.07f * boost, 0.04f).color(PAPER, color).physics(3f, 2.4f).spin(8f);
			}

			for (int i = 0; i < (int)(10 * boost); i++) {
				Particles.spawn(Kind.GLYPH, s, n.scale(Particles.rand(0.5f, 1.5f)).add(Particles.randomDir(1.6)), Particles.rand(0.5f, 0.8f))
					.size(0.09f * boost, 0.03f).color(0xFFFFFF, color).physics(0, 2.5f);
			}

			Particles.spawn(Kind.HIT, s.add(n.scale(0.05)), Vec3.ZERO, 0.22).size(0.5f * boost, 0.8f).color(0xFFFFFF, color);
		}
	}

	/** 關：封印蓋上 */
	private void stamp() {
		Vec3 n = new Vec3(normal.x, normal.y, normal.z);

		for (Vec3 s : seals()) {
			Particles.spawn(Kind.HIT, s.add(n.scale(0.05)), Vec3.ZERO, 0.25).size(0.6f * boost, 0.9f).color(0xFFFFFF, color);

			for (int i = 0; i < (int)(16 * boost); i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 dir = new Vec3(u.x * Math.cos(a) + v.x * Math.sin(a), u.y * Math.cos(a) + v.y * Math.sin(a), u.z * Math.cos(a) + v.z * Math.sin(a));
				Particles.spawn(Kind.SPARK, s, dir.scale(Particles.rand(1.5f, 3.5f)), 0.3).size(0.03f, 0.008f).color(0xFFFFFF, color).physics(0, 4f);
			}
		}

		if (mine) {
			PostState.zoom(0.15f, 120);
		}

		shake(0.08f + Math.min(combo, 10) * 0.015f);
	}

	private void milestone() {
		if (!mine) {
			return;
		}

		// 每次開關：一點點 RGB 錯開，像符咒「啪」地生效
		PostState.split(0.35f + Math.min(combo, 10) * 0.05f, open ? 0.3f : 1.9f, 160);

		if (combo >= 3) {
			Hud.combo(combo, color, "SEAL COMBO");
		}

		if (Settings.titlesOn() && combo >= 10 && combo % 10 == 0) {
			// 十連段：漫畫分格 + 四角大字
			FxClock.hitstop(160);
			PostState.look("door", 150, 900);
			PostState.impact(PostState.INK, 110);
			PostState.manga(800);
			PostState.lines(1f, true, 800, 0.3f);
			Hud.mangaName(open ? "door_open" : "door_close", 0.8);
			shake(0.4f);
		}
		else if (Settings.titlesOn() && (combo == 5 || Settings.intensity >= 2 && combo == 1)) {
			// 五連段：毛筆直書招式名
			PostState.look("door", 150, 700);
			PostState.lines(0.6f, false, 500, 0.4f);
			PostState.flash(color, 0.3f, 150, 0);
			Hud.brushName(open ? "door_open" : "door_close", 1.8);
			shake(0.25f);
		}
		else if (combo >= 6) {
			PostState.flash(color, 0.18f, 120, 0);
			PostState.lines(0.35f, false, 250, 0.45f);
		}
	}

	// ------------------------------------------------------------------ 繪製

	private void seal(Draw d, Identifier tex, Vec3 at, float radius, float rot, float r, float g, float b, float a) {
		if (flat) {
			d.flat(Mode.GLOW, tex, at, radius, rot, r, g, b, a);
		}
		else {
			d.disc(Mode.GLOW, tex, at, u, v, radius, rot, r, g, b, a);
		}
	}

	private void line(Draw d, Identifier tex, Vec3 a, Vec3 b, float w, int col, float alpha) {
		d.streak(Mode.GLOW, tex, a.x, a.y, a.z, b.x, b.y, b.z, w, r(col), g(col), b(col), alpha);
	}

	@Override
	protected void render(Draw d, double t) {
		float rc = r(color), gc = g(color), bc = b(color);
		float s = size * boost;

		if (open) {
			float appear = easeOut(span(t, 0, 0.1));
			float burst = span(t, 0.1, 0.9);
			float a = appear * (1 - burst) * (1 - burst);
			float rot = spin0 + (float)t * 4;

			for (Vec3 at : seals()) {
				seal(d, SEAL, at, s * (0.9f * appear + easeOut(burst) * 0.8f), rot, rc, gc, bc, Math.min(1f, a * 1.2f));
				seal(d, JAG, at, s * (0.4f + easeOut(burst) * 1.6f), -rot, 1f, 1f, 1f, (1 - burst) * 0.8f * span(t, 0.1, 0.13));
			}

			// 光縫：從封印中央裂開、往外撐
			float crack = span(t, 0.08, 0.6);

			if (crack > 0 && crack < 1) {
				float len = (kind == 0 ? 1.0f : 0.5f) * easeOut(Math.min(1, crack * 3));
				float w = 0.02f + easeOut(crack) * 0.18f;
				Vec3 a0 = on(0, -len, 0.02);
				Vec3 a1 = on(0, len, 0.02);
				line(d, SPARK, a0, a1, w * 2.5f, color, (1 - crack) * 0.6f);
				line(d, SPARK, a0, a1, w, 0xFFFFFF, (1 - crack));
				d.billboard(Mode.GLOW, GLOW, center.x, center.y, center.z, 0.6f + crack, 0, rc, gc, bc, (1 - crack) * 0.5f);
			}

			// 門：上下兩張符紙，開門時一閃就燒掉
			if (kind == 0) {
				float paper = (1 - span(t, 0.05, 0.35)) * appear;
				float flicker = 0.7f + 0.3f * (float)Math.sin(t * 90);

				for (int k = -1; k <= 1; k += 2) {
					line(d, TRAIL, on(0, k * 0.55, 0), on(0, k * 0.95, 0), 0.16f, color, paper * flicker);
				}
			}
		}
		else {
			// 符鏈：四條虛線從四角射向中心
			float chain = easeIn(span(t, 0, 0.25));
			float chainA = 1 - span(t, 0.3, 0.6);
			double h = kind == 0 ? 1.0 : 0.55;

			for (int k = 0; k < 4; k++) {
				double cu = (k % 2 == 0 ? -1 : 1) * 0.55;
				double cv = (k < 2 ? -1 : 1) * h;
				int dashes = 6;

				for (int j = 0; j < dashes; j++) {
					double f0 = (double)j / dashes;
					double f1 = f0 + 0.6 / dashes;
					double reach = chain;

					if (f0 > reach) {
						break;
					}

					// 從角落往中心：0 = 角落、reach = 鏈頭
					double p0 = 1 - f0, p1 = 1 - Math.min(f1, reach);
					line(d, SPARK, on(cu * p0, cv * p0, 0.02), on(cu * p1, cv * p1, 0.02), 0.035f, color, chainA);
				}
			}

			// 封印從外收束 → 蓋上 → 淡出
			float k = easeOut(span(t, 0, 0.25));
			float rot = spin0 - (float)t * 3;
			float a = span(t, 0, 0.1) * (1 - span(t, 0.5, 1.0));
			float flash = t > 0.25 && t < 0.4 ? 1 - span(t, 0.25, 0.4) : 0;

			for (Vec3 at : seals()) {
				seal(d, SEAL, at, s * (1.8f - k * 0.8f), rot, rc + flash, gc + flash, bc + flash, Math.min(1f, a + flash * 0.5f));
				float w = span(t, 0.25, 0.75);

				if (w > 0 && w < 1) {
					seal(d, RING, at, s * (0.3f + easeOut(w) * 1.5f), 0, 1f, 1f, 1f, (1 - w) * 0.8f);
				}
			}
		}

		// 活板門：外圈符文環
		if (kind == 1) {
			float a = span(t, 0, 0.1) * (1 - span(t, 0.4, 1.0));

			if (flat) {
				d.annulus(Mode.GLOW, BAND, center, s * 1.05f, s * 1.3f, 40, 4, (float)t * (open ? 0.5f : -0.5f), rc, gc, bc, a, a);
			}
			else {
				seal(d, RING, center, s * 1.25f, 0, rc, gc, bc, a);
			}
		}

		// 地面的小封印（連段 ≥ 5）
		if (combo >= 5) {
			float fa = (1 - span(t, 0.3, 1.0)) * Math.min(1f, combo / 10f);
			d.flat(Mode.GLOW, SEAL, new Vec3(center.x, Math.floor(center.y - (kind == 0 ? 0.9 : 0.4)) + 0.03, center.z), 1.3f * boost,
				-spin0 - (float)t * 2, rc, gc, bc, fa);
		}
	}
}
