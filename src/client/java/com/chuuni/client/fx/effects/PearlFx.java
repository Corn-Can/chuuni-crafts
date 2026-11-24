package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.post.PostState;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.world.phys.Vec3;

/**
 * 終界珍珠・虛空瞬步。
 * 出手時掌中綻開一圈紫色小陣；飛行中拖著紫黑色的虛空光帶、沿途剝落星屑；
 * 落地那一刻空間「裂開」——一道直立的虛空裂縫張開又闔上，星屑先被吸進裂縫再炸散。
 */
public class PearlFx extends FlightFx {
	private static final int VOID = 0xB060FF;
	private static final int TEAL = 0x40E0C0;

	private final Vec3 from;

	public PearlFx(FxPayload p) {
		super(p);
		from = p.at();
		after = 1.3;
	}

	@Override
	protected void fly(double t, double dt, Vec3 at) {
		if (cue(0.0)) {
			Sfx.anime(mine, "dash", "pearl_throw", from, 0.8f, 1f);
			sparks(from, 14, 2.5f, VOID, 0.35f);
		}

		int n = rate(70, dt);

		for (int i = 0; i < n; i++) {
			Particles.spawn(i % 3 == 0 ? Kind.GLYPH : Kind.FLARE, at.add(Particles.randomDir(0.12)), Particles.randomDir(0.4), Particles.rand(0.4f, 0.8f))
				.size(0.05f, 0.005f).color(0xFFFFFF, i % 4 == 0 ? TEAL : VOID).physics(-0.3f, 2f).spin(4f);
		}
	}

	@Override
	protected void land(Vec3 at) {
		Sfx.anime(mine, "warp", "pearl_warp", at, 1f, 1f);
		shake(0.15f);

		if (mine) {
			PostState.split(0.6f, 1.2f, 200);
			PostState.flash(VOID, 0.12f, 160, 0);
			smallName("pearl");
		}

		// 先被吸進裂縫
		for (int i = 0; i < 36; i++) {
			Vec3 o = at.add(Particles.randomDir(Particles.rand(0.8f, 1.6f)));
			Particles.spawn(Kind.FLARE, o, Vec3.ZERO, 0.3).size(0.06f, 0.01f).color(0xFFFFFF, VOID).physics(0, 0).attract(at, 25f);
		}
	}

	@Override
	protected void after(double t, double dt) {
		// 再炸散
		if (landT >= 0 && cue(landT + 0.28)) {
			sparks(head, 40, 5f, VOID, 0.6f);

			for (int i = 0; i < 24; i++) {
				Particles.spawn(Kind.GLYPH, head, Particles.randomDir(Particles.rand(1f, 2.5f)), Particles.rand(0.6f, 1.1f)).size(0.07f, 0.02f)
					.color(0xFFFFFF, i % 3 == 0 ? TEAL : VOID).physics(-0.5f, 2.5f).spin(3f);
			}
		}
	}

	@Override
	protected void lights(List<CinemaPost.Light> lights, List<CinemaPost.Seg> segs, Vec3[] rays, float[] raysP) {
		headLight(lights, VOID, 0.35f);

		if (landT >= 0) {
			float k = 1 - span(time(), landT, landT + after);
			lights.add(new CinemaPost.Light(head, 4f, r(VOID), g(VOID), b(VOID), 0.6f * k, true));
		}
	}

	@Override
	protected void render(Draw d, double t) {
		// 出手的小陣（自己丟的話就在鏡頭正前方，會擋住視線，只給旁人看）
		float hk = span(t, 0, 0.5);

		if (hk < 1 && !mine) {
			d.billboard(Mode.GLOW, SEAL, from.x, from.y, from.z, 0.25f + easeOut(hk) * 0.5f, (float)(t * 4), r(VOID), g(VOID), b(VOID), (1 - hk) * 0.8f);
		}

		ribbon(d, t, 0.22f, VOID, 0.9f);

		if (flying()) {
			// 剛出手時離鏡頭太近，先淡淡的
			float closeIn = mine ? span(t, 0.08, 0.25) : 1;
			glow(d, head, 0.55f, VOID, 0.8f * closeIn);
			d.billboard(Mode.GLOW, FLARE, head.x, head.y, head.z, 0.4f, (float)(t * 6), 1f, 1f, 1f, 0.7f * closeIn);
			return;
		}

		// 虛空裂縫：直立的一道縫張開 → 闔上
		double lt = t - landT;
		float open = lt < 0.28 ? easeOut(span(lt, 0, 0.18)) : 1 - easeIn(span(lt, 0.28, 0.5));
		float h = 1.3f;

		if (open > 0.01f) {
			d.streak(Mode.GLOW, TRAIL, head.x, head.y - h, head.z, head.x, head.y + h, head.z, 0.7f * open, r(VOID), g(VOID), b(VOID), 0.9f * open);
			d.streak(Mode.GLOW, SPARK, head.x, head.y - h * 0.9, head.z, head.x, head.y + h * 0.9, head.z, 0.18f * open, 1f, 1f, 1f, open);
		}

		float k = span(lt, 0.28, 1.0);

		if (k > 0 && k < 1) {
			d.billboard(Mode.GLOW, RING, head.x, head.y, head.z, 0.4f + easeOut(k) * 2.2f, 0, r(VOID), g(VOID), b(VOID), (1 - k) * (1 - k));
			d.billboard(Mode.GLOW, JAG, head.x, head.y, head.z, 0.3f + easeOut(k) * 1.5f, (float)lt, r(TEAL), g(TEAL), b(TEAL), (1 - k) * 0.6f);
		}

		glow(d, head, 1.4f, VOID, 0.7f * (1 - span(lt, 0.1, after)));
	}
}
