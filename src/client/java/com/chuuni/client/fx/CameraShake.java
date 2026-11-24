package com.chuuni.client.fx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 創傷值鏡頭震顫（Camera Trauma，Squirrel Eiserloh 模型）：
 * <ul>
 *   <li>衝擊累積 trauma（0~1），實際震幅 = trauma²（小衝擊幾乎不晃、大衝擊猛烈）</li>
 *   <li>以 3D Perlin 噪聲分別驅動 Yaw / Pitch / Roll 與位置偏移，頻率隨 trauma 提高</li>
 *   <li>距離衰減為平方反比：近距離滿額、遠處快速減弱</li>
 *   <li>FOV 衝擊彈簧：爆發瞬間視野向外撐開再以阻尼彈簧回彈</li>
 * </ul>
 */
public final class CameraShake {
	private static final float DECAY_PER_SECOND = 0.9f;
	/** 這個距離內不衰減 */
	private static final float NEAR = 6f;
	private static final float MAX_YAW = 3.2f;
	private static final float MAX_PITCH = 2.4f;
	private static final float MAX_ROLL = 4.0f;
	private static final float MAX_OFFSET = 0.16f;

	private static float trauma;
	private static long lastNanos = System.nanoTime();
	private static double time;

	// FOV 彈簧：kick 為目前偏移、velocity 為速度
	private static float fovKick;
	private static float fovVelocity;

	private CameraShake() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// FOV 阻尼彈簧（每 tick 積分）
			fovVelocity += -fovKick * 0.45f;
			fovVelocity *= 0.62f;
			fovKick += fovVelocity;

			if (Math.abs(fovKick) < 1e-4f && Math.abs(fovVelocity) < 1e-4f) {
				fovKick = 0;
				fovVelocity = 0;
			}
		});
	}

	/** 在 source 處加入創傷值（依與鏡頭距離平方反比衰減） */
	public static void add(Vec3 source, float amount) {
		Minecraft mc = Minecraft.getInstance();
		double distance = mc.gameRenderer.getMainCamera().position().distanceTo(source);
		float attenuation = distance <= NEAR ? 1f : (float)((NEAR * NEAR) / (distance * distance));
		trauma = Math.min(1f, trauma + amount * attenuation);
	}

	/** 直接加入（自己機體的受擊、大招） */
	public static void addDirect(float amount) {
		trauma = Math.min(1f, trauma + amount);
	}

	/** FOV 衝擊：amount 為視野放大比例（0.08 = +8%） */
	public static void fovPunch(float amount) {
		fovVelocity += amount;
	}

	/** 每幀推進噪聲時間與衰減（由鏡頭設定時呼叫） */
	public static void advance() {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;

		if (Minecraft.getInstance().isPaused()) {
			return;
		}

		// 創傷越大頻率越高（猛烈的高頻抖動），小時是緩慢的晃動
		time += dt * (6.0 + trauma * 18.0);
		trauma = Math.max(0f, trauma - DECAY_PER_SECOND * dt);
	}

	private static float shake() {
		return trauma * trauma;
	}

	public static float yaw(float partial) {
		return MAX_YAW * shake() * Perlin.noise(time, 1.3, 0.0);
	}

	public static float pitch(float partial) {
		return MAX_PITCH * shake() * Perlin.noise(time, 7.9, 3.1);
	}

	public static float roll(float partial) {
		return MAX_ROLL * shake() * Perlin.noise(time, 13.7, 9.4);
	}

	/** 鏡頭位置的上下抖動（方塊） */
	public static float bob(float partial) {
		return MAX_OFFSET * shake() * Perlin.noise(time, 4.2, 21.5);
	}

	/** 鏡頭位置的左右抖動（方塊） */
	public static float sway(float partial) {
		return MAX_OFFSET * shake() * Perlin.noise(time, 31.1, 2.2);
	}

	/** FOV 倍率（1 = 不變） */
	public static float fovScale() {
		return 1f + fovKick;
	}

	public static float trauma() {
		return trauma;
	}

	/**
	 * 3D 梯度噪聲（Ken Perlin improved noise），輸出約 -1~1。
	 */
	static final class Perlin {
		private static final int[] P = new int[512];

		static {
			int[] base = new int[256];

			for (int i = 0; i < 256; i++) {
				base[i] = i;
			}

			java.util.Random r = new java.util.Random(1337);

			for (int i = 255; i > 0; i--) {
				int j = r.nextInt(i + 1);
				int t = base[i];
				base[i] = base[j];
				base[j] = t;
			}

			for (int i = 0; i < 512; i++) {
				P[i] = base[i & 255];
			}
		}

		private Perlin() {
		}

		static float noise(double x, double y, double z) {
			int xi = Mth.floor(x) & 255;
			int yi = Mth.floor(y) & 255;
			int zi = Mth.floor(z) & 255;
			double xf = x - Math.floor(x);
			double yf = y - Math.floor(y);
			double zf = z - Math.floor(z);
			double u = fade(xf);
			double v = fade(yf);
			double w = fade(zf);
			int a = P[xi] + yi;
			int aa = P[a] + zi;
			int ab = P[a + 1] + zi;
			int b = P[xi + 1] + yi;
			int ba = P[b] + zi;
			int bb = P[b + 1] + zi;

			double r = lerp(w,
				lerp(v, lerp(u, grad(P[aa], xf, yf, zf), grad(P[ba], xf - 1, yf, zf)), lerp(u, grad(P[ab], xf, yf - 1, zf), grad(P[bb], xf - 1, yf - 1, zf))),
				lerp(v, lerp(u, grad(P[aa + 1], xf, yf, zf - 1), grad(P[ba + 1], xf - 1, yf, zf - 1)),
					lerp(u, grad(P[ab + 1], xf, yf - 1, zf - 1), grad(P[bb + 1], xf - 1, yf - 1, zf - 1))));

			return (float)Mth.clamp(r * 1.4, -1.0, 1.0);
		}

		private static double fade(double t) {
			return t * t * t * (t * (t * 6 - 15) + 10);
		}

		private static double lerp(double t, double a, double b) {
			return a + t * (b - a);
		}

		private static double grad(int hash, double x, double y, double z) {
			int h = hash & 15;
			double u = h < 8 ? x : y;
			double v = h < 4 ? y : h == 12 || h == 14 ? x : z;

			return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
		}
	}
}
