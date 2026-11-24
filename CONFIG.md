# 中二工坊 Chuuni Crafts —— 設定說明

模組有兩份設定檔，都在遊戲（或伺服器）資料夾的 `config/` 底下：

| 檔案 | 誰用 | 改法 |
|---|---|---|
| `config/chuuni.properties` | 每個玩家自己 | 遊戲內按 **K**（或輸入 `/chuuni`）開設定畫面，或直接改檔案 |
| `config/chuuni-server.properties` | 伺服器（含單人世界的內建伺服器） | OP 在遊戲內按 **K →「伺服器設定…」**、指令 `/chuuniserver`，或直接改檔案 |

伺服器端設定的項目會**蓋過**玩家自己的設定，並在玩家的設定畫面上**鎖住**（變灰、滑鼠移上去會說明原因）。
玩家自己的檔案不會被改到，離開伺服器就恢復。

---

## 一、玩家設定（`chuuni.properties`）

### 一般

| key | 值 | 說明 |
|---|---|---|
| `enabled` | true / false | 總開關。false = 完全原版 |
| `cinematic` | true / false | 電影運鏡（A 級演出時鏡頭移動、暫時收起介面）。關掉後特效只在世界中播放 |
| `intensity` | 0 / 1 / 2 | 0 日常（不運鏡、不出招式名）、1 中二（預設）、2 末期中二（全部加倍） |
| `titles` | true / false | 招式名（毛筆直書、漫畫四角大字、右上角小招式名） |
| `post` | true / false | 畫面後製（調色、集中線、衝擊幀、漫畫分格） |
| `sound` | true / false | 本模組的音效（原版音效不受影響） |
| `shake` | 0 ~ 2 | 鏡頭震動倍率 |
| `volAnime` | 0 ~ 1 | 動漫素材音效的音量 |
| `volSynth` | 0 ~ 1 | 模組自己合成的音效（低頻重量、魔法嗡鳴）的音量 |
| `volTitle` | 0 ~ 1 | 招式名音效（落款、每個字砸下）的音量 |

### 整組開關

| key | 這一組包含 |
|---|---|
| `tierA` | A 級電影演出：附魔、鐵砧、合成、製圖、熔爐、重生錨、烽火台、鍛造、砂輪、釀造、地獄門、終界門、寶庫、床、龍蛋、考古、磁石、魔像、凋零召喚 |
| `tierB` | B 級短演出：鍋釜、上蠟、刮鏽、蠟燭、營火、TNT、切石機、織布機、合成器、唱片機、講台、鐘、堆肥箱、蜂箱、南瓜、生怪磚、終界箱、界伏盒、銅傀儡、告示牌、蛋糕、終界水晶、爆炸 |
| `tierC` | C 級微型：門、箱子類、雕紋書架、架子、陶罐、音符盒、拉桿、按鈕、中繼器 / 比較器、陽光感測器、剝樹皮、耕地、小徑、骨粉、漿果、裝瓶、水桶、泥巴、紅石線、紅石礦 |
| `mobs` | 生物互動：馴服、命名牌、剪毛、擠奶、鞍 / 馬鎧、染色、犰狳、悅靈、桶裝生物、豬布林、苦力怕、治療殭屍村民、交易、盔甲座、展示框、繁殖、拴繩、騎乘 |
| `throws` | 投擲物：終界珍珠、終界之眼、雪球、雞蛋、藥水、經驗瓶、箭、三叉戟、風彈、煙火、釣竿 |
| `combat` | 戰鬥：爆擊、橫掃、不死圖騰、擊倒頭目（終界龍 / 凋零怪 / 伏守者 / 遠古守衛者） |
| `events` | 事件：鞘翅起飛、激流、升級、進度達成、睡到天亮 |

### 個別演出

每一種演出都有自己的開關 `fx.<名稱>=true / false`（例如 `fx.explosion=false` 只關掉爆炸）。
**整組開關關掉時，這一組的個別開關都不會播。**

設定畫面：按 K →「個別演出…」，上方分頁切換分組，每一格點一下切換；下方有「整組」「全部開」「全部關」。

<details>
<summary>全部的 fx.* 名稱</summary>

| 分組 | key |
|---|---|
| A 級 | `fx.enchant` `fx.anvil` `fx.craft` `fx.cartography` `fx.furnace_take` `fx.furnace_smelt` `fx.anchor` `fx.beacon` `fx.smithing` `fx.grindstone` `fx.brewing` `fx.nether_portal` `fx.end_portal` `fx.vault` `fx.bed` `fx.dragon_egg` `fx.brush` `fx.lodestone` `fx.golem` `fx.wither` |
| B 級 | `fx.cauldron` `fx.wax` `fx.scrape` `fx.candle` `fx.campfire` `fx.tnt` `fx.stonecutter` `fx.loom` `fx.crafter` `fx.jukebox` `fx.lectern` `fx.bell` `fx.composter` `fx.beehive` `fx.pumpkin` `fx.spawner` `fx.ender_chest` `fx.shulker` `fx.copper_golem` `fx.sign` `fx.cake` `fx.end_crystal` `fx.explosion` |
| C 級 | `fx.door` `fx.container` `fx.bookshelf` `fx.shelf` `fx.pot` `fx.note` `fx.lever` `fx.button` `fx.diode` `fx.daylight` `fx.strip` `fx.till` `fx.path` `fx.bonemeal` `fx.berry` `fx.bottle` `fx.bucket` `fx.mud` `fx.wire` `fx.redstone_ore` |
| 生物 | `fx.tame` `fx.name_tag` `fx.shear` `fx.milk` `fx.equip` `fx.dye_mob` `fx.armadillo` `fx.allay` `fx.bucket_mob` `fx.barter` `fx.creeper` `fx.cure` `fx.trade` `fx.armor_stand` `fx.item_frame` `fx.love` `fx.leash` `fx.ride` |
| 投擲 | `fx.pearl` `fx.eye` `fx.snowball` `fx.egg` `fx.potion` `fx.xp_bottle` `fx.arrow` `fx.trident` `fx.wind_charge` `fx.firework` `fx.fishing` |
| 戰鬥 | `fx.crit` `fx.sweep` `fx.totem` `fx.boss` |
| 事件 | `fx.elytra` `fx.riptide` `fx.level_up` `fx.advancement` `fx.sleep` |

設定檔裡每一項上面都有一行中文名稱的註解。
</details>

---

## 二、伺服器設定（`chuuni-server.properties`）

第一次啟動伺服器時自動產生；之後每次啟動會補上新版本多出來的項目（原本的值保留）。

### 值的意思

| 值 | 意思 |
|---|---|
| 留空（例如 `sound=`） | 讓玩家自己決定 |
| `true` / `false` / 數字 | 由伺服器決定，玩家的設定畫面那一項會鎖住 |

### 一般設定 + `forceClient`

```properties
forceClient=true
sound=false
intensity=1
```

- `forceClient=true`：**一般設定**（`enabled` ~ `events`，key 和玩家設定一樣）裡有填值的項目，強制套用到所有玩家。
- `forceClient=false`：一般設定全部讓玩家自己決定（填了值也不用）。

### 個別演出（不受 `forceClient` 影響）

```properties
fx.explosion=false
fx.crit=
fx.end_portal=true
```

- `fx.xxx=false`：**整個伺服器關掉**這種演出 —— 伺服器不送、玩家也不能自己打開（純客戶端偵測的演出，例如投擲物、爆炸，也會被鎖成關）。
- `fx.xxx=true`：強制開著，玩家不能關。
- 留空：玩家自己決定。

### 三種改法

1. **遊戲內（推薦）**：OP（指令權限 2 以上）或單人世界的主人按 K →「伺服器設定…」。
   「一般」分頁是 `forceClient` 和一般設定；其他分頁每一格在「玩家決定 → 強制開 → 強制關」之間切換。
   按下去立刻寫進設定檔並套用到所有在線玩家；下方有「這組全部：玩家決定 / 強制關」「重新讀取設定檔」。
2. **指令**（伺服器主控台也能用）：
   - `/chuuniserver set <key> <值>` —— 例如 `/chuuniserver set fx.explosion false`、`/chuuniserver set sound default`（`default` = 留空）
   - `/chuuniserver get <key>`
   - `/chuuniserver reload` —— 手動改完檔案後重新讀取並套用
3. **直接改檔案**：玩家重新進服、或執行 `/chuuniserver reload` 後生效。

伺服器會再檢查一次權限，沒有權限的玩家送來的修改會被忽略。

---

## 三、常見用法

| 想要 | 設定 |
|---|---|
| 伺服器關掉爆炸特效（減少大量 TNT 時的負擔） | 伺服器：`fx.explosion=false` |
| 生存伺服器不要戰鬥特效洗畫面 | 伺服器：`fx.crit=false`、`fx.sweep=false`（或玩家自己關「戰鬥」整組） |
| 所有人都不准開電影運鏡 | 伺服器：`forceClient=true`、`cinematic=false` |
| 只想要畫面、不要聲音 | 玩家：`sound=false` |
| 動漫音效太吵、合成音效剛好 | 玩家：`volAnime=0.5` |
