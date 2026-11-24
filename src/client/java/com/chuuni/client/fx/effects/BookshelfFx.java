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
 * 雕紋書架。variant 0 = 放書、1 = 取書。點到的格子（3×2）亮出一個光框：
 * 放書時符文從四周飛進格子裡、整格亮起來（之後保持微光，見 {@link com.chuuni.client.fx.Ambient}）；取書時幾張書頁從格子飄出來。
 */
public class BookshelfFx extends MicroFx {
	private static final int INK = 0xC8A0FF;

	private final boolean take;
	private final Vec3 slot;

	public BookshelfFx(FxPayload p) {
		super(p);
		take = p.variant() == 1;
		// 把點擊位置對齊到格子中心
		Vec3 rel = hit.subtract(face);
		double du = rel.x * u.x + rel.z * u.z;
		double col = Math.floor(Math.max(0, Math.min(2.999, (du + 0.5) * 3)));
		double dv = rel.y > 0 ? 0.25 : -0.25;
		slot = on((col + 0.5) / 3 - 0.5, dv, 0.01);
		com.chuuni.client.fx.Ambient.rescan();
	}

	@Override
	protected void update(double t, double dt) {
		if (!cue(0.0)) {
			return;
		}

		sfx("book_slide", take ? 1.1f : 0.95f);

		if (!take) {
			if (Sfx.has("anime_kira")) {
				Sfx.play(mine, "anime_kira", c, 0.4f, 1.2f);
			}
			else {
				sfx("book_glow", 1f);
			}
		}

		if (take) {
			burst(Kind.SHARD, slot, nv.add(0, 0.4, 0), 6, 1.4f, 0.4f, 0xFFF6E0, 0xE0D0B0, 0.05f, 2.5f, 0.8);
		}
		else {
			for (int i = 0; i < 8; i++) {
				Vec3 from = slot.add(nv.scale(0.6)).add(Particles.randomDir(0.4));
				Particles.spawn(Kind.GLYPH, from, Vec3.ZERO, 0.35).size(0.06f, 0.02f).color(0xFFFFFF, INK).physics(0, 0).attract(slot, 14f);
			}
		}
	}

	@Override
	protected void render(Draw d, double t) {
		float a = span(t, 0, 0.05) * (1 - span(t, 0.2, 0.7));
		double hw = 0.15, hh = 0.2;
		Vec3[] q = {on(slot, -hw, -hh, 0), on(slot, hw, -hh, 0), on(slot, hw, hh, 0), on(slot, -hw, hh, 0)};

		for (int i = 0; i < 4; i++) {
			line(d, SPARK, q[i], q[(i + 1) % 4], 0.03f, INK, a);
		}

		faceDisc(d, GLOW, slot, 0.25f, 0, INK, a * 0.4f);

		// 放書：整格灌滿光，再慢慢降到常駐的微光
		if (!take && dir.getAxis() != Direction.Axis.Y) {
			float fill = span(t, 0.12, 0.22) * (1 - span(t, 0.3, end)) * 0.9f;
			d.disc(Mode.GLOW, GLOW, slot, new Vector3f(u).mul(0.2f), new Vector3f(v).mul(0.26f), 1f, 0, r(INK), g(INK), b(INK), fill);
			d.disc(Mode.GLOW, GLOW, slot, new Vector3f(u).mul(0.1f), new Vector3f(v).mul(0.15f), 1f, 0, 1f, 1f, 1f, fill * 0.8f);
		}
	}
}
