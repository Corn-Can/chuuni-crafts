package com.chuuni.client.fx;

import net.minecraft.client.Minecraft;

/**
 * 特效專用時鐘（秒）：以真實時間推進，可「頓幀」（完全凍結）與「慢動作」。
 * 所有演出都讀這個時間，所以命中瞬間整個畫面的特效會一起停住，再一起慢慢流動。
 */
public final class FxClock {
	private static long last = System.nanoTime();
	private static double time;
	private static double frameDt;
	private static long hitstopUntil;
	private static long slowUntil;
	private static float slowScale = 1f;

	private FxClock() {
	}

	/** 每幀呼叫一次（由 Effects 在渲染開頭呼叫） */
	public static void frame() {
		long now = System.nanoTime();
		double dt = Math.min(0.1, (now - last) / 1.0e9);
		last = now;

		if (Minecraft.getInstance().isPaused() || now < hitstopUntil) {
			frameDt = 0;
			return;
		}

		frameDt = dt * (now < slowUntil ? slowScale : 1f);
		time += frameDt;
	}

	public static double now() {
		return time;
	}

	public static double dt() {
		return frameDt;
	}

	public static void hitstop(int millis) {
		hitstopUntil = Math.max(hitstopUntil, System.nanoTime() + millis * 1_000_000L);
	}

	public static void slowmo(float scale, int millis) {
		slowScale = scale;
		slowUntil = Math.max(slowUntil, System.nanoTime() + millis * 1_000_000L);
	}

	public static boolean frozen() {
		return System.nanoTime() < hitstopUntil;
	}
}
