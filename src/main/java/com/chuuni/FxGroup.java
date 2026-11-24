package com.chuuni;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.chuuni.net.FxPayload.Kind;

/**
 * 演出的分組（設定畫面的分頁、設定檔的分段、整組開關）。每種演出另有自己的開關 fx.&lt;種類&gt;。
 */
public enum FxGroup {
	/** A 級：電影演出 */
	A("tierA"),
	/** B 級：短演出 */
	B("tierB"),
	/** C 級：微型 */
	C("tierC"),
	/** 生物互動 */
	MOBS("mobs"),
	/** 投擲物 */
	THROWS("throws"),
	/** 戰鬥 */
	COMBAT("combat"),
	/** 事件 */
	EVENTS("events");

	/** 整組開關的設定 key */
	public final String key;

	FxGroup(String key) {
		this.key = key;
	}

	/** 這種演出屬於哪一組（REWARD 是成品掉落提示，不屬於任何一組、永遠開著 → null） */
	public static FxGroup of(Kind k) {
		return switch (k) {
			case REWARD -> null;
			case ENCHANT, ANVIL, CRAFT, CARTOGRAPHY, FURNACE_TAKE, FURNACE_SMELT, ANCHOR, BEACON, SMITHING, GRINDSTONE, BREWING, NETHER_PORTAL, END_PORTAL, VAULT,
				BED, DRAGON_EGG, BRUSH, LODESTONE, GOLEM, WITHER -> A;
			case CAULDRON, WAX, SCRAPE, CANDLE, CAMPFIRE, TNT, STONECUTTER, LOOM, CRAFTER, JUKEBOX, LECTERN, BELL, COMPOSTER, BEEHIVE, PUMPKIN, SPAWNER,
				ENDER_CHEST, SHULKER, COPPER_GOLEM, SIGN, CAKE, END_CRYSTAL, EXPLOSION -> B;
			case TAME, NAME_TAG, SHEAR, MILK, EQUIP, DYE_MOB, ARMADILLO, ALLAY, BUCKET_MOB, BARTER, CREEPER, CURE, TRADE, ARMOR_STAND, ITEM_FRAME, LOVE, LEASH,
				RIDE -> MOBS;
			case PEARL, EYE, SNOWBALL, EGG, POTION, XP_BOTTLE, ARROW, TRIDENT, WIND_CHARGE, FIREWORK, FISHING -> THROWS;
			case CRIT, SWEEP, TOTEM, BOSS -> COMBAT;
			case ELYTRA, RIPTIDE, LEVEL_UP, ADVANCEMENT, SLEEP -> EVENTS;
			default -> C;
		};
	}

	/** 這一組的所有演出（依宣告順序） */
	public List<Kind> kinds() {
		List<Kind> out = new ArrayList<>();

		for (Kind k : Kind.values()) {
			if (of(k) == this) {
				out.add(k);
			}
		}

		return out;
	}

	/** 個別演出的設定 key：fx.enchant、fx.end_portal…… */
	public static String key(Kind k) {
		return "fx." + k.name().toLowerCase(Locale.ROOT);
	}

	/** fx.xxx → 演出種類；不是演出 key 時回傳 null */
	public static Kind kind(String key) {
		if (!key.startsWith("fx.")) {
			return null;
		}

		try {
			Kind k = Kind.valueOf(key.substring(3).toUpperCase(Locale.ROOT));
			return of(k) == null ? null : k;
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
}
