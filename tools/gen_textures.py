"""
中二工坊特效材質：全部程式繪製（4 倍超取樣後縮小抗鋸齒，線條外加柔光暈）。
大多輸出為「白色 + alpha」，由遊戲內頂點色上色（同一張魔法陣可以染成紫、金、藍）。
用法：python tools/gen_textures.py   → src/client/resources/assets/chuuni/textures/fx/*.png
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "client", "resources", "assets", "chuuni")
OUT = os.path.join(ROOT, "textures", "fx")
SS = 4
FONT_KAI = "C:/Windows/Fonts/kaiu.ttf"
FONT_SERIF = "C:/Windows/Fonts/georgiab.ttf"


# ================================================================= 基礎

class Pen:
    """在超取樣灰階畫布上作畫（座標以最終像素為單位）"""

    def __init__(self, w, h=None):
        self.w, self.h = w, h or w
        self.img = Image.new("L", (self.w * SS, self.h * SS), 0)
        self.d = ImageDraw.Draw(self.img)

    def p(self, x, y):
        return (x * SS, y * SS)

    def circle(self, cx, cy, r, width=2.0, fill=None, value=255):
        box = [(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS]
        if fill is not None:
            self.d.ellipse(box, fill=fill)
        if width > 0:
            self.d.ellipse(box, outline=value, width=max(1, int(width * SS)))

    def line(self, pts, width=2.0, value=255):
        pts = [self.p(x, y) for x, y in pts]
        self.d.line(pts, fill=value, width=max(1, int(width * SS)), joint="curve")
        r = width * SS / 2
        for x, y in (pts[0], pts[-1]):
            self.d.ellipse([x - r, y - r, x + r, y + r], fill=value)

    def poly(self, pts, fill=None, width=0.0, value=255):
        pts = [self.p(x, y) for x, y in pts]
        if fill is not None:
            self.d.polygon(pts, fill=fill)
        if width > 0:
            self.d.line(pts + [pts[0]], fill=value, width=max(1, int(width * SS)), joint="curve")

    def arc(self, cx, cy, r, a0, a1, width=2.0, value=255):
        box = [(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS]
        self.d.arc(box, a0, a1, fill=value, width=max(1, int(width * SS)))

    def text(self, cx, cy, s, size, font=FONT_KAI, value=255, angle=0.0):
        f = ImageFont.truetype(font, int(size * SS))
        layer = Image.new("L", self.img.size, 0)
        dd = ImageDraw.Draw(layer)
        dd.text((cx * SS, cy * SS), s, font=f, fill=value, anchor="mm")
        if angle:
            layer = layer.rotate(angle, center=(cx * SS, cy * SS), resample=Image.BICUBIC)
        self.img = Image.fromarray(np.maximum(np.asarray(self.img), np.asarray(layer)))
        self.d = ImageDraw.Draw(self.img)

    def paste_max(self, other, ox=0, oy=0):
        a = np.asarray(self.img).copy()
        b = np.asarray(other)
        h, w = b.shape
        a[oy * SS:oy * SS + h, ox * SS:ox * SS + w] = np.maximum(a[oy * SS:oy * SS + h, ox * SS:ox * SS + w], b)
        self.img = Image.fromarray(a)
        self.d = ImageDraw.Draw(self.img)

    def final(self):
        return self.img.resize((self.w, self.h), Image.LANCZOS)


def glowify(mask, radius, strength=0.8, radius2=None, strength2=0.35):
    """白色線條 + 兩層柔光暈 → RGBA（顏色白、alpha = 亮度）"""
    a = np.asarray(mask).astype(np.float32) / 255
    g1 = np.asarray(mask.filter(ImageFilter.GaussianBlur(radius))).astype(np.float32) / 255
    out = np.maximum(a, g1 * strength * 2.2)
    if radius2:
        g2 = np.asarray(mask.filter(ImageFilter.GaussianBlur(radius2))).astype(np.float32) / 255
        out = np.maximum(out, g2 * strength2 * 3)
    out = np.clip(out, 0, 1)
    return white(out)


def white(alpha, core=None):
    """alpha 0~1 → 白色 RGBA；core 可讓最亮處保持白、較暗處稍微偏灰（給染色更多層次）"""
    h, w = alpha.shape
    img = np.zeros((h, w, 4), np.uint8)
    lum = np.clip(0.55 + 0.45 * alpha, 0, 1) if core is None else core
    img[..., 0] = img[..., 1] = img[..., 2] = (lum * 255).astype(np.uint8)
    img[..., 3] = (np.clip(alpha, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))
    print("  ", name, img.size)


def polar(cx, cy, r, deg):
    a = math.radians(deg - 90)
    return (cx + r * math.cos(a), cy + r * math.sin(a))


# ================================================================= 符文

def make_glyph(seed, size=32.0, width=2.2):
    """隨機符文：3×4 節點格上的折線 + 點 + 小弧，帶「看起來像文字」的筆劃規律"""
    rng = random.Random(seed)
    nodes = [(x, y) for x in range(3) for y in range(4)]
    strokes = []
    n = rng.randint(2, 4)
    for _ in range(n):
        k = rng.randint(2, 3)
        a = rng.choice(nodes)
        path = [a]
        for _ in range(k - 1):
            cand = [b for b in nodes if b != path[-1] and abs(b[0] - path[-1][0]) <= 1 and abs(b[1] - path[-1][1]) <= 2]
            path.append(rng.choice(cand))
        strokes.append(("line", path))
    if rng.random() < 0.5:
        strokes.append(("dot", rng.choice(nodes)))
    if rng.random() < 0.35:
        strokes.append(("ring", rng.choice(nodes)))
    if rng.random() < 0.4:
        strokes.append(("bar", rng.randint(0, 3)))

    def draw(pen, ox, oy, s, rot=0.0, w=width):
        sx = s / 3.0
        sy = s / 4.0

        def tp(nx, ny):
            x = (nx - 1) * sx * 0.85
            y = (ny - 1.5) * sy * 0.85
            c, si = math.cos(rot), math.sin(rot)
            return (ox + x * c - y * si, oy + x * si + y * c)

        for kind, val in strokes:
            if kind == "line":
                pen.line([tp(*q) for q in val], w)
            elif kind == "dot":
                x, y = tp(*val)
                pen.circle(x, y, w * 0.9, 0, fill=255)
            elif kind == "ring":
                x, y = tp(*val)
                pen.circle(x, y, s * 0.12, w * 0.7)
            elif kind == "bar":
                pen.line([tp(-0.2, val), tp(2.2, val)], w * 0.8)

    return draw


GLYPHS = [make_glyph(1000 + i) for i in range(128)]


def glyph_ring(pen, cx, cy, r, count, size, seed=0, width=2.0, inward=False):
    for i in range(count):
        deg = 360 * i / count
        x, y = polar(cx, cy, r, deg)
        rot = math.radians(deg + (180 if inward else 0))
        GLYPHS[(seed + i * 7) % len(GLYPHS)](pen, x, y, size, rot, width)


# ================================================================= 魔法陣

def circle_enchant():
    """星辰刻印：多層同心環、符文帶、八芒星 + 七芒星、星盤刻度、中央之眼"""
    S = 1024
    c = S / 2
    pen = Pen(S)
    pen.circle(c, c, 492, 7)
    pen.circle(c, c, 474, 2)
    glyph_ring(pen, c, c, 438, 40, 44, seed=3, width=3.2)
    pen.circle(c, c, 400, 4.5)
    pen.circle(c, c, 386, 1.5)
    # 八芒星（兩個正方形）
    for off in (0, 45):
        pen.poly([polar(c, c, 386, off + 90 * i) for i in range(4)], width=3.5)
    # 八個頂點的小圓
    for i in range(8):
        x, y = polar(c, c, 386, 45 * i)
        pen.circle(x, y, 26, 3, fill=0)
        pen.circle(x, y, 26, 3)
        pen.circle(x, y, 8, 0, fill=255)
    # 星盤刻度
    for i in range(120):
        r0 = 330 if i % 5 == 0 else 342
        pen.line([polar(c, c, r0, i * 3), polar(c, c, 352, i * 3)], 2.2 if i % 5 == 0 else 1.2)
    pen.circle(c, c, 356, 2)
    pen.circle(c, c, 300, 3.5)
    # 七芒星 {7/3}
    pts = [polar(c, c, 300, 360 * ((i * 3) % 7) / 7) for i in range(7)]
    pen.poly(pts, width=3.2)
    # 內圈符文（朝內）
    pen.circle(c, c, 200, 3)
    glyph_ring(pen, c, c, 172, 18, 30, seed=60, width=2.4, inward=True)
    pen.circle(c, c, 145, 2)
    # 中央之眼：八角星 + 圓 + 瞳孔
    star = []
    for i in range(16):
        star.append(polar(c, c, 120 if i % 2 == 0 else 46, i * 22.5))
    pen.poly(star, width=3)
    pen.circle(c, c, 40, 4)
    pen.circle(c, c, 16, 0, fill=255)
    # 弧形裝飾虛線
    for i in range(24):
        pen.arc(c, c, 260, i * 15 + 2, i * 15 + 10, 3)
    save(glowify(pen.final(), 3, 0.55, 14, 0.25), "circle_enchant")


def circle_forge():
    """魂鑄陣：齒輪外環、符文帶、六芒星、交叉雙錘"""
    S = 1024
    c = S / 2
    pen = Pen(S)
    teeth = []
    for i in range(48):
        a0 = i * 7.5
        teeth += [polar(c, c, 470, a0), polar(c, c, 470, a0 + 2), polar(c, c, 500, a0 + 2.6), polar(c, c, 500, a0 + 4.9), polar(c, c, 470, a0 + 5.5)]
    pen.poly(teeth, width=4)
    pen.circle(c, c, 455, 2)
    glyph_ring(pen, c, c, 420, 32, 40, seed=200, width=3.4)
    pen.circle(c, c, 382, 5)
    for k in (0, 60):
        pen.poly([polar(c, c, 382, k + 120 * i) for i in range(3)], width=4.5)
    pen.circle(c, c, 220, 3.5)
    for i in range(6):
        x, y = polar(c, c, 300, 30 + 60 * i)
        pen.circle(x, y, 34, 3)
        GLYPHS[(i * 13) % 128](pen, x, y, 34, 0, 3.0)

    # 交叉雙錘（錘頭 + 錘柄），旋轉 ±40°
    def hammer(rot):
        cs, sn = math.cos(math.radians(rot)), math.sin(math.radians(rot))

        def t(x, y):
            return (c + x * cs - y * sn, c + x * sn + y * cs)

        pen.poly([t(-10, -150), t(10, -150), t(12, 150), t(-12, 150)], fill=255)
        pen.poly([t(-70, -170), t(70, -170), t(78, -120), t(-78, -120)], fill=255)
        pen.poly([t(-58, -160), t(58, -160), t(62, -130), t(-62, -130)], fill=0)
        pen.circle(*t(0, 158), 16, 0, fill=255)

    hammer(40)
    hammer(-40)
    pen.circle(c, c, 30, 0, fill=255)
    save(glowify(pen.final(), 3, 0.6, 16, 0.3), "circle_forge")


def circle_blueprint():
    """構築藍圖：方格、外框角括號、作圖輔助圓、3×3 合成格、尺寸標註"""
    S = 1024
    c = S / 2
    pen = Pen(S)
    for i in range(17):
        v = 64 + i * 56
        w = 2.2 if i % 4 == 0 else 0.9
        pen.line([(v, 64), (v, S - 64)], w, value=150 if i % 4 else 220)
        pen.line([(64, v), (S - 64, v)], w, value=150 if i % 4 else 220)
    for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        x0 = c - sx * 490
        y0 = c - sy * 490
        pen.line([(x0, y0 + sy * 90), (x0, y0), (x0 + sx * 90, y0)], 6)
    pen.circle(c, c, 400, 3)
    pen.circle(c, c, 410, 1.2)
    for i in range(72):
        pen.line([polar(c, c, 400, i * 5), polar(c, c, 385 if i % 6 == 0 else 393, i * 5)], 1.6)
    # 3×3 合成格
    cell = 110
    for gx in range(3):
        for gy in range(3):
            x = c + (gx - 1.5) * cell
            y = c + (gy - 1.5) * cell
            pen.poly([(x + 8, y + 8), (x + cell - 8, y + 8), (x + cell - 8, y + cell - 8), (x + 8, y + cell - 8)], width=4)
    # 對角作圖線
    pen.line([(c - 300, c - 300), (c + 300, c + 300)], 1.2, value=180)
    pen.line([(c - 300, c + 300), (c + 300, c - 300)], 1.2, value=180)
    pen.circle(c, c, 236, 2)
    # 尺寸標註線
    for y in (c - 200, c + 200):
        pen.line([(c - 165, y), (c + 165, y)], 2)
        pen.line([(c - 165, y - 12), (c - 165, y + 12)], 2)
        pen.line([(c + 165, y - 12), (c + 165, y + 12)], 2)
    for x in (c - 200, c + 200):
        pen.line([(x, c - 165), (x, c + 165)], 2)
    # 角落的小說明條（假文字）
    rng = random.Random(9)
    for k in range(4):
        x = 120 if k % 2 == 0 else S - 300
        y = 120 if k < 2 else S - 170
        for j in range(3):
            pen.line([(x, y + j * 18), (x + rng.randint(80, 170), y + j * 18)], 4, value=200)
    save(glowify(pen.final(), 2.5, 0.5, 10, 0.2), "circle_blueprint")


def circle_seal():
    """封印陣：八卦爻、六角、雙環、中央「封」字"""
    S = 512
    c = S / 2
    pen = Pen(S)
    pen.circle(c, c, 246, 5)
    pen.circle(c, c, 232, 1.6)
    trigrams = ["111", "011", "101", "001", "110", "010", "100", "000"]
    for i, tg in enumerate(trigrams):
        deg = i * 45
        for j, bit in enumerate(tg):
            r = 205 - j * 13
            if bit == "1":
                pen.line([polar(c, c, r, deg - 9), polar(c, c, r, deg + 9)], 5)
            else:
                pen.line([polar(c, c, r, deg - 9), polar(c, c, r, deg - 2)], 5)
                pen.line([polar(c, c, r, deg + 2), polar(c, c, r, deg + 9)], 5)
    pen.circle(c, c, 165, 3.5)
    pen.poly([polar(c, c, 165, 60 * i) for i in range(6)], width=3)
    pen.poly([polar(c, c, 165, 30 + 60 * i) for i in range(6)], width=1.5)
    pen.circle(c, c, 98, 2.5)
    pen.text(c, c + 4, "封", 128)
    save(glowify(pen.final(), 2.5, 0.6, 10, 0.3), "circle_seal")


def circle_compass():
    """全知之眼：羅盤玫瑰、360 刻度、方位字"""
    S = 1024
    c = S / 2
    pen = Pen(S)
    pen.circle(c, c, 494, 6)
    pen.circle(c, c, 478, 1.8)
    for i in range(360):
        r0 = 440 if i % 10 == 0 else (452 if i % 5 == 0 else 462)
        pen.line([polar(c, c, r0, i), polar(c, c, 474, i)], 2.2 if i % 10 == 0 else 1.0)
    pen.circle(c, c, 430, 2.5)
    for i, ch in enumerate("北東南西"):
        x, y = polar(c, c, 385, i * 90)
        pen.text(x, y, ch, 52, angle=-i * 90)
    # 主方位（長菱形，半邊實心）
    for i in range(4):
        deg = i * 90
        tip = polar(c, c, 340, deg)
        l = polar(c, c, 58, deg - 45)
        r = polar(c, c, 58, deg + 45)
        pen.poly([(c, c), l, tip], fill=255)
        pen.poly([(c, c), tip, r], width=3)
    for i in range(4):
        deg = 45 + i * 90
        tip = polar(c, c, 230, deg)
        l = polar(c, c, 40, deg - 45)
        r = polar(c, c, 40, deg + 45)
        pen.poly([(c, c), l, tip], width=2.5)
        pen.poly([(c, c), tip, r], fill=200)
    for i in range(16):
        deg = 22.5 + i * 22.5
        if i % 2 == 0:
            pen.line([polar(c, c, 60, deg), polar(c, c, 160, deg)], 1.6)
    pen.circle(c, c, 300, 2)
    pen.circle(c, c, 160, 2)
    pen.circle(c, c, 22, 0, fill=255)
    save(glowify(pen.final(), 2.5, 0.55, 12, 0.25), "circle_compass")


def rune_band():
    """可水平循環的符文帶（環繞光柱、立體符文環）"""
    W, H = 1024, 128
    pen = Pen(W, H)
    pen.line([(0, 10), (W, 10)], 3)
    pen.line([(0, 22), (W, 22)], 1.2)
    pen.line([(0, H - 10), (W, H - 10)], 3)
    pen.line([(0, H - 22), (W, H - 22)], 1.2)
    n = 16
    for i in range(n):
        x = (i + 0.5) * W / n
        GLYPHS[(i * 11 + 5) % 128](pen, x, H / 2, 62, 0, 4.2)
        pen.circle(i * W / n, H / 2, 4, 0, fill=255)
    save(glowify(pen.final(), 2, 0.6, 7, 0.3), "rune_band")


def glyph_sheet():
    """粒子用符文圖集：8×8 格，每格 64px"""
    S = 512
    pen = Pen(S)
    for i in range(64):
        x = (i % 8) * 64 + 32
        y = (i // 8) * 64 + 32
        GLYPHS[i * 2 % 128](pen, x, y, 40, 0, 4.0)
    save(glowify(pen.final(), 2, 0.7, 6, 0.3), "glyphs")


# ================================================================= 光、粒子

def radial(S):
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    c = (S - 1) / 2
    return np.sqrt((x - c) ** 2 + (y - c) ** 2) / (S / 2)


def glow():
    r = radial(128)
    a = np.clip(1 - r, 0, 1) ** 2.2
    a = np.maximum(a, np.exp(-(r / 0.12) ** 2))
    save(white(a, core=np.ones_like(a)), "glow")


def flare():
    S = 256
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    c = (S - 1) / 2
    dx, dy = (x - c) / c, (y - c) / c
    r = np.sqrt(dx ** 2 + dy ** 2)
    core = np.exp(-(r / 0.09) ** 2) + 0.35 * np.exp(-(r / 0.3) ** 2)
    spike = np.exp(-(dy / 0.018) ** 2) * np.clip(1 - np.abs(dx), 0, 1) ** 2.5 + np.exp(-(dx / 0.018) ** 2) * np.clip(1 - np.abs(dy), 0, 1) ** 2.5
    u, v = (dx + dy) / 1.414, (dx - dy) / 1.414
    diag = (np.exp(-(v / 0.012) ** 2) * np.clip(1 - np.abs(u) * 1.6, 0, 1) ** 3 + np.exp(-(u / 0.012) ** 2) * np.clip(1 - np.abs(v) * 1.6, 0, 1) ** 3) * 0.5
    a = np.clip(core + spike + diag, 0, 1)
    save(white(a, core=np.ones_like(a)), "flare")


def spark():
    W, H = 128, 16
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    u = x / (W - 1)
    v = (y - (H - 1) / 2) / (H / 2)
    width = 0.15 + 0.85 * u ** 0.5
    a = np.exp(-(v / (0.45 * width)) ** 2) * u ** 1.5
    a = np.clip(a * 1.2 + np.exp(-((u - 0.94) / 0.05) ** 2 - (v / 0.5) ** 2), 0, 1)
    save(white(a, core=np.ones_like(a)), "spark")


def ring():
    r = radial(512)
    a = np.exp(-((r - 0.9) / 0.035) ** 2) + (r < 0.9) * 0.3 * (r / 0.9) ** 6
    a = np.clip(a, 0, 1)
    save(white(a, core=np.ones_like(a)), "ring")


def fbm(shape, rng, octaves=5, base=4, tile=True):
    """週期性 fbm（tile=True 時四邊可循環）"""
    h, w = shape
    out = np.zeros(shape, np.float32)
    amp = 1.0
    for o in range(octaves):
        f = base * 2 ** o
        g = rng.normal(0, 1, (f + 1, f + 1)).astype(np.float32)
        if tile:
            g[-1, :] = g[0, :]
            g[:, -1] = g[:, 0]
        img = Image.fromarray(g).resize((w, h), Image.BICUBIC)
        out += np.asarray(img) * amp
        amp *= 0.5
    out -= out.min()
    return out / (out.max() + 1e-9)


def smoke():
    rng = np.random.default_rng(4)
    r = radial(128)
    n = fbm((128, 128), rng, 5, 3)
    a = np.clip(1 - r * 1.05, 0, 1) ** 1.3 * (0.35 + 0.9 * n)
    a = np.clip(a, 0, 1) * 0.9
    save(white(a, core=0.8 + 0.2 * n), "smoke")


def flame():
    """火焰（灰階，遊戲內依壽命漸變上色）：水滴形 + 噪聲舌焰"""
    S = 128
    rng = np.random.default_rng(7)
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    u = (x - S / 2) / (S / 2)
    v = y / S  # 0 = 頂, 1 = 底
    n = fbm((S, S), rng, 4, 4)
    width = 0.55 * np.clip(v, 0, 1) ** 0.6 * (1 - np.clip((v - 0.78) / 0.22, 0, 1) ** 2)
    width = width * (0.75 + 0.5 * n)
    a = np.clip(1 - np.abs(u) / (width + 1e-3), 0, 1) ** 0.8
    a *= np.clip(v * 1.6 - 0.05 + n * 0.4, 0, 1)
    core = np.clip(1 - np.abs(u) / (width * 0.5 + 1e-3), 0, 1) * np.clip(v * 1.4 - 0.3, 0, 1)
    a = np.clip(a + core * 0.6, 0, 1)
    save(white(a, core=np.clip(0.6 + core * 0.4, 0, 1)), "flame")


def shard():
    S = 64
    pen = Pen(S)
    pen.poly([(32, 2), (40, 30), (32, 62), (24, 30)], fill=255)
    m = pen.final()
    save(glowify(m, 1.5, 0.6), "shard")


def beam():
    """光柱（垂直方向可循環）：白熱核心 + 流動條紋"""
    W, H = 64, 256
    rng = np.random.default_rng(11)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    u = (x - (W - 1) / 2) / (W / 2)
    v = y / H
    streak = np.zeros((H, W), np.float32)
    for _ in range(14):
        cx = rng.uniform(-0.8, 0.8)
        f = rng.integers(1, 4)
        ph = rng.uniform(0, 6.28)
        streak += np.exp(-((u - cx) / 0.05) ** 2) * (0.5 + 0.5 * np.sin(2 * np.pi * f * v + ph)) ** 3 * rng.uniform(0.2, 0.6)
    body = np.exp(-(u / 0.55) ** 2)
    core = np.exp(-(u / 0.14) ** 2)
    a = np.clip(body * 0.45 + core + streak * body, 0, 1)
    save(white(a, core=np.clip(0.75 + core * 0.25, 0, 1)), "beam")


def holo_globe():
    """全息地球（等距圓柱投影）：網點大陸 + 海岸線 + 經緯網"""
    W, H = 1024, 512
    rng = np.random.default_rng(21)
    n = fbm((H, W), rng, 6, 3)
    lat = np.abs(np.linspace(-1, 1, H))[:, None]
    land = (n + 0.12 * (1 - lat) - 0.5) > 0
    land &= lat < 0.92
    li = Image.fromarray((land * 255).astype(np.uint8))
    edge = np.asarray(li.filter(ImageFilter.FIND_EDGES)).astype(np.float32) / 255
    y, x = np.mgrid[0:H, 0:W]
    dots = ((x % 6 < 3) & (y % 6 < 3)).astype(np.float32)
    a = land * dots * 0.55 + np.clip(edge * 2, 0, 1)
    grid = ((x % (W // 24)) < 2) | ((y % (H // 12)) < 2)
    a = np.maximum(a, grid * 0.35)
    a = np.maximum(a, (np.abs(y - H / 2) < 2) * 0.8)
    a = np.clip(a, 0, 1)
    save(white(a), "holo_globe")


def map_sheet():
    """展開的地圖（彩色、不透明羊皮紙）：等高線、河流、虛線路徑、紅色叉"""
    S = 512
    rng = np.random.default_rng(31)
    n = fbm((S, S), rng, 6, 3, tile=False)
    paper = np.stack([0.93 - n * 0.12, 0.85 - n * 0.14, 0.66 - n * 0.16], -1)
    vign = radial(S)
    paper *= (1 - 0.35 * np.clip(vign - 0.6, 0, 1) * 2.5)[..., None]
    rgb = np.clip(paper, 0, 1)
    h = fbm((S, S), np.random.default_rng(32), 5, 2, tile=False)
    ink = np.zeros((S, S), np.float32)
    for lv in np.linspace(0.2, 0.85, 11):
        ink += np.exp(-((h - lv) / 0.004) ** 2)
    sea = h < 0.32
    rgb[sea] = rgb[sea] * np.array([0.72, 0.86, 0.95])
    ink = np.clip(ink, 0, 1)
    rgb = rgb * (1 - ink[..., None] * np.array([0.55, 0.6, 0.7]))
    img = Image.fromarray((rgb * 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(img)
    pts = [(60, 420), (130, 360), (170, 300), (260, 270), (300, 190), (390, 150), (440, 90)]
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        for k in range(8):
            if k % 2 == 0:
                t0, t1 = k / 8, (k + 1) / 8
                d.line([(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0), (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)], fill=(120, 40, 30), width=4)
    d.line([(425, 75), (455, 105)], fill=(190, 20, 20), width=8)
    d.line([(455, 75), (425, 105)], fill=(190, 20, 20), width=8)
    # 角落羅盤
    cx, cy = 70, 70
    d.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], outline=(80, 50, 30), width=3)
    d.polygon([(cx, cy - 46), (cx + 8, cy), (cx, cy + 46), (cx - 8, cy)], fill=(80, 50, 30))
    d.rectangle([4, 4, S - 5, S - 5], outline=(90, 60, 35), width=6)
    a = np.full((S, S), 255, np.uint8)
    out = np.dstack([np.asarray(img), a])
    save(Image.fromarray(out, "RGBA"), "map_sheet")


def holo_grid():
    """全息掃描平面：同心圓 + 放射線 + 點陣"""
    S = 512
    c = S / 2
    pen = Pen(S)
    for r in range(40, 250, 40):
        pen.circle(c, c, r, 1.5 if r % 80 else 2.5)
    for i in range(24):
        pen.line([polar(c, c, 30, i * 15), polar(c, c, 250, i * 15)], 1.0, value=170)
    pen.circle(c, c, 252, 3)
    save(glowify(pen.final(), 2, 0.5, 8, 0.2), "holo_grid")


def scan_wedge():
    """雷達掃描扇形（從 0° 起向順時針漸隱）"""
    S = 512
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    c = (S - 1) / 2
    ang = (np.degrees(np.arctan2(x - c, -(y - c))) + 360) % 360
    r = np.sqrt((x - c) ** 2 + (y - c) ** 2) / c
    a = np.clip(1 - ang / 70, 0, 1) ** 2 * (r < 0.98) * (0.2 + 0.8 * r)
    a = np.maximum(a, np.exp(-(ang / 1.2) ** 2) * (r < 0.98))
    save(white(np.clip(a, 0, 1)), "scan_wedge")


def slash_arc():
    """門的光痕（橫向漸隱的弧線條）"""
    W, H = 256, 32
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    u = x / (W - 1)
    v = (y - (H - 1) / 2) / (H / 2)
    a = np.exp(-(v / (0.2 + 0.6 * u)) ** 2) * u ** 1.2 * (1 - np.clip((u - 0.92) / 0.08, 0, 1))
    save(white(np.clip(a * 1.3, 0, 1), core=np.ones_like(a)), "slash_trail")


def starfield():
    """星空穹頂（可水平循環）"""
    W, H = 1024, 512
    rng = np.random.default_rng(41)
    a = np.zeros((H, W), np.float32)
    neb = fbm((H, W), rng, 5, 3)
    a += np.clip(neb - 0.4, 0, 1) * 0.9
    for _ in range(3000):
        x, y = rng.integers(0, W), rng.integers(0, H)
        s = rng.uniform(0.3, 1.0) ** 3
        a[y, x] = max(a[y, x], 0.5 + 0.5 * s)
        if s > 0.5:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                a[(y + dy) % H, (x + dx) % W] = max(a[(y + dy) % H, (x + dx) % W], s * 0.5)
    fade = np.sin(np.linspace(0, np.pi, H))[:, None] ** 0.5
    save(white(np.clip(a * fade, 0, 1)), "starfield")


def vignette():
    """全螢幕彩色暈影（中央透明、四周漸濃）→ textures/gui/vignette.png"""
    S = 256
    y, x = np.mgrid[0:S, 0:S].astype(np.float32)
    u, v = (x - (S - 1) / 2) / (S / 2), (y - (S - 1) / 2) / (S / 2)
    r = np.sqrt(u ** 2 * 0.9 + v ** 2 * 1.1)
    a = np.clip((r - 0.55) / 0.75, 0, 1) ** 1.6
    img = white(a, core=np.ones_like(a))
    d = os.path.join(ROOT, "textures", "gui")
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, "vignette.png"))
    print("   vignette")


def main():
    print("textures:")
    vignette()
    circle_enchant()
    circle_forge()
    circle_blueprint()
    circle_seal()
    circle_compass()
    rune_band()
    glyph_sheet()
    glow()
    flare()
    spark()
    ring()
    smoke()
    flame()
    shard()
    beam()
    holo_globe()
    holo_grid()
    scan_wedge()
    map_sheet()
    slash_arc()
    starfield()


if __name__ == "__main__":
    main()
