package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * 告示牌。variant：0 染色（extra = 顏色）/ 1 發光墨囊 / 2 墨囊（取消發光）。
 * <ul>
 *   <li>彩墨染書：四行文字的位置被一筆一筆刷上顏色，墨滴四濺</li>
 *   <li>螢光刻文：四行文字依序亮起、像刻上去的光，螢火蟲般的光點繞著告示牌飛</li>
 *   <li>墨囊：黑色墨霧把光吸走</li>
 * </ul>
 */
public class SignFx extends MinorFx {
	private static final int GLOWC = 0x90FFD0;
	private static final int INK = 0x202028;

	private final int variant;
	private final int col;
	private final Vec3 board;
	private final Vec3 side;

	public SignFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 3);
		col = variant == 0 ? (p.extra() == 0 ? 0x303030 : p.extra()) : variant == 1 ? GLOWC : INK;

		end = variant == 2 ? 1.4 : 2.0;

		// 照原版 SignRenderer 的擺法算出文字所在的平面：中心、正面法線、文字的左右方向、每像素大小、行高
		var mc = Minecraft.getInstance();
		var s = state();
		boolean front = p.power() != 0;
		double yRot = 0;
		Vec3 offset = new Vec3(0, 0.3333333, 0.0466667);
		Vec3 base = new Vec3(0, 0, 0);
		double scale = 0.015625 * 0.6666667;
		int lh = 10;

		if (s != null && s.getBlock() instanceof net.minecraft.world.level.block.SignBlock sign) {
			yRot = sign.getYRotationDegrees(s);

			if (sign instanceof net.minecraft.world.level.block.WallSignBlock) {
				base = new Vec3(0, -0.3125, -0.4375);
			}
			else if (sign instanceof net.minecraft.world.level.block.CeilingHangingSignBlock
				|| sign instanceof net.minecraft.world.level.block.WallHangingSignBlock) {
				offset = new Vec3(0, -0.32, 0.073);
				scale = 0.015625 * 0.9;
				lh = 9;
			}
		}

		double rad = Math.toRadians(yRot);
		Vec3 fwd = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
		Vec3 right = new Vec3(Math.cos(rad), 0, Math.sin(rad));
		Vec3 local = base.add(offset.x, offset.y, 0);

		if (!front) {
			fwd = fwd.scale(-1);
			right = right.scale(-1);
		}

		normal = fwd;
		side = right;
		px = scale;
		lineH = lh * scale;
		board = c.add(right.scale(local.x)).add(0, local.y, 0).add((front ? fwd : fwd.scale(-1)).scale(base.z)).add(fwd.scale(offset.z + 0.012));

		// 每一行字的實際寬度（像素）；空行為 0
		for (int i = 0; i < 4; i++) {
			widths[i] = 0;
		}

		if (mc.level != null && mc.level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be) {
			var text = be.getText(front);

			for (int i = 0; i < 4; i++) {
				widths[i] = mc.font.width(text.getMessage(i, mc.isTextFilteringEnabled()));
			}
		}

		boolean any = false;

		for (int w : widths) {
			any |= w > 0;
		}

		if (!any) {
			java.util.Arrays.fill(widths, 60);
		}

	}

	private final Vec3 normal;
	private final double px;
	private final double lineH;
	private final int[] widths = new int[4];

	/** 第 i 行字上的一點（k 0 = 行首、1 = 行尾），貼在字的前方一點點 */
	private Vec3 row(int i, double k) {
		double w = widths[i] * px;
		double y = -(i * lineH - 2 * lineH + 4.5 * px);
		return board.add(side.scale(-w / 2 + w * k)).add(0, y, 0);
	}

	private boolean hasRow(int i) {
		return widths[i] > 0;
	}

	@Override
	protected void update(double t, double dt) {
		if (cue(0.0)) {
			Sfx.play(mine, variant == 1 ? "sign_glow" : "sign_dye", c, 0.85f, variant == 2 ? 0.8f : 1f);

			if (variant != 2) {
				tint(variant == 1 ? GLOWC : col, 1.3);
				smallName(variant == 1 ? "sign_glow" : "sign_dye");
			}
			else {
				for (int i = 0; i < 16; i++) {
					Particles.spawn(Kind.SMOKE, onText(), Particles.randomDir(0.4).add(0, 0.4, 0), 1.0).size(0.15f, 0.5f)
						.color(0x303038, 0x101014).alpha(0.6f).physics(-0.2f, 1.5f);
				}
			}
		}

		if (variant == 0 && cue(0.3)) {
			pop(col, 0.3f);

			for (int i = 0; i < 20; i++) {
				Particles.spawn(Kind.SHARD, onText(), normal.scale(Particles.rand(0.5f, 1.5f)).add(Particles.randomDir(0.6)).add(0, 0.6, 0), 0.8).size(0.03f, 0.02f).color(col, col).physics(6f, 1f)
					.spin(6f);
			}
		}

		if (variant == 1) {
			if (cue(0.5)) {
				pop(GLOWC, 0.3f);
			}

			if (t > 0.3 && t < 1.6) {
				int nn = rate(14, dt);

				for (int i = 0; i < nn; i++) {
					Particles.spawn(Kind.GLOW, onText(), normal.scale(0.3).add(Particles.randomDir(0.15)).add(0, 0.25, 0), 1.2)
						.size(0.04f, 0.02f).color(0xF0FFB0, GLOWC).physics(-0.1f, 0.5f);
				}
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		if (variant == 2) {
			return;
		}

		float k = span(time(), 0.1, 0.6) * (1 - span(time(), 1.2, end));
		lights.add(new CinemaPost.Light(board.add(normal.scale(0.3)), 1.5f, r(col), g(col), b(col), (variant == 1 ? 0.5f : 0.3f) * k, true));
	}

	@Override
	protected void render(Draw d, double t) {
		float life = fade(t, 0.05, end - 0.4, end);
		float band = (float)lineH * 0.95f;

		for (int i = 0; i < 4; i++) {
			if (!hasRow(i)) {
				continue;
			}

			if (variant == 2) {
				// 墨囊：字上的光從兩端往中間被吸掉
				float k = span(t, i * 0.06, 0.5 + i * 0.06);
				line(d, GLOW, row(i, 0.5 * k), row(i, 1 - 0.5 * k), band, GLOWC, (1 - k) * 0.45f);
				continue;
			}

			// 一行一行由左到右「寫」過去：筆頭是一顆亮點，走過的字留下貼著字形的光暈
			double t0 = 0.05 + i * 0.14;
			float k = easeOut(span(t, t0, t0 + 0.22));

			if (k <= 0) {
				continue;
			}

			Vec3 head = row(i, k);

			if (variant == 0) {
				float wet = 1 - span(t, t0 + 0.25, t0 + 0.9);
				line(d, TRAIL, row(i, 0), head, band * 1.1f, col, (0.25f + 0.45f * wet) * life);
			}
			else {
				float flash = 1 - span(t, t0 + 0.22, t0 + 0.55);
				line(d, GLOW, row(i, 0), head, band * 1.3f, GLOWC, (0.3f + 0.4f * flash) * life);
				line(d, SPARK, row(i, 0), head, band * 0.25f, 0xFFFFFF, 0.5f * flash * life);
			}

			float tip = 1 - span(t, t0 + 0.2, t0 + 0.35);
			d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, band * 2.5f * tip, (float)t * 4, 1f, 1f, 1f, tip);
		}
	}

	/** 隨機一行字上的一點（粒子從字上冒出來） */
	private Vec3 onText() {
		for (int tries = 0; tries < 8; tries++) {
			int i = Particles.RNG.nextInt(4);

			if (hasRow(i)) {
				return row(i, Particles.RNG.nextDouble());
			}
		}

		return board;
	}
}
