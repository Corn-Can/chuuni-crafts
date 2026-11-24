#version 330

#moj_import <chuuni:fx_common.glsl>

// The world pass. Reconstructs every pixel's position from the depth buffer, then: bends light around a black hole,
// sinks the world into a tinted shadow, relights it from the disasters' own light sources (meteors, the core, flashes,
// beams) with real falloff and facing, adds the light scattered in the air around them, height fog, and shafts of
// light from one source. Output is the graded-but-not-yet-composited frame.

uniform sampler2D MainSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FxWorld {
    mat4 InvProj;
    mat4 InvView;
    mat4 ViewProj;
    vec4 Screen;    // width, height, seconds, -
    vec4 Dim;       // amount, tint rgb
    vec4 Fog;       // density, rgb
    vec4 FogH;      // ground y (camera-relative), height falloff, max opacity, light-in-fog
    vec4 Counts;    // lights, segments, heat shimmer, sky darkening
    vec4 LightPos[16]; // xyz, radius of influence
    vec4 LightCol[16]; // rgb, intensity (negative = no air glow)
    vec4 SegA[4];      // a.xyz, radius
    vec4 SegB[4];      // b.xyz, intensity
    vec4 SegCol[4];    // rgb, core width
    vec4 Lens;      // xyz, strength
    vec4 LensR;     // horizon radius (blocks), ring brightness, -, -
    vec4 Rays;      // xyz source, strength
    vec4 RaysCol;   // rgb, length
};

in vec2 texCoord;
out vec4 fragColor;

vec3 unproject(vec2 uv, float depth) {
    vec4 v = InvProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    v.xyz /= v.w;
    return (InvView * vec4(v.xyz, 1.0)).xyz;
}

vec3 viewDir(vec2 uv) {
    return normalize(unproject(uv, 1.0) - unproject(uv, 0.0));
}

/** Screen uv (xy) and view depth (z, <= 0 = behind) of a camera-relative point. */
vec3 project(vec3 p) {
    vec4 c = ViewProj * vec4(p, 1.0);
    if (c.w <= 0.0) {
        return vec3(0.0, 0.0, -1.0);
    }
    return vec3(c.xy / c.w * 0.5 + 0.5, c.w);
}

void main() {
    vec2 uv = texCoord;
    float time = Screen.z;
    float aspect = Screen.x / Screen.y;
    float depth = texture(DepthSampler, uv).r;
    bool sky = depth >= 0.99999;
    vec3 rd = viewDir(uv);
    vec3 P = sky ? rd * FAR : unproject(uv, depth);
    float sceneT = sky ? FAR : length(P);

    // ---- gravitational lensing: the scene behind a black hole is pulled round it into a ring
    vec2 suv = uv;
    float ring = 0.0;
    if (Lens.w > 0.001) {
        vec3 lp = project(Lens.xyz);
        float dist = length(Lens.xyz);
        if (lp.z > 0.0 && dist < sceneT + LensR.x * 4.0) {
            // horizon radius in screen units (uv height)
            vec3 edge = project(Lens.xyz + normalize(cross(Lens.xyz, vec3(0.0, 1.0, 0.0)) + vec3(1e-4)) * LensR.x);
            float rs = max(length((edge.xy - lp.xy) * vec2(aspect, 1.0)), 1e-4);
            vec2 d = (uv - lp.xy) * vec2(aspect, 1.0);
            float r = length(d);
            float bend = Lens.w * rs * rs * 2.2 / max(r, rs * 0.35);
            suv = uv - normalize(d + 1e-6) / vec2(aspect, 1.0) * min(bend, r * 0.95);
            ring = exp(-sq((r - rs * 1.55) / (rs * 0.09))) * LensR.y;
        }
    }

    // ---- heat shimmer (lava, the core): a slow wobble of the whole frame, strongest low on screen
    if (Counts.z > 0.001) {
        vec2 w = vec2(noise2(uv * vec2(14.0, 8.0) + vec2(0.0, -time * 1.6)), noise2(uv * vec2(9.0, 12.0) + vec2(time * 0.9, 3.0))) - 0.5;
        suv += w * Counts.z * 0.006 * (0.4 + 0.6 * sat(1.0 - uv.y * 1.2));
    }

    vec3 scene = texture(MainSampler, clamp(suv, vec2(0.001), vec2(0.999))).rgb;
    vec3 col = scene;

    // ---- sink the world into a tinted shadow; the sky can be darkened separately
    float dimAmt = sky ? max(Dim.x, Counts.w) : Dim.x;
    col = mix(col, col * Dim.yzw, sat(dimAmt));

    // ---- surface normal from the depth derivatives (for light facing)
    vec3 N = normalize(cross(dFdx(P), dFdy(P)) + vec3(1e-6));
    if (dot(N, P) > 0.0) {
        N = -N;
    }

    vec3 surf = vec3(0.0);   // light falling on surfaces (multiplied by their colour)
    vec3 air = vec3(0.0);    // light scattered in the air between camera and surface
    int lights = int(Counts.x + 0.5);
    for (int i = 0; i < 16; i++) {
        if (i >= lights) {
            break;
        }
        vec3 L = LightPos[i].xyz;
        float R = max(LightPos[i].w, 0.5);
        vec3 c = LightCol[i].rgb * abs(LightCol[i].w);
        if (!sky) {
            vec3 dl = L - P;
            float d = length(dl);
            float facing = max(dot(N, dl / max(d, 1e-3)), 0.0) * 0.85 + 0.15;
            surf += c * facing * (R * R) / (d * d + R * R);
        }
        if (LightCol[i].w > 0.0) {
            float t;
            float dd = rayPoint(rd, L, t);
            if (t < sceneT + R * 0.25) {
                float core = R * 0.08;
                air += c * (exp(-dd / core) * 0.9 + 0.22 * (core * core) / (dd * dd + core * core) + 0.05 * exp(-dd / (R * 0.5)));
            }
        }
    }
    int segs = int(Counts.y + 0.5);
    for (int i = 0; i < 4; i++) {
        if (i >= segs) {
            break;
        }
        vec3 a = SegA[i].xyz;
        vec3 b = SegB[i].xyz;
        float R = max(SegA[i].w, 0.5);
        vec3 c = SegCol[i].rgb * SegB[i].w;
        if (!sky) {
            vec3 ba = b - a;
            float s = sat(dot(P - a, ba) / max(dot(ba, ba), 1e-4));
            vec3 dl = a + ba * s - P;
            float d = length(dl);
            float facing = max(dot(N, dl / max(d, 1e-3)), 0.0) * 0.85 + 0.15;
            surf += c * facing * (R * R) / (d * d + R * R);
        }
        float t;
        float s;
        float dd = raySegment(rd, a, b, t, s);
        if (t < sceneT) {
            float w = max(SegCol[i].w, 0.05);
            air += c * (exp(-dd / w) * 1.0 + 0.1 * (w * w * 4.0) / (dd * dd + w * w * 4.0));
        }
    }

    // surfaces: the scene colour stands in for albedo x ambient, so lit ground keeps its texture
    surf = surf / (1.0 + surf * 0.7);
    col += scene * surf * 1.1;

    // ---- height fog, catching some of the lights
    if (Fog.x > 0.0 && !sky) {
        float h = exp(-max(P.y - FogH.x, 0.0) * FogH.y);
        float f = (1.0 - exp(-sceneT * Fog.x)) * mix(1.0, h, sat(FogH.y * 10.0)) * FogH.z;
        col = mix(col, Fog.rgb + air * FogH.w, sat(f));
    } else if (Fog.x > 0.0 && sky) {
        col = mix(col, Fog.rgb, sat(FogH.z * 0.55 * smoothstep(0.35, -0.05, rd.y)));
    }

    col += air;

    // ---- shafts of light from one source (radial march toward it)
    if (Rays.w > 0.001) {
        vec3 sp = project(Rays.xyz);
        if (sp.z > 0.0) {
            vec2 to = sp.xy - uv;
            float acc = 0.0;
            float wsum = 0.0;
            float jitter = hash2(uv * Screen.xy + time);
            for (int i = 0; i < 24; i++) {
                float k = (float(i) + jitter) / 24.0;
                vec2 q = uv + to * k * RaysCol.w;
                if (q.x < 0.0 || q.x > 1.0 || q.y < 0.0 || q.y > 1.0) {
                    continue;
                }
                float dq = texture(DepthSampler, q).r;
                vec3 sc = texture(MainSampler, q).rgb;
                float open = dq >= 0.99999 ? 1.0 : smoothstep(0.75, 1.0, luma(sc));
                float w = 1.0 - k * 0.7;
                acc += open * w;
                wsum += w;
            }
            float shafts = acc / max(wsum, 1e-3);
            // shafts only read near the source; far away they would just flood an open sky with light
            float near = exp(-length(to * vec2(aspect, 1.0)) * 2.2);
            col += RaysCol.rgb * shafts * Rays.w * 0.35 * (0.08 + 0.92 * near);
        }
    }

    col += vec3(0.85, 0.75, 1.0) * ring;

    fragColor = vec4(softClip(col), 1.0);
}
