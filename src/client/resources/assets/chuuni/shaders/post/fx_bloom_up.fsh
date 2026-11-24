#version 330

// Bloom upsample: 9-tap tent of the smaller level, added onto this level's downsample.

uniform sampler2D InSampler;
uniform sampler2D BaseSampler;

layout(std140) uniform FxBloom {
    vec4 Params; // texel x, texel y (of the smaller level), -, radius
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 ts = Params.xy * Params.w;
    vec3 s = texture(InSampler, uv).rgb * 4.0;
    s += (texture(InSampler, uv + ts * vec2(-1, 0)).rgb + texture(InSampler, uv + ts * vec2(1, 0)).rgb
        + texture(InSampler, uv + ts * vec2(0, -1)).rgb + texture(InSampler, uv + ts * vec2(0, 1)).rgb) * 2.0;
    s += texture(InSampler, uv + ts * vec2(-1, -1)).rgb + texture(InSampler, uv + ts * vec2(1, -1)).rgb
       + texture(InSampler, uv + ts * vec2(-1, 1)).rgb + texture(InSampler, uv + ts * vec2(1, 1)).rgb;
    fragColor = vec4(s / 16.0 * 0.85 + texture(BaseSampler, uv).rgb, 1.0);
}
