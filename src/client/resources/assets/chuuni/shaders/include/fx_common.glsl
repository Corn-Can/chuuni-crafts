// Shared helpers for the disaster post passes. Everything is camera-relative world space (camera at the origin,
// axes = world axes) so float precision holds far from the world origin.

#define PI 3.14159265
#define TAU 6.28318531
#define FAR 100000.0

float sat(float x) { return clamp(x, 0.0, 1.0); }

// never pow(x, 2.0): undefined for x < 0 on some drivers
float sq(float x) { return x * x; }

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float hash1(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1, 0)), u.x), mix(hash2(i + vec2(0, 1)), hash2(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise2(p);
        p = p * 2.07 + vec2(13.7, 5.1);
        a *= 0.5;
    }
    return v;
}

/** Closest distance from the ray (origin 0, direction rd) to point p; t = ray parameter there. */
float rayPoint(vec3 rd, vec3 p, out float t) {
    t = max(dot(p, rd), 0.0);
    return length(rd * t - p);
}

/** Closest distance from the ray to segment ab; t = ray parameter, s = segment parameter 0..1. */
float raySegment(vec3 rd, vec3 a, vec3 b, out float t, out float s) {
    vec3 ba = b - a;
    float baba = dot(ba, ba);
    float bard = dot(ba, rd);
    float baoa = dot(ba, -a);
    float rdoa = dot(rd, -a);
    float den = baba - bard * bard;
    s = den > 1e-6 ? sat((baoa - bard * rdoa) / den) : 0.0;
    vec3 q = a + ba * s;
    t = max(dot(q, rd), 0.0);
    return length(rd * t - q);
}

/** Soft roll-off above 0.8 so stacked light blooms toward white instead of clipping flat. */
vec3 softClip(vec3 c) {
    vec3 hot = max(c - 1.0, 0.0);
    c += dot(hot, vec3(0.3333)) * 0.45;
    vec3 over = max(c - 0.8, 0.0);
    return min(c, 0.8) + 0.2 * (1.0 - exp(-over / 0.2));
}
