package com.chuuni.net;

import com.chuuni.Chuuni;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 一次演出（S2C）。
 *
 * @param kind    演出種類（{@link Kind}）
 * @param pos     方塊位置
 * @param power   強度：附魔 / 鐵砧為經驗等級花費，合成 / 熔爐為件數，門為連段數
 * @param extra   附加資訊：附魔按鈕 0~2、鐵砧是否改名、合成是否在工作台、門開(1)/關(0)
 * @param variant 方塊變種：熔爐 0 / 高爐 1 / 煙燻爐 2；門 0 / 活板門 1 / 柵欄門 2
 * @param actor   觸發者的實體 id（客戶端據此判斷是不是自己 → 播放電影鏡頭）
 * @param item    成品（浮在空中展示用，可為空）
 * @param at      玩家實際點到的位置（世界座標；沒有時是方塊中心）
 * @param face    點到的面（Direction 序號，-1 = 不明）
 */
public record FxPayload(Kind kind, BlockPos pos, int power, int extra, int variant, int actor, ItemStack item, Vec3 at, int face) implements CustomPacketPayload {
	public enum Kind {
		ENCHANT, ANVIL, CRAFT, CARTOGRAPHY, FURNACE_TAKE, FURNACE_SMELT, DOOR,
		/** 專屬掉落物的發光提示：power = 物品實體 id、extra = 顏色 */
		REWARD,
		ANCHOR, BEACON, SMITHING, GRINDSTONE, BREWING, NETHER_PORTAL, END_PORTAL, VAULT, BED, DRAGON_EGG, BRUSH, LODESTONE,
		// B 級：短演出（不切鏡頭）
		CAULDRON, WAX, SCRAPE, CANDLE, CAMPFIRE, TNT, STONECUTTER, LOOM, CRAFTER, JUKEBOX, LECTERN, BELL, COMPOSTER, BEEHIVE, PUMPKIN, SPAWNER,
		ENDER_CHEST, SHULKER, COPPER_GOLEM, SIGN, CAKE,
		// C 級：微型（1 秒內）
		CONTAINER, BOOKSHELF, SHELF, POT, NOTE, LEVER, BUTTON, DIODE, DAYLIGHT, STRIP, TILL, PATH, BONEMEAL, BERRY,
		// 生物互動（extra = 目標生物的實體 id，客戶端據此跟著牠）與其他互動
		GOLEM, WITHER, TAME, NAME_TAG, SHEAR, MILK, EQUIP, DYE_MOB, ARMADILLO, ALLAY, BUCKET_MOB, BARTER, CREEPER, END_CRYSTAL, CURE, TRADE,
		ARMOR_STAND, ITEM_FRAME, LOVE, LEASH, RIDE, BOTTLE, BUCKET, MUD, WIRE, REDSTONE_ORE,
		// 純客戶端偵測（不經伺服器）：丟出的終界珍珠 / 終界之眼（extra = 實體 id）、爆炸（power = 半徑 ×10）
		PEARL, EYE, EXPLOSION,
		// 投擲物（純客戶端偵測，extra = 實體 id）：雪球、雞蛋、藥水、經驗瓶、箭、三叉戟、風彈、煙火、釣竿
		SNOWBALL, EGG, POTION, XP_BOTTLE, ARROW, TRIDENT, WIND_CHARGE, FIREWORK, FISHING,
		// 戰鬥：爆擊 / 橫掃（伺服器，extra = 目標）、不死圖騰（客戶端，extra = 使用者）、擊殺頭目（伺服器，variant 0 龍 1 凋零 2 伏守者 3 遠古守衛者）
		CRIT, SWEEP, TOTEM, BOSS,
		// 事件：鞘翅起飛 / 激流 / 升級（客戶端）、進度達成（伺服器，variant = 0 一般 1 挑戰 2 目標）、睡到天亮（伺服器）
		ELYTRA, RIPTIDE, LEVEL_UP, ADVANCEMENT, SLEEP
	}

	public static final Type<FxPayload> TYPE = new Type<>(Chuuni.id("fx"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeEnum(p.kind);
			buf.writeBlockPos(p.pos);
			buf.writeVarInt(p.power);
			buf.writeVarInt(p.extra);
			buf.writeVarInt(p.variant);
			buf.writeVarInt(p.actor);
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.item);
			buf.writeDouble(p.at.x);
			buf.writeDouble(p.at.y);
			buf.writeDouble(p.at.z);
			buf.writeByte(p.face);
		},
		buf -> new FxPayload(buf.readEnum(Kind.class), buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
			ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readByte()));

	public FxPayload(Kind kind, BlockPos pos, int power, int extra, int variant, int actor, ItemStack item) {
		this(kind, pos, power, extra, variant, actor, item, Vec3.atCenterOf(pos), -1);
	}

	/** 換掉件數（批次合併用） */
	public FxPayload withPower(int n) {
		return new FxPayload(kind, pos, n, extra, variant, actor, item, at, face);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
