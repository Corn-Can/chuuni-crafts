package com.chuuni.client.fx;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 動畫分鏡式運鏡：一串鏡頭（Shot），鏡頭之間是「硬切」，每個鏡頭內做推軌 / 搖鏡，可帶荷蘭角（roll）與焦段（fov 倍率）。
 * 只有第一個鏡頭會從玩家的視角平滑飛出去；結束時硬切回玩家視角並閃一下。
 * 演出期間介面隱藏、HUD 換成黑邊；按任意鍵跳過。
 */
public final class Cinematic {
	private static final double BLEND_IN = 0.3;

	/**
	 * @param dur  鏡頭長度（秒）
	 * @param from 起點、to 終點（世界座標）
	 * @param lookFrom 看向的點（起）、lookTo（終）
	 * @param roll 荷蘭角（度，起 → 終）
	 * @param fov  焦段倍率（1 = 原本，&lt;1 = 望遠特寫）
	 */
	public record Shot(double dur, Vec3 from, Vec3 to, Vec3 lookFrom, Vec3 lookTo, float roll0, float roll1, float fov0, float fov1, boolean easeIn) {
		public static Shot of(double dur, Vec3 from, Vec3 to, Vec3 look) {
			return new Shot(dur, from, to, look, look, 0, 0, 1, 1, false);
		}

		public Shot look(Vec3 a, Vec3 b) {
			return new Shot(dur, from, to, a, b, roll0, roll1, fov0, fov1, easeIn);
		}

		public Shot roll(float a, float b) {
			return new Shot(dur, from, to, lookFrom, lookTo, a, b, fov0, fov1, easeIn);
		}

		public Shot fov(float a, float b) {
			return new Shot(dur, from, to, lookFrom, lookTo, roll0, roll1, a, b, easeIn);
		}

		/** 加速運鏡（衝進去），預設是減速（滑進去停住） */
		public Shot accelerate() {
			return new Shot(dur, from, to, lookFrom, lookTo, roll0, roll1, fov0, fov1, true);
		}
	}

	/** 以方塊為中心、面向玩家的座標系：right / up / front（front = 方塊 → 玩家） */
	public record Frame(Vec3 origin, Vec3 right, Vec3 front) {
		public static Frame facingPlayer(Vec3 origin) {
			float a = (float)Math.toRadians(yawToward(origin));
			Vec3 front = new Vec3(Mth.sin(a), 0, Mth.cos(a));
			Vec3 right = new Vec3(front.z, 0, -front.x);
			return new Frame(origin, right, front);
		}

		public Vec3 at(double r, double u, double f) {
			return origin.add(right.scale(r)).add(0, u, 0).add(front.scale(f));
		}
	}

	private static final List<Shot> SHOTS = new ArrayList<>();
	private static double start;
	private static double total;
	private static double returnedAt = -100;
	private static float roll;
	private static float fov = 1;

	private Cinematic() {
	}

	private static Runnable onEnd;
	private static boolean showSelf;

	/** 這段運鏡要拍到自己（例如睡覺）：不隱藏自己的身體 */
	public static void showSelf() {
		showSelf = true;
	}

	public static boolean hidesSelf() {
		return active() && !showSelf;
	}

	public static void play(List<Shot> shots) {
		play(shots, null);
	}

	/** onEnd：演出結束或被跳過時執行一次（例如關掉介面、不回到 UI） */
	public static void play(List<Shot> shots, Runnable whenDone) {
		if (shots.isEmpty()) {
			return;
		}

		if (!Settings.cinematicOn()) {
			// 不運鏡：鏡頭不動、介面不收，但演出結束時要做的事（領取成品、關介面）照樣在同一時間點執行
			if (whenDone != null) {
				deferred = whenDone;
				deferredAt = FxClock.now() + shots.stream().mapToDouble(Shot::dur).sum();
			}

			return;
		}

		onEnd = whenDone;
		showSelf = false;
		// 運鏡開始：把目前按著的鍵全部放開（走路、右鍵…），避免演出中角色一直移動 / 一直互動
		net.minecraft.client.KeyMapping.releaseAll();

		SHOTS.clear();
		SHOTS.addAll(shots);
		start = FxClock.now();
		total = shots.stream().mapToDouble(Shot::dur).sum();
	}

	/** 舊式環繞運鏡（尚未改成分鏡的演出使用）：拆成 4 段直線近似圓弧 */
	public static void orbit(Vec3 center, float yaw0, float sweep, float r0, float r1, float h0, float h1, float lookY, double seconds) {
		List<Shot> shots = new ArrayList<>();
		Vec3 look = center.add(0, lookY, 0);
		int n = 4;

		for (int i = 0; i < n; i++) {
			shots.add(Shot.of(seconds / n, orbitPoint(center, yaw0 + sweep * i / n, Mth.lerp((float)i / n, r0, r1), Mth.lerp((float)i / n, h0, h1)),
				orbitPoint(center, yaw0 + sweep * (i + 1) / n, Mth.lerp((float)(i + 1) / n, r0, r1), Mth.lerp((float)(i + 1) / n, h0, h1)), look));
		}

		play(shots);
	}

	private static Vec3 orbitPoint(Vec3 c, float yawDeg, float r, float h) {
		double a = Math.toRadians(yawDeg);
		return new Vec3(c.x + Math.sin(a) * r, c.y + h, c.z + Math.cos(a) * r);
	}

	public static boolean active() {
		if (SHOTS.isEmpty()) {
			return false;
		}

		if (FxClock.now() - start >= total) {
			end();
			return false;
		}

		return true;
	}

	private static Runnable deferred;
	private static double deferredAt;

	/** 每幀：不運鏡時延後執行的結尾動作 */
	public static void tickDeferred() {
		if (deferred != null && FxClock.now() >= deferredAt) {
			Runnable r = deferred;
			deferred = null;
			r.run();
		}
	}

	private static double peekUntil = -1;

	/** 不動鏡頭，只把介面暫時收起 seconds 秒（B 級的切石機 / 織布機：讓玩家看到世界中的短演出，之後介面自己回來） */
	public static void peek(double seconds) {
		peekUntil = FxClock.now() + seconds;
	}

	public static boolean hidesScreen() {
		return active() || FxClock.now() < peekUntil;
	}

	public static void skip() {
		if (!SHOTS.isEmpty()) {
			end();
		}
	}

	private static void end() {
		SHOTS.clear();
		returnedAt = FxClock.now();
		roll = 0;
		fov = 1;
		Hud.flash(0xFFFFFF, 0.3f, 0.2);
		Runnable r = onEnd;
		onEnd = null;

		if (r != null) {
			r.run();
		}
	}

	public static float letterbox() {
		if (!active()) {
			return 0f;
		}

		double t = FxClock.now() - start;
		return (float)Mth.clamp(Math.min(t / 0.2, (total - t) / 0.15), 0, 1);
	}

	public static double elapsed() {
		return active() ? FxClock.now() - start : -1;
	}

	public static float roll() {
		return active() ? roll : 0f;
	}

	public static float fovScale() {
		return active() ? fov : 1f;
	}

	/** 回傳 {x, y, z, yaw, pitch, 第三人稱}，不在演出中回傳 null */
	public static double[] pose(Vec3 origPos, float origYaw, float origPitch) {
		if (!active()) {
			return null;
		}

		double t = FxClock.now() - start;
		double acc = 0;
		Shot s = SHOTS.getLast();
		int index = SHOTS.size() - 1;

		for (int i = 0; i < SHOTS.size(); i++) {
			if (t < acc + SHOTS.get(i).dur) {
				s = SHOTS.get(i);
				index = i;
				break;
			}

			acc += SHOTS.get(i).dur;
		}

		double k = Mth.clamp((t - acc) / s.dur, 0, 1);
		double e = s.easeIn ? k * k * k : 1 - Math.pow(1 - k, 2.4);
		Vec3 pos = s.from.lerp(s.to, e);
		Vec3 look = s.lookFrom.lerp(s.lookTo, e);
		roll = Mth.lerp((float)e, s.roll0, s.roll1);
		fov = Mth.lerp((float)e, s.fov0, s.fov1);
		Vec3 d = look.subtract(pos);
		float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float)-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));

		// 第一個鏡頭：從玩家眼睛飛出去
		if (index == 0 && t < BLEND_IN) {
			double b = t / BLEND_IN;
			b = b * b * (3 - 2 * b);
			pos = origPos.lerp(pos, b);
			yaw = origYaw + Mth.wrapDegrees(yaw - origYaw) * (float)b;
			pitch = Mth.lerp((float)b, origPitch, pitch);
			roll *= (float)b;
			fov = Mth.lerp((float)b, 1f, fov);
		}

		boolean away = pos.distanceTo(origPos) > 0.9;
		return new double[]{pos.x, pos.y, pos.z, yaw, pitch, away ? 1 : 0};
	}

	/** 方塊中心 → 玩家 的水平角度（度）：(sin a, cos a) = (x, z) */
	public static float yawToward(Vec3 center) {
		var p = Minecraft.getInstance().player;

		if (p == null) {
			return 0f;
		}

		return (float)Math.toDegrees(Math.atan2(p.getX() - center.x, p.getZ() - center.z));
	}
}
