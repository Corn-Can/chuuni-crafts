package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Effect;
import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxRender;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 專屬掉落物的發光提示：跟著物品實體，直到被撿走為止。
 * 柔和的光柱 + 地面光環 + 緩慢上升的光點 + 頭頂旋轉的光環；觸發者本人看得比較亮，其他人淡一些（「這不是你的」）。
 */
public class RewardFx extends Effect {
	private static final Identifier BEAM = FxRender.tex("beam");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");

	private final int entityId;
	private final int color;
	private final float strength;
	private Vec3 last;

	public RewardFx(FxPayload p) {
		super(p);
		entityId = p.power();
		color = p.extra();
		strength = mine ? 1f : 0.45f;
		last = Vec3.atBottomCenterOf(pos).add(0, 1, 0);
	}

	@Override
	public double duration() {
		// 物品是無限壽命的，演出跟著它；保險起見 30 分鐘後自動結束
		return 1800;
	}

	private Entity entity() {
		var level = Minecraft.getInstance().level;
		return level == null ? null : level.getEntity(entityId);
	}

	@Override
	protected boolean done() {
		Entity e = entity();
		// 剛生成的頭幾秒實體可能還沒同步到客戶端
		return time() > 3 && (e == null || e.isRemoved());
	}

	private Vec3 where(float partial) {
		Entity e = entity();

		if (e != null && !e.isRemoved()) {
			last = e.getPosition(partial);
		}

		return last;
	}

	@Override
	protected void update(double t, double dt) {
		Vec3 at = where(1f);
		int n = rate(6 * strength, dt);

		for (int i = 0; i < n; i++) {
			Vec3 from = at.add(Particles.randomDir(0.5)).add(0, Particles.rand(-0.1f, 0.3f), 0);
			Particles.spawn(Kind.FLARE, from, new Vec3(0, Particles.rand(0.2f, 0.5f), 0), 1.6).size(0.08f, 0.01f).color(0xFFFFFF, color).physics(0, 0.3f)
				.fadeIn(0.3f).spin(1f);
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		lights.add(new CinemaPost.Light(last.add(0, 0.3, 0), 2f, r(color), g(color), b(color), 0.35f * strength * span(time(), 0, 1), false));
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 at = where(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		float in = span(t, 0, 1.0) * strength;
		float breathe = 0.8f + 0.2f * (float)Math.sin(t * 2.4);
		float scroll = (float)(t * 0.3);
		// 光柱（越往上越淡）
		d.cylinder(Mode.GLOW, BEAM, at, 0.32f, 5f, 20, 1, 0, 2, -scroll, 1.3f, r(color), g(color), b(color), 0.22f * in * breathe, 0);
		d.cylinder(Mode.GLOW, BEAM, at, 0.1f, 4f, 12, 1, 0.4f, 2, -scroll * 1.5f, 1.0f, 1f, 1f, 1f, 0.25f * in * breathe, 0);
		// 地面光環與物品光暈
		d.flat(Mode.GLOW, GLOW, at.add(0, 0.02, 0), 0.7f * breathe, 0, r(color), g(color), b(color), 0.4f * in);
		d.flat(Mode.GLOW, RING, at.add(0, 0.03, 0), 0.45f + 0.08f * (float)Math.sin(t * 2.4), (float)t * 0.4f, r(color), g(color), b(color), 0.6f * in);
		d.billboard(Mode.GLOW, GLOW, at.x, at.y + 0.25, at.z, 0.55f * breathe, 0, r(color), g(color), b(color), 0.3f * in);
		d.billboard(Mode.GLOW, FLARE, at.x, at.y + 0.25, at.z, 0.6f * breathe, (float)t * 0.3f, 1f, 1f, 1f, 0.25f * in);
		// 頭頂旋轉的小光環
		d.flat(Mode.GLOW, RING, at.add(0, 0.75 + 0.05 * Math.sin(t * 2), 0), 0.22f, (float)t, r(color), g(color), b(color), 0.7f * in);
	}
}
