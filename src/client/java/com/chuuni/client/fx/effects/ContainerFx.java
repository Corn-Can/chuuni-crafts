package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 容器闔上（開的時候介面會擋住，所以演在關上那一下）。variant：0 箱子 / 1 陷阱箱 / 2 木桶 / 3 漏斗 / 4 發射器 / 5 投擲器 / 6 銅箱。
 * <ul>
 *   <li>箱子類：蓋縫一道光從兩端往中間收攏、「喀」地闔上，縫裡噴出一小撮灰塵（陷阱箱是紅光、銅箱是銅色）</li>
 *   <li>木桶：桶蓋上一個「✕」形的光一閃（木條封住）</li>
 *   <li>漏斗：幾顆火星順著漏斗口往下掉進去</li>
 *   <li>發射器 / 投擲器：正面的孔像眼睛一樣亮一下</li>
 * </ul>
 */
public class ContainerFx extends MicroFx {
	private final int variant;
	private final int col;
	private final Direction front;

	public ContainerFx(FxPayload p) {
		super(p);
		variant = Math.floorMod(p.variant(), 7);
		col = switch (variant) {
			case 1 -> 0xFF4030;
			case 2 -> 0xE0B070;
			case 3 -> 0x9AA8C0;
			case 4 -> 0xFF8A40;
			case 5 -> 0x90B0D0;
			case 6 -> 0xFF9A60;
			default -> 0xFFD8A0;
		};
		front = facing();
		end = 0.7;
	}

	private boolean chest() {
		return variant == 0 || variant == 1 || variant == 6;
	}

	@Override
	protected void update(double t, double dt) {
		if (!cue(0.0)) {
			return;
		}

		sfx(variant >= 3 && variant <= 5 ? "hatch_clank" : "lid_thud", variant == 2 ? 0.9f : 1f);
		Vec3 f = vec(front);

		if (chest()) {
			Vec3 seam = seam();
			burst(Kind.SMOKE, seam, f.add(0, 0.3, 0), 6, 0.6f, 0.25f, 0xD8D0C0, 0x807868, 0.12f, -0.2f, 0.7);
		}
		else if (variant == 3) {
			for (int i = 0; i < 8; i++) {
				Vec3 at = boxTop().add(Particles.rand(-0.35f, 0.35f), 0.3, Particles.rand(-0.35f, 0.35f));
				Particles.spawn(Kind.SPARK, at, new Vec3(0, -1.5, 0), 0.4).size(0.025f, 0.008f).color(0xFFFFFF, col).physics(2f, 1f).attract(boxTop().add(0, -0.2, 0), 4f);
			}
		}
		else if (variant != 2) {
			burst(Kind.SPARK, boxFace(front), f, 6, 1.2f, 0.5f, 0xFFFFFF, col, 0.025f, 3f, 0.3);
		}
	}

	/** 箱子蓋縫：正面、約外形高度的七成處 */
	private Vec3 seam() {
		var b = box();
		Vec3 face = boxFace(front);
		return new Vec3(face.x, b.minY + (b.maxY - b.minY) * 0.7, face.z);
	}

	@Override
	protected void render(Draw d, double t) {
		Vec3 f = vec(front);
		Vec3 a = across(front);

		if (chest()) {
			// 蓋縫的光：從兩端往中間收攏
			float k = easeOut(span(t, 0, 0.18));
			float fade = 1 - span(t, 0.18, 0.6);
			Vec3 seam = seam();
			double half = 0.44 * (1 - k * 0.85);
			line(d, SPARK, seam.add(a.scale(-half)), seam.add(a.scale(half)), 0.05f + k * 0.04f, col, fade);
			glow(d, seam, 0.35f, col, fade * 0.5f * k);
		}
		else if (variant == 2) {
			// 木桶蓋上的「✕」
			Vec3 lid = boxFace(front);
			Vec3 b = f.y != 0 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
			float k = easeOut(span(t, 0, 0.12));
			float fade = 1 - span(t, 0.15, 0.6);
			double r = 0.38 * k;
			line(d, SPARK, lid.add(a.scale(-r)).add(b.scale(-r)), lid.add(a.scale(r)).add(b.scale(r)), 0.06f, col, fade);
			line(d, SPARK, lid.add(a.scale(-r)).add(b.scale(r)), lid.add(a.scale(r)).add(b.scale(-r)), 0.06f, col, fade);
		}
		else if (variant == 3) {
			glow(d, boxTop(), 0.8f, col, 0.6f * (1 - span(t, 0, 0.5)));
			Vec3 top = boxTop().add(0, 0.01, 0);
			float k = easeOut(span(t, 0, 0.3));
			float fade = 1 - span(t, 0.2, 0.6);

			for (int i = 0; i < 4; i++) {
				double ang = i * Math.PI / 2 + Math.PI / 4;
				Vec3 rim = top.add(Math.cos(ang) * 0.6, 0, Math.sin(ang) * 0.6);
				line(d, SPARK, rim, lerp(rim, top.add(0, -0.25, 0), k), 0.05f, col, fade);
			}
		}
		else {
			// 正面的孔像眼睛亮一下
			Vec3 eye = boxFace(front);
			float k = span(t, 0, 0.06) * (1 - span(t, 0.1, 0.5));
			glow(d, eye, 0.45f, col, k * 0.8f);
			d.billboard(Mode.GLOW, FLARE, eye.x, eye.y, eye.z, 0.35f * k, (float)t * 3, 1f, 1f, 1f, k);
		}
	}
}
