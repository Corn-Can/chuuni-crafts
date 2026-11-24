package com.chuuni.client.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.chuuni.client.render.Draw;
import com.chuuni.client.render.FxRender;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 自製粒子（不經過原版粒子系統，才能用自己的貼圖、加法混色、頓幀與慢動作）。
 * 每顆粒子：位置 / 速度 / 重力 / 阻力、尺寸曲線、顏色從 c0 漸變到 c1、可選拖尾（spark）或吸引目標點。
 */
public final class Particles {
	public static final Identifier GLOW = FxRender.tex("glow");
	public static final Identifier FLARE = FxRender.tex("flare");
	public static final Identifier SPARK = FxRender.tex("spark");
	public static final Identifier SMOKE = FxRender.tex("smoke");
	public static final Identifier FLAME = FxRender.tex("flame");
	public static final Identifier GLYPHS = FxRender.tex("glyphs");
	public static final Identifier SHARD = FxRender.tex("shard");
	public static final Identifier HIT_SPARK = FxRender.tex("hit_spark");

	public static final Random RNG = new Random();
	private static final List<P> LIST = new ArrayList<>();
	private static final int MAX = 6000;

	public enum Kind {
		/** 柔光點 */
		GLOW,
		/** 十字星光 */
		FLARE,
		/** 沿速度方向拉長的火花 */
		SPARK,
		/** 煙（一般透明） */
		SMOKE,
		/** 火焰 */
		FLAME,
		/** 發光符文（從 8×8 圖集隨機取一格） */
		GLYPH,
		/** 結晶碎片 */
		SHARD,
		/** 打擊火花（6 幀序列，硬邊，依壽命播放） */
		HIT
	}

	public static final class P {
		Kind kind;
		double x, y, z, vx, vy, vz;
		float gravity, drag;
		float size0, size1;
		float r0, g0, b0, r1, g1, b1;
		float alpha;
		double life, age;
		float roll, spin;
		int cell;
		/** 吸引目標（粒子在壽命內被拉向該點，用於「匯聚」） */
		Vec3 attract;
		float attractStrength;
		float fadeIn = 0.1f;

		public P size(float s0, float s1) {
			size0 = s0;
			size1 = s1;
			return this;
		}

		public P color(int c0, int c1) {
			r0 = (c0 >> 16 & 255) / 255f;
			g0 = (c0 >> 8 & 255) / 255f;
			b0 = (c0 & 255) / 255f;
			r1 = (c1 >> 16 & 255) / 255f;
			g1 = (c1 >> 8 & 255) / 255f;
			b1 = (c1 & 255) / 255f;
			return this;
		}

		public P physics(float gravity, float drag) {
			this.gravity = gravity;
			this.drag = drag;
			return this;
		}

		public P alpha(float a) {
			alpha = a;
			return this;
		}

		public P spin(float s) {
			spin = s;
			return this;
		}

		public P attract(Vec3 target, float strength) {
			attract = target;
			attractStrength = strength;
			return this;
		}

		public P fadeIn(float f) {
			fadeIn = f;
			return this;
		}
	}

	private Particles() {
	}

	public static P spawn(Kind kind, double x, double y, double z, double vx, double vy, double vz, double life) {
		P p = new P();
		p.kind = kind;
		p.x = x;
		p.y = y;
		p.z = z;
		p.vx = vx;
		p.vy = vy;
		p.vz = vz;
		p.life = life;
		p.size0 = p.size1 = 0.2f;
		p.r0 = p.g0 = p.b0 = p.r1 = p.g1 = p.b1 = 1f;
		p.alpha = 1f;
		p.roll = RNG.nextFloat() * Mth.TWO_PI;
		p.cell = RNG.nextInt(64);
		p.drag = 0.5f;

		if (LIST.size() >= MAX) {
			LIST.removeFirst();
		}

		LIST.add(p);
		return p;
	}

	public static P spawn(Kind kind, Vec3 at, Vec3 vel, double life) {
		return spawn(kind, at.x, at.y, at.z, vel.x, vel.y, vel.z, life);
	}

	/** 隨機單位向量 × 速度 */
	public static Vec3 randomDir(double speed) {
		double u = RNG.nextDouble() * 2 - 1;
		double th = RNG.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - u * u);
		return new Vec3(s * Math.cos(th) * speed, u * speed, s * Math.sin(th) * speed);
	}

	public static float rand(float lo, float hi) {
		return lo + RNG.nextFloat() * (hi - lo);
	}

	public static void clear() {
		LIST.clear();
	}

	public static int count() {
		return LIST.size();
	}

	public static void update(double dt) {
		if (dt <= 0) {
			return;
		}

		LIST.removeIf(p -> {
			p.age += dt;

			if (p.age >= p.life) {
				return true;
			}

			if (p.attract != null) {
				double k = p.attractStrength * dt;
				p.vx += (p.attract.x - p.x) * k;
				p.vy += (p.attract.y - p.y) * k;
				p.vz += (p.attract.z - p.z) * k;
			}

			double damp = Math.exp(-p.drag * dt);
			p.vx *= damp;
			p.vy = p.vy * damp - p.gravity * dt;
			p.vz *= damp;
			p.x += p.vx * dt;
			p.y += p.vy * dt;
			p.z += p.vz * dt;
			p.roll += p.spin * dt;
			return false;
		});
	}

	public static void render(Draw d) {
		for (P p : LIST) {
			float t = (float)(p.age / p.life);
			float size = Mth.lerp(t, p.size0, p.size1);
			float r = Mth.lerp(t, p.r0, p.r1), g = Mth.lerp(t, p.g0, p.g1), b = Mth.lerp(t, p.b0, p.b1);
			float a = p.alpha * Math.min(1f, t / Math.max(0.001f, p.fadeIn)) * (1f - smooth(Mth.clamp((t - 0.6f) / 0.4f, 0f, 1f)));

			switch (p.kind) {
				case GLOW -> d.billboard(Draw.Mode.GLOW, GLOW, p.x, p.y, p.z, size, 0, r, g, b, a);
				case FLARE -> d.billboard(Draw.Mode.GLOW, FLARE, p.x, p.y, p.z, size, p.roll * 0.2f, r, g, b, a);
				case FLAME -> d.billboard(Draw.Mode.GLOW, FLAME, p.x, p.y, p.z, size, 0, r, g, b, a);
				case SMOKE -> d.billboard(Draw.Mode.ALPHA, SMOKE, p.x, p.y, p.z, size, p.roll, r, g, b, a);
				case SHARD -> d.billboard(Draw.Mode.GLOW, SHARD, p.x, p.y, p.z, size, p.roll, r, g, b, a);
				case GLYPH -> {
					float u0 = (p.cell % 8) / 8f;
					float v0 = (p.cell / 8) / 8f;
					d.billboard(Draw.Mode.GLOW, GLYPHS, p.x, p.y, p.z, size, 0, r, g, b, a, u0, v0, u0 + 0.125f, v0 + 0.125f);
				}
				case HIT -> {
					int f = Math.min(5, (int)(t * 6));
					d.billboard(Draw.Mode.GLOW, HIT_SPARK, p.x, p.y, p.z, size, p.roll, r, g, b, p.alpha, f / 6f, 0, (f + 1) / 6f, 1);
				}
				case SPARK -> {
					double len = Math.min(0.08, 0.035 + Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz) * 0.03);
					d.streak(Draw.Mode.GLOW, SPARK, p.x - p.vx * len, p.y - p.vy * len, p.z - p.vz * len, p.x, p.y, p.z, size, r, g, b, a);
				}
			}
		}
	}

	private static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}
}
