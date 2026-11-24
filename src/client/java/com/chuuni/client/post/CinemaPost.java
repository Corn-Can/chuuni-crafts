package com.chuuni.client.post;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.chuuni.Chuuni;
import com.chuuni.client.fx.Effects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * 後製管線（移植自小妞 1014 天災模組的 CinemaPost，同一作者）。
 * The post-processing chain, run every frame a disaster is on screen:
 * <ol>
 * <li>world pass — depth-reconstructed relighting from the disasters' own lights, light scattered in the air, height
 * fog, gravitational lensing, heat shimmer, light shafts;</li>
 * <li>bloom — 5 levels down (the first keeps only light the world pass added), 4 levels up;</li>
 * <li>composite — eye adaptation, aberration, zoom blur, grade, vignette, flash + afterimage, grain, impact frames —
 * written back into the main target.</li>
 * </ol>
 * Runs between the world and the hand, so the hand stays crisp. Costs nothing when no disaster is showing.
 */
public final class CinemaPost {
	private CinemaPost() {}

	private static final int BLOOM_LEVELS = 5;

	private static RenderPipeline.Builder pipeline(String name, String... samplers) {
		RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Chuuni.id("pipeline/post_" + name))
			.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
			.withFragmentShader(Chuuni.id("post/" + name));
		for (String s : samplers) {
			b.withSampler(s + "Sampler");
		}
		return b;
	}

	private static RenderPipeline build(RenderPipeline.Builder b, String uniform) {
		if (uniform != null) {
			b.withUniform(uniform, UniformType.UNIFORM_BUFFER);
		}
		return RenderPipelines.register(b.build());
	}

	public static final RenderPipeline WORLD = build(pipeline("fx_world", "Main", "Depth"), "FxWorld");
	public static final RenderPipeline DOWN = build(pipeline("fx_bloom_down", "In", "Orig"), "FxBloom");
	public static final RenderPipeline UP = build(pipeline("fx_bloom_up", "In", "Base"), "FxBloom");
	public static final RenderPipeline COMPOSITE = build(pipeline("fx_composite", "In", "Bloom", "Exp", "Hold", "Depth"), "FxGrade");
	public static final RenderPipeline COPY = build(pipeline("fx_copy", "In"), null);

	public static void init() {
		// touching the class registers the pipelines before the game precompiles them
	}

	// ---------------------------------------------------------------- captured per frame
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f PROJ = new Matrix4f();
	private static Vec3 cam = Vec3.ZERO;
	private static boolean captured;

	public static void capture(Camera camera, Matrix4f view, Matrix4f projection) {
		VIEW.set(view);
		PROJ.set(projection);
		cam = camera.position();
		captured = true;
	}

	// ---------------------------------------------------------------- GPU resources
	private static TextureTarget fx;
	private static TextureTarget hold;
	private static final TextureTarget[] down = new TextureTarget[BLOOM_LEVELS];
	private static final TextureTarget[] up = new TextureTarget[BLOOM_LEVELS - 1];
	private static GpuBuffer worldUbo;
	private static GpuBuffer gradeUbo;
	private static final GpuBuffer[] bloomUbo = new GpuBuffer[BLOOM_LEVELS * 2];
	private static int width;
	private static int height;
	private static boolean holdValid;

	private static final int WORLD_SIZE = 64 * 3 + 16 * 5 + 16 * 32 + 16 * 12 + 16 * 4;
	private static final int GRADE_SIZE = 16 * 14;

	private static void ensure(int w, int h) {
		if (worldUbo == null) {
			var device = RenderSystem.getDevice();
			worldUbo = device.createBuffer(() -> "chuuni fx world", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, WORLD_SIZE);
			gradeUbo = device.createBuffer(() -> "chuuni fx grade", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, GRADE_SIZE);
			for (int i = 0; i < bloomUbo.length; i++) {
				int n = i;
				bloomUbo[i] = device.createBuffer(() -> "chuuni fx bloom " + n, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
			}
		}
		if (fx != null && w == width && h == height) {
			return;
		}
		width = w;
		height = h;
		if (fx == null) {
			fx = new TextureTarget("chuuni fx", w, h, false);
			hold = new TextureTarget("chuuni fx hold", w, h, false);
			for (int i = 0; i < BLOOM_LEVELS; i++) {
				down[i] = new TextureTarget("chuuni bloom down " + i, Math.max(1, w >> (i + 1)), Math.max(1, h >> (i + 1)), false);
			}
			for (int i = 0; i < BLOOM_LEVELS - 1; i++) {
				up[i] = new TextureTarget("chuuni bloom up " + i, Math.max(1, w >> (i + 1)), Math.max(1, h >> (i + 1)), false);
			}
		} else {
			fx.resize(w, h);
			hold.resize(w, h);
			for (int i = 0; i < BLOOM_LEVELS; i++) {
				down[i].resize(Math.max(1, w >> (i + 1)), Math.max(1, h >> (i + 1)));
			}
			for (int i = 0; i < BLOOM_LEVELS - 1; i++) {
				up[i].resize(Math.max(1, w >> (i + 1)), Math.max(1, h >> (i + 1)));
			}
		}
		holdValid = false;
	}

	// ---------------------------------------------------------------- the frame
	public record Light(Vec3 pos, float radius, float r, float g, float b, float intensity, boolean air) {
		float weight() {
			return intensity * radius / (float) Math.max(8, pos.length());
		}
	}

	public record Seg(Vec3 a, Vec3 b, float radius, float r, float g, float bl, float intensity, float width) {}

	public static void render() {
		Minecraft mc = Minecraft.getInstance();
		if (!captured || mc.level == null) {
			return;
		}
		captured = false;

		if (!com.chuuni.client.fx.Settings.post || !com.chuuni.client.fx.Settings.enabled) {
			return;
		}
		PostState.update();
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		List<Light> lights = new ArrayList<>();
		List<Seg> segs = new ArrayList<>();
		Vec3[] lens = new Vec3[1];
		float[] lensP = new float[2];
		Vec3[] rays = new Vec3[1];
		float[] raysP = new float[4];
		gather(pt, lights, segs, lens, lensP, rays, raysP);
		if (!PostState.active() && lights.isEmpty() && segs.isEmpty() && lens[0] == null) {
			return;
		}
		RenderTarget main = mc.getMainRenderTarget();
		if (main.getColorTextureView() == null || main.getDepthTextureView() == null) {
			return;
		}
		ensure(main.width, main.height);
		CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
		GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		float seconds = (Util.getMillis() % 1_000_000L) / 1000F;

		lights.sort((x, y) -> Float.compare(y.weight(), x.weight()));
		writeWorld(enc, seconds, lights, segs, lens[0], lensP, rays[0], raysP);
		try (RenderPass pass = enc.createRenderPass(() -> "chuuni fx world", fx.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(WORLD);
			pass.setUniform("FxWorld", worldUbo);
			pass.bindTexture("MainSampler", main.getColorTextureView(), linear);
			pass.bindTexture("DepthSampler", main.getDepthTextureView(), nearest);
			pass.draw(0, 3);
		}

		// bloom down
		GpuTextureView src = fx.getColorTextureView();
		int sw = fx.width;
		int sh = fx.height;
		for (int i = 0; i < BLOOM_LEVELS; i++) {
			writeVec4(enc, bloomUbo[i], 1F / sw, 1F / sh, i == 0 ? 1 : 0, 1);
			try (RenderPass pass = enc.createRenderPass(() -> "chuuni bloom down", down[i].getColorTextureView(), OptionalInt.empty())) {
				pass.setPipeline(DOWN);
				pass.setUniform("FxBloom", bloomUbo[i]);
				pass.bindTexture("InSampler", src, linear);
				pass.bindTexture("OrigSampler", main.getColorTextureView(), linear);
				pass.draw(0, 3);
			}
			src = down[i].getColorTextureView();
			sw = down[i].width;
			sh = down[i].height;
		}
		// bloom up: smallest + next level, ... up to half resolution
		GpuTextureView small = down[BLOOM_LEVELS - 1].getColorTextureView();
		int smw = down[BLOOM_LEVELS - 1].width;
		int smh = down[BLOOM_LEVELS - 1].height;
		for (int i = BLOOM_LEVELS - 2; i >= 0; i--) {
			writeVec4(enc, bloomUbo[BLOOM_LEVELS + i], 1F / smw, 1F / smh, 0, 1);
			try (RenderPass pass = enc.createRenderPass(() -> "chuuni bloom up", up[i].getColorTextureView(), OptionalInt.empty())) {
				pass.setPipeline(UP);
				pass.setUniform("FxBloom", bloomUbo[BLOOM_LEVELS + i]);
				pass.bindTexture("InSampler", small, linear);
				pass.bindTexture("BaseSampler", down[i].getColorTextureView(), linear);
				pass.draw(0, 3);
			}
			small = up[i].getColorTextureView();
			smw = up[i].width;
			smh = up[i].height;
		}

		if (PostState.takeHoldRequest()) {
			try (RenderPass pass = enc.createRenderPass(() -> "chuuni fx hold", hold.getColorTextureView(), OptionalInt.empty())) {
				pass.setPipeline(COPY);
				pass.bindTexture("InSampler", fx.getColorTextureView(), linear);
				pass.draw(0, 3);
			}
			holdValid = true;
		}

		writeGrade(enc, seconds);
		try (RenderPass pass = enc.createRenderPass(() -> "chuuni fx composite", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(COMPOSITE);
			pass.setUniform("FxGrade", gradeUbo);
			pass.bindTexture("InSampler", fx.getColorTextureView(), linear);
			pass.bindTexture("BloomSampler", up[0].getColorTextureView(), linear);
			pass.bindTexture("ExpSampler", down[BLOOM_LEVELS - 1].getColorTextureView(), linear);
			pass.bindTexture("HoldSampler", (holdValid ? hold : fx).getColorTextureView(), linear);
			pass.draw(0, 3);
		}
	}

	private static void writeVec4(CommandEncoder enc, GpuBuffer buffer, float x, float y, float z, float w) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			Std140Builder b = Std140Builder.onStack(stack, 16);
			b.putVec4(x, y, z, w);
			enc.writeToBuffer(buffer.slice(), b.get());
		}
	}

	private static void writeWorld(CommandEncoder enc, float seconds, List<Light> lights, List<Seg> segs, Vec3 lens, float[] lensP, Vec3 rays, float[] raysP) {
		Matrix4f invProj = new Matrix4f(PROJ).invert();
		Matrix4f invView = new Matrix4f(VIEW).invert();
		Matrix4f viewProj = new Matrix4f(PROJ).mul(VIEW);
		PostState s = PostState.CURRENT;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			Std140Builder b = Std140Builder.onStack(stack, WORLD_SIZE);
			b.putMat4f(invProj).putMat4f(invView).putMat4f(viewProj);
			b.putVec4(width, height, seconds, 0);
			b.putVec4(s.dim, s.dimR, s.dimG, s.dimB);
			b.putVec4(s.fog, s.fogR, s.fogG, s.fogB);
			b.putVec4((float) (s.groundY - cam.y), s.fogFalloff, s.fogMax, 0.6F);
			int n = Math.min(16, lights.size());
			b.putVec4(n, Math.min(4, segs.size()), s.heat, s.skyDark);
			for (int i = 0; i < 16; i++) {
				if (i < n) {
					Light l = lights.get(i);
					b.putVec4((float) l.pos.x, (float) l.pos.y, (float) l.pos.z, l.radius);
				} else {
					b.putVec4(0, 0, 0, 1);
				}
			}
			for (int i = 0; i < 16; i++) {
				if (i < n) {
					Light l = lights.get(i);
					b.putVec4(l.r, l.g, l.b, l.air ? l.intensity : -l.intensity);
				} else {
					b.putVec4(0, 0, 0, 0);
				}
			}
			for (int i = 0; i < 4; i++) {
				Seg g = i < segs.size() ? segs.get(i) : null;
				b.putVec4(g == null ? 0 : (float) g.a.x, g == null ? 0 : (float) g.a.y, g == null ? 0 : (float) g.a.z, g == null ? 1 : g.radius);
			}
			for (int i = 0; i < 4; i++) {
				Seg g = i < segs.size() ? segs.get(i) : null;
				b.putVec4(g == null ? 0 : (float) g.b.x, g == null ? 0 : (float) g.b.y, g == null ? 0 : (float) g.b.z, g == null ? 0 : g.intensity);
			}
			for (int i = 0; i < 4; i++) {
				Seg g = i < segs.size() ? segs.get(i) : null;
				b.putVec4(g == null ? 0 : g.r, g == null ? 0 : g.g, g == null ? 0 : g.bl, g == null ? 1 : g.width);
			}
			if (lens != null) {
				b.putVec4((float) lens.x, (float) lens.y, (float) lens.z, lensP[0]);
			} else {
				b.putVec4(0, 0, 0, 0);
			}
			b.putVec4(lensP[1], 0.5F, 0, 0);
			float rayStrength = s.rays * raysP[3];
			if (rays != null && rayStrength > 0.001F) {
				b.putVec4((float) rays.x, (float) rays.y, (float) rays.z, rayStrength);
			} else {
				b.putVec4(0, 0, 0, 0);
			}
			b.putVec4(raysP[0], raysP[1], raysP[2], 0.85F);
			enc.writeToBuffer(worldUbo.slice(), b.get());
		}
	}

	private static void writeGrade(CommandEncoder enc, float seconds) {
		PostState s = PostState.CURRENT;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			Std140Builder b = Std140Builder.onStack(stack, GRADE_SIZE);
			b.putVec4(width, height, seconds, 0);
			b.putVec4(s.bloom, 1, s.exposure, 0);
			b.putVec4(s.contrast, s.saturation, s.grain, 0);
			b.putVec4(s.liftR, s.liftG, s.liftB, 0);
			b.putVec4(s.gainR, s.gainG, s.gainB, 0);
			b.putVec4(s.flashR, s.flashG, s.flashB, s.flash);
			b.putVec4(holdValid ? s.after : 0, 0, 0, 0);
			b.putVec4(s.vigR, s.vigG, s.vigB, s.vignette);
			b.putVec4(s.impact, s.impactMode, 0.5F, 0.5F);
			b.putVec4(s.aberration, s.zoom, 0, 0);
			b.putVec4(s.cel, s.outline, s.lines, s.linesDark);
			b.putVec4(s.manga, Math.max(4, height / 150F), 0, s.linesInner);
			b.putVec4(s.streak, s.split, s.shockR, s.shockAmp);
			b.putVec4(s.streakR, s.streakG, s.streakB, s.splitAngle);
			enc.writeToBuffer(gradeUbo.slice(), b.get());
		}
	}

	// ---------------------------------------------------------------- 光源：由進行中的演出提供（世界座標 → 相對鏡頭）
	private static void gather(float pt, List<Light> lights, List<Seg> segs, Vec3[] lens, float[] lensP, Vec3[] rays, float[] raysP) {
		List<Light> world = new ArrayList<>();
		List<Seg> worldSegs = new ArrayList<>();
		Effects.lights(world, worldSegs, rays, raysP);

		for (Light l : world) {
			lights.add(new Light(l.pos().subtract(cam), l.radius(), l.r(), l.g(), l.b(), l.intensity(), l.air()));
		}

		for (Seg g : worldSegs) {
			segs.add(new Seg(g.a().subtract(cam), g.b().subtract(cam), g.radius(), g.r(), g.g(), g.bl(), g.intensity(), g.width()));
		}

		if (rays[0] != null) {
			rays[0] = rays[0].subtract(cam);
		}
	}
}
