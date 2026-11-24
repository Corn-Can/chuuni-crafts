"""
招式名卡：楷書大字（漸層燙金 + 深色描邊 + 外發光）壓在一道墨痕筆刷上，下方附英文副標。
輸出 textures/gui/title/<key>.png（2048×512，遊戲內縮小顯示，保持清晰）。
用法：python tools/gen_titles.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "client", "resources", "assets", "chuuni")
OUT = os.path.join(ROOT, "textures", "gui", "title")
FONT_ZH = "C:/Windows/Fonts/kaiu.ttf"
FONT_EN = "C:/Windows/Fonts/georgiab.ttf"
W, H = 2048, 512

# key: (中文, 英文, 字頂色, 字底色, 外光色, 墨痕色)
TITLES = {
    "enchant": ("禁咒・星辰刻印", "FORBIDDEN ART  ·  STELLAR ENGRAVING", (255, 250, 220), (214, 150, 255), (170, 90, 255), (40, 10, 70)),
    "anvil": ("千錘百鍊・魂鑄", "THOUSAND STRIKES  ·  SOUL FORGE", (255, 248, 210), (255, 130, 40), (255, 90, 20), (70, 14, 6)),
    "craft": ("萬象構築・解放", "ALL CREATION  ·  RELEASE", (240, 255, 255), (90, 210, 255), (40, 170, 255), (6, 30, 60)),
    "craft_hand": ("掌中構築", "PALM OF CREATION", (240, 255, 255), (120, 230, 255), (40, 170, 255), (6, 30, 60)),
    "cartography": ("天地測繪・全知之眼", "HEAVEN AND EARTH  ·  OMNISCIENT EYE", (240, 255, 245), (60, 240, 200), (20, 220, 190), (4, 40, 40)),
    "furnace": ("獄炎煉成", "HELLFIRE  ALCHEMY", (255, 245, 200), (255, 90, 30), (255, 60, 10), (60, 6, 2)),
    "blast": ("蒼焰・極熱熔斷", "AZURE FLAME  ·  MELTDOWN", (235, 250, 255), (70, 150, 255), (40, 110, 255), (4, 16, 60)),
    "smoker": ("燻香・煙雲祕法", "INCENSE  ·  SECRET ART OF SMOKE", (255, 245, 225), (230, 160, 90), (200, 120, 255), (40, 20, 40)),
    "door_open": ("封印解除！", "SEAL  RELEASED", (245, 255, 235), (120, 255, 150), (60, 255, 140), (6, 40, 20)),
    "door_close": ("再・封印", "RESEALED", (235, 240, 255), (130, 140, 255), (90, 90, 255), (12, 10, 50)),
}


def font_fit(text, font_path, max_w, size):
    while size > 20:
        f = ImageFont.truetype(font_path, size)
        if f.getbbox(text)[2] - f.getbbox(text)[0] <= max_w:
            return f
        size -= 4
    return ImageFont.truetype(font_path, size)


def brush_swash(color, rng):
    """墨痕：沿略彎曲的路徑壓出寬窄不一、邊緣帶飛白的筆刷"""
    img = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(img)
    x0, x1 = 120, W - 120
    steps = 400
    for i in range(steps):
        t = i / (steps - 1)
        x = x0 + (x1 - x0) * t
        y = H * 0.52 + math.sin(t * 3.1 + 0.4) * 22 - t * 18
        press = (math.sin(t * math.pi) ** 0.6) * (0.75 + 0.25 * math.sin(t * 23))
        w = 150 * press + 20
        d.ellipse([x - w * 0.55, y - w, x + w * 0.55, y + w * 0.9], fill=255)
    a = np.asarray(img).astype(np.float32) / 255
    # 飛白：沿筆刷方向的細長噪聲條紋
    col = np.convolve(rng.random(H), np.ones(3) / 3, mode="same").astype(np.float32)
    streak = np.repeat(col[:, None], W, axis=1)
    xs = np.linspace(0, 1, W)[None, :]
    dry = np.clip((xs - 0.75) * 4, 0, 1) + np.clip((0.12 - xs) * 6, 0, 1)
    a *= np.clip(1 - (streak < 0.45) * dry * 1.2, 0, 1)
    a = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))).astype(np.float32) / 255
    rgba = np.zeros((H, W, 4), np.float32)
    rgba[..., :3] = np.array(color) / 255
    rgba[..., 3] = a * 0.88
    return rgba


def text_layer(text, font, cx, cy):
    m = Image.new("L", (W, H), 0)
    ImageDraw.Draw(m).text((cx, cy), text, font=font, fill=255, anchor="mm")
    return m


def compose(key, zh, en, top, bottom, glowc, ink):
    rng = np.random.default_rng(abs(hash(key)) % 2 ** 32)
    out = brush_swash(ink, rng)

    def over(dst, rgb, alpha):
        a = alpha[..., None]
        dst[..., :3] = dst[..., :3] * (1 - a) + rgb * a
        dst[..., 3] = dst[..., 3] + alpha * (1 - dst[..., 3])

    font = font_fit(zh, FONT_ZH, W - 260, 250)
    cy = int(H * 0.44)
    mask = text_layer(zh, font, W // 2, cy)
    m = np.asarray(mask).astype(np.float32) / 255
    # 外發光
    g = np.asarray(mask.filter(ImageFilter.GaussianBlur(28))).astype(np.float32) / 255
    over(out, np.array(glowc) / 255, np.clip(g * 1.6, 0, 1) * 0.9)
    # 深色描邊
    stroke = np.asarray(mask.filter(ImageFilter.MaxFilter(17))).astype(np.float32) / 255
    stroke = np.asarray(Image.fromarray((stroke * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.5))).astype(np.float32) / 255
    over(out, np.array(ink) / 255 * 0.5, stroke)
    # 細亮邊（描邊外緣的一圈光）
    rim = np.clip(np.asarray(mask.filter(ImageFilter.MaxFilter(9))).astype(np.float32) / 255 - m, 0, 1)
    over(out, np.array(glowc) / 255 * 0.6 + 0.4, rim * 0.9)
    # 漸層燙金本體 + 上半部高光帶
    bbox = mask.getbbox()
    yy = np.linspace(0, 1, H)[:, None]
    t = np.clip((yy * H - bbox[1]) / max(1, bbox[3] - bbox[1]), 0, 1)
    grad = np.array(top)[None, None, :] * (1 - t[..., None]) + np.array(bottom)[None, None, :] * t[..., None]
    shine = np.exp(-((t - 0.38) / 0.06) ** 2)[..., None] * 0.55
    body = np.clip(grad / 255 + shine, 0, 1)
    body = np.broadcast_to(body, (H, W, 3))
    over(out, body, m)
    # 英文副標
    fe = font_fit(en, FONT_EN, W - 600, 54)
    em = text_layer(en, fe, W // 2, int(H * 0.82))
    eg = np.asarray(em.filter(ImageFilter.GaussianBlur(8))).astype(np.float32) / 255
    over(out, np.array(glowc) / 255, eg * 0.7)
    over(out, np.array([1.0, 0.97, 0.9]), np.asarray(em).astype(np.float32) / 255)
    # 左右裝飾線
    deco = Image.new("L", (W, H), 0)
    dd = ImageDraw.Draw(deco)
    ey = int(H * 0.82)
    bw = fe.getbbox(en)[2] - fe.getbbox(en)[0]
    for s in (-1, 1):
        xa = W // 2 + s * (bw // 2 + 30)
        xb = W // 2 + s * (bw // 2 + 260)
        dd.line([(xa, ey), (xb, ey)], fill=255, width=4)
        dd.polygon([(xa + s * 0, ey - 9), (xa + s * 12, ey), (xa, ey + 9), (xa - s * 12, ey)], fill=255)
    over(out, np.array([1.0, 0.95, 0.85]), np.asarray(deco).astype(np.float32) / 255)
    img = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, key + ".png"))
    print("  ", key)


def main():
    print("titles:")
    for k, v in TITLES.items():
        compose(k, *v)


if __name__ == "__main__":
    main()
