#version 330

// 中二工坊特效：貼圖 × 頂點色。加法混色（光）與一般透明（煙、地圖、實體模型）共用。
// 遠處依霧淡出，避免在霧中顯得突兀。

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    float fog = 1.0 - total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd);
    color.a *= fog;

    if (color.a < 0.003) {
        discard;
    }

    fragColor = color * ColorModulator;
}
