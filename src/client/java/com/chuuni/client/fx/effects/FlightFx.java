package com.chuuni.client.fx.effects;

import java.util.ArrayDeque;
import java.util.List;

import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 跟著飛行物（extra = 實體 id）的演出骨架：記錄飛行軌跡畫成光帶，物體消失（落地 / 碎裂）那一刻呼叫 {@link #land(Vec3)}。
 */
public abstract class FlightFx extends MinorFx {
	private record Pt(Vec3 at, double t) {
	}

	private final ArrayDeque<Pt> trail = new ArrayDeque<>();
	/** 飛行物目前（或最後）的位置 */
	protected Vec3 head;
	/** 落地的時間（-1 = 還在飛） */
	protected double landT = -1;
	/** 軌跡保留幾秒 */
	protected double trailLife = 0.4;
	/** 落地後演多久 */
	protected double after = 1.2;

	protected FlightFx(FxPayload p) {
		super(p);
		head = p.at();
		end = 20;
	}

	@Override
	protected boolean done() {
		return landT >= 0 && time() > landT + after;
	}

	protected boolean flying() {
		return landT < 0;
	}

	/** 實際畫面上的位置（插值，飛得快也不會一格一格跳） */
	private Vec3 live() {
		Entity e = target();

		if (e == null || e.isRemoved()) {
			return null;
		}

		float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		return e.getPosition(pt).add(0, e.getBbHeight() * 0.5, 0);
	}

	@Override
	protected void update(double t, double dt) {
		if (landT < 0) {
			Vec3 now = live();

			if (now == null || stopped(t, now)) {
				landT = t;
				land(now == null ? head : now);
			}
			else {
				head = now;
				trail.addLast(new Pt(now, t));
				fly(t, dt, now);
			}
		}

		while (!trail.isEmpty() && t - trail.peekFirst().t > trailLife) {
			trail.removeFirst();
		}

		after(t, dt);
	}

	/** 飛行物還在、但已經停下來了（插在地上的箭、浮在水上的浮標）→ 也算落地 */
	protected boolean stopped(double t, Vec3 now) {
		return false;
	}

	/** 飛行中每幀 */
	protected abstract void fly(double t, double dt, Vec3 at);

	/** 消失的那一刻 */
	protected abstract void land(Vec3 at);

	/** 每幀（含落地後） */
	protected void after(double t, double dt) {
	}

	/** 軌跡光帶：越舊越細越淡；core = 中心白線 */
	protected void ribbon(Draw d, double t, float width, int col, float alpha) {
		Pt prev = null;

		for (Pt p : trail) {
			if (prev != null) {
				float k = 1 - (float)((t - p.t) / trailLife);
				// 貼近鏡頭的那一段（剛從自己手上丟出去）在透視下會被拉成巨大的光錐 → 4 格內淡掉
				k *= span(Math.min(prev.at.distanceTo(d.cam), p.at.distanceTo(d.cam)), 1.5, 4);
				float w = width * (0.2f + 0.8f * k);
				line(d, TRAIL, prev.at, p.at, w, col, alpha * k);
				line(d, SPARK, prev.at, p.at, w * 0.35f, 0xFFFFFF, alpha * k * 0.8f);
			}

			prev = p;
		}
	}

	protected void headLight(List<CinemaPost.Light> lights, int col, float intensity) {
		if (landT < 0) {
			lights.add(new CinemaPost.Light(head, 3f, r(col), g(col), b(col), intensity, true));
		}
	}
}
