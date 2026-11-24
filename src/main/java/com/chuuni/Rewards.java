package com.chuuni;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chuuni.net.FxNet;
import com.chuuni.net.FxPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 「從空中緩緩降下」收尾的演出：演完後成品變成方塊上的掉落物，只有觸發者撿得起來，並持續發光提示。
 * <p>
 * 伺服器在成功時記下「待領取」（物品、數量、位置）；裝了模組的客戶端演完會送 {@link com.chuuni.net.ClaimPayload}，
 * 這時才把物品從介面 / 背包移到地上。沒裝模組、關掉運鏡、或 30 秒內沒來領 → 什麼都不做，維持原版行為。
 * 只移動玩家身上真的有的同款物品，所以不會複製也不會憑空變出東西。
 */
public final class Rewards {
	public enum Source {
		/** 還在附魔台的格子裡 */
		ENCHANT,
		/** 已經進了背包 / 游標（熔爐取出） */
		INVENTORY,
		/** 伺服器代為保管（刷子刷出的遺物）：領取或逾時都會掉出來 */
		HELD
	}

	private record Pending(Source source, BlockPos pos, ItemStack stack, int color, long expires, ServerLevel level, UUID owner) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static final int TIMEOUT = 20 * 30;
	private static long now;

	private Rewards() {
	}

	public static void tick(long tick) {
		now = tick;

		if (!PENDING.isEmpty()) {
			PENDING.values().removeIf(p -> {
				if (p.expires >= now) {
					return false;
				}

				// 保管中的物品逾時也要掉出來（不能讓它消失）
				if (p.source == Source.HELD && p.level != null) {
					drop(p.level, p.pos, p.stack, p.owner, p.color);
				}

				return true;
			});
		}
	}

	private static final java.util.Set<UUID> OPTED_OUT = new java.util.HashSet<>();

	/** 客戶端關掉了 A 級演出 / 運鏡：伺服器不再保管成品（維持原版） */
	public static void prefer(Player player, boolean rewards) {
		if (rewards) {
			OPTED_OUT.remove(player.getUUID());
		}
		else {
			OPTED_OUT.add(player.getUUID());
			Pending p = PENDING.remove(player.getUUID());
			flushHeld(p);
		}
	}

	public static boolean wants(Player player) {
		return !OPTED_OUT.contains(player.getUUID());
	}

	public static void expect(Player player, Source source, BlockPos pos, ItemStack stack, int color) {
		if (stack.isEmpty() || !wants(player)) {
			return;
		}

		Pending old = PENDING.get(player.getUUID());

		// 同一 tick 的 shift 量產：同款物品累加
		if (old != null && old.source == source && old.pos.equals(pos) && ItemStack.isSameItemSameComponents(old.stack, stack)) {
			ItemStack sum = old.stack.copyWithCount(old.stack.getCount() + stack.getCount());
			PENDING.put(player.getUUID(), new Pending(source, pos.immutable(), sum, color, now + TIMEOUT, null, player.getUUID()));
			return;
		}

		flushHeld(old);
		PENDING.put(player.getUUID(), new Pending(source, pos.immutable(), stack.copy(), color, now + TIMEOUT, null, player.getUUID()));
	}

	/** 由伺服器保管物品，演出結束時才掉出 */
	public static void hold(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack, int color) {
		flushHeld(PENDING.remove(player.getUUID()));
		PENDING.put(player.getUUID(), new Pending(Source.HELD, pos.immutable(), stack, color, now + TIMEOUT, level, player.getUUID()));
	}

	/** 被新的待領取蓋掉時，保管中的物品立刻掉出 */
	private static void flushHeld(Pending p) {
		if (p != null && p.source == Source.HELD && p.level != null) {
			drop(p.level, p.pos, p.stack, p.owner, p.color);
		}
	}

	private static void drop(ServerLevel level, BlockPos pos, ItemStack stack, UUID owner, int color) {
		ItemEntity e = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack, 0, 0, 0);
		e.setTarget(owner);
		e.setUnlimitedLifetime();
		e.setPickUpDelay(10);
		level.addFreshEntity(e);
		var player = level.getPlayerByUUID(owner);
		FxNet.emit(level, new FxPayload(FxPayload.Kind.REWARD, pos, e.getId(), color, 0, player == null ? -1 : player.getId(), ItemStack.EMPTY));
	}

	public static void claim(ServerPlayer player) {
		Pending p = PENDING.remove(player.getUUID());

		if (p == null || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		if (player.distanceToSqr(p.pos.getCenter()) > 16 * 16) {
			flushHeld(p);
			return;
		}

		ItemStack taken = switch (p.source) {
			case ENCHANT -> fromEnchantSlot(player, p.stack);
			case INVENTORY -> fromInventory(player, p.stack, p.stack.getCount());
			case HELD -> p.stack;
		};

		if (!taken.isEmpty()) {
			drop(p.level != null ? p.level : level, p.pos, taken, player.getUUID(), p.color);
		}
	}

	private static ItemStack fromEnchantSlot(ServerPlayer player, ItemStack want) {
		if (player.containerMenu instanceof EnchantmentMenu menu) {
			ItemStack in = menu.getSlot(0).getItem();

			if (ItemStack.isSameItemSameComponents(in, want)) {
				menu.getSlot(0).set(ItemStack.EMPTY);
				menu.sendAllDataToRemote();
				return in;
			}
		}

		// 已經關掉介面（物品回到背包）也照樣領
		return fromInventory(player, want, want.getCount());
	}

	/** 從游標與背包拿出最多 count 個同款物品 */
	private static ItemStack fromInventory(ServerPlayer player, ItemStack want, int count) {
		int left = count;
		ItemStack carried = player.containerMenu.getCarried();

		if (ItemStack.isSameItemSameComponents(carried, want)) {
			int n = Math.min(left, carried.getCount());
			carried.shrink(n);
			left -= n;
		}

		var inv = player.getInventory();

		for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
			ItemStack s = inv.getItem(i);

			if (ItemStack.isSameItemSameComponents(s, want)) {
				int n = Math.min(left, s.getCount());
				s.shrink(n);
				left -= n;
			}
		}

		// 強制把整個背包重送給客戶端（介面開著時只同步一半，關掉後快捷欄會殘留舊畫面）
		player.containerMenu.sendAllDataToRemote();
		player.inventoryMenu.sendAllDataToRemote();
		return want.copyWithCount(count - left);
	}
}
