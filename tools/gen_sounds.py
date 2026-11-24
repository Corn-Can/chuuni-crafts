"""
中二工坊原創音效：全部程式合成（無台詞）。每個音效 3 個隨機變化 → 48kHz 單聲道 OGG + sounds.json。
用法：python tools/gen_sounds.py
"""
import json
import os
import zlib

import numpy as np
import soundfile as sf

from synth import (OUT, ROOT, SR, bandpass, env, finish, fm_metal, glass, highpass, lowpass, mix, noise, pad, reverb, soft,
                   sub, sweep_filter, t_axis, tone)


def at(x, sec, total):
    """把 x 放在 sec 秒處，總長 total 秒"""
    out = np.zeros(int(SR * total))
    s = int(SR * sec)
    n = min(len(x), len(out) - s)
    if n > 0:
        out[s:s + n] += x[:n]
    return out


def choir(dur, freqs, rng, attack=0.4, vib=5.0):
    """合唱般的和聲墊（多個微失諧的鋸齒波經低通）"""
    t = t_axis(dur)
    out = np.zeros(len(t))
    for f in freqs:
        for d in (-0.006, 0.0, 0.007):
            ph = rng.uniform(0, 6.28)
            fm = f * (1 + d) * (1 + 0.004 * np.sin(2 * np.pi * vib * t + ph))
            phase = np.cumsum(fm) / SR
            saw = 2 * (phase % 1.0) - 1
            out += saw
    out = lowpass(out, 2600)
    return out / (len(freqs) * 3) * env(dur, attack, dur, 1.2)


def riser(dur, rng, f0=200, f1=3000):
    """上升的掃頻噪聲 + 上滑音"""
    n = noise(dur, rng)
    sw = sweep_filter(n, f0, f1, q=3.0)
    t = t_axis(dur)
    amp = (t / dur) ** 1.6
    pitch = tone(lambda tt: f0 * 0.5 * (f1 / f0) ** (tt / dur), dur) * 0.25
    return (sw + pitch) * amp


# ================================================================= 附魔：禁咒・星辰刻印

def drop(dur, f0, f1, speed=18.0, decay=4.5):
    """指數下滑的低頻（重擊的「咚」）"""
    return tone(lambda t: f1 + (f0 - f1) * np.exp(-t * speed), dur) * env(dur, 0.002, dur, decay)


def dark_verb(x, size, amount, rng, cut=2200):
    """暗的大殘響：只留低中頻的尾巴，不讓高頻發亮"""
    wet = reverb(x, size, 1.0, rng)
    wet = lowpass(wet, cut, 2)
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry * (1 - amount) + wet * amount


def heavy_hit(rng, v, depth=1.0, dur=1.6):
    """厚重打擊：吸氣 → 低頻重拳 + 鼓皮 + 悶的肉感 + 一點點顆粒，經過飽和，接暗殘響"""
    pre = 0.1
    total = dur + pre
    suck = lowpass(noise(pre, rng), 900)[::-1] * np.linspace(0, 1, int(SR * pre)) ** 3 * 0.5
    kick = drop(dur, (95 - v * 6) * depth, 34 * depth, 16, 3.6) * 1.7
    skin = drop(0.4, 190 * depth, 92 * depth, 22, 9) * 0.7
    body = lowpass(noise(0.14, rng), 320) * env(0.14, 0.001, 0.14, 8) * 1.4
    grit = bandpass(noise(0.05, rng), 250, 1800) * env(0.05, 0.0005, 0.05, 14) * 0.55
    hit = soft(mix(kick, skin, body, grit), 2.6)
    out = mix(at(suck, 0, total), at(hit, pre, total))
    return dark_verb(out, 0.9, 0.3, rng)


def name_hit(rng, v):
    """招式名每個字砸下：厚重打擊 + 低沉的金屬共鳴"""
    base = heavy_hit(rng, v, 1.0 - v * 0.05)
    ring = fm_metal(1.4, 180 + v * 12, 1.414, 2.5, 3.0) * 0.22
    return mix(base, at(lowpass(ring, 1500), 0.1, 1.7))


def brush_hit(rng, v):
    """毛筆名：一筆刷過（低沉的布料 / 墨的掃動）→ 重落款"""
    swipe_d = 0.32
    swipe = sweep_filter(noise(swipe_d, rng), 1400, 220, q=1.6) * env(swipe_d, 0.12, 0.2, 3) * 0.9
    hit = heavy_hit(rng, v, 0.85, 2.0)
    return mix(at(swipe, 0, 2.4), at(hit, 0.22, 2.4))


def holy_descend(rng, v):
    """神聖降臨：溫暖的大三和弦合唱墊（低通）+ 柔和的鐘 + 低音持續，長殘響"""
    dur = 3.4
    root = 130.8 * (1 + v * 0.06)
    pad_ = choir(dur, [root, root * 1.26, root * 1.5, root * 2, root * 2.52], rng, 0.9, 4.5)
    pad_ = lowpass(pad_, 1800) * 1.1
    drone = tone(root / 2, dur) * env(dur, 0.6, dur, 1.5) * 0.35
    bells = np.zeros(int(SR * dur))
    for i, r in enumerate([4, 5, 6, 8, 6, 5]):
        g = glass(1.6, rng, root * r, 3, 2.5) * 0.18
        bells += at(lowpass(g, 3000), 0.3 + i * 0.42, dur)
    return dark_verb(mix(pad_, drone, bells), 1.4, 0.45, rng, 3000)


def enchant_charge(rng, v):
    """魔法陣展開到集氣：低沉小調和聲墊 + 低音逐漸壓上來 + 暗的上升掃頻"""
    dur = 2.4
    base = 110 * (1 + v * 0.03)
    pad_ = lowpass(choir(dur, [base, base * 1.2, base * 1.5, base * 2], rng, 0.6), 1600) * 1.1
    swell = drop(dur, 45, 60, 0.5, 0.2) * (t_axis(dur) / dur) ** 1.5 * 0.9
    bells = np.zeros(int(SR * dur))
    for i in range(14):
        s_ = (i / 14) ** 0.8 * (dur - 0.3)
        f = base * 4 * [1, 1.2, 1.5, 2][rng.integers(0, 4)]
        bells += at(lowpass(glass(0.5, rng, f, 3, 9), 2500) * (0.08 + 0.18 * i / 14), s_, dur)
    rise = riser(dur, rng, 120, 1600) * 0.5
    return dark_verb(mix(pad_, swell, bells, rise), 0.8, 0.35, rng)


def enchant_beam(rng, v):
    """天柱落下：低沉下墜 → 巨大的光爆（超低頻重擊 + 暗的玻璃風暴 + 長殘響）"""
    dur = 2.8
    fall = sweep_filter(noise(0.35, rng), 2400, 300, q=2.0) * env(0.35, 0.2, 0.2, 2)
    boom = drop(dur, 80 - v * 4, 26, 6, 2.2) * 1.8
    body = lowpass(noise(0.3, rng), 400) * env(0.3, 0.001, 0.3, 6) * 1.2
    layers = [lowpass(glass(dur, rng, 420 * (1 + i * 0.6) + v * 30, 8, 1.8 + i), 2500) * (0.5 / (1 + i)) for i in range(4)]
    shine = lowpass(choir(dur, [220, 277, 330, 440], rng, 0.05, 6), 2000) * env(dur, 0.01, dur, 2.0) * 0.5
    hit = at(soft(mix(boom, body, *layers, shine), 2.2), 0.3, dur + 0.3)
    return dark_verb(mix(fall, hit), 1.1, 0.35, rng)


def sparkle(rng, v):
    """星屑：柔和、偏中頻的鈴聲（不刺耳）"""
    dur = 1.4
    out = np.zeros(int(SR * dur))
    for i in range(8):
        f = 900 * (1.12 ** rng.integers(0, 8)) * (1 + v * 0.02)
        out += at(lowpass(glass(0.6, rng, f, 2, 7), 3200) * rng.uniform(0.2, 0.45), rng.uniform(0, 0.8), dur)
    return dark_verb(out, 0.7, 0.45, rng, 3500)


# ================================================================= 通用

def title_slam(rng, v):
    """招式名卡：太鼓重擊 + 金屬「鏘」+ 鈸類噪聲"""
    dur = 1.4
    taiko = mix(sub(0.6, 120 - v * 8, 48, 7) * 1.4, lowpass(noise(0.08, rng), 400) * env(0.08, 0.001, 0.08, 10) * 0.8)
    shing = lowpass(fm_metal(dur, 520 + v * 40, 1.414, 3.0, 3.2), 2500) * 0.35
    return mix(heavy_hit(rng, v, 0.9, dur), at(shing, 0.1, dur + 0.1))


def shockwave(rng, v):
    dur = 1.2
    boom = sub(dur, 90 - v * 5, 30, 3.8) * 1.2
    rush = sweep_filter(noise(dur, rng), 1400, 180, q=1.5) * env(dur, 0.005, dur, 3.5) * 0.7
    return reverb(soft(mix(boom, rush), 2.0), 0.6, 0.25, rng)


def whoosh(rng, v):
    dur = 0.5
    return sweep_filter(noise(dur, rng), 260 + v * 40, 1900, q=1.8) * env(dur, 0.18, 0.25, 4)


# ================================================================= 共用小元件

def crackle(dur, rng, n, lo=1500, hi=5000, amp=(0.1, 0.4), spread=1.0):
    """隨機劈啪（火星、餘燼、碎屑）；spread < 1 時集中在前段"""
    out = np.zeros(int(SR * dur))
    for _ in range(n):
        c = bandpass(noise(0.012, rng), lo, hi) * np.linspace(1, 0, int(SR * 0.012)) * rng.uniform(*amp)
        s = rng.uniform(0, max(0.01, dur * spread - 0.02))
        out += at(c, s, dur)
    return out


def flutter(dur, rng, rate=22.0, lo=300, hi=2400):
    """布 / 紙的翻飛（振幅快速起伏的帶通噪聲）"""
    t = t_axis(dur)
    am = 0.5 + 0.5 * np.sin(2 * np.pi * rate * t + rng.uniform(0, 6) + 2 * np.sin(2 * np.pi * 3 * t))
    return bandpass(noise(dur, rng), lo, hi) * am


def clunk(rng, f=900, depth=1.0, dur=0.5):
    """沉重的機械 / 木頭撞擊（短的低頻 + 悶的接觸聲）"""
    thump = drop(dur, 140 * depth, 55 * depth, 20, 9) * 0.9
    knock = lowpass(fm_metal(0.12, f, 1.53, 1.5, 28), 2200) * 0.35
    body = lowpass(noise(0.05, rng), 700) * env(0.05, 0.001, 0.05, 12) * 0.8
    return soft(mix(thump, knock, body), 1.8)


def swell(dur, rng, f0=200, f1=1400, curve=2.2):
    """反向殘響般的吸入（越來越大，最後瞬間切斷）"""
    t = t_axis(dur)
    x = sweep_filter(noise(dur, rng), f0, f1, q=1.4) * (t / dur) ** curve
    k = int(SR * 0.004)
    x[-k:] *= np.linspace(1, 0, k)
    return x


def metal_ring(dur, f, decay=2.0, amt=1.0):
    """非諧波的砧鐵鳴響（偏低、經低通，不刺耳）"""
    parts = [(1, 0.6), (2.76, 0.32), (5.4, 0.16), (8.93, 0.07)]
    ring = sum(fm_metal(dur, f * r, 1.41 + i * 0.13, 2.2 - i * 0.4, decay * (1 + i * 0.9)) * a for i, (r, a) in enumerate(parts))
    return lowpass(ring, 3800) * amt


# ================================================================= 鐵砧：千錘百鍊・魂鑄

def forge_ignite(rng, v):
    """魂鑄點火：爐膛「轟」地燃起 + 風箱的低吼 + 低沉的金屬嗡鳴逐漸浮現"""
    dur = 2.0
    whump = mix(drop(1.0, 70, 30, 5, 3) * 1.3, lowpass(noise(0.6, rng), 500) * env(0.6, 0.04, 0.6, 4) * 1.2)
    t = t_axis(dur)
    roar = lowpass(noise(dur, rng), 650) * (0.4 + 0.3 * np.sin(2 * np.pi * 1.6 * t) ** 2) * env(dur, 0.3, dur, 1.2) * 0.8
    drone = lowpass(fm_metal(dur, 110 + v * 6, 2.01, 1.2, 0.4), 900) * env(dur, 0.8, dur, 0.6) * 0.45
    return dark_verb(mix(at(whump, 0, dur), roar, drone, crackle(dur, rng, 25, 1200, 4000, (0.05, 0.2))), 0.8, 0.3, rng)


def forge_clang(rng, v):
    """靈魂之錘敲擊：厚重的打擊 + 偏低的砧鐵鳴響 + 火星劈啪"""
    dur = 2.0
    hit = heavy_hit(rng, v, 1.05, 1.4)
    ring = metal_ring(1.8, 330 + v * 22, 1.8, 0.75)
    sparks = crackle(0.7, rng, 30, 1500, 4500, (0.08, 0.3), 0.6)
    return dark_verb(mix(hit, at(ring, 0.1, dur), at(sparks, 0.1, dur)), 0.9, 0.3, rng)


def forge_final(rng, v):
    """第三擊（大招）：吸氣 → 超低頻重擊 + 雙層砧鐵長鳴 + 碎鐵四散 + 巨大暗殘響"""
    dur = 3.4
    pre = swell(0.5, rng, 150, 900, 3) * 0.8
    boom = drop(dur, 72 - v * 4, 24, 5, 1.6) * 2.0
    hit = heavy_hit(rng, v, 0.8, 2.0)
    ring = mix(metal_ring(3.0, 220 + v * 14, 0.9, 0.8), metal_ring(3.0, 331 + v * 20, 1.3, 0.45))
    debris = crackle(1.4, rng, 70, 900, 3500, (0.1, 0.35), 0.7)
    chord = lowpass(choir(dur, [55, 82.4, 110], rng, 0.02, 3), 600) * env(dur, 0.01, dur, 1.4) * 0.6
    body = soft(mix(boom, hit, at(ring, 0.05, dur), at(debris, 0.05, dur), chord), 2.0)
    return dark_verb(mix(pre, at(body, 0.5, dur + 0.5)), 1.5, 0.4, rng)


def forge_quench(rng, v):
    """淬火：燒紅的刃浸入 → 低沉的蒸氣嘶聲（壓暗）+ 沸騰的咕嘟 + 冷卻收縮的金屬呻吟"""
    dur = 2.6
    plunge = mix(drop(0.5, 120, 50, 12, 6) * 0.9, lowpass(noise(0.15, rng), 600) * env(0.15, 0.002, 0.15, 10) * 0.8)
    hiss = bandpass(noise(dur, rng), 600, 2600) * env(dur, 0.02, dur, 1.6) * 0.65
    boil = np.zeros(int(SR * dur))
    for _ in range(40):
        f = rng.uniform(120, 380)
        b = tone(lambda tt, f=f: f * (1 + tt * 6), 0.06) * env(0.06, 0.004, 0.06, 18) * rng.uniform(0.1, 0.3)
        boil += at(b, rng.uniform(0.05, dur * 0.7), dur)
    groan = lowpass(tone(lambda tt: 180 - 60 * tt / dur + 6 * np.sin(tt * 9), dur), 800) * env(dur, 0.4, dur, 1.8) * 0.3
    return dark_verb(mix(plunge, hiss, boil, groan), 1.1, 0.35, rng)


def blade_glint(rng, v):
    """淬煉完成、刃身冷光一閃：反向吸入 → 低沉冰冷的金屬鐘鳴 + 長長的暗殘響 + 低音持續"""
    dur = 3.2
    pre = swell(0.6, rng, 300, 1800, 2.5) * 0.6
    bell = mix(lowpass(fm_metal(2.6, 196 + v * 10, 3.5, 2.0, 1.2), 2400) * 0.6,
               lowpass(glass(2.6, rng, 588 + v * 30, 4, 1.6), 3000) * 0.4)
    drone = tone(49 + v * 2, dur) * env(dur, 0.3, dur, 1.4) * 0.4
    return dark_verb(mix(at(pre, 0, dur), at(bell, 0.6, dur), drone), 1.6, 0.5, rng, 2600)


# ================================================================= 工作台：萬象解放

def craft_blueprint(rng, v):
    """藍圖展開：厚紙翻開 + 木桌「咚」一聲 + 低沉的機關運轉聲往上推"""
    dur = 1.6
    paper = flutter(0.35, rng, 26, 300, 2000) * env(0.35, 0.05, 0.3, 4) * 0.6
    thunk = clunk(rng, 520, 0.9, 0.5)
    t = t_axis(dur)
    gears = lowpass(noise(dur, rng), 500) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * (6 + t * 6) * t))) * env(dur, 0.5, dur, 1) * 0.35
    hum = lowpass(choir(dur, [98, 147, 196], rng, 0.6, 3), 1200) * 0.6
    return dark_verb(mix(paper, at(thunk, 0.12, dur), gears, hum), 0.7, 0.3, rng)


def craft_assemble(rng, v):
    """零件吸附組裝：越來越快的沉重咔嗒（木 + 金屬）+ 暗的上升掃頻"""
    dur = 1.4
    out = riser(dur, rng, 100, 1300) * 0.45
    for i in range(12):
        s = 1.25 * (1 - (1 - i / 12) ** 1.7)
        out += at(clunk(rng, 700 + i * 40, 0.9 + i * 0.03, 0.3) * (0.35 + i * 0.04), s, dur)
    return dark_verb(out, 0.5, 0.25, rng)


def craft_forge(rng, v):
    """萬象歸一：全部零件壓縮成形的重擊 + 木材的爆裂 + 暗而溫暖的和弦泛光"""
    dur = 2.4
    hit = heavy_hit(rng, v, 0.95, 1.6)
    crack = lowpass(crackle(0.25, rng, 25, 600, 2800, (0.2, 0.6)), 3000)
    chord = np.zeros(int(SR * dur))
    for i, r in enumerate([1, 1.26, 1.5, 2]):
        chord += at(lowpass(glass(1.8, rng, 262 * r * (1 + v * 0.03), 3, 2.5), 2600) * 0.22, 0.1 + i * 0.04, dur)
    return dark_verb(mix(hit, at(crack, 0.1, dur), chord), 1.0, 0.35, rng)


def combo_ping(rng, v):
    """COMBO 計數 / 法陣鎖定：低沉的鐘 + 小小的重擊"""
    dur = 0.9
    f = 523 * (1 + v * 0.06)
    bell = lowpass(fm_metal(dur, f, 2.0, 1.0, 5) * 0.4 + tone(f / 2, dur) * env(dur, 0.002, dur, 6) * 0.4, 2500)
    return dark_verb(mix(bell, drop(0.3, 120, 60, 18, 10) * 0.6), 0.5, 0.3, rng)


# ================================================================= 製圖台：全知之眼

def map_unfurl(rng, v):
    """羊皮紙地圖「唰」地攤開：紙張翻飛 + 落在桌上的悶響 + 低沉的空氣"""
    dur = 1.2
    flap = flutter(0.5, rng, 30, 250, 2200) * env(0.5, 0.08, 0.35, 3) * 0.9
    slap = mix(lowpass(noise(0.08, rng), 1200) * env(0.08, 0.001, 0.08, 10) * 0.8, drop(0.4, 110, 50, 18, 8) * 0.8)
    air = lowpass(noise(dur, rng), 300) * env(dur, 0.2, dur, 2) * 0.3
    return dark_verb(mix(flap, at(slap, 0.42, dur), air), 0.6, 0.25, rng)


def holo_boot(rng, v):
    """全息投影啟動：低頻電流嗡鳴 + 壓暗的數位嗶聲 + 低沉掃描"""
    dur = 1.8
    t = t_axis(dur)
    hum = (tone(55, dur) * 0.45 + tone(110, dur) * 0.2 + lowpass(np.sign(tone(55, dur)), 300) * 0.15) * env(dur, 0.3, dur, 1.3)
    chirps = np.zeros(len(t))
    for i in range(7):
        f = 440 * 1.12 ** i * (1 + v * 0.03)
        sq = lowpass(np.sign(tone(f, 0.06)), 1800) * env(0.06, 0.002, 0.06, 18) * 0.12
        chirps += at(sq, 0.15 + i * 0.09, dur)
    scan = sweep_filter(noise(dur, rng), 200, 2200, q=5) * env(dur, 0.5, 0.7, 3) * 0.5
    return dark_verb(mix(hum, chirps, scan), 0.8, 0.3, rng)


def compass_spin(rng, v):
    """羅盤狂轉：沉重的棘輪（加速 → 減速）+ 底下的旋轉風切"""
    dur = 1.9
    out = np.zeros(int(SR * dur))
    tt = 0.0
    while tt < 1.5:
        speed = 0.02 + 0.07 * abs(tt - 0.55) ** 1.5
        c = mix(lowpass(fm_metal(0.05, 900 + rng.uniform(-60, 60), 1.7, 1.2, 30), 2500) * 0.3, drop(0.06, 160, 90, 30, 20) * 0.25)
        out += at(c, tt, dur)
        tt += speed
    t = t_axis(dur)
    whirl = bandpass(noise(dur, rng), 200, 900) * (0.4 + 0.4 * np.sin(2 * np.pi * (4 + 10 * np.exp(-((t - 0.6) ** 2) * 4)) * t)) \
        * env(dur, 0.3, dur, 2) * 0.35
    return dark_verb(mix(out, whirl), 0.6, 0.25, rng)


def eye_open(rng, v):
    """全知之眼睜開：反向吸入 → 指針「喀」鎖死 + 超低頻重擊 + 低沉詭譎的小調合唱與玻璃"""
    dur = 3.4
    pre = swell(0.45, rng, 120, 1200, 3) * 0.8
    lock = mix(lowpass(fm_metal(0.5, 620 + v * 30, 1.41, 2.5, 7), 2500) * 0.5, clunk(rng, 800, 1.0, 0.5))
    hit = heavy_hit(rng, v, 0.85, 2.0)
    ch = lowpass(choir(2.8, [110, 130.8, 164.8, 207.7], rng, 0.05, 2.5), 1500) * env(2.8, 0.05, 2.8, 1.3) * 0.7
    shimmer = lowpass(glass(2.6, rng, 830 + v * 40, 5, 1.5), 2800) * 0.25
    body = mix(lock, hit, at(ch, 0.05, dur), at(shimmer, 0.1, dur))
    return dark_verb(mix(pre, at(body, 0.45, dur + 0.45)), 1.5, 0.4, rng, 2600)


def world_scan(rng, v):
    """測繪全世界：三道低沉的聲納脈衝往外擴散，一道比一道遠"""
    dur = 3.2
    out = np.zeros(int(SR * dur))
    for i in range(3):
        f = (196 - i * 22) * (1 + v * 0.03)
        ping = mix(tone(f, 1.6) * env(1.6, 0.003, 1.6, 2.6) * 0.5, tone(f * 2.01, 1.6) * env(1.6, 0.003, 1.6, 4) * 0.2,
                   drop(0.6, 90, 45, 10, 5) * 0.6)
        out += at(ping * (1 - i * 0.2), 0.05 + i * 0.75, dur)
    return dark_verb(out, 1.4, 0.5, rng, 2400)


# ================================================================= 熔爐：獄炎煉成

def fire_ignite(rng, v):
    """爐門噴焰：瓦斯「轟」地點燃（低頻下墜 + 暗的火焰噴射）"""
    dur = 1.4
    whoomp = mix(drop(0.9, 95, 32, 7, 4) * 1.3, lowpass(noise(0.9, rng), 900) * env(0.9, 0.03, 0.9, 3.5) * 1.1)
    jet = sweep_filter(noise(dur, rng), 250, 1400, q=1.2) * env(dur, 0.05, dur, 2.5) * 0.6
    return dark_verb(soft(mix(whoomp, jet, crackle(dur, rng, 20, 1200, 3500, (0.05, 0.2))), 1.6), 0.8, 0.3, rng)


def fire_roar(rng, v):
    """火之精靈盤旋上升：厚重起伏的火焰轟鳴 + 次低頻滾動 + 火星"""
    dur = 2.2
    n = noise(dur, rng)
    flick = 0.55 + 0.45 * np.abs(lowpass(rng.normal(0, 1, int(SR * dur)), 10))
    flick /= flick.max()
    t = t_axis(dur)
    body = lowpass(n, 750) * flick * env(dur, 0.25, dur, 1.2) * (0.6 + 0.4 * t / dur) * 1.5
    rumble = tone(lambda tt: 38 + 18 * tt / dur, dur) * env(dur, 0.4, dur, 0.8) * 0.6
    return dark_verb(soft(mix(body, rumble, crackle(dur, rng, 45, 1000, 3800, (0.06, 0.25))), 1.5), 0.9, 0.3, rng)


def inferno_burst(rng, v):
    """雙精靈相撞爆炎：吸入 → 重擊 + 火焰爆裂的暗轟 + 碎屑劈啪 + 長的低頻尾巴"""
    dur = 3.0
    pre = swell(0.35, rng, 200, 1100, 3) * 0.7
    hit = heavy_hit(rng, v, 0.85, 1.8)
    blast = lowpass(noise(1.6, rng), 1100) * env(1.6, 0.005, 1.6, 2.2) * 1.2
    tail = drop(dur, 60, 28, 3, 1.3) * 1.2
    debris = crackle(1.6, rng, 60, 900, 3200, (0.08, 0.3), 0.8)
    body = soft(mix(hit, blast, tail, debris), 1.8)
    return dark_verb(mix(pre, at(body, 0.35, dur + 0.35)), 1.2, 0.35, rng)


def ember_settle(rng, v):
    """火勢退去：餘燼的細碎劈啪 + 溫暖的低音嗡鳴慢慢熄滅"""
    dur = 2.6
    hum = lowpass(choir(dur, [65.4, 98, 130.8], rng, 0.3, 2), 700) * env(dur, 0.3, dur, 1.4) * 0.8
    pops = crackle(dur, rng, 40, 900, 3000, (0.05, 0.25))
    breath = lowpass(noise(dur, rng), 400) * env(dur, 0.1, dur, 1.6) * 0.35
    return dark_verb(mix(hum, pops * np.linspace(1, 0.2, len(pops)), breath), 1.0, 0.35, rng)


def fire_puff(rng, v):
    """煉好一件：爐口吐出一團悶火"""
    dur = 0.8
    return dark_verb(mix(lowpass(noise(dur, rng), 800) * env(dur, 0.04, dur, 4.5) * 0.9, drop(0.4, 100, 45, 12, 7) * 0.8,
                         crackle(0.6, rng, 8, 1000, 3000, (0.05, 0.2))), 0.5, 0.25, rng)


# ================================================================= 高爐：蒼焰熔斷（電漿 / 熔鋼）

def plasma_charge(rng, v):
    """蒼焰電漿刃成形：低沉的電弧嗡鳴（脈衝寬度變化的方波經低通）+ 嘶嘶的電漿 + 越轉越快的旋轉風切"""
    dur = 2.4
    t = t_axis(dur)
    f = 55 * (1 + 0.6 * (t / dur) ** 2) * (1 + v * 0.03)
    ph = np.cumsum(f) / SR
    buzz = lowpass(np.sign(np.sin(2 * np.pi * ph) + 0.3 * np.sin(2 * np.pi * 7 * t)), 900) * 0.35
    hiss = bandpass(noise(dur, rng), 700, 2600) * (0.3 + 0.7 * (t / dur)) * 0.35
    spin = bandpass(noise(dur, rng), 250, 900) * (0.5 + 0.5 * np.sin(2 * np.pi * (3 + 14 * (t / dur) ** 2) * t)) * 0.45
    return dark_verb(mix(buzz, hiss, spin) * env(dur, 0.4, dur, 0.6), 0.7, 0.3, rng)


def plasma_cut(rng, v):
    """熔斷一刀：吸入 → 刀光「嘶——」地斬下（下掃的帶通）+ 重擊 + 熔鋼滴落的嘶聲"""
    dur = 3.0
    pre = swell(0.35, rng, 300, 1500, 3) * 0.7
    slash = sweep_filter(noise(0.45, rng), 2600, 300, q=2.5) * env(0.45, 0.01, 0.4, 3) * 0.9
    hit = heavy_hit(rng, v, 0.9, 1.8)
    sizzle = np.zeros(int(SR * 2.0))
    for _ in range(18):
        s = bandpass(noise(0.18, rng), 900, 3000) * env(0.18, 0.005, 0.18, 9) * rng.uniform(0.08, 0.2)
        sizzle += at(s, rng.uniform(0.2, 1.7), 2.0)
    body = mix(slash, at(hit, 0.05, dur), at(sizzle, 0.1, dur))
    return dark_verb(mix(pre, at(body, 0.35, dur + 0.35)), 1.1, 0.35, rng)


def steam_cool(rng, v):
    """熔鋼冷卻：長長的蒸氣嘆息（壓暗）+ 金屬冷縮的低鳴"""
    dur = 2.6
    sigh = bandpass(noise(dur, rng), 500, 2200) * env(dur, 0.25, dur, 1.4) * 0.5
    ring = lowpass(fm_metal(dur, 147 + v * 8, 2.76, 1.0, 1.0), 1500) * 0.3
    return dark_verb(mix(sigh, ring, crackle(dur, rng, 12, 800, 2500, (0.04, 0.12))), 1.1, 0.4, rng)


# ================================================================= 煙燻爐：煙雲祕法（煙 / 香）

def smoke_swirl(rng, v):
    """煙龍盤旋：低沉的氣流在左右來回繞（帶通掃頻往返）+ 柔軟的低音持續 + 木頭燃燒的細碎劈啪"""
    dur = 2.4
    t = t_axis(dur)
    x = noise(dur, rng)
    out = np.zeros_like(x)
    seg = 1024
    for i in range(0, len(x), seg):
        k = i / len(x)
        fc = 350 + 300 * np.sin(2 * np.pi * (0.8 + 1.2 * k) * k * 2)
        out[i:i + seg] = bandpass(x[max(0, i - 2048):i + seg], fc / 1.6, fc * 1.6)[-len(x[i:i + seg]):]
    drone = lowpass(choir(dur, [73.4, 110, 146.8], rng, 0.5, 2), 800) * 0.5
    return dark_verb(mix(out * env(dur, 0.4, dur, 0.8) * 0.8, drone, crackle(dur, rng, 18, 900, 2600, (0.04, 0.15))), 1.0, 0.35, rng)


def smoke_gong(rng, v):
    """煙幕散開：吸入 → 低沉的銅鑼（非諧波長鳴）+ 重擊 + 煙往外炸開的悶風"""
    dur = 3.6
    pre = swell(0.35, rng, 150, 800, 3) * 0.6
    gong = lowpass(sum(fm_metal(3.2, (98 + v * 4) * r, 1.0 + i * 0.37, 1.6, 0.8 + i * 0.5) * a
                       for i, (r, a) in enumerate([(1, 0.6), (1.48, 0.35), (2.13, 0.2), (2.9, 0.1)])), 1600)
    hit = heavy_hit(rng, v, 0.8, 1.6)
    puff = lowpass(noise(1.2, rng), 600) * env(1.2, 0.01, 1.2, 3) * 0.8
    body = mix(at(gong, 0.0, dur), hit, puff)
    return dark_verb(mix(pre, at(body, 0.35, dur + 0.35)), 1.4, 0.4, rng)


def incense_settle(rng, v):
    """香煙裊裊：溫暖的低音和聲 + 輕柔的氣息 + 小小的木魚"""
    dur = 2.8
    pad_ = lowpass(choir(dur, [98, 123.5, 146.8, 196], rng, 0.4, 1.8), 1000) * env(dur, 0.3, dur, 1.2) * 0.8
    air = bandpass(noise(dur, rng), 200, 900) * env(dur, 0.5, dur, 1.2) * 0.3
    blocks = np.zeros(int(SR * dur))
    for i in range(3):
        b = mix(lowpass(fm_metal(0.3, 330 - i * 30, 1.5, 0.8, 18), 1500) * 0.25, drop(0.2, 180, 120, 25, 15) * 0.3)
        blocks += at(b, 0.3 + i * 0.55, dur)
    return dark_verb(mix(pad_, air, blocks), 1.2, 0.45, rng)


# ================================================================= A 級：重生錨・魂錨契約

def anchor_charge(rng, v):
    """螢石充能：低沉的靈魂脈動往上推（每次充能更高）+ 暗的結晶劈啪 + 一聲沉悶的「咚」"""
    dur = 1.8
    base = 55 * (1 + v * 0.12)
    t = t_axis(dur)
    throb = tone(lambda tt: base * (1 + 0.5 * tt / dur), dur) * (0.6 + 0.4 * np.sin(2 * np.pi * 5 * t)) * env(dur, 0.05, dur, 2) * 0.7
    thud = drop(0.5, 110, 45, 14, 7) * 0.9
    shards = lowpass(crackle(0.8, rng, 25, 800, 3000, (0.05, 0.2)), 3000)
    return dark_verb(mix(thud, throb, shards), 0.9, 0.35, rng)


def anchor_pact(rng, v):
    """魂錨契約：反向的靈魂合唱吸入 → 鎖鏈「喀啦」鎖死 + 超低頻重擊 + 低沉的小調合唱長尾"""
    dur = 4.0
    pre = swell(0.6, rng, 120, 900, 2.5) * 0.7
    chain = np.zeros(int(SR * 0.6))
    for i in range(8):
        chain += at(lowpass(fm_metal(0.08, rng.uniform(700, 1100), 1.9, 1.4, 30), 2400) * 0.2, i * 0.04, 0.6)
    hit = heavy_hit(rng, v, 0.8, 2.0)
    ch = lowpass(choir(3.2, [73.4, 87.3, 110, 146.8], rng, 0.05, 3), 1300) * env(3.2, 0.05, 3.2, 1.0) * 0.8
    body = mix(chain, hit, at(ch, 0.05, dur))
    return dark_verb(mix(pre, at(body, 0.6, dur + 0.6)), 1.5, 0.45, rng, 2400)


def anchor_blast(rng, v):
    """錨在錯的世界爆炸：吸入 → 巨大的暗爆 + 碎石"""
    dur = 3.0
    pre = swell(0.3, rng, 300, 1500, 3) * 0.6
    boom = mix(drop(dur, 60, 22, 4, 1.4) * 2.2, lowpass(noise(1.5, rng), 700) * env(1.5, 0.003, 1.5, 2.2) * 1.4)
    rocks = lowpass(crackle(2.0, rng, 70, 400, 2000, (0.1, 0.4), 0.7), 2400)
    return dark_verb(mix(pre, at(soft(mix(boom, rocks), 2.2), 0.3, dur + 0.3)), 1.4, 0.35, rng)


# ================================================================= A 級：烽火台・天光降臨

def beacon_rise(rng, v):
    """金字塔共鳴：層層疊上去的低沉泛音嗡鳴（每層一個音）+ 越來越快的脈動"""
    dur = 2.6
    t = t_axis(dur)
    out = np.zeros(len(t))
    for i, f in enumerate([65.4, 98, 130.8, 196]):
        layer = (tone(f, dur) * 0.6 + tone(f * 2.01, dur) * 0.2) * np.clip((t - i * 0.5) / 0.3, 0, 1)
        out += layer * 0.3
    pulse = 0.65 + 0.35 * np.sin(2 * np.pi * (2 + 8 * (t / dur) ** 2) * t)
    rise = riser(dur, rng, 100, 1200) * 0.35
    return dark_verb(mix(lowpass(out * pulse, 1500), rise) * env(dur, 0.2, dur, 0.4), 1.0, 0.35, rng)


def beacon_descent(rng, v):
    """天光直落：吸入 → 重擊 + 溫暖的大調合唱湧起（壓暗）+ 低沉的鐘"""
    dur = 4.4
    pre = swell(0.45, rng, 200, 1400, 3) * 0.7
    hit = heavy_hit(rng, v, 0.9, 2.0)
    root = 98 * (1 + v * 0.04)
    ch = lowpass(choir(3.6, [root, root * 1.26, root * 1.5, root * 2, root * 2.52], rng, 0.15, 4), 2000) * env(3.6, 0.15, 3.6, 0.9)
    bell = lowpass(fm_metal(3.4, root * 4, 1.4, 1.6, 1.2), 2400) * 0.35
    body = mix(hit, at(ch, 0.05, dur), at(bell, 0.05, dur))
    return dark_verb(mix(pre, at(body, 0.45, dur + 0.45)), 1.6, 0.45, rng, 2800)


# ================================================================= A 級：鍛造台・神匠鍛冶

def smith_hammer(rng, v):
    """神匠的精準一錘：比鐵砧更沉、更緊的金屬重擊 + 獄髓的深色共鳴"""
    dur = 1.6
    hit = heavy_hit(rng, v, 1.15, 1.1)
    ring = metal_ring(1.4, 196 + v * 14, 2.4, 0.6)
    plate = lowpass(fm_metal(1.2, 82 + v * 4, 3.1, 2.2, 2.5), 900) * 0.45
    return dark_verb(mix(hit, at(ring, 0.1, dur), at(plate, 0.1, dur)), 0.8, 0.28, rng)


def smith_fuse(rng, v):
    """獄髓融合：吸入 → 磁力般的低沉「嗡——」+ 重擊 + 碎晶與暗的玻璃長鳴"""
    dur = 3.6
    pre = swell(0.5, rng, 100, 900, 2.6) * 0.7
    t = t_axis(3.0)
    hum = lowpass(np.sign(np.sin(2 * np.pi * (41 + 10 * np.exp(-t * 2)) * t)), 500) * env(3.0, 0.01, 3.0, 1.0) * 0.35
    hit = heavy_hit(rng, v, 0.85, 2.0)
    glass_ = lowpass(glass(3.0, rng, 415 + v * 20, 6, 1.3), 2600) * 0.35
    body = mix(hit, hum, glass_)
    return dark_verb(mix(pre, at(body, 0.5, dur + 0.5)), 1.4, 0.4, rng)


# ================================================================= A 級：砂輪・魔力剝奪

def grind_spin(rng, v):
    """砂輪狂轉：低沉的石輪滾動（週期性的粗糙摩擦）+ 刮擦的火花"""
    dur = 2.2
    t = t_axis(dur)
    speed = 3 + 10 * (t / dur) ** 1.5
    phase = np.cumsum(speed) / SR
    rumble = lowpass(noise(dur, rng), 400) * (0.5 + 0.5 * np.sin(2 * np.pi * phase) ** 2) * 1.0
    scrape = bandpass(noise(dur, rng), 600, 2400) * (0.3 + 0.7 * (t / dur)) * 0.4
    return dark_verb(mix(rumble, scrape, crackle(dur, rng, 30, 1000, 3000, (0.05, 0.2))) * env(dur, 0.2, dur, 0.5), 0.6, 0.25, rng)


def grind_strip(rng, v):
    """附魔被剝離：反向吸入 → 魔力「碎裂」（壓暗的玻璃爆散）+ 重擊 + 經驗珠的低沉鈴聲往上冒"""
    dur = 3.4
    pre = swell(0.4, rng, 300, 1600, 3) * 0.7
    shatter = lowpass(mix(*[glass(1.2, rng, rng.uniform(500, 1400), 4, 4) * 0.25 for _ in range(5)]), 2800)
    hit = heavy_hit(rng, v, 0.9, 1.8)
    orbs = np.zeros(int(SR * 2.6))
    for i in range(10):
        f = 330 * 1.12 ** rng.integers(0, 8)
        orbs += at(lowpass(glass(0.6, rng, f, 2, 6), 2200) * 0.12, 0.3 + i * 0.17, 2.6)
    body = mix(hit, shatter, at(orbs, 0.2, dur))
    return dark_verb(mix(pre, at(body, 0.4, dur + 0.4)), 1.2, 0.4, rng)


# ================================================================= A 級：釀造台・煉金祕藥

def brew_bubble(rng, v):
    """煉金沸騰：低沉的咕嘟（音高上滑的短正弦）+ 玻璃瓶輕碰 + 魔法漩渦的暗風"""
    dur = 2.4
    out = np.zeros(int(SR * dur))
    for _ in range(45):
        f = rng.uniform(90, 260)
        b = tone(lambda tt, f=f: f * (1 + tt * 8), 0.08) * env(0.08, 0.004, 0.08, 14) * rng.uniform(0.1, 0.3)
        out += at(b, rng.uniform(0, dur - 0.1), dur)
    clinks = np.zeros(int(SR * dur))
    for i in range(4):
        clinks += at(lowpass(glass(0.5, rng, 880 + i * 60, 3, 9), 2600) * 0.12, 0.3 + i * 0.5, dur)
    swirl = sweep_filter(noise(dur, rng), 200, 900, q=2) * env(dur, 0.6, dur, 1) * 0.4
    return dark_verb(mix(out, clinks, swirl), 0.7, 0.3, rng)


def brew_transmute(rng, v):
    """藥水質變：吸入 → 重擊 + 三個玻璃瓶一起共鳴（低沉的和弦）+ 液體翻湧"""
    dur = 3.4
    pre = swell(0.4, rng, 200, 1200, 3) * 0.7
    hit = heavy_hit(rng, v, 0.95, 1.8)
    reso = lowpass(mix(*[glass(2.6, rng, f * (1 + v * 0.03), 3, 1.6) * 0.22 for f in (392, 494, 587)]), 2400)
    slosh = lowpass(noise(1.0, rng), 600) * env(1.0, 0.05, 1.0, 3) * 0.5
    body = mix(hit, at(reso, 0.05, dur), slosh)
    return dark_verb(mix(pre, at(body, 0.4, dur + 0.4)), 1.3, 0.4, rng)


# ================================================================= A 級：地獄門・冥界開門

def portal_ignite(rng, v):
    """點火：火焰「轟」+ 傳送門的低沉顫音（大幅度的音高搖擺）開始浮現"""
    dur = 2.0
    whoomp = mix(drop(0.8, 90, 35, 7, 4) * 1.2, lowpass(noise(0.8, rng), 800) * env(0.8, 0.02, 0.8, 4) * 1.0)
    warble = lowpass(tone(lambda tt: 70 + 25 * np.sin(2 * np.pi * 3 * tt) + 30 * tt, dur), 900) * env(dur, 0.6, dur, 1.2) * 0.5
    return dark_verb(mix(whoomp, warble, crackle(dur, rng, 15, 900, 3000, (0.05, 0.2))), 1.0, 0.35, rng)


def portal_open(rng, v):
    """冥界之門開啟：吸入 → 異界的低吼（多層搖擺的鋸齒）+ 重擊 + 不協和的暗合唱"""
    dur = 4.6
    pre = swell(0.5, rng, 100, 800, 2.5) * 0.8
    t = t_axis(3.8)
    roar = np.zeros(len(t))
    for k, f in enumerate([41, 55, 61.7]):
        ph = np.cumsum(f * (1 + 0.08 * np.sin(2 * np.pi * (0.7 + k * 0.4) * t))) / SR
        roar += (2 * (ph % 1) - 1) * 0.25
    roar = lowpass(roar, 700) * env(3.8, 0.1, 3.8, 0.9)
    hit = heavy_hit(rng, v, 0.75, 2.2)
    ch = lowpass(choir(3.6, [65.4, 69.3, 98, 103.8], rng, 0.3, 2), 1100) * 0.5
    body = mix(hit, roar, at(ch, 0.1, dur))
    return dark_verb(mix(pre, at(body, 0.5, dur + 0.5)), 1.8, 0.45, rng, 2000)


# ================================================================= A 級：終界門框・終焉之門

def endeye_insert(rng, v):
    """終界之眼嵌入：沉重的結晶「喀」+ 虛空的低鳴（每顆眼音高不同）"""
    dur = 1.8
    f = 98 * 1.06 ** (v * 3)
    click = mix(clunk(rng, 900, 1.1, 0.4), lowpass(glass(0.6, rng, 700 + v * 40, 4, 6), 2500) * 0.3)
    void = lowpass(fm_metal(dur, f, 1.5, 1.4, 1.8), 1100) * 0.35
    return dark_verb(mix(click, at(void, 0.05, dur)), 1.2, 0.45, rng, 2200)


def end_open(rng, v):
    """終焉之門開啟：長長的反向吸入 → 虛空的超低頻重擊 + 星空般的暗玻璃群 + 詭譎的高八度合唱（壓暗）"""
    dur = 5.0
    pre = swell(0.8, rng, 80, 900, 2.2) * 0.8
    hit = heavy_hit(rng, v, 0.7, 2.5)
    stars = np.zeros(int(SR * 4.0))
    for _ in range(26):
        stars += at(lowpass(glass(1.6, rng, rng.uniform(500, 1500), 3, 2.2), 2600) * rng.uniform(0.04, 0.12), rng.uniform(0, 2.4), 4.0)
    ch = lowpass(choir(3.8, [110, 116.5, 164.8, 220], rng, 0.3, 1.8), 1600) * env(3.8, 0.3, 3.8, 0.8) * 0.6
    body = mix(hit, at(stars, 0.05, dur), at(ch, 0.1, dur))
    return dark_verb(mix(pre, at(body, 0.8, dur + 0.8)), 2.0, 0.5, rng, 2400)


# ================================================================= A 級：寶庫・祕寶解封

def vault_unlock(rng, v):
    """鑰匙轉動：沉重的機關一段一段解鎖（齒輪咔嗒 + 三聲越來越重的「咚」）"""
    dur = 2.4
    out = np.zeros(int(SR * dur))
    for i in range(14):
        out += at(lowpass(fm_metal(0.05, 1300 + rng.uniform(-80, 80), 1.7, 1.2, 35), 2600) * 0.15, 0.1 + i * 0.06, dur)
    for i in range(3):
        out += at(clunk(rng, 500 - i * 60, 0.9 + i * 0.15, 0.5) * (0.6 + i * 0.25), 0.95 + i * 0.4, dur)
    return dark_verb(out, 0.8, 0.3, rng)


def vault_burst(rng, v):
    """寶物噴出：重擊 + 寶光（低沉的金色鈴群）+ 金幣嘩啦"""
    dur = 3.2
    hit = heavy_hit(rng, v, 0.9, 1.8)
    root = 196 * (1 + v * 0.05)
    bells = np.zeros(int(SR * dur))
    for i, r in enumerate([1, 1.26, 1.5, 2, 2.52, 3]):
        bells += at(lowpass(glass(1.6, rng, root * r, 3, 2.6), 2600) * 0.16, 0.05 + i * 0.06, dur)
    coins = np.zeros(int(SR * dur))
    for _ in range(40):
        coins += at(lowpass(fm_metal(0.12, rng.uniform(1500, 2600), 1.41, 1.5, 25), 3000) * rng.uniform(0.03, 0.09), rng.uniform(0.1, 1.6), dur)
    return dark_verb(mix(hit, bells, coins), 1.2, 0.38, rng)


# ================================================================= A 級：床・夢境結界

def dream_fall(rng, v):
    """沉入夢境：反向的柔和吸入 + 很慢的低沉搖籃和聲 + 遠方的音樂盒（壓暗）"""
    dur = 4.0
    pre = swell(1.0, rng, 150, 700, 2) * 0.4
    pad_ = lowpass(choir(dur, [87.3, 110, 130.8, 174.6], rng, 1.0, 1.2), 1100) * 0.8
    box = np.zeros(int(SR * dur))
    for i, f in enumerate([523, 659, 784, 659, 587, 523]):
        box += at(lowpass(glass(1.0, rng, f * 0.5, 2, 4), 1800) * 0.1, 0.6 + i * 0.5, dur)
    return dark_verb(mix(pre, pad_, box), 1.8, 0.55, rng, 2200)


def dream_dawn(rng, v):
    """夢醒天明：溫暖的大調和聲湧起 + 輕柔的重音 + 晨鐘"""
    dur = 3.6
    pad_ = lowpass(choir(dur, [98, 123.5, 146.8, 196, 246.9], rng, 0.4, 2), 1700) * env(dur, 0.3, dur, 1.0) * 0.9
    soft_hit = drop(0.8, 90, 45, 8, 4) * 0.7
    bell = lowpass(fm_metal(3.0, 392, 1.4, 1.2, 1.4), 2200) * 0.25
    return dark_verb(mix(soft_hit, pad_, at(bell, 0.2, dur)), 1.5, 0.45, rng, 2600)


# ================================================================= A 級：龍蛋・龍之遷躍

def egg_warp(rng, v):
    """龍蛋遷躍：空間往內塌陷（反向吸入 + 音高往下墜的扭曲）→ 「咻」地消失"""
    dur = 1.8
    pre = swell(0.6, rng, 1500, 200, 2) * 0.7
    warp = lowpass(tone(lambda tt: 300 * np.exp(-tt * 3) + 40, 1.0), 1200) * env(1.0, 0.01, 1.0, 3) * 0.6
    zip_ = sweep_filter(noise(0.3, rng), 300, 1800, q=2) * env(0.3, 0.02, 0.25, 4) * 0.6
    return dark_verb(mix(pre, at(warp, 0.5, dur), at(zip_, 0.6, dur)), 1.0, 0.4, rng)


def egg_appear(rng, v):
    """龍蛋重現：重擊 + 低沉的龍吟（FM 低頻的咆哮，壓暗）+ 紫色的玻璃餘韻"""
    dur = 3.0
    hit = heavy_hit(rng, v, 0.85, 1.6)
    t = t_axis(2.2)
    growl = lowpass(np.sin(2 * np.pi * np.cumsum(70 + 15 * np.sin(2 * np.pi * 9 * t) - 20 * t) / SR
                           + 3 * np.sin(2 * np.pi * 35 * t)), 900) * env(2.2, 0.15, 2.2, 1.6) * 0.5
    shimmer = lowpass(glass(2.4, rng, 466 + v * 20, 4, 1.8), 2400) * 0.25
    return dark_verb(mix(hit, at(growl, 0.05, dur), at(shimmer, 0.1, dur)), 1.3, 0.4, rng)


# ================================================================= A 級：考古刷子・古代遺物

def brush_sweep(rng, v):
    """刷去沙土：一下一下柔軟的刷聲（帶通噪聲的短掃動）+ 沙粒灑落"""
    dur = 1.6
    out = np.zeros(int(SR * dur))
    for i in range(5):
        s = bandpass(noise(0.22, rng), 500, 2400) * env(0.22, 0.06, 0.15, 4) * 0.5
        out += at(s, i * 0.3, dur)
    sand = lowpass(crackle(dur, rng, 60, 1000, 3000, (0.02, 0.08)), 2800)
    return dark_verb(mix(out, sand), 0.5, 0.25, rng)


def relic_reveal(rng, v):
    """遺物出土：古老的低沉吟唱（五聲音階的暗合唱）→ 重擊 + 遠古的風 + 低沉的陶笛"""
    dur = 4.0
    pre = swell(0.5, rng, 150, 900, 2.5) * 0.6
    hit = heavy_hit(rng, v, 0.85, 2.0)
    ch = lowpass(choir(3.2, [73.4, 82.4, 110, 123.5], rng, 0.2, 2), 1200) * env(3.2, 0.2, 3.2, 1.0) * 0.6
    wind = sweep_filter(noise(3.0, rng), 300, 700, q=1.5) * env(3.0, 0.5, 3.0, 1.2) * 0.4
    flute = lowpass(tone(lambda tt: 294 * (1 + 0.01 * np.sin(2 * np.pi * 5 * tt)), 1.6), 1500) * env(1.6, 0.2, 1.6, 2) * 0.2
    body = mix(hit, at(ch, 0.05, dur), wind, at(flute, 0.8, dur))
    return dark_verb(mix(pre, at(body, 0.5, dur + 0.5)), 1.5, 0.45, rng)


# ================================================================= A 級：磁石・磁極指引

def magnet_hum(rng, v):
    """磁場湧動：低沉的電磁嗡鳴往上推，帶著搖擺（羅盤被拉扯）"""
    dur = 2.2
    t = t_axis(dur)
    f = 60 * (1 + 0.4 * (t / dur)) * (1 + 0.03 * np.sin(2 * np.pi * 6 * t))
    hum = lowpass(np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)), 600) * 0.3 + tone(lambda tt: 120 + 50 * tt / dur, dur) * 0.2
    wobble = bandpass(noise(dur, rng), 200, 800) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t)) * 0.3
    return dark_verb(mix(hum, wobble) * env(dur, 0.3, dur, 0.5), 0.8, 0.3, rng)


def magnet_lock(rng, v):
    """指針咬合：吸入 → 「喀」地鎖死 + 重擊 + 磁極共鳴的長鳴"""
    dur = 3.2
    pre = swell(0.35, rng, 200, 1300, 3) * 0.6
    lock = mix(lowpass(fm_metal(0.5, 740 + v * 30, 1.41, 2.5, 8), 2500) * 0.5, clunk(rng, 900, 1.0, 0.4))
    hit = heavy_hit(rng, v, 0.9, 1.6)
    ring = lowpass(fm_metal(2.6, 147 + v * 6, 2.0, 1.3, 1.1), 1400) * 0.4
    body = mix(lock, hit, at(ring, 0.05, dur))
    return dark_verb(mix(pre, at(body, 0.35, dur + 0.35)), 1.2, 0.4, rng)


# ================================================================= 門：封印解除／結界再封

def seal_open(rng, v):
    """封印解除：沉重的門閂「喀」地脫落 + 鐵鏈鬆開的嘩啦 + 符紙翻飛 + 低沉的釋放氣流"""
    dur = 1.3
    latch = clunk(rng, 600 + v * 40, 1.0, 0.5) * 1.1
    chain = np.zeros(int(SR * dur))
    for i in range(9):
        ck = lowpass(fm_metal(0.08, rng.uniform(1100, 1700), 1.9, 1.5, 30), 3000) * rng.uniform(0.08, 0.22)
        chain += at(ck, 0.08 + i * rng.uniform(0.025, 0.05), dur)
    paper = flutter(0.4, rng, 28, 400, 2400) * env(0.4, 0.05, 0.3, 4) * 0.35
    release = sweep_filter(noise(0.9, rng), 300, 1200, q=1.3) * env(0.9, 0.05, 0.6, 3) * 0.45
    return dark_verb(mix(latch, chain, at(paper, 0.1, dur), at(release, 0.05, dur)), 0.7, 0.3, rng)


def seal_close(rng, v):
    """結界再封：氣流往內吸 → 符印「砰」地蓋上 + 低沉的封印共鳴"""
    dur = 1.4
    suck = swell(0.3, rng, 1200, 250, 2.5) * 0.6
    stamp = heavy_hit(rng, v, 1.1, 0.9)
    hum = lowpass(fm_metal(1.0, 147 + v * 8, 2.0, 1.2, 2.5), 1200) * 0.35
    return dark_verb(mix(at(suck, 0, dur), at(stamp, 0.2, dur), at(hum, 0.3, dur)), 0.7, 0.3, rng)


# ================================================================= B 級（短演出，3 秒內）

def bubbles(dur, rng, n, lo=80, hi=240, amp=(0.1, 0.3), rise=8):
    """低沉的咕嘟（音高上滑的短正弦）"""
    out = np.zeros(int(SR * dur))
    for _ in range(n):
        f = rng.uniform(lo, hi)
        b = tone(lambda tt, f=f: f * (1 + tt * rise), 0.09) * env(0.09, 0.004, 0.09, 13) * rng.uniform(*amp)
        out += at(b, rng.uniform(0, max(0.01, dur - 0.1)), dur)
    return out


def cauldron_fill(rng, v):
    """魔女之釜・注水：水「嘩」地灌入（低通的水流）+ 鍋身的低沉鐵鳴 + 咕嘟冒泡"""
    dur = 2.0
    pour = lowpass(noise(0.9, rng), 900) * env(0.9, 0.05, 0.9, 2.5) * 0.8
    pour = pour * (0.7 + 0.3 * np.sin(2 * np.pi * 11 * t_axis(0.9)))
    pot = lowpass(metal_ring(1.8, 110 + v * 6, 1.6), 1200) * 0.45
    bub = bubbles(1.6, rng, 26, 90, 260)
    return dark_verb(mix(pour, at(pot, 0.05, dur), at(bub, 0.3, dur)), 0.8, 0.35, rng)


def cauldron_lava(rng, v):
    """熔岩灌入：濃稠的低頻咕嚕 + 熔岩的悶吼 + 火星劈啪"""
    dur = 2.2
    glug = bubbles(1.6, rng, 18, 45, 110, (0.25, 0.5), 3)
    roar = lowpass(noise(dur, rng), 380) * env(dur, 0.2, dur, 2.2) * 0.9
    boom = drop(1.0, 80, 32, 10, 3) * 0.9
    sp = crackle(1.6, rng, 16, 900, 2800, (0.05, 0.2))
    return dark_verb(soft(mix(boom, roar, at(glug, 0.1, dur), at(sp, 0.2, dur)), 1.6), 0.8, 0.3, rng)


def cauldron_frost(rng, v):
    """細雪落入：冰晶凝結的低沉玻璃 + 冷風 + 結霜的細碎聲"""
    dur = 2.2
    wind = sweep_filter(noise(dur, rng), 900, 260, q=1.4) * env(dur, 0.2, dur, 2) * 0.5
    ice = lowpass(glass(1.8, rng, 520 + v * 30, 6, 2.2), 2200) * 0.35
    frost = crackle(1.2, rng, 30, 1200, 3200, (0.03, 0.12))
    thud = drop(0.6, 110, 50, 14, 7) * 0.6
    return dark_verb(mix(wind, thud, at(ice, 0.05, dur), at(frost, 0.1, dur)), 1.0, 0.4, rng, 2600)


def cauldron_swirl(rng, v):
    """舀水 / 洗去顏色：水在鍋中迴旋（往返的帶通掃頻）+ 吸入般的收尾 + 低沉鐵鳴"""
    dur = 1.8
    t = t_axis(dur)
    sw = bandpass(noise(dur, rng), 180, 1100) * (0.5 + 0.5 * np.sin(2 * np.pi * (2 + t * 3) * t)) * env(dur, 0.1, dur, 2.2) * 0.8
    slurp = swell(0.4, rng, 1200, 300, 2) * 0.5
    pot = lowpass(metal_ring(1.2, 98 + v * 5, 2.2), 1000) * 0.3
    return dark_verb(mix(sw, at(slurp, 1.0, dur), at(pot, 1.35, dur + 1)), 0.7, 0.3, rng)


def wax_seal(rng, v):
    """蜜蠟封緘：黏稠的蜜被抹開（低通的濕噪聲）→ 蠟印「咚」地蓋上 + 溫暖的低鳴"""
    dur = 2.0
    smear = lowpass(noise(0.45, rng), 700) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t_axis(0.45))) * env(0.45, 0.1, 0.35, 2) * 0.6
    stamp = heavy_hit(rng, v, 1.15, 1.0) * 0.8
    hum = lowpass(choir(1.4, [196 + v * 6, 247, 294], rng, 0.1), 1200) * 0.4
    return dark_verb(mix(smear, at(stamp, 0.35, dur), at(hum, 0.4, dur)), 0.8, 0.3, rng)


def copper_scrape(rng, v):
    """返璞歸真：斧刃刮過銅面（沙沙的金屬刮擦）+ 鏽屑剝落 + 新銅的低沉鳴響"""
    dur = 1.9
    t = t_axis(0.55)
    scr = bandpass(noise(0.55, rng), 600, 2600) * (0.4 + 0.6 * np.abs(np.sin(2 * np.pi * 9 * t))) * env(0.55, 0.03, 0.5, 1.5) * 0.6
    flakes = crackle(0.8, rng, 22, 900, 2600, (0.04, 0.16))
    ring = metal_ring(1.5, 220 + v * 12, 1.8, 0.6)
    return dark_verb(mix(scr, at(flakes, 0.1, dur), at(ring, 0.5, dur)), 0.8, 0.3, rng)


def candle_light(rng, v):
    """燭火點靈：火柴般的「嗤」→ 火苗「噗」地立起 + 溫暖的低沉鐘音"""
    dur = 1.8
    strike = bandpass(noise(0.1, rng), 900, 3000) * env(0.1, 0.005, 0.1, 6) * 0.4
    puff = lowpass(noise(0.35, rng), 600) * env(0.35, 0.02, 0.35, 5) * 0.8
    bell = lowpass(glass(1.5, rng, 330 + v * 25, 4, 2.6), 1800) * 0.3
    warm = tone(165 + v * 12, 1.5) * env(1.5, 0.1, 1.5, 2.2) * 0.3
    return dark_verb(mix(strike, at(puff, 0.06, dur), at(bell, 0.1, dur), at(warm, 0.1, dur)), 0.8, 0.35, rng)


def candle_snuff(rng, v):
    """吹熄：一口氣（下掃的柔和噪聲）+ 火苗消失的悶聲 + 一縷煙"""
    dur = 1.0
    breath = sweep_filter(noise(0.35, rng), 1200, 300, q=1.2) * env(0.35, 0.08, 0.25, 3) * 0.6
    pop = drop(0.25, 120, 60, 18, 10) * 0.4
    return dark_verb(mix(breath, at(pop, 0.2, dur)), 0.5, 0.3, rng)


def camp_ignite(rng, v):
    """營火祭禮：柴堆「轟」地燃起 + 木柴劈啪 + 低沉的火焰持續"""
    dur = 2.4
    whump = mix(drop(0.8, 110, 40, 12, 5) * 0.9, lowpass(noise(0.6, rng), 900) * env(0.6, 0.02, 0.6, 4) * 0.8)
    body = lowpass(noise(dur, rng), 500) * env(dur, 0.3, dur, 1.8) * 0.5
    pops = crackle(2.0, rng, 28, 800, 2800, (0.08, 0.3))
    return dark_verb(soft(mix(whump, body, at(pops, 0.1, dur)), 1.4), 0.8, 0.3, rng)


def camp_douse(rng, v):
    """熄滅營火：長長的「嘶——」（壓暗）+ 濕柴的悶響"""
    dur = 1.8
    hiss = bandpass(noise(dur, rng), 500, 2400) * env(dur, 0.01, dur, 2.6) * 0.5
    hiss = lowpass(hiss, 1800)
    thud = drop(0.5, 90, 40, 12, 7) * 0.6
    return dark_verb(mix(thud, hiss, crackle(0.8, rng, 8, 700, 2000, (0.03, 0.12))), 0.7, 0.3, rng)


def camp_place(rng, v):
    """放上食材：「滋——」的煎烤聲 + 木頭輕碰"""
    dur = 1.4
    knock = clunk(rng, 700, 1.2, 0.4) * 0.5
    sizzle = bandpass(noise(1.2, rng), 900, 3200) * (0.6 + 0.4 * rng.uniform(0, 1, int(SR * 1.2))) * env(1.2, 0.05, 1.2, 2.5) * 0.25
    return dark_verb(mix(knock, at(lowpass(sizzle, 2600), 0.08, dur), crackle(1.2, rng, 14, 1000, 3000, (0.03, 0.1))), 0.5, 0.25, rng)


def tnt_fuse(rng, v):
    """爆炎序曲：引信的嘶嘶聲 + 越來越近的低沉不祥脈動（心跳）+ 往上推的暗掃頻"""
    dur = 3.0
    fuse = bandpass(noise(dur, rng), 1200, 3600) * (0.5 + 0.5 * rng.uniform(0, 1, int(SR * dur))) * 0.18
    fuse = lowpass(fuse, 3000) * env(dur, 0.05, dur, 0.4)
    beats = np.zeros(int(SR * dur))
    tt = 0.15
    i = 0
    while tt < dur - 0.2:
        beats += at(drop(0.3, 70, 40, 18, 9) * (0.5 + i * 0.07), tt, dur)
        tt += max(0.16, 0.5 - i * 0.05)
        i += 1
    rise = riser(dur, rng, 80, 900) * 0.45
    return soft(mix(fuse, beats, rise), 1.5)


def stone_saw(rng, v):
    """斷岩一閃：鋸片高速轉動的低吼 → 一刀切穿（下掃）+ 石塊裂開的重擊 + 碎石"""
    dur = 1.8
    t = t_axis(0.5)
    whirr = bandpass(noise(0.5, rng), 300, 1400) * (0.6 + 0.4 * np.sin(2 * np.pi * 60 * t)) * env(0.5, 0.15, 0.4, 1.5) * 0.5
    cut = sweep_filter(noise(0.25, rng), 2400, 300, q=2) * env(0.25, 0.01, 0.25, 3) * 0.7
    crack = heavy_hit(rng, v, 1.1, 0.9) * 0.7
    grit = crackle(0.8, rng, 24, 700, 2400, (0.05, 0.2))
    return dark_verb(mix(whirr, at(cut, 0.4, dur), at(crack, 0.5, dur), at(grit, 0.6, dur)), 0.7, 0.3, rng)


def loom_weave(rng, v):
    """天衣織紋：梭子來回的木頭咔嗒（越來越快）+ 布料翻飛 + 溫暖的和聲浮現"""
    dur = 2.2
    clicks = np.zeros(int(SR * dur))
    tt = 0.0
    i = 0
    while tt < 1.2:
        clicks += at(lowpass(clunk(rng, 1100 + (i % 2) * 200, 1.4, 0.2), 2000) * 0.35, tt, dur)
        tt += max(0.07, 0.22 - i * 0.02)
        i += 1
    cloth = flutter(1.4, rng, 16, 200, 1600) * env(1.4, 0.2, 1.4, 1.5) * 0.35
    chord = lowpass(choir(1.4, [220 + v * 8, 277, 330, 440], rng, 0.3), 1500) * 0.45
    return dark_verb(mix(clicks, at(cloth, 0.1, dur), at(chord, 1.0, dur + 0.6)), 0.9, 0.3, rng)


def crafter_pulse(rng, v):
    """機巧自律：齒輪咬合的連續咔嗒 + 活塞「嘶」+ 沉重的機械落定"""
    dur = 1.8
    gears = np.zeros(int(SR * dur))
    for i in range(7):
        gears += at(lowpass(fm_metal(0.08, 600 + i * 40, 1.7, 2, 30), 1800) * 0.3, 0.05 + i * 0.07, dur)
    steam = bandpass(noise(0.4, rng), 600, 2200) * env(0.4, 0.01, 0.4, 4) * 0.3
    land = clunk(rng, 500, 0.9, 0.6) * 0.9
    hum = lowpass(fm_metal(1.0, 82 + v * 4, 2, 1, 2.5), 900) * 0.3
    return dark_verb(mix(gears, at(steam, 0.5, dur), at(land, 0.6, dur), at(hum, 0.6, dur)), 0.7, 0.3, rng)


def disc_drop(rng, v):
    """天籟之音：唱片落入的「喀」+ 唱盤轉起來的低沉嗡鳴（音高上升）+ 遠處的和弦"""
    dur = 2.4
    click = clunk(rng, 1000, 1.3, 0.3) * 0.5
    spin = tone(lambda tt: 40 + 40 * (1 - np.exp(-tt * 3)), 1.6) * env(1.6, 0.2, 1.6, 1.5) * 0.5
    chord = lowpass(choir(1.8, [261.6 * (1 + v * 0.06), 329.6, 392, 523], rng, 0.5), 1600) * 0.5
    bells = np.zeros(int(SR * dur))
    for i, f in enumerate([523, 659, 784, 659]):
        bells += at(lowpass(glass(0.9, rng, f, 3, 4), 2200) * 0.1, 0.6 + i * 0.25, dur)
    return dark_verb(mix(click, at(spin, 0.1, dur), at(chord, 0.5, dur), bells), 1.1, 0.35, rng)


def book_place(rng, v):
    """智典開示・放書：厚重的典籍「咚」落在講台 + 紙頁翻飛 + 低沉的吟唱"""
    dur = 2.2
    thump = clunk(rng, 600, 0.8, 0.6)
    pages = flutter(0.5, rng, 30, 400, 2200) * env(0.5, 0.02, 0.5, 3) * 0.35
    chant = lowpass(choir(1.6, [110 + v * 4, 165, 220], rng, 0.4), 1100) * 0.5
    return dark_verb(mix(thump, at(pages, 0.1, dur), at(chant, 0.25, dur)), 1.0, 0.35, rng)


def page_turn(rng, v):
    """翻頁：一張厚紙翻過 + 低低的耳語般氣流"""
    dur = 0.9
    flip = flutter(0.3, rng, 40, 500, 2400) * env(0.3, 0.03, 0.27, 2.5) * 0.45
    air = sweep_filter(noise(0.6, rng), 400, 1000, q=1.4) * env(0.6, 0.15, 0.4, 3) * 0.25
    return dark_verb(mix(flip, at(air, 0.05, dur)), 0.5, 0.25, rng)


def bell_toll(rng, v):
    """晨鐘破邪：深沉的寺鐘（非諧波長鳴，壓暗）+ 重擊 + 低沉的嗡嗡餘振"""
    dur = 3.2
    strike = heavy_hit(rng, v, 1.0, 1.0) * 0.6
    partials = [(0.5, 0.5), (1.0, 0.6), (1.19, 0.3), (1.5, 0.25), (2.0, 0.18), (2.52, 0.1)]
    base = 196 * (1 + v * 0.04)
    ring = sum(tone(base * r, dur) * env(dur, 0.002, dur, 1.0 + i * 0.5) * a for i, (r, a) in enumerate(partials))
    beat = 1 + 0.15 * np.sin(2 * np.pi * 2.3 * t_axis(dur))
    return dark_verb(mix(strike, lowpass(ring * beat, 1800) * 0.6), 1.4, 0.4, rng)


def compost_add(rng, v):
    """輪迴・投入：濕潤的泥土「噗嗤」+ 植物被壓碎的沙沙"""
    dur = 1.0
    squish = lowpass(noise(0.3, rng), 500) * env(0.3, 0.01, 0.3, 6) * 0.9
    rustle = bandpass(noise(0.4, rng), 600, 2400) * env(0.4, 0.02, 0.4, 5) * 0.25
    thud = drop(0.3, 100, 50, 18, 10) * 0.5
    return dark_verb(mix(squish, rustle, thud), 0.5, 0.25, rng)


def compost_harvest(rng, v):
    """輪迴新生：泥土翻開 + 新芽冒出的柔和上滑音 + 溫暖的大調和聲"""
    dur = 2.4
    dig = lowpass(noise(0.4, rng), 700) * env(0.4, 0.02, 0.4, 4) * 0.7
    sprout = np.zeros(int(SR * dur))
    for i, f in enumerate([196, 247, 294, 392]):
        sprout += at(tone(lambda tt, f=f: f * (1 + 0.3 * (1 - np.exp(-tt * 6))), 0.8) * env(0.8, 0.02, 0.8, 4) * 0.18, 0.2 + i * 0.12, dur)
    chord = lowpass(choir(1.8, [196 * (1 + v * 0.05), 247, 294, 392], rng, 0.3), 1500) * 0.45
    return dark_verb(mix(dig, sprout, at(chord, 0.4, dur)), 1.0, 0.35, rng)


def honey_drip(rng, v):
    """蜂蜜取瓶：黏稠的「咕嚕」灌入瓶中 + 蜂群的低鳴 + 玻璃輕響"""
    dur = 1.8
    glug = bubbles(1.0, rng, 10, 70, 160, (0.25, 0.45), 4)
    t = t_axis(dur)
    buzz = lowpass(np.sign(np.sin(2 * np.pi * (170 + 8 * np.sin(2 * np.pi * 5 * t)) * t)), 700) * env(dur, 0.3, dur, 2) * 0.12
    clink = lowpass(glass(0.8, rng, 700 + v * 40, 3, 6), 2200) * 0.15
    return dark_verb(mix(glug, buzz, at(clink, 0.85, dur)), 0.7, 0.3, rng)


def comb_cut(rng, v):
    """剪下蜂巢片：剪刀「喀嚓」+ 蜂蠟剝落 + 蜂群被驚動的低鳴"""
    dur = 1.8
    snip = mix(clunk(rng, 1600, 1.6, 0.2) * 0.4, bandpass(noise(0.05, rng), 1500, 4000) * env(0.05, 0.001, 0.05, 10) * 0.3)
    t = t_axis(dur)
    buzz = lowpass(np.sign(np.sin(2 * np.pi * (190 + 20 * np.sin(2 * np.pi * 7 * t)) * t)), 800) * env(dur, 0.1, dur, 1.6) * 0.16
    wax = crackle(0.6, rng, 12, 800, 2400, (0.05, 0.15))
    return dark_verb(mix(snip, at(wax, 0.05, dur), at(buzz, 0.1, dur)), 0.6, 0.3, rng)


def pumpkin_carve(rng, v):
    """南瓜鬼面：刀刃挖過（濕潤的刮擦）+ 空心的「咚」+ 詭譎的低沉笑聲般的搖擺音"""
    dur = 2.2
    t = t_axis(0.4)
    carve = lowpass(noise(0.4, rng), 1200) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 6 * t))) * env(0.4, 0.02, 0.4, 2) * 0.6
    hollow = drop(0.6, 150, 80, 10, 6) * 0.7
    tt = t_axis(1.6)
    eerie = lowpass(np.sin(2 * np.pi * np.cumsum(110 * (1 + 0.06 * np.sin(2 * np.pi * 4 * tt))) / SR), 900) * env(1.6, 0.3, 1.6, 2) * 0.35
    eerie2 = tone(lambda x: 116.5 * (1 + 0.05 * np.sin(2 * np.pi * 3.3 * x)), 1.6) * env(1.6, 0.3, 1.6, 2) * 0.25
    return dark_verb(mix(carve, at(hollow, 0.35, dur), at(eerie + eerie2, 0.45, dur)), 0.9, 0.35, rng)


def spawner_bind(rng, v):
    """魂籠召喚：鐵籠的低沉嗡鳴 + 靈魂被吸入的反向尖嘯（壓暗）→ 封存的「喀」"""
    dur = 2.4
    cage = lowpass(fm_metal(2.0, 73 + v * 3, 1.5, 3, 1.5), 900) * 0.5
    suck = swell(0.9, rng, 1600, 200, 2.5) * 0.6
    lock = clunk(rng, 800, 1.1, 0.5) * 0.8
    whisper = lowpass(choir(1.4, [138, 146.8, 207], rng, 0.2), 1000) * 0.35
    return dark_verb(mix(cage, at(suck, 0.1, dur), at(lock, 1.0, dur), at(whisper, 1.0, dur)), 1.0, 0.35, rng)


def ender_open(rng, v):
    """虛空寶庫：沉重石蓋滑開 + 虛空的低沉嗡鳴 + 詭異的反向和弦"""
    dur = 2.4
    lid = lowpass(noise(0.6, rng), 450) * env(0.6, 0.1, 0.5, 2) * 0.7
    void = tone(lambda tt: 55 + 6 * np.sin(2 * np.pi * 0.8 * tt), dur) * env(dur, 0.2, dur, 1.6) * 0.6
    chord = lowpass(choir(1.6, [146.8, 174.6, 207.7, 277], rng, 0.6), 1300)[::-1] * 0.4
    glints = lowpass(glass(1.2, rng, 600 + v * 30, 5, 3), 2000) * 0.15
    return dark_verb(mix(lid, void, at(chord, 0.0, dur), at(glints, 1.6, dur + 1)), 1.2, 0.4, rng)


def shulker_open(rng, v):
    """匣中異界：殼「喀啦」掀開 + 漂浮的低沉顫音 + 空間摺疊的掃頻"""
    dur = 2.0
    creak = lowpass(fm_metal(0.3, 300, 2.3, 4, 6), 1500) * 0.4
    shell = clunk(rng, 900, 1.1, 0.4) * 0.6
    t = t_axis(1.6)
    float_ = lowpass(np.sin(2 * np.pi * np.cumsum(220 * (1 + 0.02 * np.sin(2 * np.pi * 6 * t))) / SR), 1200) * env(1.6, 0.3, 1.6, 2) * 0.3
    fold = sweep_filter(noise(0.7, rng), 300, 1400, q=2.5) * env(0.7, 0.3, 0.4, 3) * 0.4
    return dark_verb(mix(shell, at(creak, 0.02, dur), at(float_, 0.2, dur), at(fold, 0.1, dur)), 0.9, 0.35, rng)


def golem_pose(rng, v):
    """銅像覺醒：銅關節「嘎吱」轉動 + 沉重的金屬落定 + 銅的鳴響"""
    dur = 1.8
    creak = lowpass(fm_metal(0.35, 180 + v * 20, 2.7, 5, 4), 1600) * 0.4
    servo = sweep_filter(noise(0.35, rng), 400, 900, q=3) * env(0.35, 0.05, 0.3, 2) * 0.3
    land = clunk(rng, 700, 1.0, 0.5) * 0.8
    ring = metal_ring(1.3, 240 + v * 15, 1.8, 0.5)
    return dark_verb(mix(creak, servo, at(land, 0.32, dur), at(ring, 0.33, dur)), 0.8, 0.3, rng)


def sign_dye(rng, v):
    """彩墨染書：毛筆一筆刷過（低沉的布料掃動）+ 墨滴落下的悶響"""
    dur = 1.4
    stroke = sweep_filter(noise(0.4, rng), 1300, 300, q=1.6) * env(0.4, 0.12, 0.3, 3) * 0.7
    drip = drop(0.4, 220, 110, 14, 8) * 0.4
    return dark_verb(mix(stroke, at(drip, 0.35, dur)), 0.6, 0.3, rng)


def sign_glow(rng, v):
    """螢光刻文：文字一個一個亮起（低沉的玻璃鈴，逐字上升）+ 溫柔的光之嗡鳴"""
    dur = 2.0
    out = np.zeros(int(SR * dur))
    for i in range(5):
        out += at(lowpass(glass(0.9, rng, 330 * (1 + i * 0.12) * (1 + v * 0.03), 4, 4), 2200) * 0.15, 0.05 + i * 0.12, dur)
    hum = lowpass(choir(1.6, [220, 330, 440], rng, 0.4), 1400) * 0.35
    return dark_verb(mix(out, at(hum, 0.2, dur)), 1.0, 0.4, rng, 2800)


def cake_bite(rng, v):
    """甘味祝宴：柔軟的一口（悶的咬合）+ 溫暖的小調轉大調鈴聲 + 輕柔的低音"""
    dur = 1.8
    bite = lowpass(noise(0.12, rng), 900) * env(0.12, 0.005, 0.12, 6) * 0.6
    chew = lowpass(noise(0.3, rng), 600) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t_axis(0.3))) * env(0.3, 0.02, 0.3, 4) * 0.3
    bells = np.zeros(int(SR * dur))
    for i, f in enumerate([392, 494, 587, 784]):
        bells += at(lowpass(glass(1.0, rng, f * (1 + v * 0.04), 3, 4), 2200) * 0.12, 0.2 + i * 0.09, dur)
    warm = tone(196, 1.4) * env(1.4, 0.1, 1.4, 2.5) * 0.25
    return dark_verb(mix(bite, at(chew, 0.1, dur), bells, at(warm, 0.2, dur)), 0.9, 0.35, rng)


B_SFX = {
    "cauldron_fill": cauldron_fill, "cauldron_lava": cauldron_lava, "cauldron_frost": cauldron_frost, "cauldron_swirl": cauldron_swirl,
    "wax_seal": wax_seal, "copper_scrape": copper_scrape, "candle_light": candle_light, "candle_snuff": candle_snuff,
    "camp_ignite": camp_ignite, "camp_douse": camp_douse, "camp_place": camp_place, "tnt_fuse": tnt_fuse,
    "stone_saw": stone_saw, "loom_weave": loom_weave, "crafter_pulse": crafter_pulse, "disc_drop": disc_drop,
    "book_place": book_place, "page_turn": page_turn, "bell_toll": bell_toll, "compost_add": compost_add,
    "compost_harvest": compost_harvest, "honey_drip": honey_drip, "comb_cut": comb_cut, "pumpkin_carve": pumpkin_carve,
    "spawner_bind": spawner_bind, "ender_open": ender_open, "shulker_open": shulker_open, "golem_pose": golem_pose,
    "sign_dye": sign_dye, "sign_glow": sign_glow, "cake_bite": cake_bite,
}


# ================================================================= C 級（微型，1 秒內、每天按很多次：短、輕、不刺耳）

def lid_thud(rng, v):
    """箱蓋闔上：沉悶的木頭「咚」+ 一點點鎖扣金屬聲"""
    dur = 0.7
    thud = clunk(rng, 520 + v * 30, 0.9, 0.5)
    latch = lowpass(fm_metal(0.15, 900 + v * 40, 1.6, 1.2, 22), 2000) * 0.15
    return dark_verb(mix(thud, at(latch, 0.03, dur)), 0.3, 0.2, rng)


def hatch_clank(rng, v):
    """漏斗 / 發射器 / 投擲器：鐵製機關的悶響「喀鏘」"""
    dur = 0.7
    hit = clunk(rng, 700, 1.1, 0.4) * 0.8
    ring = metal_ring(0.6, 260 + v * 20, 3.5, 0.35)
    return dark_verb(mix(hit, ring), 0.3, 0.2, rng)


def book_slide(rng, v):
    """書本滑進 / 抽出書架：紙與木的摩擦 + 輕輕的「叩」"""
    dur = 0.6
    slide = bandpass(noise(0.22, rng), 400, 2000) * env(0.22, 0.03, 0.2, 2.5) * 0.35
    tap = clunk(rng, 900, 1.3, 0.3) * 0.45
    return dark_verb(mix(slide, at(tap, 0.18, dur)), 0.3, 0.2, rng)


def shelf_place(rng, v):
    """架上換物：木頭輕碰 + 短短的布料摩擦"""
    dur = 0.5
    return dark_verb(mix(clunk(rng, 1000, 1.4, 0.3) * 0.5, bandpass(noise(0.12, rng), 500, 2200) * env(0.12, 0.01, 0.12, 4) * 0.2), 0.3, 0.2, rng)


def pot_drop(rng, v):
    """物品落進陶罐：空心的陶土「咚」（帶共鳴）"""
    dur = 0.8
    hollow = drop(0.6, 260 + v * 20, 170, 10, 6) * 0.6
    clay = lowpass(noise(0.05, rng), 1200) * env(0.05, 0.001, 0.05, 12) * 0.5
    return dark_verb(mix(clay, hollow), 0.4, 0.3, rng)


def plant_settle(rng, v):
    """種進花盆：泥土的沙沙 + 葉子輕輕展開"""
    dur = 0.7
    soil = lowpass(noise(0.2, rng), 800) * env(0.2, 0.01, 0.2, 5) * 0.5
    leaf = flutter(0.3, rng, 26, 600, 2400) * env(0.3, 0.05, 0.25, 3) * 0.2
    return dark_verb(mix(soil, at(leaf, 0.08, dur)), 0.3, 0.2, rng)


def note_glow(rng, v):
    """調音：柔和的低沉玻璃泛音（音高由遊戲端依音符調整）"""
    dur = 0.8
    return dark_verb(lowpass(glass(0.7, rng, 330, 3, 5), 1800) * 0.4, 0.5, 0.35, rng)


def lever_click(rng, v):
    """拉桿：沉重的木柄「喀達」兩段"""
    dur = 0.5
    a = clunk(rng, 800, 1.2, 0.25) * 0.5
    b = clunk(rng, 600, 1.0, 0.3) * 0.7
    return dark_verb(mix(a, at(b, 0.07, dur)), 0.25, 0.2, rng)


def button_tick(rng, v):
    """按鈕：短而悶的「嗒」"""
    return dark_verb(clunk(rng, 1100, 1.5, 0.25) * 0.6, 0.2, 0.15, rng)


def diode_tick(rng, v):
    """中繼器 / 比較器：細小的機械「咔」+ 一點紅石的低鳴"""
    dur = 0.5
    tick = lowpass(fm_metal(0.06, 1200, 1.9, 1.5, 40), 2400) * 0.35
    hum = tone(110 + v * 10, 0.4) * env(0.4, 0.01, 0.4, 6) * 0.2
    return dark_verb(mix(tick, hum), 0.25, 0.2, rng)


def daylight_flip(rng, v):
    """陽光感測器切換：柔和的上滑（日）/ 下滑（夜，遊戲端降音）玻璃音"""
    dur = 0.8
    sw = tone(lambda t: 300 * (1 + 0.6 * t / 0.5), 0.5) * env(0.5, 0.02, 0.5, 4) * 0.25
    return dark_verb(mix(lowpass(sw, 1600), lowpass(glass(0.6, rng, 520, 3, 6), 1800) * 0.15), 0.5, 0.35, rng)


def bark_peel(rng, v):
    """剝樹皮：粗糙的撕裂（帶通噪聲的快速起伏）+ 木屑"""
    dur = 0.6
    t = t_axis(0.3)
    rip = bandpass(noise(0.3, rng), 500, 2400) * (0.4 + 0.6 * np.abs(np.sin(2 * np.pi * 14 * t))) * env(0.3, 0.02, 0.28, 2) * 0.5
    return dark_verb(mix(rip, crackle(0.4, rng, 8, 800, 2400, (0.05, 0.15))), 0.25, 0.2, rng)


def soil_till(rng, v):
    """鋤地：泥土被翻開的悶聲 + 土塊散落"""
    dur = 0.6
    turn = lowpass(noise(0.18, rng), 600) * env(0.18, 0.01, 0.18, 5) * 0.8
    return dark_verb(mix(turn, drop(0.25, 110, 60, 18, 10) * 0.4, crackle(0.4, rng, 8, 500, 1600, (0.05, 0.15))), 0.25, 0.2, rng)


def path_press(rng, v):
    """鏟出小徑：泥土被壓實的「噗」+ 塵土"""
    dur = 0.6
    press = drop(0.3, 120, 55, 16, 8) * 0.6
    dust = lowpass(noise(0.3, rng), 900) * env(0.3, 0.02, 0.3, 5) * 0.4
    return dark_verb(mix(press, dust), 0.25, 0.2, rng)


def bloom_burst(rng, v):
    """骨粉：輕柔的生長上滑音（三個音）+ 葉片沙沙"""
    dur = 0.8
    out = np.zeros(int(SR * dur))
    for i, f in enumerate([294, 370, 440]):
        out += at(tone(lambda tt, f=f: f * (1 + 0.15 * (1 - np.exp(-tt * 10))), 0.4) * env(0.4, 0.01, 0.4, 7) * 0.12, i * 0.06, dur)
    leaf = flutter(0.3, rng, 24, 600, 2200) * env(0.3, 0.03, 0.3, 4) * 0.15
    return dark_verb(mix(lowpass(out, 1800), leaf), 0.5, 0.3, rng)


def berry_pop(rng, v):
    """摘莓果：幾顆果實「啵」地彈出（短的上滑正弦）"""
    dur = 0.6
    out = np.zeros(int(SR * dur))
    for i in range(3):
        f = rng.uniform(200, 320)
        out += at(tone(lambda tt, f=f: f * (1 + tt * 6), 0.07) * env(0.07, 0.003, 0.07, 12) * 0.35, i * 0.07 + rng.uniform(0, 0.02), dur)
    return dark_verb(mix(out, lowpass(noise(0.1, rng), 1200) * env(0.1, 0.005, 0.1, 8) * 0.25), 0.3, 0.2, rng)


C_SFX = {
    "lid_thud": lid_thud, "hatch_clank": hatch_clank, "book_slide": book_slide, "shelf_place": shelf_place, "pot_drop": pot_drop,
    "plant_settle": plant_settle, "note_glow": note_glow, "lever_click": lever_click, "button_tick": button_tick, "diode_tick": diode_tick,
    "daylight_flip": daylight_flip, "bark_peel": bark_peel, "soil_till": soil_till, "path_press": path_press, "bloom_burst": bloom_burst,
    "berry_pop": berry_pop,
}


# ================================================================= 生物互動（D 批）

def buzz(dur, f, rng, wobble=5.0, depth=0.04):
    """低沉的方波嗡鳴（蜂、魔力、電流）"""
    t = t_axis(dur)
    return lowpass(np.sign(np.sin(2 * np.pi * np.cumsum(f * (1 + depth * np.sin(2 * np.pi * wobble * t))) / SR)), 900)


def golem_awaken(rng, v):
    """鐵壁守護・甦醒：鐵塊互相擠壓的低沉呻吟 + 核心點燃的嗡鳴往上推 + 齒輪咔嗒"""
    dur = 2.6
    groan = lowpass(fm_metal(dur, 55 + v * 3, 1.33, 4, 0.8), 600) * env(dur, 0.4, dur, 1.2) * 0.8
    core = tone(lambda t: 70 + 40 * (t / dur) ** 2, dur) * env(dur, 0.8, dur, 0.6) * 0.5
    clicks = np.zeros(int(SR * dur))
    for i in range(10):
        clicks += at(lowpass(clunk(rng, 500 + i * 30, 0.8, 0.3), 1600) * 0.3, 0.2 + i * 0.2 + rng.uniform(0, 0.05), dur)
    return dark_verb(mix(groan, core, clicks), 1.0, 0.35, rng)


def golem_stomp(rng, v):
    """鐵壁守護・踏地：吸入 → 鐵拳砸地的巨大重擊 + 金屬長鳴 + 碎石"""
    dur = 2.8
    pre = swell(0.4, rng, 200, 900, 3) * 0.6
    hit = heavy_hit(rng, v, 0.75, 2.0) * 1.2
    ring = metal_ring(2.2, 90 + v * 6, 1.2, 0.8)
    rubble = crackle(1.2, rng, 30, 400, 1800, (0.08, 0.3))
    return dark_verb(mix(pre, at(mix(hit, ring, rubble), 0.4, dur)), 1.3, 0.4, rng)


def wither_storm(rng, v):
    """凋零降世・凝聚：黑色風暴的低吼（搖擺的噪聲）+ 三個頭骨的不協和低鳴 + 骨頭碎響"""
    dur = 3.4
    t = t_axis(dur)
    storm = lowpass(noise(dur, rng), 500) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.7 * t)) * env(dur, 1.0, dur, 0.8)
    skulls = mix(*[tone(lambda tt, f=f: f * (1 + 0.03 * np.sin(2 * np.pi * 4.5 * tt)), dur) * env(dur, 1.2, dur, 0.9) * 0.18 for f in (61.7, 65.4, 92.5)])
    bones = crackle(dur, rng, 30, 600, 2000, (0.05, 0.2))
    return dark_verb(mix(storm, skulls, bones, riser(dur, rng, 60, 700) * 0.4), 1.4, 0.45, rng)


def wither_roar(rng, v):
    """凋零降世・爆發：超低頻重擊 + 撕裂的低吼（FM 咆哮）+ 長而暗的殘響"""
    dur = 3.4
    hit = heavy_hit(rng, v, 0.6, 2.4) * 1.3
    t = t_axis(2.6)
    roar = lowpass(np.sin(2 * np.pi * 70 * t + 6 * np.sin(2 * np.pi * 37 * t) * np.exp(-t * 0.8)), 900) * env(2.6, 0.05, 2.6, 1.3) * 0.8
    return dark_verb(mix(hit, at(roar, 0.08, dur)), 1.6, 0.5, rng)


def snow_spirit(rng, v):
    """霜雪之靈：冷風捲起 + 冰晶玻璃鈴 + 南瓜燈「噗」地點亮"""
    dur = 2.2
    wind = sweep_filter(noise(dur, rng), 300, 1200, q=1.6) * env(dur, 0.4, dur, 1.5) * 0.45
    ice = lowpass(glass(1.6, rng, 600 + v * 30, 5, 2.5), 2200) * 0.25
    pop = mix(drop(0.4, 140, 70, 14, 8) * 0.6, lowpass(noise(0.2, rng), 700) * env(0.2, 0.01, 0.2, 6) * 0.5)
    return dark_verb(mix(wind, at(ice, 0.3, dur), at(pop, 0.9, dur)), 1.0, 0.4, rng, 2600)


def copper_birth(rng, v):
    """銅偶誕生：銅的齒輪轉起 + 「叮」的溫暖銅鈴 + 小小的關節嘎吱"""
    dur = 1.8
    gears = np.zeros(int(SR * dur))
    for i in range(6):
        gears += at(lowpass(fm_metal(0.07, 700 + i * 50, 1.8, 2, 30), 1800) * 0.25, i * 0.08, dur)
    bell = metal_ring(1.4, 330 + v * 15, 1.6, 0.5)
    return dark_verb(mix(gears, at(bell, 0.5, dur)), 0.8, 0.3, rng)


def tame_bond(rng, v):
    """主從之契：溫暖的大調和聲緩緩湧起 + 一聲柔和的鐘 + 心跳般的兩下低音"""
    dur = 2.2
    chord = lowpass(choir(1.8, [196 * (1 + v * 0.04), 247, 294, 392], rng, 0.4), 1500) * 0.5
    beat = mix(drop(0.3, 90, 50, 18, 9), at(drop(0.3, 80, 45, 18, 9) * 0.8, 0.22, 0.6)) * 0.6
    bell = lowpass(glass(1.5, rng, 392, 4, 2.6), 2000) * 0.2
    return dark_verb(mix(beat, at(chord, 0.15, dur), at(bell, 0.4, dur)), 1.1, 0.4, rng)


def name_bestow(rng, v):
    """真名賜予：毛筆一筆刷過 + 莊嚴的低音鐘一下 + 玻璃般的名字光芒"""
    dur = 2.0
    swipe = sweep_filter(noise(0.35, rng), 1300, 260, q=1.6) * env(0.35, 0.1, 0.25, 3) * 0.6
    gong = metal_ring(1.8, 130 + v * 8, 1.0, 0.6)
    shine = lowpass(glass(1.2, rng, 700, 4, 3), 2200) * 0.15
    return dark_verb(mix(swipe, at(gong, 0.28, dur), at(shine, 0.32, dur)), 1.0, 0.35, rng)


def shear_snip(rng, v):
    """銀剪流舞：兩下俐落的「喀嚓」+ 羊毛蓬鬆落下的柔軟悶聲"""
    dur = 1.2
    snip = lambda: mix(clunk(rng, 1500, 1.6, 0.15) * 0.4, bandpass(noise(0.04, rng), 1500, 3800) * env(0.04, 0.001, 0.04, 10) * 0.3)
    fluff = lowpass(noise(0.5, rng), 500) * env(0.5, 0.05, 0.5, 4) * 0.5
    return dark_verb(mix(snip(), at(snip(), 0.12, dur), at(fluff, 0.2, dur)), 0.5, 0.25, rng)


def milk_pour(rng, v):
    """白乳恩賜：奶水注入桶中的柔和水聲（低通）+ 桶身的悶響 + 一聲輕鈴"""
    dur = 1.4
    t = t_axis(0.7)
    pour = lowpass(noise(0.7, rng), 900) * (0.7 + 0.3 * np.sin(2 * np.pi * 13 * t)) * env(0.7, 0.05, 0.7, 2.5) * 0.6
    pail = lowpass(metal_ring(1.0, 180, 3, 0.4), 1200)
    bell = lowpass(glass(0.8, rng, 520 + v * 30, 3, 5), 2000) * 0.12
    return dark_verb(mix(pour, pail, at(bell, 0.5, dur)), 0.6, 0.3, rng)


def equip_armor(rng, v):
    """鞍甲加身：皮革扣上的悶響 + 鎧甲的金屬碰撞 + 一聲低鳴"""
    dur = 1.4
    leather = lowpass(noise(0.15, rng), 700) * env(0.15, 0.01, 0.15, 6) * 0.6
    plate = metal_ring(1.0, 200 + v * 15, 2.5, 0.5)
    buckle = clunk(rng, 900, 1.2, 0.3) * 0.5
    return dark_verb(mix(leather, at(buckle, 0.08, dur), at(plate, 0.12, dur)), 0.6, 0.3, rng)


def mob_dye(rng, v):
    """七彩染身：顏料潑上的濕潤「啪」+ 七個由低到高的柔和鈴音"""
    dur = 1.6
    splash = lowpass(noise(0.2, rng), 1000) * env(0.2, 0.005, 0.2, 7) * 0.6
    out = np.zeros(int(SR * dur))
    for i, f in enumerate([262, 294, 330, 392, 440, 523, 587]):
        out += at(lowpass(glass(0.6, rng, f, 3, 6), 2000) * 0.08, 0.1 + i * 0.06, dur)
    return dark_verb(mix(splash, out), 0.8, 0.35, rng)


def scute_brush(rng, v):
    """鱗甲拂落：刷子刷過甲殼（帶通噪聲短掃）+ 鱗甲「喀」地掉下"""
    dur = 1.0
    strokes = np.zeros(int(SR * dur))
    for i in range(3):
        strokes += at(sweep_filter(noise(0.12, rng), 2400, 700, q=2) * env(0.12, 0.02, 0.1, 3) * 0.35, i * 0.11, dur)
    drop_ = clunk(rng, 1100, 1.3, 0.3) * 0.5
    return dark_verb(mix(strokes, at(drop_, 0.36, dur)), 0.5, 0.25, rng)


def allay_chime(rng, v):
    """悅靈之約：輕快的上行琶音（壓暗的玻璃）+ 精靈的柔和嗡鳴"""
    dur = 1.6
    out = np.zeros(int(SR * dur))
    for i, f in enumerate([523, 659, 784, 1047]):
        out += at(lowpass(glass(0.7, rng, f * (1 + v * 0.02), 3, 5), 2400) * 0.12, i * 0.07, dur)
    hum = lowpass(choir(1.2, [262, 392], rng, 0.2), 1400) * 0.25
    return dark_verb(mix(out, hum), 0.9, 0.4, rng, 3000)


def bucket_catch(rng, v):
    """水牢捕獲：水被一勺舀起的「嘩」+ 泡泡 + 桶的悶響"""
    dur = 1.2
    scoop = sweep_filter(noise(0.35, rng), 300, 1100, q=1.4) * env(0.35, 0.03, 0.3, 3) * 0.6
    bub = np.zeros(int(SR * dur))
    for _ in range(10):
        f = rng.uniform(140, 320)
        bub += at(tone(lambda tt, f=f: f * (1 + tt * 8), 0.08) * env(0.08, 0.004, 0.08, 13) * 0.15, rng.uniform(0.1, 0.8), dur)
    pail = lowpass(metal_ring(0.8, 170, 3.5, 0.35), 1200)
    return dark_verb(mix(scoop, bub, at(pail, 0.25, dur)), 0.6, 0.3, rng)


def barter_gold(rng, v):
    """黃金交易：金塊被接過的厚重「叮」+ 金幣嘩啦 + 豬布林的低沉哼聲"""
    dur = 1.6
    clink = metal_ring(1.2, 440 + v * 20, 2.2, 0.5)
    coins = np.zeros(int(SR * dur))
    for i in range(10):
        coins += at(lowpass(glass(0.25, rng, rng.uniform(1200, 1800), 2, 14), 2600) * 0.06, 0.15 + i * 0.05 + rng.uniform(0, 0.03), dur)
    grunt = lowpass(np.sign(np.sin(2 * np.pi * np.cumsum(np.full(int(SR * 0.4), 95.0)) / SR)), 500) * env(0.4, 0.03, 0.35, 4) * 0.25
    return dark_verb(mix(clink, coins, at(grunt, 0.7, dur)), 0.7, 0.3, rng)


def creeper_hiss(rng, v):
    """引爆倒數：苦力怕的嘶聲（壓暗的噪聲，越來越大）+ 不祥的上升低音"""
    dur = 1.8
    t = t_axis(dur)
    hiss = lowpass(bandpass(noise(dur, rng), 700, 3000), 2400) * (t / dur) ** 1.2 * 0.5
    rise = tone(lambda tt: 50 + 70 * (tt / dur) ** 2, dur) * env(dur, 0.3, dur, 0.4) * 0.5
    return soft(mix(hiss, rise), 1.4)


def crystal_place(rng, v):
    """終界水晶：結晶「喀」地落座 + 虛空的低鳴 + 冷冽的玻璃泛音"""
    dur = 2.0
    seat = clunk(rng, 1200, 1.2, 0.3) * 0.6
    void = tone(lambda t: 73 + 5 * np.sin(2 * np.pi * 1.5 * t), dur) * env(dur, 0.2, dur, 1.4) * 0.45
    shine = lowpass(glass(1.6, rng, 620 + v * 30, 6, 2.5), 2200) * 0.22
    return dark_verb(mix(seat, void, at(shine, 0.05, dur)), 1.1, 0.4, rng)


def cure_start(rng, v):
    """淨化儀式：金蘋果的光「嗡」地罩住 + 低沉的祈禱合唱 + 虛弱藥水的嘶聲"""
    dur = 2.4
    chant = lowpass(choir(2.0, [147, 220, 294], rng, 0.6), 1200) * 0.55
    hiss = lowpass(bandpass(noise(0.8, rng), 800, 2600), 2000) * env(0.8, 0.1, 0.7, 3) * 0.2
    glow_ = lowpass(glass(1.8, rng, 440, 4, 2.2), 2000) * 0.2
    return dark_verb(mix(hiss, at(chant, 0.2, dur), at(glow_, 0.3, dur)), 1.2, 0.4, rng)


def cure_finish(rng, v):
    """人性回歸：吸入 → 柔和的重音 + 溫暖的大調和聲湧起 + 晨鐘"""
    dur = 2.6
    pre = swell(0.4, rng, 300, 1000, 2.5) * 0.4
    hit = heavy_hit(rng, v, 1.2, 1.2) * 0.6
    chord = lowpass(choir(2.0, [196, 247, 294, 392], rng, 0.3), 1600) * 0.5
    bell = metal_ring(1.8, 262, 1.4, 0.35)
    return dark_verb(mix(pre, at(mix(hit, chord, bell), 0.4, dur)), 1.3, 0.45, rng)


def trade_seal(rng, v):
    """商談成立：印章「咚」地蓋下 + 錢袋的叮噹 + 輕快的兩音"""
    dur = 1.4
    stamp = heavy_hit(rng, v, 1.3, 0.8) * 0.55
    coins = np.zeros(int(SR * dur))
    for i in range(6):
        coins += at(lowpass(glass(0.2, rng, rng.uniform(1100, 1600), 2, 14), 2600) * 0.06, 0.15 + i * 0.04, dur)
    two = mix(at(lowpass(glass(0.5, rng, 392, 3, 6), 2000) * 0.1, 0.25, dur), at(lowpass(glass(0.6, rng, 523, 3, 5), 2000) * 0.12, 0.38, dur))
    return dark_verb(mix(stamp, coins, two), 0.7, 0.3, rng)


# C 級的生物 / 雜項互動

def stand_equip(rng, v):
    """盔甲架換裝：木架輕晃 + 盔甲碰撞的悶響"""
    return dark_verb(mix(clunk(rng, 800, 1.1, 0.3) * 0.5, lowpass(metal_ring(0.6, 230 + v * 15, 4, 0.3), 1500)), 0.3, 0.2, rng)


def frame_place(rng, v):
    """展示框放入：物品「喀」地卡進框裡 + 一點木框共鳴"""
    return dark_verb(mix(clunk(rng, 1100, 1.4, 0.25) * 0.5, drop(0.2, 300, 200, 14, 12) * 0.25), 0.3, 0.2, rng)


def frame_rotate(rng, v):
    """展示框旋轉：短促的木頭「咔」"""
    return dark_verb(lowpass(fm_metal(0.06, 900 + v * 60, 1.6, 1.2, 40), 2000) * 0.4, 0.2, 0.15, rng)


def love_heart(rng, v):
    """餵食發情：兩下柔軟的心跳 + 一個溫暖的上滑音"""
    dur = 0.9
    beats = mix(drop(0.25, 110, 60, 18, 10) * 0.45, at(drop(0.25, 100, 55, 18, 10) * 0.35, 0.18, 0.5))
    rise = tone(lambda t: 330 * (1 + 0.25 * (1 - np.exp(-t * 8))), 0.5) * env(0.5, 0.02, 0.5, 5) * 0.12
    return dark_verb(mix(beats, at(lowpass(rise, 1600), 0.3, dur)), 0.4, 0.25, rng)


def leash_tie(rng, v):
    """拴繩：繩子拉緊的摩擦 + 打結的悶「啪」"""
    dur = 0.7
    pull = bandpass(noise(0.25, rng), 300, 1500) * env(0.25, 0.05, 0.2, 3) * 0.4
    knot = lowpass(noise(0.05, rng), 1000) * env(0.05, 0.001, 0.05, 12) * 0.6
    return dark_verb(mix(pull, at(knot, 0.2, dur)), 0.3, 0.2, rng)


def mount_ride(rng, v):
    """騎乘：跨上去的布料摩擦 + 一聲低低的「咚」"""
    dur = 0.7
    swish = sweep_filter(noise(0.25, rng), 400, 1400, q=1.4) * env(0.25, 0.05, 0.2, 3) * 0.4
    return dark_verb(mix(swish, at(drop(0.3, 120, 60, 16, 9) * 0.5, 0.18, dur)), 0.3, 0.2, rng)


def bottle_fill(rng, v):
    """裝水瓶：咕嘟咕嘟（上滑的短正弦）+ 玻璃瓶輕響"""
    dur = 0.8
    out = np.zeros(int(SR * dur))
    for i in range(4):
        f = 160 + i * 40
        out += at(tone(lambda tt, f=f: f * (1 + tt * 6), 0.09) * env(0.09, 0.004, 0.09, 12) * 0.3, i * 0.08, dur)
    clink = lowpass(glass(0.4, rng, 900, 3, 8), 2200) * 0.12
    return dark_verb(mix(out, at(clink, 0.35, dur)), 0.4, 0.25, rng)


def bucket_fill(rng, v):
    """舀起：液體被一口氣舀進桶的「嘩」+ 桶身悶響"""
    dur = 0.9
    scoop = sweep_filter(noise(0.4, rng), 250, 900, q=1.3) * env(0.4, 0.03, 0.35, 3) * 0.6
    pail = lowpass(metal_ring(0.7, 160 + v * 10, 3.5, 0.3), 1200)
    return dark_verb(mix(scoop, at(pail, 0.2, dur)), 0.4, 0.25, rng)


def bucket_pour(rng, v):
    """倒出：液體傾瀉落地的水聲（往下掃）"""
    dur = 1.0
    pour = sweep_filter(noise(0.7, rng), 1000, 250, q=1.3) * env(0.7, 0.03, 0.6, 2.5) * 0.6
    return dark_verb(mix(pour, at(drop(0.3, 100, 50, 14, 8) * 0.4, 0.15, dur)), 0.4, 0.25, rng)


def mud_make(rng, v):
    """泥巴：水滲進泥土的濕潤「噗嗤」"""
    dur = 0.8
    squish = lowpass(noise(0.3, rng), 450) * env(0.3, 0.01, 0.3, 5) * 0.8
    bub = np.zeros(int(SR * dur))
    for _ in range(4):
        f = rng.uniform(80, 160)
        bub += at(tone(lambda tt, f=f: f * (1 + tt * 5), 0.1) * env(0.1, 0.004, 0.1, 10) * 0.25, rng.uniform(0.1, 0.5), dur)
    return dark_verb(mix(squish, bub), 0.3, 0.2, rng)


def wire_toggle(rng, v):
    """紅石線切換：細小的電流「滋」"""
    dur = 0.4
    zap = lowpass(bandpass(noise(0.12, rng), 600, 2400), 2000) * env(0.12, 0.003, 0.12, 8) * 0.4
    hum = buzz(0.3, 110, rng) * env(0.3, 0.01, 0.3, 6) * 0.15
    return dark_verb(mix(zap, hum), 0.2, 0.15, rng)


def ore_glow(rng, v):
    """紅石礦發光：低沉的脈動「嗡」+ 結晶劈啪"""
    dur = 0.8
    hum = buzz(0.6, 82 + v * 4, rng, 6, 0.05) * env(0.6, 0.02, 0.6, 4) * 0.3
    return dark_verb(mix(hum, crackle(0.5, rng, 8, 900, 2600, (0.04, 0.12))), 0.4, 0.25, rng)


D_SFX = {
    "golem_awaken": golem_awaken, "golem_stomp": golem_stomp, "wither_storm": wither_storm, "wither_roar": wither_roar,
    "snow_spirit": snow_spirit, "copper_birth": copper_birth, "tame_bond": tame_bond, "name_bestow": name_bestow, "shear_snip": shear_snip,
    "milk_pour": milk_pour, "equip_armor": equip_armor, "mob_dye": mob_dye, "scute_brush": scute_brush, "allay_chime": allay_chime,
    "bucket_catch": bucket_catch, "barter_gold": barter_gold, "creeper_hiss": creeper_hiss, "crystal_place": crystal_place,
    "cure_start": cure_start, "cure_finish": cure_finish, "trade_seal": trade_seal,
    "stand_equip": stand_equip, "frame_place": frame_place, "frame_rotate": frame_rotate, "love_heart": love_heart, "leash_tie": leash_tie,
    "mount_ride": mount_ride, "bottle_fill": bottle_fill, "bucket_fill": bucket_fill, "bucket_pour": bucket_pour, "mud_make": mud_make,
    "wire_toggle": wire_toggle, "ore_glow": ore_glow,
}



# ================================================================= 新增：鐘十二響、珍珠、終界之眼、爆炸、終界門十二法陣、書架

def fo(x, frac=0.2):
    """尾端淡出（最後 frac 比例、至少 0.15 秒）：避免某一層在固定長度被截斷時發出「啪」的爆音"""
    n = min(len(x), max(int(SR * 0.15), int(len(x) * frac)))
    y = x.copy()
    y[-n:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n))
    return y


def xmix(*parts):
    return mix(*[fo(p) for p in parts])

def temple_bell(dur, base, rng, decay=0.8, bright=1800):
    """寺鐘本體：非諧波泛音 + 兩個略失諧的拍頻（嗡——嗡——的晃動）"""
    partials = [(0.5, 0.55), (1.0, 0.6), (1.19, 0.32), (1.5, 0.26), (2.0, 0.2), (2.52, 0.12), (3.0, 0.06)]
    t = t_axis(dur)
    ring = np.zeros(len(t))
    for i, (r, a) in enumerate(partials):
        for det in (-0.0025, 0.0025):
            ring += tone(base * r * (1 + det), dur, rng.uniform(0, 6.28)) * env(dur, 0.002, dur, decay + i * 0.45) * a * 0.5
    beat = 1 + 0.18 * np.sin(2 * np.pi * 1.7 * t)
    return lowpass(ring * beat, bright)


def bell_deep(rng, v):
    """六～十一響：更低更厚的巨鐘 + 一記重擊 + 地鳴般的低頻 + 壓上來的暗合唱"""
    dur = 4.2
    strike = heavy_hit(rng, v, 1.1, 1.2) * 0.7
    bell = temple_bell(dur, 130 * (1 + v * 0.03), rng, 0.65, 1600) * 0.75
    under = drop(dur, 70, 38, 4, 1.2) * 0.5
    ch = lowpass(choir(3.2, [130.8, 155.6, 196], rng, 0.6, 2.0), 1400) * 0.35
    return dark_verb(xmix(strike, bell, under, at(ch, 0.25, dur)), 1.8, 0.5, rng, 2000)


def bell_final(rng, v):
    """十二響歸墟：倒吸的反向鐘聲 → 三口巨鐘同時轟鳴（和弦）+ 超低頻重擊 + 光雨般的玻璃 + 天上的合唱，長長的尾巴"""
    dur = 7.0
    pre = swell(0.7, rng, 120, 1400, 2.4) * 0.8
    hit = heavy_hit(rng, v, 1.3, 2.5)
    bells = xmix(temple_bell(6, 98, rng, 0.45, 1500), temple_bell(6, 123.5, rng, 0.5, 1700) * 0.7, temple_bell(6, 146.8, rng, 0.55, 1900) * 0.55) * 0.7
    boom = drop(5.0, 80, 24, 2.5, 0.9) * 1.1
    rain = np.zeros(int(SR * 5))
    for _ in range(30):
        rain += at(lowpass(glass(1.4, rng, rng.uniform(700, 1800), 3, 2.5), 3000) * rng.uniform(0.03, 0.09), rng.uniform(0.3, 3.5), 5)
    ch = lowpass(choir(5.5, [98, 123.5, 146.8, 196, 246.9], rng, 0.5, 1.6), 1800) * env(5.5, 0.4, 5.5, 0.7) * 0.55
    body = soft(xmix(hit, bells, boom, rain, at(ch, 0.1, 6)), 1.4)
    return dark_verb(xmix(pre, at(body, 0.7, dur)), 2.6, 0.55, rng, 2200)


def pearl_throw(rng, v):
    """珍珠出手：短促的風切 + 往上滑的紫色顫音（FM）"""
    dur = 0.8
    air = sweep_filter(noise(dur, rng), 400 + v * 60, 2600, q=2.2) * env(dur, 0.05, 0.4, 5) * 0.6
    t = t_axis(dur)
    shim = np.sin(2 * np.pi * np.cumsum(420 * 2 ** (t * 1.6) * (1 + 0.02 * np.sin(2 * np.pi * 14 * t))) / SR) * env(dur, 0.01, dur, 5) * 0.18
    return dark_verb(xmix(air, lowpass(shim, 3000)), 0.4, 0.3, rng, 3000)


def pearl_warp(rng, v):
    """虛空瞬步：空間被吸進裂縫（反向吸入 0.28 秒）→ 裂縫闔上的「啪」+ 低沉的相位扭曲尾巴"""
    dur = 2.0
    suck = swell(0.28, rng, 300, 2200, 2.0) * 0.9
    snap = xmix(clunk(rng, 1400, 0.8, 0.3) * 0.6, lowpass(glass(0.5, rng, 900 + v * 60, 5, 7), 3200) * 0.4, sub(0.6, 95, 40, 6) * 0.9)
    t = t_axis(1.6)
    wob = lowpass(fm_metal(1.6, 73 + v * 4, 1.41, 2.2, 1.6), 900) * (1 + 0.5 * np.sin(2 * np.pi * 6 * t)) * 0.3
    return dark_verb(xmix(suck, at(snap, 0.28, dur), at(wob, 0.3, dur)), 1.0, 0.45, rng, 2400)


def eye_launch(rng, v):
    """終焉指引・升空：結晶的上行琶音 + 往上捲的氣流 + 虛空的低鳴"""
    dur = 2.2
    notes = [293.7, 370, 440, 587.3, 740]
    arp = np.zeros(int(SR * dur))
    for i, f in enumerate(notes):
        arp += at(lowpass(glass(1.4, rng, f * 2 * (1 + v * 0.02), 3, 3.5), 3200) * 0.13, i * 0.09, dur)
    lift = riser(1.0, rng, 200, 1800) * 0.25
    hum = lowpass(fm_metal(dur, 110, 1.5, 1.2, 1.4), 1000) * 0.22
    return dark_verb(xmix(arp, lift, hum), 1.2, 0.4, rng, 3000)


def eye_shatter(rng, v):
    """終界之眼碎裂：結晶爆散（一群短玻璃 + 細碎劈啪）+ 一聲悶響 + 虛空的嘆息"""
    dur = 2.0
    shards = np.zeros(int(SR * 1.2))
    for _ in range(18):
        shards += at(lowpass(glass(0.5, rng, rng.uniform(1200, 2600), 4, 9), 4200) * rng.uniform(0.05, 0.14), rng.uniform(0, 0.25), 1.2)
    crack = crackle(0.5, rng, 30, 1800, 5000, (0.05, 0.2), 0.5)
    thud = sub(0.5, 120, 50, 7) * 0.6
    sigh = lowpass(noise(1.4, rng), 700) * env(1.4, 0.2, 1.4, 2.5) * 0.25
    return dark_verb(xmix(shards, crack, thud, at(sigh, 0.1, dur)), 1.1, 0.4, rng, 3200)


def blast_boom(rng, v):
    """爆裂破滅：疊在原版爆炸上的「重量」——超低頻下墜 + 胸腔般的悶轟 + 碎屑雨 + 遠處滾動的雷鳴尾巴"""
    dur = 4.0
    hit = heavy_hit(rng, v, 1.4, 1.6)
    body = lowpass(noise(1.4, rng), 600) * env(1.4, 0.003, 1.4, 2.6) * 1.3
    sub_ = drop(dur, 75 - v * 5, 22, 3.5, 1.1) * 1.3
    debris = crackle(2.0, rng, 90, 700, 3000, (0.06, 0.25), 0.7)
    t = t_axis(3.0)
    roll = lowpass(noise(3.0, rng), 220) * (0.6 + 0.4 * np.sin(2 * np.pi * 2.2 * t + 1)) * env(3.0, 0.3, 3.0, 1.6) * 0.8
    return dark_verb(soft(xmix(hit, body, sub_, at(debris, 0.05, dur), at(roll, 0.25, dur)), 2.0), 1.6, 0.4, rng, 1800)


def beam_rise(rng, v):
    """嵌眼的光柱衝上天：上滑的明亮掃頻 + 結晶泛音尾巴"""
    dur = 1.6
    t = t_axis(dur)
    up = np.sin(2 * np.pi * np.cumsum(180 * 2 ** (np.minimum(t, 0.4) * 5)) / SR) * env(dur, 0.01, 0.9, 4) * 0.2
    air = sweep_filter(noise(0.6, rng), 300, 4000, q=2.5) * env(0.6, 0.05, 0.6, 4) * 0.35
    tail = lowpass(glass(1.4, rng, 1400 + v * 80, 4, 2.8), 3500) * 0.18
    return dark_verb(xmix(lowpass(up, 3000), air, at(tail, 0.25, dur)), 1.0, 0.4, rng, 3200)


def portal_spiral(rng, v):
    """十二道光柱螺旋收攏：越轉越快的旋轉風切（左右往返的帶通）+ 往上推的虛空和聲 + 低頻逐漸壓上來"""
    dur = 1.8
    t = t_axis(dur)
    rate = 2 + 14 * (t / dur) ** 2
    ph = np.cumsum(rate) / SR
    n = noise(dur, rng)
    whirl = (bandpass(n, 400, 1400) * (0.5 + 0.5 * np.sin(2 * np.pi * ph)) + bandpass(n, 900, 2600) * (0.5 + 0.5 * np.cos(2 * np.pi * ph))) * 0.5
    whirl *= (t / dur) ** 0.8
    ch = lowpass(choir(dur, [110, 164.8, 220, 277.2], rng, 0.8, 3.0), 1500) * (t / dur) ** 1.5 * 0.5
    low = tone(lambda tt: 45 + 30 * (tt / dur), dur) * (t / dur) ** 2 * 0.5
    return dark_verb(xmix(whirl, ch, low), 1.0, 0.35, rng, 2400)


def circle_burst(rng, v):
    """十二法陣炸開：無數結晶碎片同時爆散 + 十二聲細鈴往外飛 + 巨大的玻璃轟鳴"""
    dur = 3.0
    shards = np.zeros(int(SR * dur))
    for _ in range(40):
        shards += at(lowpass(glass(0.9, rng, rng.uniform(800, 2400), 4, 5), 3800) * rng.uniform(0.04, 0.12), rng.uniform(0, 0.2), dur)
    bells = np.zeros(int(SR * dur))
    for i in range(12):
        bells += at(lowpass(glass(1.5, rng, 1046.5 * 2 ** (i / 12 * 0.5), 3, 3), 3500) * 0.06, 0.04 + i * 0.035, dur)
    roar = lowpass(noise(1.6, rng), 1500) * env(1.6, 0.003, 1.6, 3) * 0.5
    return dark_verb(xmix(shards, bells, roar), 1.8, 0.45, rng, 3200)


def book_glow(rng, v):
    """書放進格子、整格亮起：柔軟的魔法「叮」+ 紙頁的輕嘆"""
    dur = 1.2
    ting = lowpass(glass(1.0, rng, 1318.5 * (1 + v * 0.03), 4, 4.5), 3000) * 0.3
    hum = tone(659.3, 0.8) * env(0.8, 0.08, 0.8, 4) * 0.08
    page = bandpass(noise(0.25, rng), 1200, 4000) * env(0.25, 0.04, 0.25, 6) * 0.12
    return dark_verb(xmix(page, at(fo(ting, 0.4), 0.04, dur), at(fo(hum, 0.4), 0.04, dur)), 0.6, 0.35, rng, 3500)


X_SFX = {
    "bell_deep": bell_deep, "bell_final": bell_final, "pearl_throw": pearl_throw, "pearl_warp": pearl_warp, "eye_launch": eye_launch,
    "eye_shatter": eye_shatter, "blast_boom": blast_boom, "beam_rise": beam_rise, "portal_spiral": portal_spiral, "circle_burst": circle_burst,
    "book_glow": book_glow,
}


SFX = {
    "name_hit": name_hit,
    "brush_hit": brush_hit,
    "holy_descend": holy_descend,
    "enchant_charge": enchant_charge,
    "enchant_beam": enchant_beam,
    "sparkle": sparkle,
    "title_slam": title_slam,
    "shockwave": shockwave,
    "whoosh": whoosh,
    "forge_ignite": forge_ignite,
    "forge_clang": forge_clang,
    "forge_final": forge_final,
    "forge_quench": forge_quench,
    "blade_glint": blade_glint,
    "craft_blueprint": craft_blueprint,
    "craft_assemble": craft_assemble,
    "craft_forge": craft_forge,
    "combo_ping": combo_ping,
    "map_unfurl": map_unfurl,
    "holo_boot": holo_boot,
    "compass_spin": compass_spin,
    "eye_open": eye_open,
    "world_scan": world_scan,
    "fire_ignite": fire_ignite,
    "fire_roar": fire_roar,
    "inferno_burst": inferno_burst,
    "ember_settle": ember_settle,
    "fire_puff": fire_puff,
    "plasma_charge": plasma_charge,
    "plasma_cut": plasma_cut,
    "steam_cool": steam_cool,
    "smoke_swirl": smoke_swirl,
    "smoke_gong": smoke_gong,
    "incense_settle": incense_settle,
    "anchor_charge": anchor_charge,
    "anchor_pact": anchor_pact,
    "anchor_blast": anchor_blast,
    "beacon_rise": beacon_rise,
    "beacon_descent": beacon_descent,
    "smith_hammer": smith_hammer,
    "smith_fuse": smith_fuse,
    "grind_spin": grind_spin,
    "grind_strip": grind_strip,
    "brew_bubble": brew_bubble,
    "brew_transmute": brew_transmute,
    "portal_ignite": portal_ignite,
    "portal_open": portal_open,
    "endeye_insert": endeye_insert,
    "end_open": end_open,
    "vault_unlock": vault_unlock,
    "vault_burst": vault_burst,
    "dream_fall": dream_fall,
    "dream_dawn": dream_dawn,
    "egg_warp": egg_warp,
    "egg_appear": egg_appear,
    "brush_sweep": brush_sweep,
    "relic_reveal": relic_reveal,
    "magnet_hum": magnet_hum,
    "magnet_lock": magnet_lock,
    "seal_open": seal_open,
    "seal_close": seal_close,
    **B_SFX,
    **C_SFX,
    **D_SFX,
    **X_SFX,
}


def main(only=None):
    os.makedirs(OUT, exist_ok=True)
    if not only:
        for f in os.listdir(OUT):
            if not f.startswith("anime_"):
                os.remove(os.path.join(OUT, f))
    data = {}
    js = os.path.join(ROOT, "sounds.json")
    if os.path.exists(js):
        # 保留其他工具產生的項目（例如 fetch_anime_sfx.py 的 anime_*）
        with open(js, encoding="utf-8") as f:
            data = {k: v for k, v in json.load(f).items() if k not in SFX}
    for name, fn in SFX.items():
        data[name] = {"sounds": [{"name": f"chuuni:{name}_{v + 1}"} for v in range(3)], "subtitle": f"subtitles.chuuni.{name}"}
        if only and name not in only:
            continue
        for v in range(3):
            rng = np.random.default_rng(zlib.crc32(name.encode()) % 10000 + v * 31)
            sf.write(os.path.join(OUT, f"{name}_{v + 1}.ogg"), finish(fn(rng, v)), SR, format="OGG", subtype="VORBIS")
    with open(os.path.join(ROOT, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)
    print("sounds ok:", len(SFX))


if __name__ == "__main__":
    import sys
    main(set(sys.argv[1:]))
