package com.chuuni.client.fx;

import com.chuuni.net.FxPayload;
import com.chuuni.net.FxPayload.Kind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 純客戶端偵測的演出（不需要伺服器裝模組）：
 * <ul>
 *   <li>剛丟出去的終界珍珠 / 終界之眼 → 跟著飛的演出</li>
 *   <li>爆炸封包 → 爆炸演出（風彈的「爆炸」不算）</li>
 *   <li>玩家丟出 / 射出的投擲物（雪球、蛋、藥水、經驗瓶、箭、三叉戟、風彈、煙火、釣竿浮標）</li>
 *   <li>玩家開始鞘翅滑翔 / 激流衝刺、自己升級、不死圖騰發動（實體事件 35）</li>
 *   <li>世界中的常駐光 → {@link Ambient}</li>
 * </ul>
 */
public final class ClientWatch {
	/** 爆炸洗版保護：0.5 秒內最多演幾次 */
	private static final int BLAST_BURST = 6;
	private static final double[] BLASTS = new double[BLAST_BURST];
	private static int blastIdx;

	private ClientWatch() {
	}

	/** 每種演出 0.5 秒內最多幾次（機槍雪球、多重射擊……） */
	private static final int THROW_BURST = 6;
	private static final Map<Kind, double[]> BURSTS = new EnumMap<>(Kind.class);
	private static final Map<Kind, int[]> BURST_IDX = new EnumMap<>(Kind.class);
	/** 每個玩家上一個 tick 的狀態：[滑翔中, 激流中] */
	private static final Map<Integer, boolean[]> STATE = new HashMap<>();
	private static Player levelOwner;
	private static int lastLevel = -1;

	public static void init() {
		ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> onEntity(entity));
		ClientTickEvents.END_CLIENT_TICK.register(Ambient::tick);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> watchPlayers());
	}

	/** 洗版保護 */
	private static boolean burst(Kind kind) {
		double[] times = BURSTS.computeIfAbsent(kind, k -> new double[THROW_BURST]);
		int[] idx = BURST_IDX.computeIfAbsent(kind, k -> new int[1]);
		double now = FxClock.now();

		if (now - times[idx[0]] < 0.5) {
			return false;
		}

		times[idx[0]] = now;
		idx[0] = (idx[0] + 1) % THROW_BURST;
		return true;
	}

	/** 投擲物的種類（不是玩家會丟的東西 → null） */
	private static Kind throwKind(Entity e, Player owner) {
		if (e instanceof Snowball) {
			return Kind.SNOWBALL;
		}

		if (e instanceof ThrownEgg) {
			return Kind.EGG;
		}

		if (e instanceof AbstractThrownPotion) {
			return Kind.POTION;
		}

		if (e instanceof ThrownExperienceBottle) {
			return Kind.XP_BOTTLE;
		}

		if (e instanceof ThrownTrident) {
			return Kind.TRIDENT;
		}

		if (e instanceof AbstractArrow) {
			return Kind.ARROW;
		}

		if (e instanceof WindCharge) {
			return Kind.WIND_CHARGE;
		}

		// 鞘翅加速用的煙火不算
		if (e instanceof FireworkRocketEntity) {
			return owner.isFallFlying() ? null : Kind.FIREWORK;
		}

		return e instanceof FishingHook ? Kind.FISHING : null;
	}

	/** 鞘翅起飛、激流衝刺、自己升級 */
	private static void watchPlayers() {
		var mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null) {
			STATE.clear();
			levelOwner = null;
			return;
		}

		java.util.Set<Integer> seen = new java.util.HashSet<>();

		for (Player p : mc.level.players()) {
			if (p.distanceTo(mc.player) > 64) {
				continue;
			}

			seen.add(p.getId());
			boolean flying = p.isFallFlying();
			boolean spin = p.isAutoSpinAttack();
			boolean[] was = STATE.get(p.getId());

			if (was != null) {
				if (flying && !was[0]) {
					follow(Kind.ELYTRA, p, 1);
				}

				if (spin && !was[1]) {
					follow(Kind.RIPTIDE, p, 1);
				}
			}

			STATE.put(p.getId(), new boolean[]{flying, spin});
		}

		STATE.keySet().retainAll(seen);

		// 自己升級（換了玩家實體 = 重生 / 換維度，重新記起點，不算升級）
		if (levelOwner != mc.player) {
			levelOwner = mc.player;
			lastLevel = mc.player.experienceLevel;
		}
		else if (mc.player.experienceLevel > lastLevel) {
			lastLevel = mc.player.experienceLevel;
			follow(Kind.LEVEL_UP, mc.player, lastLevel);
		}
		else {
			lastLevel = mc.player.experienceLevel;
		}
	}

	/** 跟著某個實體的演出（extra = 實體 id） */
	private static void follow(Kind kind, Entity e, int power) {
		int actor = e instanceof Player ? e.getId() : -1;
		Vec3 at = e.position().add(0, e.getBbHeight() * 0.5, 0);
		Effects.handle(new FxPayload(kind, e.blockPosition(), power, e.getId(), 0, actor, ItemStack.EMPTY, at, -1));
	}

	/** 由 ClientPacketListener mixin 呼叫：實體事件 35 = 不死圖騰發動 */
	public static void entityEvent(ClientboundEntityEventPacket pk) {
		var mc = Minecraft.getInstance();

		if (pk.getEventId() != 35 || mc.level == null) {
			return;
		}

		Entity e = pk.getEntity(mc.level);

		if (e != null) {
			follow(Kind.TOTEM, e, 1);
		}
	}

	private static void onEntity(Entity e) {
		var mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null) {
			return;
		}

		if (e instanceof ThrownEnderpearl pearl) {
			Entity owner = pearl.getOwner();

			// 只演「剛丟出去」的：珍珠滯留艙裡早就存在、只是剛進入視野的珍珠不算
			if (owner == null || owner.distanceTo(pearl) > 4) {
				return;
			}

			fire(Kind.PEARL, e, owner.getId());
		}
		else if (e instanceof EyeOfEnder) {
			Player p = mc.level.getNearestPlayer(e, 4);

			if (p != null) {
				fire(Kind.EYE, e, p.getId());
			}
		}
		else if (e instanceof Projectile pr && pr.getOwner() instanceof Player owner && owner.distanceTo(pr) < 6.5) {
			// 只演玩家「剛丟出 / 射出」的：已經存在、只是剛進入視野的不算
			Kind k = throwKind(e, owner);

			if (k != null && burst(k)) {
				fire(k, e, owner.getId());
			}
		}
	}

	private static void fire(Kind kind, Entity e, int actor) {
		Effects.handle(new FxPayload(kind, e.blockPosition(), 1, e.getId(), 0, actor, ItemStack.EMPTY, e.position(), -1));
	}

	/** 由 ClientPacketListener mixin 呼叫（已在主執行緒） */
	public static void explosion(ClientboundExplodePacket pk) {
		var mc = Minecraft.getInstance();

		if (mc.player == null) {
			return;
		}

		var type = pk.explosionParticle().getType();

		// 風彈 / 旋風人的風爆不是真的爆炸
		if (type == ParticleTypes.GUST_EMITTER_LARGE || type == ParticleTypes.GUST_EMITTER_SMALL) {
			return;
		}

		double now = FxClock.now();

		if (now - BLASTS[blastIdx] < 0.5) {
			return;
		}

		BLASTS[blastIdx] = now;
		blastIdx = (blastIdx + 1) % BLAST_BURST;

		Vec3 at = pk.center();
		float r = Math.max(0.5f, pk.radius());
		// 夠近才算「自己的」爆炸（直接震鏡頭、出招式名）
		int actor = mc.player.position().distanceTo(at) < r * 3 + 4 ? mc.player.getId() : -1;
		Effects.handle(new FxPayload(Kind.EXPLOSION, BlockPos.containing(at), Math.round(r * 10), 0, 0, actor, ItemStack.EMPTY, at, -1));
	}
}
