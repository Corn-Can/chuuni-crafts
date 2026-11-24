"""
動漫風音效素材：出處「効果音ラボ」(https://soundeffect-lab.info/)，轉成 Minecraft 能播的 OGG。
規約（https://soundeffect-lab.info/agreement/）：商用免費、不需標註；可作為操作音組進 App（音檔直接露出也可），
禁止把素材本身再配布 / 販售。這裡只做格式轉換（mp3 → 單聲道 ogg，Minecraft 只吃 ogg，單聲道才有方位感），不剪輯、不加工內容。
音量與音高在 Java 端播放時調整。

用法：
  1. 在瀏覽器打開下面 SOURCES 列的頁面，按各音效的下載鈕，把 mp3 原檔名放進 tools/anime_src/
  2. python tools/fetch_anime_sfx.py（缺哪些檔會列出來；有的就轉好並寫進 sounds.json）
  Java 端有素材就播動漫音效，沒有就自動退回原本的合成音效。
"""
import json
import os
import sys

import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.join(HERE, "..", "src", "client", "resources", "assets", "chuuni")
OUT = os.path.join(ROOT, "sounds")
CACHE = os.path.join(HERE, "anime_src")
BASE = "https://soundeffect-lab.info/sound/"
MAX_LEN = 3.0

# key（音效名 chuuni:anime_<key>）: (頁面, 檔名, 原標題, 用途)
SOURCES = {
    "inhale": ("battle", "super-arts-motion1.mp3", "超必殺技発動", "A 級吸氣（集中線收緊）"),
    "hit": ("battle", "super-arts-hit1.mp3", "必殺技ヒット", "A 級衝擊"),
    "slowmo": ("battle", "slow-motion1.mp3", "スローモーション", "A 級英雄慢動作"),
    "title": ("anime", "text-impact1.mp3", "文字表示の衝撃音1", "毛筆招式名落款"),
    "char": ("anime", "text-impact3.mp3", "文字表示の衝撃音3", "招式名每個字砸下"),
    "shakin": ("anime", "shakin1.mp3", "シャキーン1", "漫畫四角大字（時停）"),
    "kira": ("anime", "kira2.mp3", "キラッ2", "B 級小招式名、書架亮起"),
    "eye_shine": ("anime", "eye-shine1.mp3", "きらーん1", "終界之眼嵌入"),
    "circle": ("battle", "magic-circle1.mp3", "魔法陣を展開", "終界門十二法陣展開"),
    "aura": ("battle", "aura2.mp3", "オーラ2", "終界門光柱合一"),
    "dash": ("battle", "highspeed-movement1.mp3", "高速移動", "珍珠出手"),
    "warp": ("battle", "magic-worp1.mp3", "ワープ", "珍珠落地瞬移"),
    "rise": ("battle", "magic-statusup2.mp3", "ステータス上昇魔法2", "終界之眼升空"),
    "temple_bell": ("anime", "temple-bell1.mp3", "お寺の鐘", "鐘 一～十一響"),
    "holy": ("battle", "magic-attack-holy1.mp3", "聖魔法", "鐘 第十二響"),
    "bomb": ("battle", "bomb3.mp3", "爆発3", "大爆炸"),
    "bomb_small": ("battle", "bomb1.mp3", "爆発1", "小爆炸"),
}


def local(page, name, title):
    """素材請自己在瀏覽器裡從網站頁面下載（網站禁止外部直接連結音檔），放進 tools/anime_src/"""
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        # 從網站按下載鈕時，存下來的檔名是日文標題（例如「必殺技ヒット.mp3」）
        path = os.path.join(CACHE, title + ".mp3")
    if not os.path.exists(path):
        print(f"  [缺] {name}  ←「{title}」 {BASE}{page}/" + (" （第 2 頁 battle2.html）" if name.startswith("bomb") else ""))
        return None
    return path


def convert(src, key):
    audio, sr = sf.read(src, always_2d=True)
    mono = audio.mean(axis=1).astype(np.float32)
    # 一次寫入太長的音檔會讓 libsndfile 的 Vorbis 編碼器堆疊溢位 → 分段寫
    with sf.SoundFile(os.path.join(OUT, f"anime_{key}.ogg"), "w", sr, 1, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(mono), 32768):
            f.write(mono[i:i + 32768])
    return len(mono) / sr


def file_key(path):
    """素材路徑 → 音效 key（anime/mp3/shine4.mp3 → shine4）"""
    return os.path.basename(path)[:-4].replace("-", "_")


def build_map(data, credits):
    """anime_map.py 的對照表：轉檔 + 寫出 assets/chuuni/anime_map.json（Java 端播合成音效時查表）"""
    from anime_map import MAP
    with open(os.path.join(HERE, "anime_catalog.json"), encoding="utf-8") as f:
        catalog = json.load(f)
    done = {}
    lengths = {}
    out = {}
    missing = 0
    for synth, (path, mode, vol) in MAP.items():
        key = file_key(path)
        if key not in done:
            info = catalog[path]
            name = os.path.basename(path)
            src = os.path.join(CACHE, name)
            if not os.path.exists(src):
                src = os.path.join(CACHE, info["title"] + ".mp3")
            if not os.path.exists(src):
                print(f"  [缺] {name}  ←「{info['title']}」 {info['page']}")
                done[key] = False
                missing += 1
                continue
            lengths[key] = convert(src, key)
            data[f"anime_{key}"] = {"sounds": [{"name": f"chuuni:anime_{key}"}], "subtitle": f"subtitles.chuuni.{synth}"}
            credits.append(f"- anime_{key}.ogg ← 「{info['title']}」 {info['page']}")
            done[key] = True
        if done[key]:
            # 長素材（環境音、BGM 類）只播開頭，Java 端在 max 秒內淡出停止（0 = 整段播完）
            out[synth] = {"sound": f"anime_{key}", "layer": mode == "l", "volume": vol, "max": MAX_LEN if lengths[key] > MAX_LEN + 1 else 0}
    with open(os.path.join(ROOT, "anime_map.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print(f"對照表：{len(out)} 個合成音效改用動漫素材，缺 {missing} 個素材檔")


def main():
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(ROOT, "sounds.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    credits = ["# 動漫風音效素材出處（不需標註，僅作紀錄）", "素材提供：効果音ラボ https://soundeffect-lab.info/", ""]
    for key, (page, name, title, use) in SOURCES.items():
        src = local(page, name, title)
        if src is None:
            data.pop(f"anime_{key}", None)
            continue
        length = convert(src, key)
        data[f"anime_{key}"] = {"sounds": [{"name": f"chuuni:anime_{key}"}], "subtitle": f"subtitles.chuuni.anime_{key}"}
        credits.append(f"- anime_{key}.ogg ← 「{title}」 {BASE}{page}/ （{use}）")
        print(f"  {key:12s} {length:5.2f}s  {title}")
    build_map(data, credits)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)
    with open(os.path.join(HERE, "ANIME_SFX_CREDITS.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(credits) + "\n")


if __name__ == "__main__":
    main()
