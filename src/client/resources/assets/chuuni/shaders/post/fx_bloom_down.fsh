#version 330

#moj_import <chuuni:fx_common.glsl>

// Bloom downsample: 13 taps (overlapping 2x2 boxes), stable without flicker. The first level keeps only the light the
// world pass ADDED over the untouched scene (OrigSampler) plus anything blazing near white, so snow and a bright sky
// don't smear but meteors, fire and beams glow.

uniform sampler2D InSampler;
uniform sampler2D OrigSampler;

layout(std140) uniform FxBloom {
    vec4 Params; // texel x, texel y, prefilter (0/1), radius
};

in vec2 texCoord;
out vec4 fragColor;

vec3 tap(vec2 uv) {
    vec3 c = texture(InSampler, uv).rgb;
    if (Params.z > 0.5) {
        vec3 o = texture(OrigSampler, uv).rgb;
        vec3 diff = c - o;
        // light that raised every channel blooms in its own colour; a pixel that was recoloured (some channels
        // up, some down) only blooms by the brightness it gained, in its new colour, never in a complementary hue
        float lo = min(diff.r, min(diff.g, diff.b));
        float hi = max(diff.r, max(diff.g, diff.b));
        float additive = lo >= 0.0 ? 1.0 : sat(1.0 + lo / max(0.1 * hi, 1e-4));
        float gained = max(luma(c) - luma(o), 0.0);
        vec3 added = mix(c * (gained / max(luma(c), 1e-4)), max(diff, 0.0), additive);
        // 中二工坊：特效本身是加法發光，世界又被壓暗，所以亮的像素（特效）也要泛光
        float knee = smoothstep(0.55, 1.0, luma(c));
        c = added * 0.45 + c * knee * 0.28;
    }
    return c;
}

void main() {
    vec2 uv = texCoord;
    vec2 ts = Params.xy;
    vec3 a = tap(uv + ts * vec2(-2, 2));
    vec3 b = tap(uv + ts * vec2(0, 2));
    vec3 c = tap(uv + ts * vec2(2, 2));
    vec3 d = tap(uv + ts * vec2(-2, 0));
    vec3 e = tap(uv);
    vec3 f = tap(uv + ts * vec2(2, 0));
    vec3 g = tap(uv + ts * vec2(-2, -2));
    vec3 h = tap(uv + ts * vec2(0, -2));
    vec3 i = tap(uv + ts * vec2(2, -2));
    vec3 j = tap(uv + ts * vec2(-1, 1));
    vec3 k = tap(uv + ts * vec2(1, 1));
    vec3 l = tap(uv + ts * vec2(-1, -1));
    vec3 m = tap(uv + ts * vec2(1, -1));
    vec3 col = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    fragColor = vec4(col, 1.0);
}
