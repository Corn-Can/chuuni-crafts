"""
招式名排版素材（v3，取代名卡與狀聲字）：
  A. 直式毛筆招式名（參考：動畫裡畫面右側直書的大毛筆字 + 旁邊的假名讀音 + 下方字幕）
     - <key>_brush.png  毛筆字直排（黑墨 + 飛白 + 毛邊 + 白色光暈）
     - <key>_ruby.png   直排片假名讀音（黑字白框）
     - <key>_sub.png    字幕（白字黑框）
  B. 漫畫四角大字（參考：跨頁漫畫四個角各一個極粗大字 + 每字旁的平假名讀音，中間是集中線）
     - <key>_c0..3.png  極粗明朝大字（黑字 + 白粗框 + 黑細外框）
     - <key>_r0..3.png  直排平假名讀音
字體（SIL OFL，見 tools/fonts）：Yuji Boku（毛筆）、思源宋體 TW Heavy / Bold
用法：python tools/gen_names.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(__file__)
ROOT = os.path.join(HERE, "..", "src", "client", "resources", "assets", "chuuni")
OUT = os.path.join(ROOT, "textures", "gui", "name")
BRUSH = os.path.join(HERE, "fonts", "YujiBoku-Regular.ttf")
HEAVY = os.path.join(HERE, "fonts", "SourceHanSerifTW-Heavy.otf")
BOLD = os.path.join(HERE, "fonts", "SourceHanSerifTW-Bold.otf")

# key: (招式名, 整串片假名讀音, 每字平假名讀音)
NAMES = {
    "enchant1": ("星屑祝福", "スターダスト・ブレス", ["ほし", "くず", "しゅく", "ふく"]),
    "enchant2": ("星辰刻印", "ステラー・エングレイヴ", ["ほし", "たつ", "きざむ", "しるし"]),
    "enchant3": ("天穹禁咒", "フォービドゥン・ファーマメント", ["てん", "きゅう", "きん", "じゅ"]),
    "anvil": ("千錘百鍊", "ソウル・フォージ", ["せん", "つい", "ひゃく", "れん"]),
    "craft": ("萬象解放", "オール・クリエイション", ["ばん", "しょう", "かい", "ほう"]),
    "craft_hand": ("掌中乾坤", "パーム・オブ・クリエイション", ["たなごころ", "なか", "けん", "こん"]),
    "cartography": ("全知之眼", "オムニシエント・アイ", ["すべて", "しる", "の", "まなこ"]),
    "furnace": ("獄炎煉成", "ヘルファイア・アルケミー", ["ごく", "えん", "れん", "せい"]),
    "blast": ("蒼焰熔斷", "アズール・メルトダウン", ["あお", "ほむら", "とかし", "たつ"]),
    "smoker": ("煙雲祕法", "シークレット・スモーク", ["けむり", "くも", "ひ", "ほう"]),
    "door_open": ("封印解除", "シール・リリース", ["ふう", "いん", "かい", "じょ"]),
    "door_close": ("結界再封", "リ・シール", ["けっ", "かい", "さい", "ふう"]),
    "anchor": ("魂錨契約", "ソウル・アンカー", ["こん", "びょう", "けい", "やく"]),
    "beacon": ("天光降臨", "ヘヴンズ・ビーコン", ["てん", "こう", "こう", "りん"]),
    "smithing": ("神匠鍛冶", "ディヴァイン・スミス", ["しん", "しょう", "たん", "や"]),
    "grindstone": ("魔力剝奪", "ディスエンチャント", ["ま", "りょく", "はく", "だつ"]),
    "brewing": ("煉金祕藥", "アルケミー・エリクシル", ["れん", "きん", "ひ", "やく"]),
    "nether_portal": ("冥界開門", "ゲート・オブ・ネザー", ["めい", "かい", "かい", "もん"]),
    "end_portal": ("終焉之門", "ジ・エンド・ゲート", ["しゅう", "えん", "の", "もん"]),
    "vault": ("祕寶解封", "トレジャー・アンシール", ["ひ", "ほう", "かい", "ふう"]),
    "vault_ominous": ("凶寶解封", "カースド・トレジャー", ["きょう", "ほう", "かい", "ふう"]),
    "bed": ("夢境結界", "ドリーム・バリア", ["む", "きょう", "けっ", "かい"]),
    "dragon_egg": ("龍之遷躍", "ドラゴン・シフト", ["りゅう", "の", "せん", "やく"]),
    "brush": ("古代遺物", "エンシェント・レリック", ["こ", "だい", "い", "ぶつ"]),
    "lodestone": ("磁極指引", "マグネティック・ガイド", ["じ", "きょく", "し", "いん"]),
}

# 英文介面：(字幕, 每字的英文意譯) —— 漢字本身當作書法 / 漫畫美術保留，讀音與字幕換成英文
ENGLISH = {
    "enchant1": ("STARDUST BLESSING", ["STAR", "DUST", "BLESS", "ING"]),
    "enchant2": ("STELLAR ENGRAVING", ["STAR", "CELESTIAL", "ENGRAVE", "SEAL"]),
    "enchant3": ("FORBIDDEN FIRMAMENT", ["HEAVEN", "VAULT", "FORBIDDEN", "CURSE"]),
    "anvil": ("THOUSAND-FOLD FORGE", ["THOUSAND", "HAMMER", "HUNDRED", "TEMPER"]),
    "craft": ("ALL CREATION UNBOUND", ["MYRIAD", "FORMS", "UN", "BOUND"]),
    "craft_hand": ("UNIVERSE IN MY PALM", ["PALM", "WITHIN", "HEAVEN", "EARTH"]),
    "cartography": ("THE OMNISCIENT EYE", ["ALL", "KNOWING", "OF", "EYE"]),
    "furnace": ("HELLFIRE ALCHEMY", ["HELL", "FLAME", "TRANS", "MUTE"]),
    "blast": ("AZURE MELTDOWN", ["AZURE", "BLAZE", "MELT", "SEVER"]),
    "smoker": ("SECRET ART OF SMOKE", ["SMOKE", "CLOUD", "SECRET", "ART"]),
    "door_open": ("SEAL RELEASED", ["SEAL", "MARK", "UN", "BIND"]),
    "door_close": ("BARRIER RESEALED", ["BARRIER", "BOUND", "RE", "SEAL"]),
    "anchor": ("SOUL ANCHOR PACT", ["SOUL", "ANCHOR", "PACT", "OATH"]),
    "beacon": ("DESCENT OF HEAVENLY LIGHT", ["HEAVEN", "LIGHT", "DESCEND", "ARRIVE"]),
    "smithing": ("DIVINE SMITHCRAFT", ["DIVINE", "ARTISAN", "FORGE", "SMITH"]),
    "grindstone": ("MANA STRIPPING", ["MAGIC", "POWER", "STRIP", "SEIZE"]),
    "brewing": ("ALCHEMIC ELIXIR", ["REFINE", "GOLD", "SECRET", "DRAUGHT"]),
    "nether_portal": ("GATE OF THE UNDERWORLD", ["NETHER", "REALM", "OPEN", "GATE"]),
    "end_portal": ("GATE OF THE END", ["FINAL", "END", "OF", "GATE"]),
    "vault": ("TREASURE UNSEALED", ["SECRET", "TREASURE", "UN", "SEAL"]),
    "vault_ominous": ("CURSED TREASURE UNSEALED", ["OMEN", "TREASURE", "UN", "SEAL"]),
    "bed": ("DREAMSCAPE BARRIER", ["DREAM", "REALM", "BARRIER", "BOUND"]),
    "dragon_egg": ("DRAGON'S SHIFT", ["DRAGON", "OF", "SHIFT", "LEAP"]),
    "brush": ("ANCIENT RELIC", ["ANCIENT", "ERA", "LEGACY", "RELIC"]),
    "lodestone": ("MAGNETIC GUIDANCE", ["MAGNET", "POLE", "POINT", "GUIDE"]),
}

# 直排時要轉 90° 的字
ROTATE = set("ー～…")


# B 級（短演出）的招式名
NAMES.update({
    "cauldron": ("魔女之釜", "ウィッチ・カルドロン", ["ま", "じょ", "の", "かま"]),
    "wax": ("蜜蠟封緘", "ハニー・シール", ["みつ", "ろう", "ふう", "かん"]),
    "scrape": ("返璞歸真", "リストア・オリジン", ["へん", "ぼく", "き", "しん"]),
    "candle": ("燭火點靈", "キャンドル・スピリット", ["しょく", "か", "てん", "れい"]),
    "campfire": ("營火祭禮", "キャンプファイア・リチュアル", ["えい", "か", "さい", "れい"]),
    "tnt": ("爆炎序曲", "エクスプロージョン・プレリュード", ["ばく", "えん", "じょ", "きょく"]),
    "stonecutter": ("斷岩一閃", "ロック・クリーヴ", ["だん", "がん", "いっ", "せん"]),
    "loom": ("天衣織紋", "ヘヴンリー・ウィーヴ", ["てん", "い", "しょく", "もん"]),
    "crafter": ("機巧自律", "オートマトン・クラフト", ["き", "こう", "じ", "りつ"]),
    "jukebox": ("天籟之音", "セレスティアル・メロディ", ["てん", "らい", "の", "おと"]),
    "lectern": ("智典開示", "グリモワール・オープン", ["ち", "てん", "かい", "じ"]),
    "bell": ("晨鐘破邪", "ホーリー・ベル", ["しん", "しょう", "は", "じゃ"]),
    # 鐘的十二響（連敲同一口鐘，一響一名；毛筆字型沒有「眾」，用異體「衆」）
    "bell1": ("一響破邪", "エクソシズム", ["いち", "きょう", "は", "じゃ"]),
    "bell2": ("二響撼心", "ハート・クエイク", ["に", "きょう", "かん", "しん"]),
    "bell3": ("三響震天", "ヘヴン・クエイク", ["さん", "きょう", "しん", "てん"]),
    "bell4": ("四響懾靈", "スピリット・テラー", ["よん", "きょう", "しょう", "れい"]),
    "bell5": ("五響伏衆", "オール・ニール", ["ご", "きょう", "ふく", "しゅう"]),
    "bell6": ("六響斷魔", "デーモン・セヴァー", ["ろっ", "きょう", "だん", "ま"]),
    "bell7": ("七響裂空", "スカイ・リッパー", ["なな", "きょう", "れっ", "くう"]),
    "bell8": ("八響鎮魂", "レクイエム", ["はっ", "きょう", "ちん", "こん"]),
    "bell9": ("九響焚獄", "インフェルノ・パージ", ["きゅう", "きょう", "ふん", "ごく"]),
    "bell10": ("十響逆命", "リバース・デスティニー", ["じっ", "きょう", "ぎゃく", "めい"]),
    "bell11": ("十一響滅界", "ワールド・エンド", ["じゅう", "いち", "きょう", "めっ", "かい"]),
    "bell12": ("十二響歸墟", "リターン・トゥ・ヴォイド", ["じゅう", "に", "きょう", "き", "きょ"]),
    "explosion": ("爆裂破滅", "ブラスト・ルイン", ["ばく", "れつ", "は", "めつ"]),
    "pearl": ("虛空瞬步", "ヴォイド・ステップ", ["きょ", "くう", "しゅん", "ぽ"]),
    "eye": ("終焉指引", "エンド・ガイダンス", ["しゅう", "えん", "し", "いん"]),
    "composter": ("輪迴新生", "リインカーネーション", ["りん", "ね", "しん", "せい"]),
    "beehive": ("蜂王蜜宴", "ロイヤル・ハニー", ["ほう", "おう", "みつ", "えん"]),
    "pumpkin": ("南瓜鬼面", "ジャック・オ・マスク", ["なん", "か", "き", "めん"]),
    "spawner": ("魂籠召喚", "ソウル・サモン", ["こん", "ろう", "しょう", "かん"]),
    "ender_chest": ("虛空寶庫", "ヴォイド・ストレージ", ["きょ", "くう", "ほう", "こ"]),
    "shulker": ("匣中異界", "ポケット・ディメンション", ["こう", "ちゅう", "い", "かい"]),
    "copper_golem": ("銅像覺醒", "ブロンズ・アウェイク", ["どう", "ぞう", "かく", "せい"]),
    "sign_dye": ("彩墨染書", "カラー・インク", ["さい", "ぼく", "せん", "しょ"]),
    "sign_glow": ("螢光刻文", "ルミナス・スクリプト", ["けい", "こう", "こく", "もん"]),
    "cake": ("甘味祝宴", "スイート・フィースト", ["かん", "み", "しゅく", "えん"]),
})
ENGLISH.update({
    "cauldron": ("WITCH'S CAULDRON", ["WITCH", "CRAFT", "OF", "CAULDRON"]),
    "wax": ("HONEYWAX SEAL", ["HONEY", "WAX", "SEAL", "CLOSE"]),
    "scrape": ("RETURN TO ORIGIN", ["RETURN", "RAW", "BACK", "TRUE"]),
    "candle": ("CANDLE SPIRIT", ["CANDLE", "FIRE", "KINDLE", "SOUL"]),
    "campfire": ("CAMPFIRE RITE", ["CAMP", "FIRE", "FESTIVAL", "RITE"]),
    "tnt": ("PRELUDE TO EXPLOSION", ["BURST", "FLAME", "PRE", "LUDE"]),
    "stonecutter": ("ROCK-CLEAVING FLASH", ["SEVER", "ROCK", "ONE", "FLASH"]),
    "loom": ("HEAVENLY WEAVE", ["HEAVEN", "ROBE", "WEAVE", "CREST"]),
    "crafter": ("AUTOMATON CRAFT", ["MACHINE", "ARTIFICE", "SELF", "RULE"]),
    "jukebox": ("CELESTIAL MELODY", ["HEAVEN", "SOUND", "OF", "MUSIC"]),
    "lectern": ("GRIMOIRE REVEALED", ["WISDOM", "TOME", "OPEN", "REVEAL"]),
    "bell": ("BELL OF BANISHMENT", ["DAWN", "BELL", "BREAK", "EVIL"]),
    "bell1": ("FIRST TOLL: EXORCISM", ["ONE", "TOLL", "BREAK", "EVIL"]),
    "bell2": ("SECOND TOLL: HEARTQUAKE", ["TWO", "TOLL", "SHAKE", "HEART"]),
    "bell3": ("THIRD TOLL: SKYQUAKE", ["THREE", "TOLL", "QUAKE", "HEAVEN"]),
    "bell4": ("FOURTH TOLL: SPIRIT DREAD", ["FOUR", "TOLL", "AWE", "SPIRIT"]),
    "bell5": ("FIFTH TOLL: ALL SHALL KNEEL", ["FIVE", "TOLL", "SUBDUE", "ALL"]),
    "bell6": ("SIXTH TOLL: DEMON SEVER", ["SIX", "TOLL", "SEVER", "DEMON"]),
    "bell7": ("SEVENTH TOLL: SKY RENDER", ["SEVEN", "TOLL", "REND", "SKY"]),
    "bell8": ("EIGHTH TOLL: REQUIEM", ["EIGHT", "TOLL", "CALM", "SOUL"]),
    "bell9": ("NINTH TOLL: INFERNO PURGE", ["NINE", "TOLL", "BURN", "HELL"]),
    "bell10": ("TENTH TOLL: DEFY FATE", ["TEN", "TOLL", "DEFY", "FATE"]),
    "bell11": ("ELEVENTH TOLL: WORLD'S END", ["TEN", "ONE", "TOLL", "END", "WORLD"]),
    "bell12": ("TWELFTH TOLL: THE ABYSS", ["TEN", "TWO", "TOLL", "RETURN", "ABYSS"]),
    "explosion": ("RUINOUS BLAST", ["BURST", "RUPTURE", "BREAK", "RUIN"]),
    "pearl": ("VOID STEP", ["VOID", "SKY", "BLINK", "STEP"]),
    "eye": ("GUIDANCE OF THE END", ["END", "DEMISE", "POINT", "GUIDE"]),
    "composter": ("REINCARNATION", ["CYCLE", "REBIRTH", "NEW", "LIFE"]),
    "beehive": ("ROYAL HONEY FEAST", ["BEE", "QUEEN", "HONEY", "FEAST"]),
    "pumpkin": ("PUMPKIN DEMON MASK", ["SOUTH", "GOURD", "DEMON", "MASK"]),
    "spawner": ("SOUL CAGE SUMMONING", ["SOUL", "CAGE", "SUMMON", "CALL"]),
    "ender_chest": ("VOID TREASURY", ["VOID", "SKY", "TREASURE", "VAULT"]),
    "shulker": ("POCKET DIMENSION", ["BOX", "WITHIN", "OTHER", "WORLD"]),
    "copper_golem": ("BRONZE AWAKENING", ["COPPER", "STATUE", "AWAKEN", "STIR"]),
    "sign_dye": ("COLORED INK SCRIPT", ["COLOR", "INK", "DYE", "SCRIPT"]),
    "sign_glow": ("LUMINOUS SCRIPT", ["FIREFLY", "LIGHT", "CARVE", "SCRIPT"]),
    "cake": ("SWEET FEAST", ["SWEET", "TASTE", "BLESS", "FEAST"]),
})


# 生物互動的招式名
NAMES.update({
    "iron_golem": ("鐵壁守護", "アイアン・ガーディアン", ["てっ", "ぺき", "しゅ", "ご"]),
    "wither": ("凋零降世", "ウィザー・ディセント", ["ちょう", "れい", "こう", "せい"]),
    "snow_golem": ("霜雪之靈", "フロスト・スピリット", ["そう", "せつ", "の", "れい"]),
    "copper_birth": ("銅偶誕生", "ブロンズ・バース", ["どう", "ぐう", "たん", "じょう"]),
    "tame": ("主從之契", "マスター・ボンド", ["しゅ", "じゅう", "の", "ちぎり"]),
    "name_tag": ("真名賜予", "トゥルー・ネーム", ["しん", "めい", "し", "よ"]),
    "shear": ("銀剪流舞", "シルバー・シアーズ", ["ぎん", "せん", "りゅう", "ぶ"]),
    "milk": ("白乳恩賜", "ミルキー・ブレス", ["はく", "にゅう", "おん", "し"]),
    "equip": ("鞍甲加身", "アーマー・オン", ["あん", "こう", "か", "しん"]),
    "dye_mob": ("七彩染身", "レインボー・ダイ", ["しち", "さい", "せん", "しん"]),
    "armadillo": ("鱗甲拂落", "スケール・スウィープ", ["りん", "こう", "ふつ", "らく"]),
    "allay": ("悅靈之約", "アレイ・プロミス", ["えつ", "れい", "の", "やく"]),
    "bucket_mob": ("水牢捕獲", "アクア・プリズン", ["すい", "ろう", "ほ", "かく"]),
    "barter": ("黃金交易", "ゴールド・バーター", ["おう", "ごん", "こう", "えき"]),
    "creeper": ("引爆倒數", "デトネーション", ["いん", "ばく", "とう", "すう"]),
    "end_crystal": ("終界水晶", "エンド・クリスタル", ["しゅう", "かい", "すい", "しょう"]),
    "cure_start": ("淨化儀式", "ピュリフィケーション", ["じょう", "か", "ぎ", "しき"]),
    "cure_end": ("人性回歸", "リバース・ヒューマン", ["じん", "せい", "かい", "き"]),
    "trade": ("商談成立", "ディール・クローズ", ["しょう", "だん", "せい", "りつ"]),
})
ENGLISH.update({
    "iron_golem": ("IRON GUARDIAN", ["IRON", "WALL", "GUARD", "PROTECT"]),
    "wither": ("THE WITHER DESCENDS", ["WITHER", "DECAY", "DESCEND", "WORLD"]),
    "snow_golem": ("SPIRIT OF FROST", ["FROST", "SNOW", "OF", "SPIRIT"]),
    "copper_birth": ("BIRTH OF BRONZE", ["COPPER", "PUPPET", "BIRTH", "LIFE"]),
    "tame": ("BOND OF MASTER", ["MASTER", "SERVANT", "OF", "BOND"]),
    "name_tag": ("TRUE NAME BESTOWED", ["TRUE", "NAME", "BESTOW", "GIVE"]),
    "shear": ("SILVER SHEAR DANCE", ["SILVER", "SHEAR", "FLOW", "DANCE"]),
    "milk": ("MILKY BLESSING", ["WHITE", "MILK", "GRACE", "GIFT"]),
    "equip": ("ARMOR BESTOWED", ["SADDLE", "ARMOR", "ADD", "BODY"]),
    "dye_mob": ("SEVEN-HUED DYE", ["SEVEN", "HUES", "DYE", "BODY"]),
    "armadillo": ("SCALE SWEEP", ["SCALE", "SHELL", "SWEEP", "FALL"]),
    "allay": ("ALLAY'S PROMISE", ["JOY", "SPIRIT", "OF", "PROMISE"]),
    "bucket_mob": ("AQUA PRISON", ["WATER", "PRISON", "CATCH", "CAPTURE"]),
    "barter": ("GOLDEN BARTER", ["YELLOW", "GOLD", "EXCHANGE", "TRADE"]),
    "creeper": ("DETONATION COUNTDOWN", ["IGNITE", "BLAST", "COUNT", "DOWN"]),
    "end_crystal": ("END CRYSTAL", ["FINAL", "REALM", "WATER", "CRYSTAL"]),
    "cure_start": ("RITE OF PURIFICATION", ["PURE", "CLEANSE", "RITE", "RITUAL"]),
    "cure_end": ("HUMANITY RESTORED", ["HUMAN", "NATURE", "RETURN", "HOME"]),
    "trade": ("DEAL SEALED", ["TRADE", "TALK", "SEALED", "DONE"]),
})

# 投擲物、戰鬥、事件的招式名
NAMES.update({
    "snowball": ("冰華連彈", "アイシクル・バレット", ["ひょう", "か", "れん", "だん"]),
    "egg": ("創生之卵", "ジェネシス・エッグ", ["そう", "せい", "の", "たまご"]),
    "potion": ("祕藥飛散", "エリクシル・スプラッシュ", ["ひ", "やく", "ひ", "さん"]),
    "xp_bottle": ("智慧結晶", "ウィズダム・クリスタル", ["ち", "え", "けっ", "しょう"]),
    "arrow": ("流星一矢", "メテオ・アロー", ["りゅう", "せい", "いっ", "し"]),
    "trident": ("海神之槍", "ポセイドン・ランス", ["かい", "しん", "の", "やり"]),
    "wind_charge": ("風神裂破", "ゲイル・バースト", ["ふう", "じん", "れっ", "ぱ"]),
    "firework": ("星火繚亂", "スターファイア・カーニバル", ["せい", "か", "りょう", "らん"]),
    "fishing": ("天釣一線", "ヘヴンズ・ライン", ["てん", "ちょう", "いっ", "せん"]),
    "crit": ("會心一擊", "クリティカル・ヒット", ["かい", "しん", "いち", "げき"]),
    "sweep": ("橫掃千軍", "スウィープ・スラッシュ", ["おう", "そう", "せん", "ぐん"]),
    "totem": ("不死涅槃", "リザレクション", ["ふ", "し", "ね", "はん"]),
    "boss_dragon": ("屠龍之刻", "ドラゴンスレイヤー", ["と", "りゅう", "の", "とき"]),
    "boss_wither": ("凋零殞落", "ウィザー・フォール", ["ちょう", "れい", "いん", "らく"]),
    "boss_warden": ("深淵鎮魂", "アビス・レクイエム", ["しん", "えん", "ちん", "こん"]),
    "boss_elder": ("古神長眠", "エンシェント・スリープ", ["こ", "しん", "ちょう", "みん"]),
    "elytra": ("天翼展開", "ウィング・リリース", ["てん", "よく", "てん", "かい"]),
    "riptide": ("激流迴天", "リップタイド・ストーム", ["げき", "りゅう", "かい", "てん"]),
    "level_up": ("境界突破", "リミット・ブレイク", ["きょう", "かい", "とっ", "ぱ"]),
    "advancement": ("偉業達成", "アチーブメント", ["い", "ぎょう", "たっ", "せい"]),
    "advancement_challenge": ("傳奇刻名", "レジェンド・エングレイヴ", ["でん", "き", "こく", "めい"]),
    "sleep": ("黎明降臨", "ドーン・ブレイク", ["れい", "めい", "こう", "りん"]),
})
ENGLISH.update({
    "snowball": ("FROST FLOWER BARRAGE", ["ICE", "BLOOM", "CHAIN", "SHOT"]),
    "egg": ("GENESIS EGG", ["CREATE", "LIFE", "OF", "EGG"]),
    "potion": ("ELIXIR SPLASH", ["SECRET", "DRAUGHT", "FLY", "SCATTER"]),
    "xp_bottle": ("WISDOM CRYSTAL", ["WISE", "MIND", "BIND", "CRYSTAL"]),
    "arrow": ("METEOR ARROW", ["FLOW", "STAR", "ONE", "ARROW"]),
    "trident": ("LANCE OF THE SEA GOD", ["SEA", "GOD", "OF", "LANCE"]),
    "wind_charge": ("GALE BURST", ["WIND", "GOD", "REND", "BREAK"]),
    "firework": ("STARFIRE CARNIVAL", ["STAR", "FIRE", "WHIRL", "WILD"]),
    "fishing": ("HEAVEN'S LINE", ["HEAVEN", "ANGLE", "ONE", "LINE"]),
    "crit": ("CRITICAL STRIKE", ["MEET", "HEART", "ONE", "STRIKE"]),
    "sweep": ("SWEEPING SLASH", ["SIDE", "SWEEP", "THOUSAND", "ARMY"]),
    "totem": ("UNDYING NIRVANA", ["NOT", "DEATH", "NIR", "VANA"]),
    "boss_dragon": ("THE DRAGONSLAYER'S HOUR", ["SLAY", "DRAGON", "OF", "HOUR"]),
    "boss_wither": ("FALL OF THE WITHER", ["WITHER", "DECAY", "PERISH", "FALL"]),
    "boss_warden": ("REQUIEM OF THE ABYSS", ["DEEP", "ABYSS", "QUELL", "SOUL"]),
    "boss_elder": ("SLUMBER OF THE OLD GOD", ["ANCIENT", "GOD", "LONG", "SLEEP"]),
    "elytra": ("WINGS UNFURLED", ["HEAVEN", "WING", "SPREAD", "OPEN"]),
    "riptide": ("RIPTIDE STORM", ["FIERCE", "CURRENT", "TURN", "HEAVEN"]),
    "level_up": ("LIMIT BREAK", ["BOUND", "REALM", "BREAK", "THROUGH"]),
    "advancement": ("FEAT ACHIEVED", ["GREAT", "DEED", "REACH", "DONE"]),
    "advancement_challenge": ("LEGEND ENGRAVED", ["LEGEND", "WONDER", "CARVE", "NAME"]),
    "sleep": ("DAWN DESCENDS", ["DAWN", "LIGHT", "DESCEND", "ARRIVE"]),
})


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    full = os.path.join(OUT, name + ".png")
    img.save(full)
    with open(full + ".mcmeta", "w") as f:
        json.dump({"texture": {"blur": True, "clamp": True}}, f)


# ------------------------------------------------------------------ 雜訊（向量化的值雜訊，可在任意座標取樣）

def _hash(ix, iy, seed):
    h = (ix * 374761393 + iy * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, seed=0):
    ix = np.floor(x).astype(np.int64)
    iy = np.floor(y).astype(np.int64)
    fx = x - ix
    fy = y - iy
    ux = fx * fx * (3 - 2 * fx)
    uy = fy * fy * (3 - 2 * fy)
    a = _hash(ix, iy, seed)
    b = _hash(ix + 1, iy, seed)
    c = _hash(ix, iy + 1, seed)
    d = _hash(ix + 1, iy + 1, seed)
    return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy


def fbm(x, y, seed=0, octaves=4):
    v = np.zeros_like(x, dtype=np.float64)
    amp = 0.5
    for o in range(octaves):
        v += amp * vnoise(x, y, seed + o * 17)
        x = x * 2.03 + 7.1
        y = y * 2.03 + 3.3
        amp *= 0.5
    return v


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255


def stroke_text(draw, xy, ch, font, fill, stroke, stroke_fill):
    draw.text(xy, ch, font=font, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill)


# ------------------------------------------------------------------ A. 毛筆

def brush_glyph(ch, size, seed):
    """一個毛筆字的墨跡遮罩：毛邊 + 筆畫末端的飛白 + 少量墨點"""
    pad = int(size * 0.28)
    S = size + pad * 2
    font = ImageFont.truetype(BRUSH, size)
    m = Image.new("L", (S, S), 0)
    ImageDraw.Draw(m).text((S / 2, S / 2), ch, font=font, fill=255, anchor="mm")
    base = np.asarray(m, np.float32) / 255
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)

    # 毛邊：用雜訊擾動取樣座標
    dx = (fbm(xx / 9, yy / 9, seed) - 0.5) * size * 0.022
    dy = (fbm(xx / 9 + 40, yy / 9, seed + 5) - 0.5) * size * 0.022
    sx = np.clip(xx + dx, 0, S - 1).astype(np.int32)
    sy = np.clip(yy + dy, 0, S - 1).astype(np.int32)
    ink = base[sy, sx]

    # 筆畫方向：模糊遮罩的梯度的垂直方向
    sm = blur(ink, size * 0.03)
    gy, gx = np.gradient(sm)
    ang = np.arctan2(gy, gx) + math.pi / 2
    # 深度：在筆畫裡多深（細處 / 邊緣 ≈ 0）
    depth = blur(ink, size * 0.05)
    # 飛白：沿筆畫方向拉長的條紋雜訊，只出現在邊緣與淺處
    u = xx * np.cos(ang) + yy * np.sin(ang)
    v = -xx * np.sin(ang) + yy * np.cos(ang)
    streak = vnoise(u / (size * 0.28), v / (size * 0.007), seed + 9) * 0.6 + vnoise(u / (size * 0.5), v / (size * 0.02), seed + 3) * 0.4
    edge = np.clip(1.0 - (depth - 0.35) / 0.4, 0, 1)
    patch = fbm(xx / (size * 0.25), yy / (size * 0.25), seed + 21)
    dry = (streak > 0.6 + 0.35 * (1 - edge)) & (patch > 0.5)
    ink = ink * np.where(dry, 0.0, 1.0)
    ink = np.clip(ink * 1.15, 0, 1)

    # 墨點：在字的外圍撒少量
    rng = np.random.default_rng(seed)
    sp = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(sp)
    ys, xs = np.nonzero(base > 0.5)
    if len(xs):
        for _ in range(rng.integers(3, 8)):
            i = rng.integers(len(xs))
            a = rng.uniform(0, math.tau)
            r = rng.uniform(0.06, 0.16) * size
            px, py = xs[i] + math.cos(a) * r, ys[i] + math.sin(a) * r
            rr = rng.uniform(0.004, 0.014) * size
            d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=255)
    ink = np.maximum(ink, np.asarray(sp, np.float32) / 255)
    return ink


def brush_column(key, text):
    size = 400
    step = int(size * 0.94)
    rng = np.random.default_rng(sum(map(ord, text)))
    W = int(size * 1.7)
    H = step * len(text) + int(size * 0.7)
    ink = np.zeros((H, W), np.float32)
    cells = []
    for i, ch in enumerate(text):
        # 第一字稍大；每字微微錯位、傾斜，像一筆一筆寫下去
        k = 1.12 if i == 0 else rng.uniform(0.94, 1.02)
        s = int(size * k)
        g = brush_glyph(ch, s, seed=i * 31 + ord(ch))
        gi = Image.fromarray((g * 255).astype(np.uint8)).rotate(rng.uniform(-4, 4), Image.BICUBIC)
        g = np.asarray(gi, np.float32) / 255
        gh, gw = g.shape
        cx = W / 2 + rng.uniform(-0.04, 0.04) * size
        cy = size * 0.35 + step * i + step / 2 + (size * 0.03 if i == 0 else 0)
        x0, y0 = int(cx - gw / 2), int(cy - gh / 2)
        xs0, ys0 = max(0, x0), max(0, y0)
        xs1, ys1 = min(W, x0 + gw), min(H, y0 + gh)
        ink[ys0:ys1, xs0:xs1] = np.maximum(ink[ys0:ys1, xs0:xs1], g[ys0 - y0:ys1 - y0, xs0 - x0:xs1 - x0])
        cells.append([int(cy - step / 2), int(cy + step / 2)])
    # 白色光暈（兩層：貼字的硬一點 + 外圈柔）
    halo = np.clip(blur(ink, 10) * 1.6, 0, 1) * 0.85 + blur(ink, 34) * 0.55
    halo = np.clip(halo, 0, 1)
    rgb = np.ones((H, W, 3), np.float32) * 255
    inkc = np.array([10, 6, 14], np.float32)
    rgb = rgb * (1 - ink[..., None]) + inkc * ink[..., None]
    a = np.maximum(ink, halo * 0.92)
    out = np.dstack([rgb, a * 255]).clip(0, 255).astype(np.uint8)
    save(Image.fromarray(out, "RGBA"), f"{key}_brush")
    return [W, H], cells


def vertical_text(text, font, fill, stroke, stroke_fill, height=None, pad=None):
    """直排文字；height 指定時把字平均撐滿這個高度"""
    size = font.size
    pad = pad if pad is not None else stroke * 2 + 4
    n = len(text)
    adv = size * 1.02
    total = adv * n if height is None else height
    adv = total / n
    W = int(size * 1.15 + pad * 2)
    H = int(total + pad * 2)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for i, ch in enumerate(text):
        cell = Image.new("RGBA", (int(size * 1.4) + stroke * 2, int(size * 1.4) + stroke * 2), (0, 0, 0, 0))
        cw, chh = cell.size
        ImageDraw.Draw(cell).text((cw / 2, chh / 2), ch, font=font, fill=fill, anchor="mm", stroke_width=stroke, stroke_fill=stroke_fill)
        if ch in ROTATE:
            cell = cell.rotate(-90, Image.BICUBIC)
        cy = pad + adv * i + adv / 2
        img.alpha_composite(cell, (int(W / 2 - cw / 2), int(cy - chh / 2)))
    return img


def ruby_column(key, kana, height):
    font = ImageFont.truetype(BOLD, 64)
    img = vertical_text(kana, font, (12, 8, 16, 255), 7, (255, 255, 255, 255), height=height * 0.82)
    # 外圈柔光
    a = np.asarray(img, np.float32)
    glow = blur(a[..., 3] / 255, 10) * 0.7
    out = a.copy()
    out[..., :3] = np.where(a[..., 3:4] > 0, a[..., :3], 255)
    out[..., 3] = np.maximum(a[..., 3], glow * 255)
    save(Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA"), f"{key}_ruby")
    return [img.width, img.height]


def subtitle(key, text):
    font = ImageFont.truetype(BOLD, 72)
    sp = 10
    w = int(sum(font.getlength(c) for c in text) + sp * (len(text) - 1)) + 40
    h = 110
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = 20
    for c in text:
        d.text((x, h / 2), c, font=font, fill=(255, 255, 255, 255), anchor="lm", stroke_width=5, stroke_fill=(0, 0, 0, 255))
        x += font.getlength(c) + sp
    save(img, f"{key}_sub")
    return [w, h]


# ------------------------------------------------------------------ B. 漫畫四角大字

def corner_char(key, i, ch):
    size = 420
    font = ImageFont.truetype(HEAVY, size)
    S = int(size * 1.4)
    m = Image.new("L", (S, S), 0)
    ImageDraw.Draw(m).text((S / 2, S / 2), ch, font=font, fill=255, anchor="mm")
    # 一點點右傾，筆畫更有衝勁
    m = m.transform((S, S), Image.AFFINE, (1, 0.08, -0.08 * S / 2, 0, 1, 0), Image.BICUBIC)
    inner = np.asarray(m, np.float32) / 255
    white = np.asarray(m.filter(ImageFilter.MaxFilter(27)), np.float32) / 255
    outer = np.asarray(m.filter(ImageFilter.MaxFilter(27)).filter(ImageFilter.MaxFilter(9)), np.float32) / 255
    rgb = np.zeros((S, S, 3), np.float32)
    rgb[...] = 255 * (white > 0.5)[..., None] * (1 - inner[..., None])
    a = np.clip(np.maximum(outer, inner), 0, 1)
    out = np.dstack([rgb, a * 255]).clip(0, 255).astype(np.uint8)
    save(Image.fromarray(out, "RGBA"), f"{key}_c{i}")
    return [S, S]


def corner_ruby(key, i, kana):
    font = ImageFont.truetype(BOLD, 78)
    img = vertical_text(kana, font, (8, 6, 10, 255), 8, (255, 255, 255, 255))
    save(img, f"{key}_r{i}")
    return [img.width, img.height]


def rotated_text(text, font, fill, stroke, stroke_fill, spacing=0.12):
    """橫寫英文轉 90°（由上往下讀），用於直排讀音"""
    sp = font.size * spacing
    w = int(sum(font.getlength(c) for c in text) + sp * (len(text) - 1) + stroke * 4 + 8)
    h = int(font.size * 1.35 + stroke * 4)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = stroke * 2 + 4
    for c in text:
        d.text((x, h / 2), c, font=font, fill=fill, anchor="lm", stroke_width=stroke, stroke_fill=stroke_fill)
        x += font.getlength(c) + sp
    return img.rotate(-90, Image.BICUBIC, expand=True)


def english(key, sub, words, column_h):
    font = ImageFont.truetype(BOLD, 64)
    r = rotated_text(sub, font, (12, 8, 16, 255), 7, (255, 255, 255, 255), 0.22)
    # 讀音直排不超過毛筆字欄的 82%
    if r.height > column_h * 0.82:
        r = r.resize((int(r.width * column_h * 0.82 / r.height), int(column_h * 0.82)), Image.LANCZOS)
    a = np.asarray(r, np.float32)
    glow = blur(a[..., 3] / 255, 10) * 0.7
    out = a.copy()
    out[..., :3] = np.where(a[..., 3:4] > 0, a[..., :3], 255)
    out[..., 3] = np.maximum(a[..., 3], glow * 255)
    save(Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA"), f"{key}_ruby_en")
    ruby = [r.width, r.height]
    # 字幕
    sf = ImageFont.truetype(BOLD, 72)
    sp = 14
    w = int(sum(sf.getlength(c) for c in sub) + sp * (len(sub) - 1)) + 40
    img = Image.new("RGBA", (w, 110), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = 20
    for c in sub:
        d.text((x, 55), c, font=sf, fill=(255, 255, 255, 255), anchor="lm", stroke_width=5, stroke_fill=(0, 0, 0, 255))
        x += sf.getlength(c) + sp
    save(img, f"{key}_sub_en")
    # 四角每字的英文
    rf = ImageFont.truetype(BOLD, 78)
    rubies = []
    for i, word in enumerate(words):
        im = rotated_text(word, rf, (8, 6, 10, 255), 8, (255, 255, 255, 255), 0.08)
        save(im, f"{key}_r{i}_en")
        rubies.append([im.width, im.height])
    return ruby, [w, 110], rubies


def main():
    path = os.path.join(ROOT, "names.json")
    data = {}
    if os.path.exists(path):
        # 只重做部分名字時保留其他的
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    for key, (text, kana, per) in NAMES.items():
        bsize, cells = brush_column(key, text)
        rsize = ruby_column(key, kana, bsize[1])
        ssize = subtitle(key, text)
        corners = [corner_char(key, i, ch) for i, ch in enumerate(text)]
        rubies = [corner_ruby(key, i, k) for i, k in enumerate(per)]
        en_sub, en_words = ENGLISH[key]
        eruby, esub, erubies = english(key, en_sub, en_words, bsize[1])
        data[key] = {"brush": bsize, "cells": cells, "ruby": rsize, "sub": ssize, "corners": corners, "rubies": rubies,
                     "en": {"ruby": eruby, "sub": esub, "rubies": erubies}}
        print("  name", key, text)
    with open(os.path.join(ROOT, "names.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)


if __name__ == "__main__":
    import sys
    only = sys.argv[1:]
    if only:
        NAMES = {k: v for k, v in NAMES.items() if k in only}
    main()
