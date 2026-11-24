package com.chuuni.net;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.chuuni.Rewards;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 伺服器端廣播。
 * <ul>
 *   <li>同一 tick 內的連續合成 / 取出（shift 一鍵量產）合併成一次演出，件數累加</li>
 *   <li>門的「封印連段」：同一玩家 3 秒內持續開關門，連段數遞增，演出越來越誇張</li>
 * </ul>
 */
public final class FxNet {
	private static final double RANGE = 64.0;
	private static final int DOOR_COMBO_WINDOW = 60;

	private record BatchKey(UUID player, FxPayload.Kind kind) {
	}

	private static final Map<BatchKey, Batch> BATCHES = new LinkedHashMap<>();
	private static final Map<UUID, int[]> DOOR_COMBO = new HashMap<>();
	private static long tick;

	private static final class Batch {
		final ServerLevel level;
		FxPayload payload;
		int count;

		Batch(ServerLevel level, FxPayload payload) {
			this.level = level;
			this.payload = payload;
		}
	}

	private FxNet() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(FxPayload.TYPE, FxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerSettingsPayload.TYPE, ServerSettingsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ServerEditPayload.TYPE, ServerEditPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClaimPayload.TYPE, ClaimPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ClaimPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> Rewards.claim(ctx.player())));
		PayloadTypeRegistry.playC2S().register(PrefsPayload.TYPE, PrefsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PrefsPayload.TYPE, (payload, ctx) -> ctx.server().execute(() -> Rewards.prefer(ctx.player(), payload.rewards())));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tick++;
			Rewards.tick(tick);

			if (BATCHES.isEmpty()) {
				return;
			}

			for (Batch b : BATCHES.values()) {
				FxPayload p = b.payload;
				send(b.level, p.withPower(b.count));
			}

			BATCHES.clear();
		});
	}

	/** 立即廣播 */
	public static void emit(ServerLevel level, FxPayload payload) {
		send(level, payload);
	}

	/** 累加到本 tick 的批次（件數 = power 的總和） */
	public static void batch(ServerLevel level, Player player, FxPayload payload) {
		Batch b = BATCHES.computeIfAbsent(new BatchKey(player.getUUID(), payload.kind()), k -> new Batch(level, payload));
		b.count += Math.max(1, payload.power());

		if (b.payload.item().isEmpty() && !payload.item().isEmpty()) {
			b.payload = payload;
		}
	}

	public static long tick() {
		return tick;
	}

	/** 門的連段數（3 秒內持續開關 +1） */
	public static int doorCombo(Player player) {
		int[] c = DOOR_COMBO.computeIfAbsent(player.getUUID(), k -> new int[]{0, -1000});
		c[0] = tick - c[1] <= DOOR_COMBO_WINDOW ? c[0] + 1 : 1;
		c[1] = (int)tick;
		return c[0];
	}

	/** 指定廣播範圍（頭目戰這種大場面用） */
	public static void emit(ServerLevel level, FxPayload payload, double range) {
		send(level, payload, range);
	}

	private static void send(ServerLevel level, FxPayload payload) {
		send(level, payload, RANGE);
	}

	private static void send(ServerLevel level, FxPayload payload, double range) {
		// 伺服器設定檔關掉的演出（fx.xxx=false）一律不送
		if (com.chuuni.ServerConfig.off(payload.kind())) {
			return;
		}

		BlockPos pos = payload.pos();

		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= range * range && ServerPlayNetworking.canSend(p, FxPayload.TYPE)) {
				ServerPlayNetworking.send(p, payload);
			}
		}
	}

	public static ItemStack copy(ItemStack stack) {
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
	}
}
