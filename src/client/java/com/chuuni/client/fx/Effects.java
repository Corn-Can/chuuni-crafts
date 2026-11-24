package com.chuuni.client.fx;

import java.util.ArrayList;
import java.util.List;

import com.chuuni.client.fx.effects.*;
import com.chuuni.client.fx.effects.CartographyFx;
import com.chuuni.client.fx.effects.CraftFx;
import com.chuuni.client.fx.effects.DoorFx;
import com.chuuni.client.fx.effects.EnchantFx;
import com.chuuni.client.fx.effects.FurnaceFx;
import com.chuuni.client.fx.effects.RewardFx;
import com.chuuni.client.render.Draw;
import com.chuuni.net.FxPayload;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import net.minecraft.client.Minecraft;

/**
 * 演出管理：收到封包 → 建立對應演出；每幀推進時鐘、粒子與演出。
 */
public final class Effects {
	private static final List<Effect> ACTIVE = new ArrayList<>();
	private static long lastFrame = -1;

	private Effects() {
	}

	public static void init() {
		WorldRenderEvents.BEFORE_ENTITIES.register(Effects::beforeEntities);
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(Effects::render);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level == null && !ACTIVE.isEmpty()) {
				ACTIVE.clear();
				Particles.clear();
			}
		});
	}

	public static void handle(FxPayload p) {
		if (!Settings.allowed(p.kind())) {
			return;
		}

		Effect e = switch (p.kind()) {
			case ENCHANT -> new EnchantFx(p);
			case ANVIL -> new AnvilFx(p);
			case CRAFT -> new CraftFx(p);
			case CARTOGRAPHY -> new CartographyFx(p);
			case FURNACE_TAKE, FURNACE_SMELT -> new FurnaceFx(p);
			case DOOR -> new DoorFx(p);
			case REWARD -> new RewardFx(p);
			case ANCHOR -> new AnchorFx(p);
			case BEACON -> new BeaconFx(p);
			case SMITHING -> new SmithingFx(p);
			case GRINDSTONE -> new GrindstoneFx(p);
			case BREWING -> new BrewingFx(p);
			case NETHER_PORTAL -> new NetherPortalFx(p);
			case END_PORTAL -> new EndPortalFx(p);
			case VAULT -> new VaultFx(p);
			case BED -> new BedFx(p);
			case DRAGON_EGG -> new DragonEggFx(p);
			case BRUSH -> new BrushFx(p);
			case LODESTONE -> new LodestoneFx(p);
			case CAULDRON -> new CauldronFx(p);
			case WAX -> new WaxFx(p);
			case SCRAPE -> new ScrapeFx(p);
			case CANDLE -> new CandleFx(p);
			case CAMPFIRE -> new CampfireFx(p);
			case TNT -> new TntFx(p);
			case STONECUTTER -> new StonecutterFx(p);
			case LOOM -> new LoomFx(p);
			case CRAFTER -> new CrafterFx(p);
			case JUKEBOX -> new JukeboxFx(p);
			case LECTERN -> new LecternFx(p);
			case BELL -> new BellFx(p);
			case COMPOSTER -> new ComposterFx(p);
			case BEEHIVE -> new BeehiveFx(p);
			case PUMPKIN -> new PumpkinFx(p);
			case SPAWNER -> new SpawnerFx(p);
			case ENDER_CHEST -> new EnderChestFx(p);
			case SHULKER -> new ShulkerFx(p);
			case COPPER_GOLEM -> new CopperGolemFx(p);
			case SIGN -> new SignFx(p);
			case CAKE -> new CakeFx(p);
			case CONTAINER -> new ContainerFx(p);
			case BOOKSHELF -> new BookshelfFx(p);
			case SHELF -> new ShelfFx(p);
			case POT -> new PotFx(p);
			case NOTE -> new NoteFx(p);
			case LEVER -> new LeverFx(p);
			case BUTTON -> new ButtonFx(p);
			case DIODE -> new DiodeFx(p);
			case DAYLIGHT -> new DaylightFx(p);
			case STRIP -> new StripFx(p);
			case TILL -> new TillFx(p);
			case PATH -> new PathFx(p);
			case BONEMEAL -> new BonemealFx(p);
			case BERRY -> new BerryFx(p);
			case GOLEM -> new GolemFx(p);
			case WITHER -> new WitherFx(p);
			case TAME -> new TameFx(p);
			case NAME_TAG -> new NameTagFx(p);
			case SHEAR -> new ShearFx(p);
			case MILK -> new MilkFx(p);
			case EQUIP -> new EquipFx(p);
			case DYE_MOB -> new DyeMobFx(p);
			case ARMADILLO -> new ArmadilloFx(p);
			case ALLAY -> new AllayFx(p);
			case BUCKET_MOB -> new BucketMobFx(p);
			case BARTER -> new BarterFx(p);
			case CREEPER -> new CreeperFx(p);
			case END_CRYSTAL -> new EndCrystalFx(p);
			case CURE -> new CureFx(p);
			case TRADE -> new TradeFx(p);
			case ARMOR_STAND -> new ArmorStandFx(p);
			case ITEM_FRAME -> new ItemFrameFx(p);
			case LOVE -> new LoveFx(p);
			case LEASH -> new LeashFx(p);
			case RIDE -> new RideFx(p);
			case BOTTLE -> new BottleFx(p);
			case BUCKET -> new BucketFx(p);
			case MUD -> new MudFx(p);
			case WIRE -> new WireFx(p);
			case REDSTONE_ORE -> new RedstoneOreFx(p);
			case PEARL -> new PearlFx(p);
			case EYE -> new EyeFx(p);
			case EXPLOSION -> new ExplosionFx(p);
			case SNOWBALL, EGG, POTION, XP_BOTTLE, ARROW, TRIDENT, WIND_CHARGE, FIREWORK, FISHING -> new ThrowFx(p);
			case CRIT -> new CritFx(p);
			case SWEEP -> new SweepFx(p);
			case TOTEM -> new TotemFx(p);
			case BOSS -> new BossFx(p);
			case ELYTRA -> new ElytraFx(p);
			case RIPTIDE -> new RiptideFx(p);
			case LEVEL_UP -> new LevelUpFx(p);
			case ADVANCEMENT -> new AdvancementFx(p);
			case SLEEP -> new SleepFx(p);
		};

		if (e != null) {
			// 同一位置的同類演出只留最新的（避免連點疊成一團）
			ACTIVE.removeIf(o -> o.getClass() == e.getClass() && o.pos.equals(e.pos) && o.p.kind() == e.p.kind() && o.p.kind() != FxPayload.Kind.REWARD);
			ACTIVE.add(e);
		}
	}

	/** 後製收集光源 */
	public static void lights(List<com.chuuni.client.post.CinemaPost.Light> lights, List<com.chuuni.client.post.CinemaPost.Seg> segs,
		net.minecraft.world.phys.Vec3[] rays, float[] raysP) {
		for (Effect e : ACTIVE) {
			e.collectLights(lights, segs, rays, raysP);
		}

		Ambient.lights(lights);
	}

	public static int count() {
		return ACTIVE.size();
	}

	private static Draw draw(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		return new Draw(ctx.consumers(), ctx.commandQueue(), ctx.matrices(), mc.gameRenderer.getMainCamera());
	}

	private static void beforeEntities(WorldRenderContext ctx) {
		// 每幀只推進一次
		long frame = System.nanoTime();

		if (frame != lastFrame) {
			lastFrame = frame;
			FxClock.frame();
			Particles.update(FxClock.dt());
			Cinematic.tickDeferred();
			ACTIVE.removeIf(e -> !e.step());
		}

		if (ACTIVE.isEmpty()) {
			return;
		}

		Draw d = draw(ctx);

		for (Effect e : ACTIVE) {
			e.items(d, e.time());
		}
	}

	private static void render(WorldRenderContext ctx) {
		if (ACTIVE.isEmpty() && Particles.count() == 0 && !Ambient.any()) {
			return;
		}

		Draw d = draw(ctx);
		Ambient.render(d);

		for (Effect e : ACTIVE) {
			e.render(d, e.time());
		}

		Particles.render(d);
	}
}
