"""
特效 3D 模型：程式建模 → 四邊形清單 JSON（assets/chuuni/fxmodels/*.json），附手繪材質。
JSON 格式：{"texture": "chuuni:textures/fx/x.png", "quads": [[x,y,z,u,v,nx,ny,nz] × 4 攤平成 32 個 float, ...]}
三角形以「重複最後一個頂點」的退化四邊形表示。座標單位：方塊。
用法：python tools/gen_meshes.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "client", "resources", "assets", "chuuni")
MESH_OUT = os.path.join(ROOT, "fxmodels")
TEX_OUT = os.path.join(ROOT, "textures", "fx")


class Mesh:
    def __init__(self, texture):
        self.texture = texture
        self.quads = []

    def quad(self, verts, uvs, normal=None):
        """verts：4 個 (x,y,z)，uvs：4 個 (u,v)（0~1）"""
        if normal is None:
            a, b, c = (np.array(v, float) for v in verts[:3])
            n = np.cross(b - a, c - a)
            ln = np.linalg.norm(n)
            n = n / ln if ln > 1e-9 else np.array([0, 1, 0.0])
        else:
            n = normal
        q = []
        for (x, y, z), (u, v) in zip(verts, uvs):
            q += [round(float(x), 5), round(float(y), 5), round(float(z), 5), round(float(u), 5), round(float(v), 5),
                  round(float(n[0]), 4), round(float(n[1]), 4), round(float(n[2]), 4)]
        self.quads.append(q)

    def tri(self, a, b, c, ua, ub, uc):
        self.quad([a, b, c, c], [ua, ub, uc, uc])

    def box(self, lo, hi, region, rot=None):
        """軸對齊方塊（region = 材質上的 (u0,v0,u1,v1)，每面拉伸填滿）。rot：繞 Y 軸角度（度）"""
        x0, y0, z0 = lo
        x1, y1, z1 = hi
        u0, v0, u1, v1 = region
        faces = [
            ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]),  # 南 +z
            ([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)]),  # 北 -z
            ([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)]),  # 東 +x
            ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)]),  # 西 -x
            ([(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)]),  # 上
            ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)]),  # 下
        ]
        uv = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
        for f in faces:
            if rot:
                a = math.radians(rot)
                f = [(x * math.cos(a) - z * math.sin(a), y, x * math.sin(a) + z * math.cos(a)) for x, y, z in f]
            self.quad(f, uv)

    def save(self, name):
        os.makedirs(MESH_OUT, exist_ok=True)
        with open(os.path.join(MESH_OUT, name + ".json"), "w") as f:
            json.dump({"texture": self.texture, "quads": self.quads}, f, separators=(",", ":"))
        print("  mesh", name, len(self.quads), "quads")


# ================================================================= 材質

def hammer_texture():
    """靈魂之錘材質（256²）：
    A 區 (0,0)-(128,128) 錘頭金屬：深色鋼板 + 斜面高光 + 刮痕 + 發光符文刻槽
    B 區 (128,0)-(256,128) 錘頭端面：同心金屬圈 + 發光核心
    C 區 (0,128)-(128,256) 握柄：纏繞皮革條
    D 區 (128,128)-(256,256) 金屬箍：亮金色"""
    S = 256
    rng = np.random.default_rng(5)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    # A：鋼板
    base = rng.normal(0, 1, (128, 128))
    base = np.asarray(Image.fromarray(((base * 0.5 + 0.5).clip(0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2))).astype(float) / 255
    steel = np.stack([60 + base * 30, 66 + base * 32, 80 + base * 36], -1)
    yy, xx = np.mgrid[0:128, 0:128]
    bevel = np.minimum(np.minimum(xx, 127 - xx), np.minimum(yy, 127 - yy))
    steel += (bevel < 8)[..., None] * np.array([70, 70, 80]) * (1 - bevel / 8)[..., None]
    steel -= ((bevel >= 8) & (bevel < 11))[..., None] * 25
    for _ in range(40):
        x, y = rng.integers(10, 118, 2)
        L = rng.integers(5, 20)
        a = rng.uniform(0, 3.14)
        for k in range(L):
            px, py = int(x + math.cos(a) * k), int(y + math.sin(a) * k)
            if 0 <= px < 128 and 0 <= py < 128:
                steel[py, px] += 35
    A = Image.fromarray(steel.clip(0, 255).astype(np.uint8), "RGB").convert("RGBA")
    img.paste(A, (0, 0))
    # 符文刻槽（發光橘金）
    glow = Image.new("L", (S, S), 0)
    gd = ImageDraw.Draw(glow)
    for i in range(5):
        x = 20 + i * 22
        pts = [(x, 30), (x + 8, 44), (x, 58), (x + 10, 70), (x + 2, 92)]
        gd.line(pts, fill=255, width=3)
        gd.ellipse([x - 3 + 4, 100 - 3, x + 3 + 4, 100 + 3], fill=255)
    gd.line([(14, 20), (114, 20)], fill=255, width=2)
    gd.line([(14, 108), (114, 108)], fill=255, width=2)
    # B：端面
    cx, cy = 192, 64
    for r, c in ((60, (90, 96, 110)), (52, (55, 60, 72)), (40, (100, 104, 118)), (34, (45, 48, 58))):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c + (255,))
    gd.ellipse([cx - 22, cy - 22, cx + 22, cy + 22], fill=255)
    for k in range(8):
        a = k * math.pi / 4
        gd.line([(cx + math.cos(a) * 26, cy + math.sin(a) * 26), (cx + math.cos(a) * 46, cy + math.sin(a) * 46)], fill=255, width=3)
    # C：握柄皮革
    for y in range(128, 256):
        t = (y - 128) % 16
        shade = 50 + 40 * math.sin(t / 16 * math.pi)
        d.line([(0, y), (127, y)], fill=(int(shade * 1.1), int(shade * 0.7), int(shade * 0.45), 255))
    for y in range(128, 256, 16):
        d.line([(0, y), (127, y + 6)], fill=(30, 18, 10, 255), width=2)
    # D：金屬箍
    for y in range(128, 256):
        t = (y - 128) / 128
        v = 0.6 + 0.4 * math.sin(t * math.pi * 4)
        d.line([(128, y), (255, y)], fill=(int(220 * v), int(170 * v), int(70 * v), 255))
    # 疊上發光
    g = glow.filter(ImageFilter.GaussianBlur(2))
    arr = np.asarray(img).astype(float)
    ga = np.maximum(np.asarray(glow), np.asarray(g) * 1.5).clip(0, 255)[..., None] / 255
    arr[..., :3] = arr[..., :3] * (1 - ga) + np.array([255, 190, 90]) * ga
    Image.fromarray(arr.clip(0, 255).astype(np.uint8), "RGBA").save(os.path.join(TEX_OUT, "hammer.png"))


def spirit_texture():
    """火之精靈（加法混色）：U = 環繞角、V = 高度；底部白熱 → 中段金黃 → 頂端暗紅舌焰"""
    W, H = 128, 128
    rng = np.random.default_rng(9)
    v = np.linspace(0, 1, H)[:, None]
    n = rng.normal(0, 1, (8, 9))
    n[:, -1] = n[:, 0]
    nz = np.asarray(Image.fromarray(n.astype(np.float32)).resize((W + 16, H), Image.BICUBIC))[:, :W]
    nz = (nz - nz.min()) / (nz.max() - nz.min())
    heat = np.clip(1.15 - v * 1.1 + (nz - 0.5) * 0.5, 0, 1)
    r = np.clip(heat * 2.2, 0, 1)
    g = np.clip(heat * 1.6 - 0.35, 0, 1)
    b = np.clip(heat * 2.0 - 1.2, 0, 1)
    a = np.clip(heat * 1.4, 0, 1)
    img = np.dstack([r, g, b, a]) * 255
    Image.fromarray(img.astype(np.uint8), "RGBA").save(os.path.join(TEX_OUT, "spirit.png"))


def needle_texture():
    """羅盤指針：左半紅（北）、右半銀白（南），中間金色軸心"""
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for x in range(64):
        t = x / 63
        if t < 0.5:
            c = (int(200 + 55 * (1 - t * 2)), int(40 + 30 * t), 40, 255)
        else:
            c = (int(200 + 40 * (t - 0.5)), int(205 + 40 * (t - 0.5)), int(215 + 40 * (t - 0.5)), 255)
        d.line([(x, 0), (x, 15)], fill=c)
    d.rectangle([29, 0, 34, 15], fill=(255, 210, 90, 255))
    img.save(os.path.join(TEX_OUT, "needle.png"))


def crystal_texture():
    """星辰結晶：中心白、邊緣淡紫（加法）"""
    S = 64
    y, x = np.mgrid[0:S, 0:S].astype(float)
    t = np.abs(x - S / 2) / (S / 2)
    a = np.clip(1 - t * 0.6, 0, 1)
    img = np.dstack([np.full_like(t, 0.85) + 0.15 * (1 - t), 0.75 + 0.25 * (1 - t), np.ones_like(t), a]) * 255
    Image.fromarray(img.astype(np.uint8), "RGBA").save(os.path.join(TEX_OUT, "crystal.png"))


# ================================================================= 模型

def hammer():
    """靈魂之錘：握柄底端在原點、沿 +Y；錘頭沿 X 軸（揮擊時繞 Z 軸旋轉）。總高約 1.9"""
    m = Mesh("chuuni:textures/fx/hammer.png")
    A = (0, 0, 0.5, 0.5)
    B = (0.5, 0, 1, 0.5)
    C = (0, 0.5, 0.5, 1)
    D = (0.5, 0.5, 1, 1)
    m.box((-0.07, 0.0, -0.07), (0.07, 1.45, 0.07), C)          # 握柄
    m.box((-0.1, -0.08, -0.1), (0.1, 0.02, 0.1), D)             # 柄尾
    m.box((-0.095, 0.5, -0.095), (0.095, 0.56, 0.095), D)       # 金箍
    m.box((-0.095, 1.05, -0.095), (0.095, 1.11, 0.095), D)
    m.box((-0.13, 1.38, -0.13), (0.13, 1.47, 0.13), D)          # 頸
    m.box((-0.48, 1.45, -0.24), (0.48, 1.93, 0.24), A)          # 錘頭
    m.box((-0.56, 1.5, -0.2), (-0.48, 1.88, 0.2), B)            # 端面
    m.box((0.48, 1.5, -0.2), (0.56, 1.88, 0.2), B)
    m.box((-0.3, 1.93, -0.16), (0.3, 2.0, 0.16), D)             # 頂飾
    m.box((-0.5, 1.6, -0.26), (0.5, 1.66, 0.26), D)             # 錘頭金帶
    m.box((-0.5, 1.72, -0.26), (0.5, 1.78, 0.26), D)
    m.save("hammer")


def globe():
    m = Mesh("chuuni:textures/fx/holo_globe.png")
    nu, nv = 32, 16
    for i in range(nu):
        for j in range(nv):
            def p(ii, jj):
                th = 2 * math.pi * ii / nu
                ph = math.pi * jj / nv
                return (math.sin(ph) * math.cos(th), math.cos(ph), math.sin(ph) * math.sin(th))

            verts = [p(i, j + 1), p(i + 1, j + 1), p(i + 1, j), p(i, j)]
            uvs = [(i / nu, (j + 1) / nv), ((i + 1) / nu, (j + 1) / nv), ((i + 1) / nu, j / nv), (i / nu, j / nv)]
            m.quad(verts, uvs, normal=np.array(p(i + 0.5, j + 0.5)))
    m.save("globe")


def needle():
    """羅盤指針：沿 X 軸的扁平菱形雙錐（-X 北 / 紅），長 1.6"""
    m = Mesh("chuuni:textures/fx/needle.png")
    L, Wd, T = 0.8, 0.11, 0.035
    tipN, tipS = (-L, 0, 0), (L, 0, 0)
    side = [(0, 0, Wd), (0, T, 0), (0, 0, -Wd), (0, -T, 0)]
    for k in range(4):
        a, b = side[k], side[(k + 1) % 4]
        m.tri(tipN, b, a, (0, 0.5), (0.5, 0), (0.5, 1))
        m.tri(tipS, a, b, (1, 0.5), (0.5, 0), (0.5, 1))
    m.save("needle")


def spirit():
    """火之精靈本體：扭曲的水滴形（底部圓、頂端尖、帶 5 道舌焰），高約 1"""
    m = Mesh("chuuni:textures/fx/spirit.png")
    nu, nv = 20, 12

    def p(i, j):
        t = j / nv
        th = 2 * math.pi * i / nu + t * 1.4
        r = 0.32 * math.sin(math.pi * min(1, t * 1.25)) ** 0.8 * (1 - t) ** 0.4
        r *= 1 + 0.35 * max(0, math.sin(5 * th)) * t
        return (r * math.cos(th), t * 1.0 - 0.3, r * math.sin(th))

    for i in range(nu):
        for j in range(nv):
            verts = [p(i, j), p(i + 1, j), p(i + 1, j + 1), p(i, j + 1)]
            uvs = [(i / nu, 1 - j / nv), ((i + 1) / nu, 1 - j / nv), ((i + 1) / nu, 1 - (j + 1) / nv), (i / nu, 1 - (j + 1) / nv)]
            m.quad(verts, uvs)
    m.save("spirit")


def crystal():
    """細長八面體結晶（高 1，寬 0.3）"""
    m = Mesh("chuuni:textures/fx/crystal.png")
    top, bot = (0, 0.5, 0), (0, -0.5, 0)
    ring = [(0.15 * math.cos(k * math.pi / 3), 0.05, 0.15 * math.sin(k * math.pi / 3)) for k in range(6)]
    for k in range(6):
        a, b = ring[k], ring[(k + 1) % 6]
        m.tri(top, b, a, (0.5, 0), (1, 0.5), (0, 0.5))
        m.tri(bot, a, b, (0.5, 1), (0, 0.5), (1, 0.5))
    m.save("crystal")


def main():
    os.makedirs(TEX_OUT, exist_ok=True)
    hammer_texture()
    spirit_texture()
    needle_texture()
    crystal_texture()
    hammer()
    globe()
    needle()
    spirit()
    crystal()


if __name__ == "__main__":
    main()
