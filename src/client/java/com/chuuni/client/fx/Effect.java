package com.chuuni.client.fx;

import com.chuuni.client.render.Draw;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 一段演出。時間軸以 {@link FxClock} 秒計；{@link #cue(double)} 在時間首次越過某點時回傳 true（排定音效、爆發、名卡）。
 */
public abstract class Effect {
	protected final FxPayload p;
	/** 觸發者是不是自己（自己才有電影鏡頭與名卡） */
	protected final boolean mine;
	protected final BlockPos pos;
	/** 方塊中心 */
	protected final Vec3 c;
	/** 強度倍率（設定的 intensity 與觸發強度綜合） */
	protected final float power;
	private final double start;
	private double last = -1;
	private double now;

	protected Effect(FxPayload p) {
		this.p = p;
		var player = Minecraft.getInstance().player;
		this.mine = player != null && p.actor() == player.getId();
		this.pos = p.pos();
		this.c = Vec3.atCenterOf(pos);
		this.power = Settings.intensity >= 2 ? 1.6f : 1f;
		this.start = FxClock.now();
	}

	public abstract double duration();

	/** 每幀開頭呼叫：處理 cue 與生成粒子 */
	protected abstract void update(double t, double dt);

	/** 世界特效（BEFORE_TRANSLUCENT） */
	protected abstract void render(Draw d, double t);

	/** 提供給後製的光源（世界座標）：照亮地面、在空氣中散射、泛光 */
	protected void lights(java.util.List<com.chuuni.client.post.CinemaPost.Light> lights, java.util.List<com.chuuni.client.post.CinemaPost.Seg> segs,
		Vec3[] rays, float[] raysP) {
	}

	final void collectLights(java.util.List<com.chuuni.client.post.CinemaPost.Light> lights, java.util.List<com.chuuni.client.post.CinemaPost.Seg> segs,
		Vec3[] rays, float[] raysP) {
		lights(lights, segs, rays, raysP);
	}

	/** 浮空物品（BEFORE_ENTITIES，才能送進指令佇列） */
	protected void items(Draw d, double t) {
	}

	final boolean step() {
		now = FxClock.now() - start;
		double dt = last < 0 ? 0 : now - last;
		update(now, dt);
		last = now;
		return now < duration() && !done();
	}

	/** 提早結束（例如跟隨的實體不見了） */
	protected boolean done() {
		return false;
	}

	protected final double time() {
		return now;
	}

	protected boolean cue(double at) {
		return last < at && now >= at;
	}

	/** 生成粒子用：dt 秒內以 rate（每秒）生成的數量（含隨機進位） */
	protected static int rate(double perSecond, double dt) {
		double n = perSecond * dt;
		int k = (int)n;
		return k + (Particles.RNG.nextDouble() < n - k ? 1 : 0);
	}

	protected void shake(float amount) {
		if (Settings.shake <= 0) {
			return;
		}

		if (mine) {
			CameraShake.addDirect(amount * Settings.shake);
		}
		else {
			CameraShake.add(c, amount * 0.6f * Settings.shake);
		}
	}

	/** 演出結束就不回介面：若玩家還開著該類介面就關掉（物品回到背包），直接回到世界 */
	protected static Runnable closeMenu(Class<?> menu) {
		return () -> {
			var mc = Minecraft.getInstance();

			if (mc.player != null && menu.isInstance(mc.player.containerMenu)) {
				mc.player.closeContainer();
			}
		};
	}

	/** 演出結束：先向伺服器領取成品（變成只有自己能撿、會發光的掉落物），再關掉介面回到世界 */
	protected static Runnable claimAndClose(Class<?> menu) {
		Runnable close = closeMenu(menu);
		return () -> {
			if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(com.chuuni.net.ClaimPayload.TYPE)) {
				net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.chuuni.net.ClaimPayload());
			}

			close.run();
		};
	}

	/** 附近（14 格內）或觸發者本人才套用全畫面色調 */
	protected boolean near() {
		var player = Minecraft.getInstance().player;
		return mine || player != null && player.position().distanceTo(c) < 14;
	}

	protected void title(String key, double duration) {
		if (mine && Settings.titlesOn()) {
			Hud.title(key, duration, 1f, 0.3f);
		}
	}

	// ------------------------------------------------------------------ 緩動

	protected static float clamp01(double x) {
		return (float)Mth.clamp(x, 0, 1);
	}

	/** x 在 [a, b] 間 0 → 1 */
	protected static float span(double t, double a, double b) {
		return clamp01((t - a) / (b - a));
	}

	protected static float easeOut(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	protected static float easeIn(float x) {
		return x * x * x;
	}

	protected static float easeOutBack(float x) {
		float c1 = 1.9f;
		float c3 = c1 + 1;
		return 1 + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
	}

	/** 淡入 a 秒、持續到 end 前 b 秒開始淡出 */
	protected static float fade(double t, double in, double outStart, double outEnd) {
		return Math.min(span(t, 0, in), 1 - span(t, outStart, outEnd));
	}

	protected static float r(int rgb) {
		return (rgb >> 16 & 255) / 255f;
	}

	protected static float g(int rgb) {
		return (rgb >> 8 & 255) / 255f;
	}

	protected static float b(int rgb) {
		return (rgb & 255) / 255f;
	}
}
