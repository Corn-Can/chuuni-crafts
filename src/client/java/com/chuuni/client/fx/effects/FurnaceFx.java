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

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 獄炎煉成（熔爐）／蒼焰熔斷（高爐）／煙雲祕法（煙燻爐）。
 * <ul>
 *   <li>每煉好一件（FURNACE_SMELT）：爐口吐出一團火，火之精靈探頭繞一圈（無鏡頭、無招式名）</li>
 *   <li>取出成品（FURNACE_TAKE）——打架動漫分鏡（一次取 16 件以上：漫畫分格時停 + 四角大字）：
 * <pre>
 * 鏡1 0.00 點火  貼地仰拍爐口：爐門噴出烈焰
 * 鏡2 1.00 盤旋  廣角：兩隻火之精靈以雙螺旋繞爐上升，腳下火柱環
 * 鏡3 2.20 吸氣  兩隻精靈會合點極近特寫、荷蘭角：黑色集中線
 * 衝擊 2.55     精靈相撞爆炎：衝擊幀 + 頓幀 + 折射衝擊波 + RGB 分離，毛筆直書招式名
 * 鏡4 2.55 英雄  低角度仰望（慢動作）：成品在烈焰中現身
 * 鏡5 4.00 餘燼  火勢退去成飄散的餘燼，成品緩緩降到爐頂 → 直接回到世界
 * </pre></li>
 * </ul>
 */
public class FurnaceFx extends Effect {
	private static final Identifier CIRCLE = FxRender.tex("circle_enchant");
	private static final Identifier BAND = FxRender.tex("rune_band");
	private static final Identifier BEAM = FxRender.tex("beam");
	private static final Identifier RING = FxRender.tex("ring");
	private static final Identifier JAG = FxRender.tex("jag_ring");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier FLARE = FxRender.tex("flare");
	private static final Identifier TRAIL = FxRender.tex("slash_trail");
	private static final Identifier SMOKE = FxRender.tex("smoke");

	/** 每個變種：火焰起色、火焰末色、魔法陣色 */
	private static final int[][] PALETTE = {
		{0xFFF0B0, 0xFF3A08, 0xFF6A20},
		{0xE8F6FF, 0x2A60FF, 0x50A0FF},
		{0xFFD8A0, 0x7A4A3A, 0xE0A060},
	};
	private static final String[] TITLES = {"furnace", "blast", "smoker"};
	private static final int[] IMPACTS = {PostState.EMBER, PostState.FROST, PostState.INK};

	private static final double S2 = 1.0;
	private static final double S3 = 2.2;
	private static final double BURST = 2.55;
	private static final double S5 = 4.0;
	private static final double TAKE_DURATION = 5.6;
	private static final int MANGA_MS = 1100;

	private final boolean take;
	private final boolean big;
	private final int variant;
	private final int[] pal;
	private final Vec3 front;
	private final Vec3 out;
	private final Vec3 apex;
	private final double duration;

	public FurnaceFx(FxPayload p) {
		super(p);
		take = p.kind() == FxPayload.Kind.FURNACE_TAKE;
		variant = Math.floorMod(p.variant(), 3);
		big = Settings.intensity >= 2 || p.power() >= 16;
		pal = PALETTE[variant];
		Direction facing = Direction.NORTH;
		BlockState state = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockState(pos);

		if (state != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}

		out = new Vec3(facing.getStepX(), 0, facing.getStepZ());
		front = c.add(out.scale(0.52)).add(0, -0.1, 0);
		apex = new Vec3(c.x, pos.getY() + 1.9, c.z);
		duration = take ? TAKE_DURATION : 1.3;

		if (take && near()) {
			PostState.look(TITLES[variant], 350, (int)(S5 * 1000));
		}

		if (take && mine) {
			Frame f = Frame.facingPlayer(new Vec3(c.x, pos.getY(), c.z));
			Vec3 mouth = front.add(out.scale(0.2));
			Cinematic.play(List.of(
				Shot.of(S2, f.at(1.5, 0.35, 2.9), f.at(1.15, 0.42, 2.45), mouth).look(mouth, mouth.add(0, 0.4, 0)).roll(6, 9).fov(0.9f, 0.82f),
				Shot.of(S3 - S2, f.at(-2.4, 1.3, 2.6), f.at(-1.4, 2.3, 3.0), f.at(0, 1.2, 0)).look(f.at(0, 1.0, 0), f.at(0, 1.6, 0)).roll(-3, -5),
				Shot.of(BURST - S3, f.at(0.2, 2.1, 1.6), f.at(0.12, 2.04, 1.25), apex).roll(-9, -13).fov(0.7f, 0.6f).accelerate(),
				Shot.of(S5 - BURST, f.at(1.5, 0.3, 2.3), f.at(1.9, 0.38, 2.9), apex.add(0, 0.4, 0)).look(apex.add(0, 0.4, 0), apex).roll(12, 9).fov(1.15f, 1.08f),
				Shot.of(TAKE_DURATION - S5, f.at(-0.9, 1.75, 2.2), f.at(-0.6, 1.6, 1.7), apex).look(apex, itemAt(TAKE_DURATION)).roll(-4, -1).fov(0.9f, 0.82f)
			), claimAndClose(AbstractFurnaceMenu.class));
		}
	}

	@Override
	public double duration() {
		return duration;
	}

	/** 成品：在爆炎中現身 → 餘燼中緩緩降到爐頂 */
	private Vec3 itemAt(double t) {
		float d = span(t, S5, TAKE_DURATION - 0.3);
		d = d * d * (3 - 2 * d);
		return new Vec3(c.x, apex.y + 0.1 - d * 0.75 + Math.sin(t * 2.5) * 0.04 * (1 - d), c.z);
	}

	/** 第 i 隻精靈的位置 */
	private Vec3 spirit(int i, double t) {
		if (!take) {
			double k = span(t, 0.05, 1.1);
			double ang = k * Math.PI * 2;
			Vec3 o = front.add(out.scale(0.4 + Math.sin(k * Math.PI) * 0.4));
			return o.add(Math.cos(ang) * 0.35, k * 0.9, Math.sin(ang) * 0.35);
		}

		double k = span(t, 0.5, BURST);
		double ang = i * Math.PI + k * Math.PI * 4.2;
		double r = 1.4 * (1 - easeIn((float)k) * 0.95);
		double y = pos.getY() + 0.3 + easeOut((float)k) * (apex.y - pos.getY() - 0.3);
		Vec3 start = front.add(out.scale(0.7));
		Vec3 orbit = new Vec3(c.x + Math.cos(ang) * r, y, c.z + Math.sin(ang) * r);
		return start.lerp(orbit, span(t, 0.45, 0.85));
	}

	@Override
	protected void update(double t, double dt) {
		if (!take) {
			smelt(t, dt);
			return;
		}

		float pitch = variant == 1 ? 1.12f : variant == 2 ? 0.9f : 1f;

		if (cue(0.0)) {
			Sfx.play(mine, "fire_ignite", c, 1f, pitch);
		}

		if (cue(0.85)) {
			Sfx.play(mine, variant == 0 ? "fire_roar" : variant == 1 ? "plasma_charge" : "smoke_swirl", c, 1f, variant == 0 ? 1f : 1f);
		}

		// 爐口噴焰
		if (t < 1.0) {
			int n = rate(170 * (1 - span(t, 0.5, 1.0)), dt);

			for (int i = 0; i < n; i++) {
				Vec3 v = out.scale(Particles.rand(1.5f, 5f)).add(Particles.randomDir(0.6)).add(0, Particles.rand(0.5f, 1.5f), 0);
				Particles.spawn(Kind.FLAME, front.add(Particles.randomDir(0.15)), v, Particles.rand(0.4f, 0.9f))
					.size(Particles.rand(0.2f, 0.4f), 0.05f).color(pal[0], pal[1]).physics(-1.5f, 2.5f).fadeIn(0.05f);
			}
		}

		if (variant == 1) {
			bladeParticles(t, dt);
		}
		else if (variant == 2) {
			serpentParticles(t, dt);
		}

		// 精靈的火焰尾跡（熔爐）
		if (variant == 0 && t > 0.4 && t < S3 - 0.5) {
			for (int s = 0; s < 2; s++) {
				int n = rate(90, dt);
				Vec3 at = spirit(s, t);

				for (int i = 0; i < n; i++) {
					Particles.spawn(Kind.FLAME, at.add(Particles.randomDir(0.08)), Particles.randomDir(0.3).add(0, 0.6, 0), Particles.rand(0.3f, 0.6f))
						.size(Particles.rand(0.14f, 0.26f), 0.02f).color(pal[0], pal[1]).physics(-0.8f, 2f);
				}
			}
		}

		// ---------------------------------------------------------- 鏡3：吸氣
		if (cue(S3)) {
			if (near()) {
				PostState.darken(0.35f);
			}

			if (mine) {
				PostState.lines(0.8f, true, (int)((BURST - S3) * 1000) + 30, 0.32f);
			}
		}

		if (t > S3 && t < BURST) {
			int n = rate(130, dt);

			for (int i = 0; i < n; i++) {
				Vec3 from = apex.add(Particles.randomDir(1.5 + Particles.RNG.nextDouble()));
				Particles.spawn(Kind.SPARK, from, apex.subtract(from).scale(3), 0.3).size(0.035f, 0.01f).color(0xFFFFFF, pal[1]).physics(0, 0).attract(apex, 30f);
			}
		}

		if (cue(BURST - 0.35)) {
			Sfx.play(mine, variant == 0 ? "inferno_burst" : variant == 1 ? "plasma_cut" : "smoke_gong", c, 1.3f, 1f);
		}

		// ---------------------------------------------------------- 衝擊
		if (cue(BURST)) {
			burst();
		}

		if (big && cue(BURST + 0.07) && mine && Settings.titlesOn()) {
			FxClock.hitstop(MANGA_MS);
			PostState.manga(MANGA_MS);
			PostState.lines(1f, true, MANGA_MS, 0.3f);
			Hud.mangaName(TITLES[variant], MANGA_MS / 1000.0);
		}

		if (cue(BURST + 0.08)) {
			FxClock.slowmo(0.35f, 1100);

			if (mine) {
				PostState.lines(0.75f, false, 1000, 0.42f);
				PostState.streak(1f, 900);
				PostState.zoom(0.7f, 450);
			}
		}

		if (cue(BURST + 0.3) && mine && Settings.titlesOn()) {
			Hud.brushName(TITLES[variant], 3.0);
		}

		// 成品四周持續的火焰
		if (t > BURST && t < S5 + 0.3) {
			float k = 1 - span(t, BURST + 0.5, S5 + 0.3);
			int n = rate(60 * k, dt);
			Vec3 item = itemAt(t);

			for (int i = 0; i < n; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 at = item.add(Math.cos(a) * 0.55, -0.35, Math.sin(a) * 0.55);
				Particles.spawn(Kind.FLAME, at, new Vec3(-Math.cos(a) * 0.3, Particles.rand(1f, 2f), -Math.sin(a) * 0.3), Particles.rand(0.4f, 0.7f))
					.size(Particles.rand(0.15f, 0.28f), 0.03f).color(pal[0], pal[1]).physics(-1f, 2f);
			}
		}

		// ---------------------------------------------------------- 鏡5：餘燼
		if (cue(S5 - 0.1)) {
			Sfx.play(mine, variant == 0 ? "ember_settle" : variant == 1 ? "steam_cool" : "incense_settle", c, 1f, 1f);

			if (near()) {
				PostState.look(TITLES[variant], 900, (int)((TAKE_DURATION - S5) * 400));
			}
		}

		if (t > S5 - 0.3 && variant == 1) {
			// 高爐：熔鋼冷卻的蒸氣
			int n = rate(10, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = itemAt(t).add(Particles.rand(-0.3f, 0.3f), -0.2, Particles.rand(-0.3f, 0.3f));
				Particles.spawn(Kind.SMOKE, at, new Vec3(Particles.rand(-0.2f, 0.2f), Particles.rand(0.6f, 1.2f), Particles.rand(-0.2f, 0.2f)), 1.8)
					.size(0.2f, 1.0f).color(0xD8E4F0, 0x8090A0).alpha(0.35f).physics(-0.2f, 1f).spin(0.5f);
			}
		}

		if (t > S5 - 0.3 && variant == 2) {
			// 煙燻爐：細細的香煙裊裊上升、捲曲
			int n = rate(14, dt);

			for (int i = 0; i < n; i++) {
				double a = t * 1.5 + i;
				Vec3 at = itemAt(t).add(Math.cos(a) * 0.35, -0.3, Math.sin(a) * 0.35);
				Particles.spawn(Kind.SMOKE, at, new Vec3(-Math.sin(a) * 0.3, Particles.rand(0.4f, 0.8f), Math.cos(a) * 0.3), 2.4)
					.size(0.12f, 0.7f).color(0xE8D0B0, 0x806070).alpha(0.4f).physics(-0.05f, 0.4f).spin(1.2f);
			}
		}

		if (t > S5 - 0.3 && variant == 0) {
			int n = rate(30, dt);

			for (int i = 0; i < n; i++) {
				Vec3 at = c.add(Particles.rand(-1.4f, 1.4f), Particles.rand(0.2f, 1.8f), Particles.rand(-1.4f, 1.4f));
				Particles.spawn(Kind.GLOW, at, new Vec3(Particles.rand(-0.2f, 0.2f), Particles.rand(0.3f, 0.9f), Particles.rand(-0.2f, 0.2f)), 1.6)
					.size(0.04f, 0.01f).color(pal[0], pal[1]).physics(-0.15f, 0.4f).fadeIn(0.2f);
			}

			int m = rate(6, dt);

			for (int i = 0; i < m; i++) {
				Vec3 at = c.add(Particles.rand(-0.5f, 0.5f), 0.6, Particles.rand(-0.5f, 0.5f));
				Particles.spawn(Kind.SMOKE, at, new Vec3(0, Particles.rand(0.4f, 0.9f), 0), 2.2).size(0.3f, 1.2f).color(0x504840, 0x302820).alpha(0.35f)
					.physics(-0.1f, 0.8f).spin(0.4f);
			}
		}

		if (cue(TAKE_DURATION - 0.5) && mine) {
			Hud.whiteout(pal[0], 0.45, 0.9);
		}
	}

	/** 高爐：四片蒼焰電漿刃繞著爐子旋轉、收攏、上升（角度 / 半徑 / 高度） */
	private double bladeAngle(int i, double t) {
		double k = span(t, 0.5, BURST);
		return i * Math.PI / 2 + k * k * Math.PI * 7 + t * 2;
	}

	private double bladeRadius(double t) {
		return 1.5 * (1 - easeIn(span(t, 0.6, BURST)) * 0.85) * easeOut(span(t, 0.4, 0.8));
	}

	private double bladeHeight(double t) {
		return pos.getY() + 0.4 + easeOut(span(t, 0.5, BURST)) * (apex.y - pos.getY() - 0.4);
	}

	private void bladeParticles(double t, double dt) {
		if (t < 0.5 || t > S3 - 0.3) {
			return;
		}

		double r = bladeRadius(t);
		double y = bladeHeight(t);

		for (int i = 0; i < 4; i++) {
			double a = bladeAngle(i, t);
			Vec3 tip = new Vec3(c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r);
			Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a));
			int n = rate(30, dt);

			for (int k = 0; k < n; k++) {
				// 被甩出的電漿火花
				Particles.spawn(Kind.SPARK, tip, tangent.scale(Particles.rand(2f, 4f)).add(Particles.randomDir(0.6)), 0.35).size(0.03f, 0.008f)
					.color(0xFFFFFF, pal[1]).physics(1f, 2f);
			}

			if (Particles.RNG.nextDouble() < dt * 10) {
				// 熔鋼滴落
				Particles.spawn(Kind.SPARK, tip, new Vec3(0, -0.5, 0), 0.9).size(0.05f, 0.03f).color(0xFFFFFF, 0x80C0FF).physics(9f, 0.2f);
			}
		}
	}

	/** 煙燻爐：一條煙龍沿螺旋繞爐上升（頭的位置） */
	private Vec3 serpent(double t, double lag) {
		double k = span(t - lag, 0.4, BURST);
		double a = k * Math.PI * 4.5;
		double r = 1.3 * (1 - easeIn((float)k) * 0.9);
		double y = pos.getY() + 0.2 + easeOut((float)k) * (apex.y - pos.getY() - 0.2);
		return new Vec3(c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r);
	}

	private void serpentParticles(double t, double dt) {
		if (t < 0.4 || t > S3 - 0.4) {
			return;
		}

		Vec3 head = serpent(t, 0);
		int n = rate(45, dt);

		for (int i = 0; i < n; i++) {
			Particles.spawn(Kind.SMOKE, head.add(Particles.randomDir(0.12)), Particles.randomDir(0.15).add(0, 0.15, 0), Particles.rand(0.9f, 1.4f))
				.size(0.22f, 0.6f).color(0xC8A890, 0x5A4050).alpha(0.55f).physics(-0.1f, 1.2f).spin(0.8f);
		}

		if (Particles.RNG.nextDouble() < dt * 20) {
			Particles.spawn(Kind.GLOW, head, Particles.randomDir(0.3), 0.6).size(0.06f, 0.01f).color(pal[0], pal[2]).physics(0, 1f);
		}
	}

	private void smelt(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, "fire_puff", c, 0.7f, variant == 1 ? 1.15f : 1f);
		}

		if (t < 0.35) {
			int n = rate(50, dt);

			for (int i = 0; i < n; i++) {
				Vec3 v = out.scale(Particles.rand(1.5f, 2.5f)).add(Particles.randomDir(0.6)).add(0, Particles.rand(0.5f, 1.5f), 0);
				Particles.spawn(Kind.FLAME, front.add(Particles.randomDir(0.15)), v, Particles.rand(0.4f, 0.8f))
					.size(Particles.rand(0.18f, 0.35f), 0.05f).color(pal[0], pal[1]).physics(-1.5f, 2.5f).fadeIn(0.05f);
			}
		}

		if (t > 0.15 && t < 1.15) {
			int n = rate(35, dt);
			Vec3 at = spirit(0, t);

			for (int i = 0; i < n; i++) {
				Particles.spawn(Kind.FLAME, at.add(Particles.randomDir(0.08)), Particles.randomDir(0.3).add(0, 0.6, 0), Particles.rand(0.3f, 0.6f))
					.size(Particles.rand(0.12f, 0.22f), 0.02f).color(pal[0], pal[1]).physics(-0.8f, 2f);
			}
		}
	}

	private void burst() {
		FxClock.hitstop(150);
		shake(0.8f);

		if (near()) {
			PostState.darken(0);
			PostState.flash(pal[0], 0.45f, 160, mine ? 0.12f : 0.05f);
		}

		if (mine) {
			PostState.impact(IMPACTS[variant], 150);
			PostState.shock(1.1f, 650);
			PostState.split(1.8f, 0.4f, 450);
			CameraShake.fovPunch(0.12f);

			if (p.power() > 1) {
				Hud.combo(p.power(), pal[0], "ITEMS");
			}
		}

		Sfx.play(mine, "shockwave", c, 1f, 0.85f);
		Particles.spawn(Kind.HIT, apex, Vec3.ZERO, 0.38).size(1.5f, 2.2f).color(0xFFFFFF, pal[0]);

		if (variant == 1) {
			// 熔斷：熔鋼往下噴灑
			for (int i = 0; i < 90; i++) {
				Vec3 vel = Particles.randomDir(Particles.rand(2f, 6f)).add(0, 2, 0);
				Particles.spawn(Kind.SPARK, apex, vel, Particles.rand(0.6f, 1.2f)).size(0.06f, 0.03f).color(0xFFFFFF, 0x70B0FF).physics(10f, 0.3f);
			}
		}
		else if (variant == 2) {
			// 煙幕：一圈濃煙水平炸開
			for (int i = 0; i < 70; i++) {
				double a = Particles.RNG.nextDouble() * Math.PI * 2;
				Vec3 vel = new Vec3(Math.cos(a), Particles.rand(-0.1f, 0.3f), Math.sin(a)).scale(Particles.rand(2.5f, 5f));
				Particles.spawn(Kind.SMOKE, apex.add(0, -0.3, 0), vel, Particles.rand(1.2f, 2f)).size(0.5f, 1.6f).color(0xD0B8A0, 0x504048).alpha(0.6f)
					.physics(-0.2f, 2.2f).spin(0.6f);
			}
		}

		for (int i = 0; i < (variant == 0 ? 160 : 50); i++) {
			Particles.spawn(Kind.FLAME, apex, Particles.randomDir(Particles.rand(2f, 7f)), Particles.rand(0.4f, 1f))
				.size(Particles.rand(0.28f, 0.5f), 0.05f).color(pal[0], pal[1]).physics(-1f, 3f);
		}

		for (int i = 0; i < 120; i++) {
			Particles.spawn(Kind.SPARK, apex, Particles.randomDir(Particles.rand(4f, 10f)), Particles.rand(0.4f, 1f)).size(0.05f, 0.015f).color(0xFFFFFF, pal[1])
				.physics(8f, 1f);
		}

		for (int i = 0; i < 30; i++) {
			Particles.spawn(Kind.SMOKE, apex.add(Particles.randomDir(0.6)), Particles.randomDir(1.4).add(0, 1, 0), 2.2)
				.size(0.6f, 1.9f).color(0x403830, 0x201810).alpha(0.5f).physics(-0.3f, 1.5f).spin(0.6f);
		}
	}

	// ------------------------------------------------------------------ 光源（後製）

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		double t = time();
		float life = 1 - span(t, duration - 0.5, duration);

		if (!take) {
			float m = 1 - span(t, 0.1, 0.8);
			lights.add(new CinemaPost.Light(front, 2.4f, r(pal[1]), g(pal[1]), b(pal[1]), 0.6f * m, true));
			return;
		}

		float mouth = 1 - span(t, 0.6, 1.4);
		float charge = span(t, 0.8, BURST);
		float blast = t > BURST ? 1 - span(t, BURST, S5) : 0;
		float ember = span(t, S5 - 0.3, S5 + 0.3) * life;
		lights.add(new CinemaPost.Light(front, 3f, r(pal[1]), g(pal[1]), b(pal[1]), 1.2f * mouth, true));
		lights.add(new CinemaPost.Light(apex, 3f + blast * 4, r(pal[2]), g(pal[2]), b(pal[2]), (charge * 0.4f + blast * 0.7f) * life, true));
		lights.add(new CinemaPost.Light(new Vec3(c.x, pos.getY() + 0.2, c.z), 3.5f, r(pal[2]), g(pal[2]), b(pal[2]), 0.6f * span(t, 0, 0.5) * (1 - ember * 0.6f), false));

		if (ember > 0.01f) {
			lights.add(new CinemaPost.Light(itemAt(t), 2f, r(pal[2]), g(pal[2]), b(pal[2]), 0.35f * ember, true));
		}

		float rr = t > BURST ? 1 - span(t, BURST, BURST + 1.3) : 0;

		if (rr > 0.01f) {
			rays[0] = apex;
			raysP[0] = r(pal[0]);
			raysP[1] = g(pal[0]);
			raysP[2] = b(pal[0]);
			raysP[3] = rr * 0.5f;
		}
	}

	// ------------------------------------------------------------------ 繪製

	@Override
	protected void items(Draw d, double t) {
		if (take && t > BURST && !p.item().isEmpty()) {
			float pop = easeOutBack(span(t, BURST, BURST + 0.3));
			float spin = (float)(Math.min(t, S5) * 2.5 + Math.max(0, t - S5) * 1.2);
			d.item(p.item(), itemAt(t), 1.2f * pop - span(t, S5, TAKE_DURATION) * 0.3f, spin, 0);
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.15, duration - 0.45, duration);
		FxMesh spiritMesh = FxMesh.get("spirit");

		// 爐口亮光
		float mouth = (take ? 1f : 0.6f) * (1 - span(t, 0.3, take ? 1.2 : 0.6));
		d.billboard(Mode.GLOW, GLOW, front.x, front.y, front.z, 1.0f, 0, r(pal[1]), g(pal[1]), b(pal[1]), mouth);
		d.billboard(Mode.GLOW, GLOW, front.x, front.y, front.z, 0.5f, 0, r(pal[0]), g(pal[0]), b(pal[0]), mouth);

		if (take) {
			float open = easeOutBack(span(t, 0, 0.5));
			float main = 1 - span(t, S5, S5 + 0.8);
			Vec3 floor = new Vec3(c.x, pos.getY() + 0.03, c.z);

			// 火柱環（熔爐：精靈盤旋時）
			float pillar = variant == 0 ? Math.min(span(t, 0.8, 1.3), 1 - span(t, S3 - 0.25, S3)) : 0;
			d.cylinder(Mode.GLOW, BEAM, floor, 1.4f, 2.4f, 40, 4, (float)(t * 0.3), 1, (float)(-t), 0.6f, r(pal[1]), g(pal[1]), b(pal[1]), 0.75f * pillar, 0f);

			if (t > BURST) {
				float k = span(t, BURST, BURST + 0.8);
				float a = (1 - k) * (1 - k);
				d.flat(Mode.GLOW, JAG, floor.add(0, 0.03, 0), 0.5f + easeOut(k) * 8f, 0, r(pal[0]), g(pal[0]), b(pal[0]), a);
				d.disc(Mode.GLOW, JAG, apex, Draw.X, Draw.Y, 0.3f + easeOut(k) * 3.5f, 0.4f, r(pal[1]), g(pal[1]), b(pal[1]), a);
				d.flat(Mode.GLOW, RING, apex, 0.3f + easeOut(k) * 4f, 0, 1f, 1f, 1f, a);

				if (variant == 1) {
					meltdown(d, t);
				}

				// 衝天火柱（熔爐）；高爐是細而銳利的蒼焰光束；煙燻爐沒有光柱，改成煙柱
				float beam = variant == 2 ? 0 : 1 - span(t, BURST + 0.1, BURST + (variant == 1 ? 0.7 : 1.1));

				if (variant == 1 && beam > 0.01f) {
					float w = easeOut(span(t, BURST, BURST + 0.04)) * beam;
					d.cylinder(Mode.GLOW, BEAM, apex.add(0, -2, 0), 0.22f * w, 30f, 16, 1, 0, 6, (float)(t * 6), 1f, r(pal[1]), g(pal[1]), b(pal[1]), beam * 0.8f,
						beam * 0.2f);
					d.cylinder(Mode.GLOW, BEAM, apex.add(0, -2, 0), 0.07f * w, 30f, 12, 1, 0.5f, 6, (float)(t * 9), 1f, 1f, 1f, 1f, beam, beam * 0.4f);
				}
				else if (variant == 0 && beam > 0.01f) {
					float w = easeOut(span(t, BURST, BURST + 0.06)) * (0.5f + 0.5f * beam);
					d.cylinder(Mode.GLOW, BEAM, apex.add(0, -0.4, 0), 0.8f * w, 26f, 28, 1, 0, 5, (float)(t * 3), 1.3f, r(pal[1]), g(pal[1]), b(pal[1]), beam * 0.5f,
						beam * 0.1f);
					d.cylinder(Mode.GLOW, BEAM, apex.add(0, -0.4, 0), 0.3f * w, 26f, 16, 1, 0.5f, 4, (float)(t * 5), 1.1f, r(pal[0]), g(pal[0]), b(pal[0]), beam,
						beam * 0.3f);
				}

				// 成品背後的火光
				Vec3 item = itemAt(t);
				float glow = (1 - span(t, S5, TAKE_DURATION)) * 0.7f + 0.15f;
				d.billboard(Mode.GLOW, GLOW, item.x, item.y, item.z, 1.6f, 0, r(pal[1]), g(pal[1]), b(pal[1]), glow * life);
				d.billboard(Mode.GLOW, FLARE, item.x, item.y, item.z, 1.5f * (0.6f + 0.4f * glow), (float)t * 0.3f, r(pal[0]), g(pal[0]), b(pal[0]), 0.5f * glow * life);
			}
		}

		if (take && variant == 1) {
			blades(d, t);
		}

		if (take && variant == 2) {
			serpentBody(d, t);
		}

		// 火之精靈（熔爐，以及三種爐子煉好一件時的小精靈）
		double spiritEnd = take ? BURST : 1.15;

		if ((!take || variant == 0) && t > 0.1 && t < spiritEnd + 0.05) {
			for (int s = 0; s < (take ? 2 : 1); s++) {
				Vec3 at = spirit(s, t);
				float a = Math.min(span(t, take ? 0.4 : 0.1, take ? 0.65 : 0.3), 1 - span(t, spiritEnd - 0.1, spiritEnd));
				float wob = (float)Math.sin(t * 20 + s) * 0.08f;
				float shrink = take ? 1 - 0.65f * span(t, S3 - 0.4, BURST) : 1;
				Matrix4f m = new Matrix4f().rotateY((float)(t * 5 + s * 3)).scale((take ? 0.8f : 0.5f) * shrink, ((take ? 0.9f : 0.55f) + wob) * shrink, (take ? 0.8f : 0.5f) * shrink);
				d.mesh(Mode.GLOW, spiritMesh, at, m, r(pal[0]), g(pal[0]), b(pal[0]), a, false);
				d.billboard(Mode.GLOW, GLOW, at.x, at.y, at.z, (take ? 0.9f : 0.5f) * shrink, 0, r(pal[1]), g(pal[1]), b(pal[1]), 0.7f * a);
			}
		}
	}

	/** 高爐：四片蒼焰電漿刃（沿切線方向的刀光） */
	private void blades(Draw d, double t) {
		float a = Math.min(span(t, 0.45, 0.8), 1 - span(t, BURST - 0.05, BURST + 0.05));

		if (a <= 0.01f) {
			return;
		}

		double r = bladeRadius(t);
		double y = bladeHeight(t);
		float shrink = 1 - 0.6f * span(t, S3 - 0.3, BURST);

		for (int i = 0; i < 4; i++) {
			double ang = bladeAngle(i, t);
			Vec3 tip = new Vec3(c.x + Math.cos(ang) * r, y, c.z + Math.sin(ang) * r);
			double back = ang - 0.9 * shrink;
			Vec3 tail = new Vec3(c.x + Math.cos(back) * r, y - 0.25 * shrink, c.z + Math.sin(back) * r);
			d.streak(Mode.GLOW, TRAIL, tail.x, tail.y, tail.z, tip.x, tip.y, tip.z, 0.32f * shrink, r(pal[1]), g(pal[1]), b(pal[1]), 0.85f * a);
			d.streak(Mode.GLOW, TRAIL, tail.x, tail.y, tail.z, tip.x, tip.y, tip.z, 0.12f * shrink, 1f, 1f, 1f, a);
			d.billboard(Mode.GLOW, GLOW, tip.x, tip.y, tip.z, 0.5f * shrink, 0, r(pal[1]), g(pal[1]), b(pal[1]), 0.7f * a);
		}

		// 刀刃之間的電漿環
		d.flat(Mode.GLOW, RING, new Vec3(c.x, y, c.z), (float)r, (float)t, r(pal[1]), g(pal[1]), b(pal[1]), 0.5f * a);
	}

	/** 高爐衝擊：交叉的兩道熔斷刀光（X）由小到大劃開 */
	private void meltdown(Draw d, double t) {
		float k = span(t, BURST, BURST + 0.18);
		float fadeK = 1 - span(t, BURST + 0.2, BURST + 0.9);

		if (fadeK <= 0.01f) {
			return;
		}

		var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 right = new Vec3(cam.leftVector()).scale(-1);
		Vec3 up = new Vec3(cam.upVector());

		for (int i = 0; i < 2; i++) {
			Vec3 dir = right.scale(i == 0 ? 1 : -1).add(up).normalize();
			double len = 2.4 * easeOut(k);
			Vec3 a0 = apex.subtract(dir.scale(len));
			Vec3 a1 = apex.add(dir.scale(len));
			d.streak(Mode.GLOW, TRAIL, a0.x, a0.y, a0.z, a1.x, a1.y, a1.z, 0.55f * fadeK, r(pal[1]), g(pal[1]), b(pal[1]), 0.8f * fadeK);
			d.streak(Mode.GLOW, TRAIL, a0.x, a0.y, a0.z, a1.x, a1.y, a1.z, 0.16f * fadeK, 1f, 1f, 1f, fadeK);
		}
	}

	/** 煙燻爐：煙龍的身體（一串沿螺旋排列的煙團，越往尾巴越細越淡）+ 發光的頭 */
	private void serpentBody(Draw d, double t) {
		float a = Math.min(span(t, 0.4, 0.8), 1 - span(t, BURST - 0.1, BURST + 0.05));

		if (a <= 0.01f) {
			return;
		}

		int seg = 22;

		for (int i = seg - 1; i >= 0; i--) {
			float f = i / (float)seg;
			Vec3 at = serpent(t, f * 0.6);
			float sz = (0.45f - f * 0.3f) * (1 - 0.5f * span(t, S3 - 0.3, BURST));
			float alpha = (1 - f) * 0.7f * a;
			d.billboard(Mode.ALPHA, SMOKE, at.x, at.y, at.z, sz, (float)(t * 0.8 + i), 0.82f - f * 0.3f, 0.7f - f * 0.3f, 0.62f - f * 0.2f, alpha);
			d.billboard(Mode.GLOW, GLOW, at.x, at.y, at.z, sz * 1.2f, 0, r(pal[2]), g(pal[2]), b(pal[2]), alpha * 0.25f);
		}

		Vec3 head = serpent(t, 0);
		d.billboard(Mode.GLOW, GLOW, head.x, head.y, head.z, 0.5f, 0, r(pal[0]), g(pal[0]), b(pal[0]), 0.8f * a);
		d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, 0.6f, (float)t * 2, 1f, 0.9f, 0.7f, 0.5f * a);
	}
}
