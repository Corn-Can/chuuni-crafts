package com.chuuni.client.render;

import java.util.HashMap;
import java.util.Map;

import com.chuuni.Chuuni;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * 三種特效管線（共用 fx shader，POSITION_TEX_COLOR）：
 * <ul>
 *   <li>{@link #glow} 加法混色：魔法陣、光柱、火花、星光（越疊越亮）</li>
 *   <li>{@link #alpha} 一般透明：煙、羊皮紙地圖</li>
 *   <li>{@link #solid} 寫入深度的透明：鐵鎚等實體感模型（自行算明暗）</li>
 * </ul>
 */
public final class FxRender {
	private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(Chuuni.id("pipeline/fx_glow"))
			.withVertexShader(Chuuni.id("core/fx"))
			.withFragmentShader(Chuuni.id("core/fx"))
			.withSampler("Sampler0")
			.withBlend(BlendFunction.LIGHTNING)
			.withDepthWrite(false)
			.withCull(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build());

	private static final RenderPipeline ALPHA_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(Chuuni.id("pipeline/fx_alpha"))
			.withVertexShader(Chuuni.id("core/fx"))
			.withFragmentShader(Chuuni.id("core/fx"))
			.withSampler("Sampler0")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withCull(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build());

	private static final RenderPipeline SOLID_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(Chuuni.id("pipeline/fx_solid"))
			.withVertexShader(Chuuni.id("core/fx"))
			.withFragmentShader(Chuuni.id("core/fx"))
			.withSampler("Sampler0")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(true)
			.withCull(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build());

	private static final Map<Identifier, RenderType> GLOW = new HashMap<>();
	private static final Map<Identifier, RenderType> ALPHA = new HashMap<>();
	private static final Map<Identifier, RenderType> SOLID = new HashMap<>();

	private FxRender() {
	}

	public static void init() {
	}

	public static RenderType glow(Identifier texture) {
		return GLOW.computeIfAbsent(texture, t -> RenderType.create("chuuni_glow_" + t.getPath().replace('/', '_'),
			RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", t).setOutputTarget(OutputTarget.WEATHER_TARGET).sortOnUpload().bufferSize(262144).createRenderSetup()));
	}

	public static RenderType alpha(Identifier texture) {
		return ALPHA.computeIfAbsent(texture, t -> RenderType.create("chuuni_alpha_" + t.getPath().replace('/', '_'),
			RenderSetup.builder(ALPHA_PIPELINE).withTexture("Sampler0", t).setOutputTarget(OutputTarget.WEATHER_TARGET).sortOnUpload().bufferSize(131072).createRenderSetup()));
	}

	public static RenderType solid(Identifier texture) {
		return SOLID.computeIfAbsent(texture, t -> RenderType.create("chuuni_solid_" + t.getPath().replace('/', '_'),
			RenderSetup.builder(SOLID_PIPELINE).withTexture("Sampler0", t).sortOnUpload().bufferSize(65536).createRenderSetup()));
	}

	public static Identifier tex(String name) {
		return Chuuni.id("textures/fx/" + name + ".png");
	}
}
