package com.chuuni;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;
import com.chuuni.net.FxPayload.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** mixin 共用的觸發邏輯（mixin 套件裡不能放一般類別） */
public final class Triggers {
	private Triggers() {
	}

	/** 門 / 活板門 / 柵欄門被玩家開關後 → 封印解除！／再・封印 */
	public static void door(Level level, BlockPos pos, Player player, BlockHitResult hit, InteractionResult result, int variant) {
		if (!(level instanceof ServerLevel server) || !result.consumesAction()) {
			return;
		}

		BlockState now = level.getBlockState(pos);

		if (!now.hasProperty(BlockStateProperties.OPEN)) {
			return;
		}

		boolean open = now.getValue(BlockStateProperties.OPEN);
		FxNet.emit(server, new FxPayload(FxPayload.Kind.DOOR, pos.immutable(), FxNet.doorCombo(player), open ? 1 : 0, variant, player.getId(), ItemStack.EMPTY,
			hit.getLocation(), hit.getDirection().ordinal()));
	}

	/** 沒有直接玩家參數的觸發（烽火台、地獄門、龍蛋、爆炸）：最近的玩家當觸發者，沒有則 -1 */
	public static int nearest(ServerLevel level, BlockPos pos) {
		Player p = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
		return p == null ? -1 : p.getId();
	}

	/** 熔爐 0 / 高爐 1 / 煙燻爐 2 */
	public static int furnaceVariant(BlockState state) {
		if (state.is(Blocks.BLAST_FURNACE)) {
			return 1;
		}

		return state.is(Blocks.SMOKER) ? 2 : 0;
	}

	private static final Map<String, Long> LAST = new HashMap<>();

	/**
	 * B 級短演出：actor 為 null 時找最近的玩家；at 為 null 時用方塊中心。
	 * cooldown（tick）&gt; 0 時，同一位置同一種演出在冷卻內不重複（紅石自動化 / 連點時不洗版）。
	 */
	public static void minor(Level level, FxPayload.Kind kind, BlockPos pos, int power, int extra, int variant, Entity actor, ItemStack item, Vec3 at, int face,
		int cooldown) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		if (cooldown > 0 && !cool(kind.name() + pos.asLong() + "/" + variant, cooldown)) {
			return;
		}

		int id = actor instanceof Player ? actor.getId() : nearest(server, pos);
		FxNet.emit(server, new FxPayload(kind, pos.immutable(), power, extra, variant, id, FxNet.copy(item), at == null ? Vec3.atCenterOf(pos) : at, face));
	}

	public static void minor(Level level, FxPayload.Kind kind, BlockPos pos, int power, int extra, int variant, Entity actor, ItemStack item, BlockHitResult hit) {
		minor(level, kind, pos, power, extra, variant, actor, item, hit == null ? null : hit.getLocation(), hit == null ? -1 : hit.getDirection().ordinal(), 4);
	}

	private static final Map<Long, long[]> BELL = new HashMap<>();
	/** 連敲的間隔上限（tick）：超過就從第一響重新數起 */
	private static final int BELL_WINDOW = 50;

	/** 鐘：同一口鐘 2.5 秒內持續敲響，一響一響往上數到十二響，之後重新開始 */
	public static void bell(Level level, BlockPos pos, Entity actor, int face) {
		if (!(level instanceof ServerLevel) || !cool(Kind.BELL.name() + pos.asLong(), 8)) {
			return;
		}

		long now = FxNet.tick();
		long[] c = BELL.computeIfAbsent(pos.asLong(), k -> new long[]{0, -1000});
		c[0] = now - c[1] <= BELL_WINDOW ? c[0] % 12 + 1 : 1;
		c[1] = now;

		if (BELL.size() > 256) {
			BELL.entrySet().removeIf(e -> now - e.getValue()[1] > BELL_WINDOW);
		}

		minor(level, Kind.BELL, pos, (int)c[0], 0, 0, actor, ItemStack.EMPTY, null, face, 0);
	}

	/** 冷卻：同一個 key 在 cooldown（tick）內只放行一次 */
	private static boolean cool(String key, int cooldown) {
		long now = FxNet.tick();
		Long last = LAST.get(key);

		if (last != null && now - last < cooldown) {
			return false;
		}

		if (LAST.size() > 512) {
			LAST.clear();
		}

		LAST.put(key, now);
		return true;
	}

	/** 箱子類介面背後的容器（mixin 套件內的 accessor 只能從這裡轉型呼叫） */
	public static net.minecraft.world.Container menuContainer(Object menu) {
		if (menu instanceof com.chuuni.mixin.ChestMenuAccessor a) {
			return a.chuuni$box();
		}

		if (menu instanceof com.chuuni.mixin.HopperMenuAccessor a) {
			return a.chuuni$box();
		}

		return menu instanceof com.chuuni.mixin.DispenserMenuAccessor a ? a.chuuni$box() : null;
	}

	/**
	 * 生物互動：pos = 生物所在方塊、at = 生物身體中心、extra = 生物的實體 id（客戶端跟著牠）。
	 */
	public static void mob(Level level, FxPayload.Kind kind, Entity target, int power, int variant, Entity actor, ItemStack item, int cooldown) {
		if (!(level instanceof ServerLevel server) || target == null) {
			return;
		}

		BlockPos pos = target.blockPosition();

		if (cooldown > 0) {
			String key = kind.name() + target.getId() + "/" + variant;
			long now = FxNet.tick();
			Long last = LAST.get(key);

			if (last != null && now - last < cooldown) {
				return;
			}

			LAST.put(key, now);
		}

		int id = actor instanceof Player ? actor.getId() : nearest(server, pos);
		Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
		FxNet.emit(server, new FxPayload(kind, pos, power, target.getId(), variant, id, FxNet.copy(item), at, -1));
	}

	/** 爆擊（目標 = 被打的生物）：同一個玩家 0.5 秒內只演一次 */
	public static void crit(Player player, Entity target) {
		if (player.level().isClientSide() || target == null || !cool("crit" + player.getId(), 10)) {
			return;
		}

		mob(player.level(), Kind.CRIT, target, 1, 0, player, player.getMainHandItem(), 0);
	}

	/** 橫掃（目標 = 主要目標）：同一個玩家 0.5 秒內只演一次 */
	public static void sweep(Player player, Entity target) {
		if (player.level().isClientSide() || target == null || !cool("sweep" + player.getId(), 10)) {
			return;
		}

		mob(player.level(), Kind.SWEEP, target, 1, 0, player, player.getMainHandItem(), 0);
	}

	/** 頭目倒下：0 終界龍、1 凋零怪、2 伏守者、3 遠古守衛者。整個戰場都看得到（範圍 160 格） */
	public static void boss(Entity boss, int variant, Entity killer) {
		if (!(boss.level() instanceof ServerLevel server)) {
			return;
		}

		BlockPos pos = boss.blockPosition();
		Player p = killer instanceof Player k ? k : server.getNearestPlayer(boss, 64);
		Vec3 at = boss.position().add(0, boss.getBbHeight() * 0.5, 0);
		FxNet.emit(server, new FxPayload(Kind.BOSS, pos, 1, boss.getId(), variant, p == null ? -1 : p.getId(), ItemStack.EMPTY, at, -1), 160);
	}

	/** 進度達成：variant 0 一般、1 挑戰、2 目標；item = 進度圖示。同一個玩家 2 秒內只演一次 */
	public static void advancement(Player player, int variant, ItemStack icon) {
		if (player.level().isClientSide() || !cool("adv" + player.getId(), 40)) {
			return;
		}

		mob(player.level(), Kind.ADVANCEMENT, player, 1, variant, player, icon, 0);
	}

	/** 睡到天亮：每個在床上的玩家 */
	public static void sleep(Player player) {
		mob(player.level(), Kind.SLEEP, player, 1, 0, player, ItemStack.EMPTY, 0);
	}

	private record Deal(Entity merchant, int count, ItemStack last) {
	}

	private static final Map<java.util.UUID, Deal> DEALS = new HashMap<>();

	/** 交易成立一筆：先記下來，關掉交易介面時才演（介面開著會擋住） */
	public static void countTrade(Player player, Entity merchant, ItemStack result) {
		// 單人模式下客戶端與伺服器共用同一份靜態資料：只在伺服器端記帳，否則客戶端關介面時會先把紀錄吃掉
		if (player == null || player.level().isClientSide()) {
			return;
		}

		Deal d = DEALS.get(player.getUUID());
		DEALS.put(player.getUUID(), new Deal(merchant, d == null ? 1 : d.count() + 1, result.copyWithCount(1)));
	}

	public static void flushTrade(Player player) {
		if (player.level().isClientSide()) {
			return;
		}

		Deal d = DEALS.remove(player.getUUID());

		if (d != null && !d.merchant().isRemoved()) {
			mob(player.level(), FxPayload.Kind.TRADE, d.merchant(), d.count(), 0, player, d.last(), 0);
		}
	}
}
