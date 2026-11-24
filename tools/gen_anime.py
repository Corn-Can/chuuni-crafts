"""
打架動漫風的畫面素材（v2）：
  - 名卡（照 1014 天災的排版）：紅色直式印章 + 思源宋體 Heavy 標題 + 彩色光暈 + 字距拉開的英文
  - 狀聲字（轟、鏘、嗡……）：極粗宋體、白字黑框、斜切、背後帶速度筆觸
  - 集中線 4 幀（遊戲裡「一拍二」輪播）
  - 打擊火花 6 幀序列、硬邊斬擊弧、雷光貼圖
字體：tools/fonts/SourceHanSerifTW-{Bold,Heavy}.otf（SIL OFL，見 tools/fonts/LICENSE.txt）
用法：python tools/gen_anime.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(__file__)
ROOT = os.path.join(HERE, "..", "src", "client", "resources", "assets", "chuuni")
GUI = os.path.join(ROOT, "textures", "gui")
FX = os.path.join(ROOT, "textures", "fx")
HEAVY = os.path.join(HERE, "fonts", "SourceHanSerifTW-Heavy.otf")
BOLD = os.path.join(HERE, "fonts", "SourceHanSerifTW-Bold.otf")
SS = 3

# key: (印章兩字, 標題, 英文, 光暈色)
CARDS = {
    "enchant": ("禁咒", "星辰刻印", "STELLAR  ENGRAVING", (170, 110, 255)),
    "anvil": ("魂鑄", "千錘百鍊", "SOUL  FORGE", (255, 120, 40)),
    "craft": ("構築", "萬象解放", "ALL  CREATION", (80, 190, 255)),
    "craft_hand": ("構築", "掌中乾坤", "PALM  OF  CREATION", (80, 190, 255)),
    "cartography": ("測繪", "全知之眼", "THE  OMNISCIENT  EYE", (40, 230, 200)),
    "furnace": ("獄炎", "煉成", "HELLFIRE  ALCHEMY", (255, 90, 20)),
    "blast": ("蒼焰", "極熱熔斷", "AZURE  MELTDOWN", (70, 140, 255)),
    "smoker": ("燻香", "煙雲祕法", "SECRET  ART  OF  SMOKE", (220, 150, 90)),
    "door_open": ("封印", "解除", "SEAL  RELEASED", (90, 255, 160)),
    "door_close": ("封印", "再封", "RESEALED", (130, 120, 255)),
}

SFX = ["轟", "鏘", "嗡", "咚", "斬", "爆", "啪", "鎖", "閃"]


def save(img, path, blur=True):
    full = os.path.join(FX, path[3:] + ".png") if path.startswith("fx/") else os.path.join(GUI, path + ".png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    if blur:
        with open(full + ".mcmeta", "w") as f:
            json.dump({"texture": {"blur": True, "clamp": True}}, f)


def text_width(text, font, spacing):
    return sum(font.getlength(ch) for ch in text) + spacing * (len(text) - 1)


def spaced(draw, xy, text, font, fill, spacing):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += font.getlength(ch) + spacing


# ================================================================= 名卡

def seal(key, chars):
    """直式紅印：圓角紅底、米白字、印泥斑駁"""
    w, h = 200, 380
    rng = np.random.default_rng(abs(hash(key)) % 2 ** 31)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((8, 8, w - 8, h - 8), radius=16, fill=(184, 30, 26, 255))
    d.rounded_rectangle((20, 20, w - 20, h - 20), radius=10, outline=(250, 236, 222, 255), width=4)
    font = ImageFont.truetype(HEAVY, 140)
    for i, ch in enumerate(chars):
        cw = font.getlength(ch)
        d.text(((w - cw) / 2, 22 + i * 168), ch, font=font, fill=(250, 240, 228, 255))
    a = np.asarray(img, np.float32)
    noise = rng.random((h, w))
    big = np.asarray(Image.fromarray((rng.random((h // 8, w // 8)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), np.float32) / 255
    holes = (noise > 0.985) | (big < 0.1)
    a[..., 3] = np.where(holes, a[..., 3] * 0.15, a[..., 3])
    a[..., :3] *= (0.85 + 0.25 * big)[..., None]
    save(Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA"), f"card/{key}_seal")
    return w, h


def title(key, text, tint):
    size = 230
    font = ImageFont.truetype(HEAVY, size)
    spacing = int(size * 0.12)
    w = int(text_width(text, font, spacing)) + 160
    h = int(size * 1.5)
    mask = Image.new("L", (w, h), 0)
    spaced(ImageDraw.Draw(mask), (80, int(size * 0.08)), text, font, 255, spacing)
    m = np.asarray(mask, np.float32) / 255
    # 白字：上方微暖、下方微冷的垂直漸層，加一道銳利的高光線
    yy = np.linspace(0, 1, h)[:, None]
    grad = np.concatenate([np.ones((h, 1, 1)) * np.array([255, 252, 246]) * (1 - yy[..., None] * 0.16)], -1)
    grad = np.broadcast_to(grad, (h, w, 3)).copy()
    hi = np.exp(-((yy - 0.36) / 0.015) ** 2)[..., None]
    grad = np.clip(grad + hi * 40, 0, 255)
    shadow = np.asarray(mask.filter(ImageFilter.GaussianBlur(10)), np.float32) / 255
    out = np.zeros((h, w, 4), np.float32)
    out[..., 3] = shadow * 0.8 * 255
    out[..., :3] = grad * m[..., None]
    out[..., 3] = np.maximum(out[..., 3], m * 255)
    save(Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA"), f"card/{key}")
    glow = np.asarray(mask.filter(ImageFilter.GaussianBlur(28)), np.float32) / 255
    g = np.zeros((h, w, 4), np.float32)
    g[..., 0], g[..., 1], g[..., 2] = tint
    g[..., 3] = np.clip(glow * 1.7, 0, 1) * 255
    save(Image.fromarray(g.astype(np.uint8), "RGBA"), f"card/{key}_glow")
    return w, h


def english(key, text):
    ef = ImageFont.truetype(BOLD, 40)
    es = 20
    ew = int(text_width(text, ef, es)) + 40
    em = Image.new("RGBA", (ew, 66), (0, 0, 0, 0))
    spaced(ImageDraw.Draw(em), (20, 4), text, ef, (228, 224, 216, 255), es)
    save(em, f"card/{key}_en")
    return ew, 66


# ================================================================= 狀聲字

def onomatopoeia(ch):
    """極粗宋體、斜切、白字 + 粗黑框 + 外圈白框，背後一束速度筆觸"""
    S = 640
    font = ImageFont.truetype(HEAVY, 470)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).text((S / 2, S / 2 - 20), ch, font=font, fill=255, anchor="mm")
    # 斜切 + 稍微拉高
    mask = mask.transform((S, S), Image.AFFINE, (1, 0.28, -0.28 * S / 2, 0, 1.06, -0.03 * S), Image.BICUBIC)
    inner = np.asarray(mask, np.float32) / 255
    black = np.asarray(mask.filter(ImageFilter.MaxFilter(31)), np.float32) / 255
    outer = np.asarray(mask.filter(ImageFilter.MaxFilter(31)).filter(ImageFilter.MaxFilter(15)), np.float32) / 255
    # 速度筆觸（左下 → 右上）
    rng = np.random.default_rng(len(ch) * 97 + ord(ch))
    streak = Image.new("L", (S * SS, S * SS), 0)
    d = ImageDraw.Draw(streak)
    for _ in range(26):
        y = rng.uniform(0.2, 0.85) * S * SS
        x0 = rng.uniform(-0.1, 0.3) * S * SS
        L = rng.uniform(0.3, 0.75) * S * SS
        wdt = rng.uniform(4, 16) * SS
        ang = -0.32
        x1 = x0 + L * math.cos(ang)
        y1 = y + L * math.sin(ang)
        nx, ny = -math.sin(ang) * wdt, math.cos(ang) * wdt
        d.polygon([(x0, y), (x1 + nx * 0.1, y1 + ny * 0.1), (x1 - nx * 0.1, y1 - ny * 0.1), (x0 - nx, y - ny)], fill=255)
    streak = np.asarray(streak.resize((S, S), Image.LANCZOS), np.float32) / 255 * 0.85
    out = np.zeros((S, S, 4), np.float32)
    # 由外到內：速度筆觸（白）→ 外白框 → 黑框 → 白字
    a = np.maximum(streak * (1 - outer), outer)
    rgb = np.ones((S, S, 3)) * 255
    rgb = np.where((black > 0.5)[..., None] & (inner < 0.5)[..., None], 0, rgb)
    # 白字內的網點陰影（下半部）
    yy, xx = np.mgrid[0:S, 0:S]
    dots = ((xx % 9 - 4) ** 2 + (yy % 9 - 4) ** 2) < (yy / S) ** 2 * 18
    shade = (inner > 0.5) & dots & (yy > S * 0.5)
    rgb = np.where(shade[..., None], 170, rgb)
    out[..., :3] = rgb
    out[..., 3] = np.clip(np.maximum(a, black), 0, 1) * 255
    img = Image.fromarray(out.astype(np.uint8), "RGBA")
    save(img, f"sfx/{ord(ch):x}")


# ================================================================= 集中線

def speed_lines(i):
    W, H = 1280, 720
    rng = np.random.default_rng(100 + i)
    img = Image.new("L", (W * 2, H * 2), 0)
    d = ImageDraw.Draw(img)
    cx, cy = W, H
    diag = math.hypot(W, H) * 1.05
    for _ in range(230):
        a = rng.uniform(0, math.tau)
        inner = rng.uniform(0.42, 0.78) * diag
        width = rng.uniform(2, 13) * (1.4 if rng.random() < 0.15 else 1)
        dx, dy = math.cos(a), math.sin(a)
        # 橢圓化：讓中心留白區域符合畫面比例
        ix, iy = cx + dx * inner * 1.0, cy + dy * inner * 0.62
        ox, oy = cx + dx * diag * 1.5, cy + dy * diag * 1.5
        nx, ny = -dy * width, dx * width
        d.polygon([(ix, iy), (ox + nx, oy + ny), (ox - nx, oy - ny)], fill=255)
    img = img.resize((W, H), Image.LANCZOS)
    a = np.asarray(img, np.float32) / 255
    save(Image.fromarray(np.dstack([np.full_like(a, 255), np.full_like(a, 255), np.full_like(a, 255), a * 255]).astype(np.uint8), "RGBA"),
         f"speed/{i}")


# ================================================================= 特效貼圖（硬邊）

def hit_spark():
    """打擊火花 6 幀（橫排 6×256）：尖刺星形爆開 → 變細 → 斷成碎片"""
    S = 256
    frames = []
    rng = np.random.default_rng(5)
    spikes = [(rng.uniform(0, math.tau), rng.uniform(0.6, 1.0), rng.uniform(0.06, 0.13)) for _ in range(11)]
    for k in range(6):
        t = k / 5
        big = Image.new("L", (S * SS, S * SS), 0)
        d = ImageDraw.Draw(big)
        c = S * SS / 2
        reach = (0.35 + 0.6 * (1 - (1 - t) ** 2)) * c
        for ang, ln, wd in spikes:
            L = reach * ln
            w = wd * (1 - t * 0.85) * c
            inner = c * 0.12 * t * 3.5
            tip = (c + math.cos(ang) * L, c + math.sin(ang) * L)
            base = (c + math.cos(ang) * inner, c + math.sin(ang) * inner)
            nx, ny = -math.sin(ang) * w, math.cos(ang) * w
            d.polygon([(base[0] + nx, base[1] + ny), tip, (base[0] - nx, base[1] - ny)], fill=255)
        core = c * 0.28 * (1 - t) ** 1.5
        if core > 1:
            d.ellipse([c - core, c - core, c + core, c + core], fill=255)
        if t > 0.4:
            # 中心被「打穿」
            hole = c * 0.5 * (t - 0.4) * 2
            d.ellipse([c - hole, c - hole, c + hole, c + hole], fill=0)
        frames.append(big.resize((S, S), Image.LANCZOS))
    strip = Image.new("L", (S * 6, S), 0)
    for i, f in enumerate(frames):
        strip.paste(f, (i * S, 0))
    a = np.asarray(strip, np.float32) / 255
    save(Image.fromarray(np.dstack([np.full_like(a, 255)] * 3 + [a * 255]).astype(np.uint8), "RGBA"), "fx/hit_spark")


def slash_arc():
    """硬邊斬擊弧（新月形），內含幾道平行刷痕；u = 沿弧、v = 內外"""
    W, H = 1024, 256
    big = Image.new("L", (W * SS, H * SS), 0)
    d = ImageDraw.Draw(big)
    pts_top, pts_bot = [], []
    for i in range(101):
        u = i / 100
        thick = math.sin(math.pi * u ** 0.7) ** 1.2
        x = u * W * SS
        pts_top.append((x, (0.5 - 0.42 * thick) * H * SS))
        pts_bot.append((x, (0.5 + 0.12 * thick) * H * SS))
    d.polygon(pts_top + pts_bot[::-1], fill=255)
    for k in range(4):
        yy = (0.22 + k * 0.07) * H * SS
        d.line([(W * SS * 0.2, yy), (W * SS * 0.85, yy + 8 * SS)], fill=0, width=int(3 * SS))
    a = np.asarray(big.resize((W, H), Image.LANCZOS), np.float32) / 255
    save(Image.fromarray(np.dstack([np.full_like(a, 255)] * 3 + [a * 255]).astype(np.uint8), "RGBA"), "fx/slash_arc")


def bolt():
    """雷光線條：白熱細芯 + 窄邊，硬邊緣（不是柔光）"""
    W, H = 64, 32
    y = np.abs(np.linspace(-1, 1, H))[:, None] * np.ones((1, W))
    a = np.clip(1.15 - y * 2.2, 0, 1) ** 0.6
    core = (y < 0.18).astype(np.float32)
    lum = 0.7 + 0.3 * core
    img = np.dstack([lum * 255, lum * 255, lum * 255, a * 255]).astype(np.uint8)
    save(Image.fromarray(img, "RGBA"), "fx/bolt")


def jag_ring():
    """斷續的衝擊環：粗細不一、帶缺口，邊緣銳利"""
    S = 512
    rng = np.random.default_rng(12)
    big = Image.new("L", (S * SS, S * SS), 0)
    d = ImageDraw.Draw(big)
    c = S * SS / 2
    n = 40
    for i in range(n):
        if rng.random() < 0.18:
            continue
        a0 = i / n * 360
        a1 = a0 + 360 / n * rng.uniform(0.55, 0.95)
        w = rng.uniform(6, 22) * SS
        r = c * 0.9
        d.arc([c - r, c - r, c + r, c + r], a0, a1, fill=255, width=int(w))
    a = np.asarray(big.resize((S, S), Image.LANCZOS), np.float32) / 255
    save(Image.fromarray(np.dstack([np.full_like(a, 255)] * 3 + [a * 255]).astype(np.uint8), "RGBA"), "fx/jag_ring")


def main():
    sizes = {}
    for key, (sl, tt, en, tint) in CARDS.items():
        sw, sh = seal(key, sl)
        tw, th = title(key, tt, tint)
        ew, eh = english(key, en)
        sizes[key] = {"seal": [sw, sh], "title": [tw, th], "en": [ew, eh]}
        print("  card", key)
    with open(os.path.join(ROOT, "cards.json"), "w", encoding="utf-8") as f:
        json.dump(sizes, f, indent=1)
    for ch in SFX:
        onomatopoeia(ch)
    print("  sfx", "".join(SFX))
    for i in range(4):
        speed_lines(i)
    print("  speed lines")
    hit_spark()
    slash_arc()
    bolt()
    jag_ring()
    print("  fx")


if __name__ == "__main__":
    main()
