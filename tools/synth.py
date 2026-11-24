"""合成器元件（沿用終焉試煉的 gen_sounds.py）"""
import os

import numpy as np
from scipy import signal

SR = 48000
ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "client", "resources", "assets", "chuuni")
OUT = os.path.join(ROOT, "sounds")


# ================================================================= 元件

def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def env(dur, attack=0.004, decay=None, curve=5.0, hold=0.0):
    t = t_axis(dur)
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = decay if decay is not None else dur
    rel = np.clip((t - attack - hold) / max(d, 1e-4), 0, None)
    return a * np.exp(-curve * rel)


def noise(dur, rng):
    return rng.uniform(-1, 1, int(SR * dur))


def bandpass(x, lo, hi, order=2):
    sos = signal.butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def lowpass(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, btype="low", fs=SR, output="sos"), x)


def highpass(x, f, order=2):
    return signal.sosfilt(signal.butter(order, f, btype="high", fs=SR, output="sos"), x)


def sweep_filter(x, f0, f1, q=2.5):
    """時變帶通（逐段濾波），做出「咻」的掃頻感"""
    out = np.zeros_like(x)
    n = len(x)
    seg = 512
    for i in range(0, n, seg):
        k = i / max(1, n - 1)
        fc = f0 * (f1 / f0) ** k
        lo, hi = max(40, fc / (1 + 1 / q)), min(SR / 2 - 100, fc * (1 + 1 / q))
        out[i:i + seg] = bandpass(x[max(0, i - 2048):i + seg], lo, hi)[-len(x[i:i + seg]):]
    return out


def tone(freq, dur, phase=0.0):
    t = t_axis(dur)
    if callable(freq):
        f = freq(t)
        return np.sin(2 * np.pi * np.cumsum(f) / SR + phase)
    return np.sin(2 * np.pi * freq * t + phase)


def glass(dur, rng, base=2200, n=9, decay=6.0):
    """非諧波玻璃泛音（結晶）"""
    out = np.zeros(int(SR * dur))
    for i in range(n):
        f = base * (1 + i * 0.73 + rng.uniform(-0.08, 0.08)) * rng.uniform(0.9, 1.1)
        if f > SR / 2.2:
            continue
        out += tone(f, dur, rng.uniform(0, 6.28)) * env(dur, 0.001, dur, decay * (1 + i * 0.25)) / (1 + i * 0.4)
    return out


def fm_metal(dur, carrier, ratio, index, decay=5.0):
    t = t_axis(dur)
    mod = np.sin(2 * np.pi * carrier * ratio * t) * index * np.exp(-decay * 0.7 * t)
    return np.sin(2 * np.pi * carrier * t + mod) * env(dur, 0.001, dur, decay)


def sub(dur, f0, f1, decay=4.0):
    return tone(lambda t: f0 + (f1 - f0) * (t / dur), dur) * env(dur, 0.003, dur, decay)


def reverb(x, size=0.35, mix=0.3, rng=None):
    rng = rng or np.random.default_rng(1)
    n = int(SR * size * 2.5)
    ir = rng.normal(0, 1, n) * np.exp(-np.arange(n) / (SR * size / 3))
    ir = lowpass(ir, 6000)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    wet = signal.fftconvolve(x, ir)[: len(x) + n // 2]
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry * (1 - mix) + wet * mix


def pad(x, dur):
    n = int(SR * dur)
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:n] if len(x) < n else x


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


def soft(x, drive=1.5):
    return np.tanh(x * drive) / np.tanh(drive)


def weight(x):
    """整體壓暗加厚：削掉刺耳高頻、補一點低頻（使用者回饋：太亮、太輕）"""
    x = lowpass(x, 8500, 2)
    return x + lowpass(x, 180, 2) * 0.55


def finish(x, peak=0.9):
    x = weight(x)
    x = highpass(x, 28, 1)
    m = np.max(np.abs(x)) + 1e-9
    x = x / m * peak
    fade = int(SR * 0.01)
    x[-fade:] *= np.linspace(1, 0, fade)
    return x.astype(np.float32)


# ================================================================= 音效
