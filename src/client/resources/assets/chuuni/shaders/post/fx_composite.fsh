#version 330

#moj_import <chuuni:fx_common.glsl>

// The final grade: bloom, eye adaptation, chromatic aberration, radial zoom blur, colour grade (lift/gain, contrast,
// saturation), coloured vignette, a flash that leaves a burned-in afterimage, film grain, and impact frames — a few
// frames where the whole picture collapses into two stark tones at the moment of a hit.

uniform sampler2D InSampler;
uniform sampler2D BloomSampler;
uniform sampler2D ExpSampler;
uniform sampler2D HoldSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FxGrade {
    vec4 Screen;  // width, height, seconds, -
    vec4 BloomP;  // bloom strength, auto exposure 0/1, exposure multiplier, -
    vec4 Grade;   // contrast, saturation, grain, -
    vec4 Lift;    // rgb added in the shadows
    vec4 Gain;    // rgb multiplier
    vec4 Flash;   // rgb, amount (>1 = white-out)
    vec4 After;   // afterimage amount, -, -, -
    vec4 Vig;     // rgb, amount
    vec4 Impact;  // amount, mode, centre uv
    vec4 Motion;  // aberration, zoom blur, shake x, shake y
    vec4 Anime;   // cel bands amount, ink outline amount, focus lines amount, focus lines dark (0/1)
    vec4 Manga;   // manga panel amount, tone cell px, -, focus line inner radius
    vec4 Extra;   // anamorphic streak amount, rgb split amount, shock ring radius, shock amplitude
    vec4 Streak;  // streak rgb, split angle
};

in vec2 texCoord;
out vec4 fragColor;

vec3 px(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

vec3 fetch(vec2 uv, vec2 dir, float ca) {
    if (ca < 1e-5) {
        return px(uv);
    }
    return vec3(px(uv + dir * ca).r, px(uv).g, px(uv - dir * ca).b);
}

vec3 bloomAt(vec2 uv) {
    return texture(BloomSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

vec3 spectrum(float h) {
    return 0.5 + 0.5 * cos(TAU * (fract(h) + vec3(0.0, 0.33, 0.67)));
}

float linDepth(vec2 uv) {
    float z = texture(DepthSampler, clamp(uv, vec2(0.001), vec2(0.999))).r * 2.0 - 1.0;
    const float n = 0.05;
    const float f = 1024.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

// ink outline from depth discontinuities (block silhouettes), relative so it holds at any distance
float depthEdge(vec2 uv, float px1) {
    vec2 st = px1 / Screen.xy;
    float c = linDepth(uv);
    float e = abs(linDepth(uv + vec2(st.x, 0.0)) - c) + abs(linDepth(uv - vec2(st.x, 0.0)) - c)
            + abs(linDepth(uv + vec2(0.0, st.y)) - c) + abs(linDepth(uv - vec2(0.0, st.y)) - c);
    return smoothstep(0.06, 0.16, e / max(c, 0.05));
}

// hand-drawn focus lines: wedges pointing at the centre, redrawn 12 times a second (animating on twos)
float focusLines(vec2 tc, vec2 centre, float inner, float t) {
    float aspect = Screen.x / Screen.y;
    vec2 d = (tc - centre) * vec2(aspect, 1.0);
    float r = length(d) / max(aspect, 1.0) * 1.25;
    float a = atan(d.y, d.x);
    float beat = floor(t * 12.0);
    float sum = 0.0;
    for (int layer = 0; layer < 2; layer++) {
        float n = layer == 0 ? 110.0 : 190.0;
        float s = a / TAU * n + hash1(beat * 3.1 + float(layer)) * 7.0;
        float id = floor(s);
        float f = fract(s) - 0.5;
        float h = hash1(id * 1.37 + beat * 17.3 + float(layer) * 91.0);
        if (h < (layer == 0 ? 0.2 : 0.45)) {
            continue;
        }
        float r0 = inner * (0.75 + 0.7 * hash1(id * 7.9 + beat));
        float grow = smoothstep(r0, r0 + 0.3, r);
        float w = (0.16 + 0.6 * h * h) * grow;
        float aa = fwidth(s) * 1.5 + 0.02;
        sum = max(sum, (1.0 - smoothstep(w - aa, w + aa, abs(f) * 2.0)) * step(r0, r));
    }
    return sum;
}

void main() {
    float t = Screen.z;
    float aspect = Screen.x / Screen.y;
    vec2 uv = texCoord + Motion.zw;

    // shockwave: a ring of refraction racing outward from the hit
    if (Extra.w > 0.001) {
        vec2 dd = (uv - Impact.zw) * vec2(aspect, 1.0);
        float rr = length(dd);
        float ring = exp(-sq((rr - Extra.z) / 0.045));
        uv -= normalize(dd + 1e-5) / vec2(aspect, 1.0) * ring * Extra.w * 0.035;
    }
    vec2 c = uv - 0.5;

    float ca = Motion.x * 0.02 * dot(c * vec2(aspect, 1.0), c * vec2(aspect, 1.0)) * 2.0;
    vec3 col = fetch(uv, c, ca);
    if (Extra.y > 0.001) {
        // hard RGB split along one direction (the hit shoved the colour planes apart)
        vec2 sd = vec2(cos(Streak.w), sin(Streak.w)) * Extra.y * 0.012;
        col = vec3(px(uv + sd).r, col.g, px(uv - sd).b);
    }
    vec3 bloom = bloomAt(uv);
    if (Motion.y > 0.001) {
        vec3 acc = col;
        vec3 bacc = bloom;
        float total = 1.0;
        for (int i = 1; i < 12; i++) {
            float k = float(i) / 12.0;
            vec2 q = 0.5 + c * (1.0 - Motion.y * 0.14 * k);
            float w = 1.0 - k * 0.6;
            acc += fetch(q, c, ca) * w;
            bacc += bloomAt(q) * w;
            total += w;
        }
        col = acc / total;
        bloom = bacc / total;
    }

    // eye adaptation: the more light the disaster pours into the frame, the further the camera stops down
    float ev = BloomP.z;
    if (BloomP.y > 0.5) {
        float added = 0.0;
        for (int i = 0; i < 16; i++) {
            vec2 p = (vec2(float(i - (i / 4) * 4), float(i / 4)) + 0.5) / 4.0;
            added += luma(texture(ExpSampler, p).rgb);
        }
        added /= 16.0;
        ev *= 1.0 / (1.0 + max(added - 0.04, 0.0) * 3.5);
    }
    col *= ev;
    bloom *= ev;

    // anime cel look: luminance pressed into flat bands, block silhouettes inked
    if (Anime.x > 0.001) {
        float l0 = luma(col);
        float bands = 4.0;
        float q = (floor(l0 * bands) + smoothstep(0.35, 0.65, fract(l0 * bands))) / bands;
        col *= mix(1.0, q / max(l0, 1e-3), Anime.x);
    }
    if (Anime.y > 0.001) {
        col *= 1.0 - depthEdge(uv, 1.2) * Anime.y * 0.9;
    }

    vec3 b = bloom * BloomP.x;
    col = col + b - col * b * 0.3;

    // anamorphic streak: the bright core smeared into a long horizontal line, like a lens flare in an anime highlight
    if (Extra.x > 0.001) {
        vec3 st = vec3(0.0);
        float tw = 0.0;
        for (int i = -10; i <= 10; i++) {
            float k = float(i) / 10.0;
            float w = exp(-k * k * 2.5);
            vec3 sb = bloomAt(uv + vec2(k * 0.38, 0.0));
            st += max(sb - 0.3, 0.0) * w;
            tw += w;
        }
        st /= tw;
        col += Streak.rgb * luma(st) * Extra.x * 2.0;
    }

    // grade
    col = col * Gain.rgb + Lift.rgb * (1.0 - col);
    float g = luma(col);
    col = mix(vec3(g), col, Grade.y);
    col = (col - 0.45) * Grade.x + 0.45;

    float edge = smoothstep(0.25, 0.95, length(c * vec2(aspect * 0.8, 1.0)) * 1.3);
    col = mix(col, Vig.rgb * (0.3 + 0.7 * g), sat(Vig.a * edge));

    // flash blindness: the bright parts of the held frame stay burned in, soft and warm, fading
    if (After.x > 0.001) {
        vec3 held = vec3(0.0);
        for (int i = 0; i < 5; i++) {
            vec2 o = i == 0 ? vec2(0.0) : vec2(cos(float(i) * 1.571), sin(float(i) * 1.571)) * 0.007;
            held += texture(HoldSampler, clamp(texCoord + o, vec2(0.001), vec2(0.999))).rgb;
        }
        float ghost = smoothstep(0.62, 0.97, luma(held / 5.0)) * sat(After.x);
        col = 1.0 - (1.0 - col) * (1.0 - ghost * vec3(1.0, 0.95, 0.86));
    }

    col += Flash.rgb * min(Flash.a, 1.0);
    col = mix(col, vec3(1.0), sat(Flash.a - 1.0));

    if (Grade.z > 0.001) {
        col += (hash2(texCoord * Screen.xy + fract(t * 7.3) * 431.0) - 0.5) * Grade.z * 0.18;
    }

    // ---- manga panel: the frame turns into a printed page — paper, ink outlines, halftone, cross-hatching
    if (Manga.x > 0.001) {
        // saturated glow (violet beam) must read as white paper, not mid-grey
        float l = mix(luma(col), max(col.r, max(col.g, col.b)), 0.65);
        float hot = smoothstep(0.55, 0.9, luma(bloomAt(uv)) * BloomP.x);
        vec2 st1 = 1.5 / Screen.xy;
        float gx = luma(px(uv + vec2(st1.x, 0.0))) - luma(px(uv - vec2(st1.x, 0.0)));
        float gy = luma(px(uv + vec2(0.0, st1.y))) - luma(px(uv - vec2(0.0, st1.y)));
        float ink = max(smoothstep(0.1, 0.25, length(vec2(gx, gy))) * 0.6, depthEdge(uv, 1.6)) * (1.0 - smoothstep(0.7, 0.9, l));
        vec2 p = texCoord * Screen.xy;
        float cell = max(Manga.y, 3.0);
        vec2 tp = mat2(0.7071, -0.7071, 0.7071, 0.7071) * p / cell;
        float tl = sat((l - 0.08) / 0.62);
        float dots = step(length(fract(tp) - 0.5), sqrt(1.0 - tl) * 0.6);
        float hatch1 = step(fract((p.x + p.y) / (cell * 0.9)), 0.38);
        float hatch2 = step(fract((p.x - p.y) / (cell * 0.9)), 0.38);
        vec3 paper = vec3(0.96, 0.95, 0.92);
        vec3 inkc = vec3(0.04, 0.035, 0.05);
        float dark;
        if (tl > 0.62) {
            dark = 0.0;
        } else if (tl > 0.3) {
            dark = dots;
        } else if (tl > 0.12) {
            dark = max(hatch1, dots * 0.0);
        } else if (tl > 0.04) {
            dark = max(hatch1, hatch2);
        } else {
            dark = 1.0;
        }
        dark = max(dark * (1.0 - hot), ink);
        vec3 page = mix(paper, inkc, dark);
        page *= 0.97 + 0.03 * noise2(p * 0.15);
        col = mix(col, page, sat(Manga.x));
    }

    if (Anime.z > 0.001) {
        float fl = focusLines(texCoord, Impact.zw, Manga.w, t);
        col = mix(col, Anime.w > 0.5 ? vec3(0.02, 0.015, 0.03) : vec3(1.0), fl * sat(Anime.z));
    }

    // ---- impact frame
    if (Impact.x > 0.001) {
        int mode = int(Impact.y + 0.5);
        vec2 st = 1.5 / Screen.xy;
        float gx = luma(px(uv + vec2(st.x, 0.0))) - luma(px(uv - vec2(st.x, 0.0)));
        float gy = luma(px(uv + vec2(0.0, st.y))) - luma(px(uv - vec2(0.0, st.y)));
        float ink = smoothstep(0.05, 0.16, length(vec2(gx, gy)));
        float l = luma(px(uv));
        float avg = 0.0;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.785398;
            avg += luma(px(uv + vec2(cos(a) / aspect, sin(a)) * 0.05));
        }
        avg /= 8.0;
        float two = step(avg + 0.004, l);
        float core = step(avg + 0.2, l);
        vec2 d = (texCoord - Impact.zw) * vec2(aspect, 1.0);
        // screentone dots in the midtones
        vec2 tp = mat2(0.7071, -0.7071, 0.7071, 0.7071) * (texCoord * Screen.xy) / 5.0;
        float tone = sat((l - avg) * 5.0 + 0.5);
        float dots = step(length(fract(tp) - 0.5), sqrt(sat(1.0 - tone)) * 0.62);
        float mid = smoothstep(0.15, 0.35, tone) * (1.0 - smoothstep(0.65, 0.85, tone));
        vec3 frame;
        vec3 dark;
        vec3 lit;
        vec3 hot;
        if (mode == 0) {        // ink: black on white paper
            dark = vec3(0.0); lit = vec3(1.0); hot = vec3(1.0);
        } else if (mode == 1) { // ember: black and fire
            dark = vec3(0.015, 0.006, 0.004); lit = vec3(1.0, 0.36, 0.06); hot = vec3(1.0, 0.93, 0.8);
        } else if (mode == 2) { // frost: navy and ice
            dark = vec3(0.02, 0.04, 0.12); lit = vec3(0.55, 0.9, 1.0); hot = vec3(1.0);
        } else if (mode == 3) { // crimson: white and blood
            dark = vec3(0.6, 0.0, 0.03); lit = vec3(1.0, 0.95, 0.94); hot = vec3(1.0);
        } else if (mode == 6) { // violet
            dark = vec3(0.03, 0.0, 0.06); lit = vec3(0.68, 0.45, 1.0); hot = vec3(0.97, 0.94, 1.0);
        } else {
            dark = vec3(0.0); lit = vec3(1.0); hot = vec3(1.0);
        }
        if (mode == 4) {
            // glitch: bands slid sideways, blocks knocked out, channels split, crushed to black / red / white
            float beat = floor(t * 20.0);
            float band = floor(texCoord.y * Screen.y / 16.0);
            float slide = (hash1(band * 13.1 + beat) - 0.5) * 0.18 * step(0.45, hash1(band * 7.7 + beat * 3.0));
            vec2 blk = floor(texCoord * Screen.xy / vec2(64.0, 36.0));
            vec2 knock = (vec2(hash2(blk + beat), hash2(blk * 1.7 + beat)) - 0.5) * 0.09 * step(0.85, hash2(blk * 3.1 + beat));
            vec2 q = texCoord + vec2(slide, 0.0) + knock;
            float split = 0.006 + 0.012 * hash1(beat);
            vec3 gc = vec3(px(q + vec2(split, 0.0)).r, px(q).g, px(q - vec2(split, 0.0)).b);
            float lv = luma(gc);
            frame = lv < 0.25 ? vec3(0.01) : lv < 0.6 ? vec3(0.85, 0.04, 0.12) : vec3(1.0);
            frame *= 0.75 + 0.25 * step(0.5, fract(texCoord.y * Screen.y / 3.0));
            float data = step(0.975, hash2(floor(texCoord * Screen.xy / vec2(24.0, 8.0)) + beat * 7.0));
            frame = mix(frame, vec3(1.0), data * 0.85);
        } else if (mode == 5) {
            // spectral: the world's edges on black, split into rainbow outward from the hit
            vec2 out1 = normalize(d + 1e-5) / vec2(aspect, 1.0);
            vec3 acc = vec3(0.0);
            for (int k = 0; k < 7; k++) {
                vec2 o = uv + out1 * float(k - 3) * (0.004 + 0.005 * length(d));
                float ex = luma(px(o + vec2(st.x, 0.0))) - luma(px(o - vec2(st.x, 0.0)));
                float ey = luma(px(o + vec2(0.0, st.y))) - luma(px(o - vec2(0.0, st.y)));
                acc += spectrum(float(k) / 7.0) * smoothstep(0.04, 0.15, length(vec2(ex, ey)));
            }
            frame = vec3(0.01, 0.008, 0.02) + acc * 0.8;
            frame = mix(frame, vec3(1.0), core * 0.9);
        } else if (mode == 7) {
            // negative, pushed hard
            vec3 neg = 1.0 - px(uv);
            float ng = luma(neg);
            frame = clamp((mix(vec3(ng), neg, 1.8) - 0.5) * 1.6 + 0.5, 0.0, 1.0) * (1.0 - ink * 0.7);
        } else {
            frame = mix(dark, lit, two);
            frame = mix(frame, hot, core);
            frame = mix(frame, mix(dark, lit, dots), mid * 0.75);
            frame = mix(frame, dark, ink * two);
        }
        col = mix(col, frame, sat(Impact.x));
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
