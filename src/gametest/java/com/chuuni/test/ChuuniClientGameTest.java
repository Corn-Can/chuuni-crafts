package com.chuuni.test;

import java.util.Locale;
import java.util.function.Consumer;

import com.chuuni.Chuuni;
import com.chuuni.client.fx.Cinematic;
import com.chuuni.client.fx.Effects;
import com.chuuni.client.fx.Particles;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 中二工坊驗收：在高空平台上擺好各種方塊，用真實的伺服器端操作（開介面、放材料、點按鈕 / 取成品）觸發，
 * 在演出的各個時間點截圖（build/run/clientGameTest/screenshots）。-Ponly=Enchant|Anvil|Craft|Map|Furnace|Door 只跑其中一段。
 */
public class ChuuniClientGameTest implements FabricClientGameTest {
	private static final int Y = 100;
	private static final BlockPos ENCHANT = new BlockPos(0, Y, 0);
	private static final BlockPos ANVIL = new BlockPos(14, Y, 0);
	private static final BlockPos CRAFT = new BlockPos(-14, Y, 0);
	private static final BlockPos MAP = new BlockPos(0, Y, 14);
	private static final BlockPos FURNACE = new BlockPos(-14, Y, 14);
	private static final BlockPos BLAST = new BlockPos(-17, Y, 14);
	private static final BlockPos SMOKER = new BlockPos(-20, Y, 14);
	private static final BlockPos DOOR = new BlockPos(14, Y, 14);

	/** B 級：每個測項一個方塊，兩排（z = 62 / 74），x = 2 + 4 * i */
	private static final String[] B_PARTS = {
		"Cauldron", "Lava", "Wax", "Scrape", "Candle", "Campfire", "Food", "Douse", "Stonecutter", "Loom", "Crafter", "Jukebox", "Lectern", "Bell",
		"Compost", "Harvest", "Honey", "Comb", "Pumpkin", "Spawner", "Ender", "Shulker", "Golem", "Dye", "Glow", "Cake", "Wash", "Tnt"};
	private static final String[] B_BLOCKS = {
		"cauldron", "cauldron", "copper_block", "oxidized_copper", "candle[candles=3]", "campfire[lit=false]", "campfire[lit=true]", "campfire[lit=true]",
		"stonecutter[facing=south]", "loom[facing=south]", "crafter[orientation=south_up]", "jukebox", "lectern[facing=south]", "bell[attachment=floor,facing=north]",
		"composter[level=6]", "composter[level=8]", "beehive[facing=south,honey_level=5]", "bee_nest[facing=south,honey_level=5]", "pumpkin", "spawner",
		"ender_chest[facing=south]", "magenta_shulker_box", "copper_golem_statue[facing=south]", "oak_sign[rotation=0]" + ChuuniClientGameTest.SIGN_TEXT, "oak_sign[rotation=0]" + ChuuniClientGameTest.SIGN_TEXT, "cake",
		"water_cauldron[level=3]", "tnt"};

	/** 空白告示牌不能染色，先寫上四行字 */
	private static final String SIGN_TEXT = "{front_text:{messages:['Chuuni','Crafts','B-Tier','Test']}}";

	/** C 級：z = 90 一排，x = 2 + 3 * i */
	private static final String[] C_PARTS = {
		"Chest", "Barrel", "Hopper", "Dispenser", "Bookshelf", "Shelf", "Pot", "FlowerPot", "Note", "Lever", "Button", "Repeater", "Comparator",
		"Daylight", "Strip", "Till", "Path", "Bonemeal", "Berry"};
	private static final String[] C_BLOCKS = {
		"chest[facing=south]", "barrel[facing=up]", "hopper", "dispenser[facing=south]", "chiseled_bookshelf[facing=south]", "oak_shelf[facing=south]",
		"decorated_pot", "flower_pot", "note_block", "lever[face=floor,facing=south]", "stone_button[face=floor,facing=south]", "repeater[facing=north]",
		"comparator[facing=north]", "daylight_detector", "oak_log", "dirt", "grass_block", "grass_block", "sweet_berry_bush[age=3]"};

	/** 生物互動：兩排（z = 104 / 112），x = 2 + 5 * i */
	private static final String[] D_PARTS = {
		"Golem", "SnowGolem", "CopperGolem", "Tame", "NameTag", "Shear", "Milk", "Equip", "DyeMob", "Armadillo", "Allay", "BucketMob", "Barter",
		"EndCrystal", "Cure", "Trade", "ArmorStand", "ItemFrame", "Love", "Leash", "Ride", "Bottle", "Bucket", "Mud", "Wire", "Ore", "Creeper", "Wither"};

	private static BlockPos dPos(int i) {
		return new BlockPos(2 + 5 * (i % 14), Y, i < 14 ? 104 : 112);
	}

	private static <T extends net.minecraft.world.entity.Entity> T spawn(ServerPlayer pl, net.minecraft.world.entity.EntityType<T> type, BlockPos at) {
		T e = type.create(pl.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);

		if (e instanceof net.minecraft.world.entity.Mob mob) {
			mob.setNoAi(true);
			mob.setYRot(0);
			mob.setYBodyRot(0);
			mob.setYHeadRot(0);
		}

		pl.level().addFreshEntity(e);
		return e;
	}

	private static void interact(ServerPlayer pl, net.minecraft.world.entity.Entity e, ItemStack held) {
		var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
		pl.setItemInHand(hand, held);
		var r = pl.interactOn(e, hand);
		log("interact {} with {} -> {}", e.getType().toShortString(), held.getItem(), r.getClass().getSimpleName());
	}

	private static void dTier(int i, ClientGameTestContext context, TestSingleplayerContext world) {
		String part = D_PARTS[i];
		BlockPos at = dPos(i);
		String key = "cc_d_" + part.toLowerCase(Locale.ROOT);
		boolean big = part.equals("Golem") || part.equals("Wither");
		tp(world, at.getX() + 0.5, Y, at.getZ() + (big ? 6.5 : 3.6), 180, big ? 8 : 18);
		context.waitTicks(6);
		world.getClientWorld().waitForChunksRender();
		int x = at.getX(), z = at.getZ();
		var srv = world.getServer();
		// 這一區的地板（開場時區塊還沒載入，fill 會失敗）
		srv.runCommand(String.format(Locale.ROOT, "fill %d 99 %d %d 99 %d polished_deepslate", x - 3, z - 3, x + 3, z + 8));
		srv.runCommand(String.format(Locale.ROOT, "fill %d 100 %d %d 106 %d air", x - 3, z - 3, x + 3, z + 8));
		context.waitTicks(2);
		tp(world, at.getX() + 0.5, Y, at.getZ() + (big ? 6.5 : 3.6), 180, big ? 8 : 18);
		context.waitTicks(4);

		switch (part) {
			case "Golem" -> {
				srv.runCommand(String.format(Locale.ROOT, "fill %d 100 %d %d 101 %d iron_block", x, z, x, z));
				srv.runCommand(String.format(Locale.ROOT, "fill %d 101 %d %d 101 %d iron_block", x - 1, z, x + 1, z));
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 102 %d carved_pumpkin[facing=south]", x, z));
			}
			case "SnowGolem" -> {
				srv.runCommand(String.format(Locale.ROOT, "fill %d 100 %d %d 101 %d snow_block", x, z, x, z));
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 102 %d carved_pumpkin[facing=south]", x, z));
			}
			case "CopperGolem" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 100 %d copper_block", x, z));
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 101 %d carved_pumpkin[facing=south]", x, z));
			}
			case "Wither" -> {
				srv.runCommand("difficulty normal");
				srv.runCommand(String.format(Locale.ROOT, "fill %d 100 %d %d 101 %d soul_sand", x, z, x, z));
				srv.runCommand(String.format(Locale.ROOT, "fill %d 101 %d %d 101 %d soul_sand", x - 1, z, x + 1, z));
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 102 %d wither_skeleton_skull", x - 1, z));
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 102 %d wither_skeleton_skull", x, z));
				server(world, pl -> click(pl, new BlockPos(x + 1, 101, z), Direction.UP, new ItemStack(Items.WITHER_SKELETON_SKULL)));
			}
			case "Tame" -> server(world, pl -> {
				var wolf = spawn(pl, net.minecraft.world.entity.EntityType.WOLF, at);

				for (int k = 0; k < 30 && !wolf.isTame(); k++) {
					interact(pl, wolf, new ItemStack(Items.BONE));
				}
			});
			case "NameTag" -> server(world, pl -> {
				ItemStack tag = new ItemStack(Items.NAME_TAG);
				tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Chuuni"));
				interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.PIG, at), tag);
			});
			case "Shear" -> server(world, pl -> {
				var sheep = spawn(pl, net.minecraft.world.entity.EntityType.SHEEP, at);
				sheep.setColor(net.minecraft.world.item.DyeColor.PINK);
				interact(pl, sheep, new ItemStack(Items.SHEARS));
			});
			case "Milk" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.COW, at), new ItemStack(Items.BUCKET)));
			case "Equip" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.PIG, at), new ItemStack(Items.SADDLE)));
			case "DyeMob" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.SHEEP, at), new ItemStack(Items.BLUE_DYE)));
			case "Armadillo" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.ARMADILLO, at), new ItemStack(Items.BRUSH)));
			case "Allay" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.ALLAY, at.above()), new ItemStack(Items.DIAMOND)));
			case "BucketMob" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.AXOLOTL, at), new ItemStack(Items.WATER_BUCKET)));
			case "Barter" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.PIGLIN, at), new ItemStack(Items.GOLD_INGOT)));
			case "Creeper" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.CREEPER, at), new ItemStack(Items.FLINT_AND_STEEL)));
			case "EndCrystal" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 100 %d obsidian", x, z));
				server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.END_CRYSTAL)));
			}
			case "Cure" -> server(world, pl -> {
				var zv = spawn(pl, net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, at);
				zv.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 600));
				interact(pl, zv, new ItemStack(Items.GOLDEN_APPLE));
			});
			case "Trade" -> {
				server(world, pl -> {
					var v = spawn(pl, net.minecraft.world.entity.EntityType.VILLAGER, at);
					var offers = new net.minecraft.world.item.trading.MerchantOffers();
					offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.WHEAT, 20),
						new ItemStack(Items.EMERALD), 12, 2, 0.05f));
					v.setOffers(offers);
					interact(pl, v, ItemStack.EMPTY);
					AbstractContainerMenu menu = pl.containerMenu;
					log("trade menu: {}", menu.getClass().getSimpleName());
					menu.getSlot(0).set(new ItemStack(Items.WHEAT, 40));
					menu.slotsChanged(menu.getSlot(0).container);
					log("trade result: {}", menu.getSlot(2).getItem());
					menu.clicked(2, 0, ClickType.PICKUP, pl);
					log("trade carried: {}", menu.getCarried());
				});
				context.waitTicks(10);
				context.runOnClient(mc -> {
					log("client screen before close: {}", mc.screen == null ? null : mc.screen.getClass().getSimpleName());
					mc.player.closeContainer();
				});
			}
			case "ArmorStand" -> server(world, pl -> {
				var stand = spawn(pl, net.minecraft.world.entity.EntityType.ARMOR_STAND, at);
				var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
				pl.setItemInHand(hand, new ItemStack(Items.DIAMOND_CHESTPLATE));
				log("armor stand -> {}", stand.interactAt(pl, new Vec3(0, 1.2, 0), hand).getClass().getSimpleName());
			});
			case "ItemFrame" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 101 %d stone", x, z));
				server(world, pl -> {
				var frame = new net.minecraft.world.entity.decoration.ItemFrame(pl.level(), new BlockPos(x, 101, z + 1), Direction.SOUTH);
				pl.level().addFreshEntity(frame);
				interact(pl, frame, new ItemStack(Items.DIAMOND_SWORD));
				});
			}
			case "Love" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.COW, at), new ItemStack(Items.WHEAT)));
			case "Leash" -> server(world, pl -> interact(pl, spawn(pl, net.minecraft.world.entity.EntityType.PIG, at), new ItemStack(Items.LEAD)));
			case "Ride" -> server(world, pl -> {
				var boat = spawn(pl, net.minecraft.world.entity.EntityType.OAK_BOAT, at);
				log("ride -> {}", pl.startRiding(boat));
			});
			case "Bottle", "Bucket" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 99 %d water", x, z));
				tp(world, x + 0.5, Y, z + 1.6, 180, 50);
				context.waitTicks(4);
				server(world, pl -> {
					var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
					ItemStack held = new ItemStack(part.equals("Bottle") ? Items.GLASS_BOTTLE : Items.BUCKET);
					pl.setItemInHand(hand, held);
					log("{} -> {}", part, held.getItem().use(pl.level(), pl, hand).getClass().getSimpleName());
				});
			}
			case "Mud" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 100 %d dirt", x, z));
				server(world, pl -> click(pl, at, Direction.UP, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
					net.minecraft.world.item.alchemy.Potions.WATER)));
			}
			case "Wire" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 100 %d redstone_wire", x, z));
				server(world, pl -> click(pl, at, Direction.UP, ItemStack.EMPTY));
			}
			case "Ore" -> {
				srv.runCommand(String.format(Locale.ROOT, "setblock %d 100 %d redstone_ore", x, z));
				server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.STICK)));
			}
			default -> log("dTier: unknown part {}", part);
		}

		shots(context, key, big ? new int[]{10, 30, 52, 60, 76, 96, 116} : new int[]{3, 8, 16, 26, 38}, big && run(part));

		switch (part) {
			case "Creeper" -> srv.runCommand("kill @e[type=creeper]");
			case "Wither" -> srv.runCommand("kill @e[type=wither]");
			case "Ride" -> server(world, ServerPlayer::stopRiding);
			default -> {
			}
		}

		context.waitTicks(8);
	}

	private static BlockPos cPos(int i) {
		return new BlockPos(2 + 3 * i, Y, 90);
	}

	private static BlockPos bPos(int i) {
		return new BlockPos(2 + 4 * (i % 14), Y, i < 14 ? 62 : 74);
	}

	private static void log(String fmt, Object... args) {
		Chuuni.LOGGER.info("[CC-TEST] " + fmt, args);
	}

	private static boolean run(String part) {
		String only = System.getProperty("chuuni.only");
		return only == null || java.util.Arrays.stream(only.split(",")).anyMatch(part::equalsIgnoreCase);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(s -> s.setAllowCommands(true)).create()) {
			world.getClientWorld().waitForChunksRender();

			for (String cmd : new String[]{"gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false",
				"time set 13200", "weather clear", "gamemode creative @a"}) {
				world.getServer().runCommand(cmd);
			}

			build(world);
			context.waitTicks(20);
			world.getClientWorld().waitForChunksRender();
			context.runOnClient(mc -> mc.options.hideGui = false);

			if (run("Settings")) {
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.SettingsScreen(null)));
				context.waitTicks(10);
				context.takeScreenshot("cc_settings_screen");
				context.runOnClient(mc -> {
					mc.setScreen(null);
					com.chuuni.client.fx.Settings.cinematic = false;
					com.chuuni.client.fx.Settings.save();
				});
				context.waitTicks(5);
				enchant(context, world);
				server(world, pl -> log("no-cinematic enchant: slot0={} inventory swords={}", pl.containerMenu.getSlot(0).getItem(),
					pl.getInventory().countItem(Items.DIAMOND_SWORD)));
				context.runOnClient(mc -> {
					com.chuuni.client.fx.Settings.cinematic = true;
					com.chuuni.client.fx.Settings.save();
				});
			}

			if (run("Enchant")) {
				enchant(context, world);
			}

			if (run("Anvil")) {
				anvil(context, world);
			}

			if (run("Craft")) {
				craft(context, world);
			}

			if (run("Map")) {
				cartography(context, world);
			}

			if (run("Furnace")) {
				furnace(context, world);
			}

			if (run("Door")) {
				door(context, world);
			}

			for (String part : new String[]{"Smithing", "Grindstone", "Brewing", "Beacon", "Portal", "End", "Vault", "Bed", "Egg", "Brush", "Lodestone", "Anchor"}) {
				if (run(part)) {
					aTier(part, context, world);
				}
			}

			for (int i = 0; i < D_PARTS.length; i++) {
				if (run(D_PARTS[i]) || run("DTier") && !D_PARTS[i].equals("Wither")) {
					dTier(i, context, world);
				}
			}

			for (int i = 0; i < C_PARTS.length; i++) {
				if (run(C_PARTS[i]) || run("CTier")) {
					cTier(i, context, world);
				}
			}

			for (int i = 0; i < B_PARTS.length; i++) {
				if (run(B_PARTS[i]) || run("BTier")) {
					bTier(i, context, world);
				}
			}

			// 新增：鐘十二響、書架常駐光、珍珠、終界之眼、伺服器鎖設定、爆炸（爆炸會炸壞地板，放最後）
			for (String part : new String[]{"Bell12", "Books", "Pearl", "Eye", "Lock", "Explode"}) {
				if (run(part) || run("Extra")) {
					extra(part, context, world);
				}
			}

			// 投擲物、戰鬥、事件、設定畫面
			for (String part : new String[]{"Screens", "Snowball", "Potion", "Arrow", "Trident", "Wind", "Firework", "Crit", "Sweep", "Totem", "Boss", "LevelUp",
				"Advance", "Challenge", "Elytra", "Riptide", "Sleep"}) {
				if (run(part) || run("New")) {
					fresh(part, context, world);
				}
			}
		}
	}

	// ------------------------------------------------------------------ 場景

	private static void build(TestSingleplayerContext world) {
		String[] cmds = {
			"fill -24 99 -8 22 99 22 polished_deepslate",
			"fill -24 100 -8 22 106 22 air",
			"fill 23 99 -8 80 99 50 polished_deepslate",
			"fill 23 100 -8 80 104 20 air",
			"fill 23 100 21 80 104 50 air",
			"fill -24 99 23 22 99 50 polished_deepslate",
			// A 級
			"setblock 28 100 0 respawn_anchor",
			"setblock 28 100 14 smithing_table",
			"setblock 42 100 14 grindstone[face=floor,facing=south]",
			"setblock 56 100 14 brewing_stand",
			// 烽火台：4 層鐵塊金字塔（挖進地板下）
			"fill 36 96 -4 44 96 4 iron_block", "fill 37 97 -3 43 97 3 iron_block", "fill 38 98 -2 42 98 2 iron_block", "fill 39 99 -1 41 99 1 iron_block",
			"setblock 40 100 0 beacon",
			// 地獄門框（X 軸，內部 2×3）
			"fill 27 100 28 30 104 28 obsidian", "fill 28 101 28 29 103 28 air",
			// 終界門框：10 顆眼，東側留兩格
			"setblock 43 100 26 end_portal_frame[facing=south,eye=true]", "setblock 44 100 26 end_portal_frame[facing=south,eye=true]", "setblock 45 100 26 end_portal_frame[facing=south,eye=true]",
			"setblock 43 100 30 end_portal_frame[facing=north,eye=true]", "setblock 44 100 30 end_portal_frame[facing=north,eye=true]", "setblock 45 100 30 end_portal_frame[facing=north,eye=true]",
			"setblock 42 100 27 end_portal_frame[facing=east,eye=true]", "setblock 42 100 28 end_portal_frame[facing=east,eye=true]", "setblock 42 100 29 end_portal_frame[facing=east,eye=true]",
			"setblock 46 100 27 end_portal_frame[facing=west,eye=false]", "setblock 46 100 28 end_portal_frame[facing=west,eye=false]", "setblock 46 100 29 end_portal_frame[facing=west,eye=true]",
			"setblock 60 100 28 vault[facing=south]",
			"setblock 28 100 42 red_bed[facing=south,part=foot]", "setblock 28 100 41 red_bed[facing=south,part=head]",
			"setblock 44 100 42 dragon_egg",
			"setblock 60 100 42 suspicious_sand{LootTable:\"minecraft:archaeology/desert_pyramid\"}",
			"setblock 72 100 42 lodestone",
			// 附魔台 + 書架（南側留缺口給玩家）
			"setblock 0 100 0 enchanting_table",
			"fill -2 100 -2 2 101 -2 bookshelf", "fill -2 100 -1 -2 101 2 bookshelf", "fill 2 100 -1 2 101 2 bookshelf",
			"setblock 14 100 0 anvil",
			"setblock -14 100 0 crafting_table",
			// B 級的地板
			"fill -2 99 56 60 99 82 polished_deepslate",
			"fill -2 100 56 60 104 82 air",
			"fill -2 99 83 60 99 96 polished_deepslate",
			"fill -2 100 83 60 104 96 air",
			"fill -2 99 97 80 99 120 polished_deepslate",
			"fill -2 100 97 80 108 120 air",
			"setblock 0 100 14 cartography_table",
			"setblock -14 100 14 furnace[facing=south]",
			"setblock -17 100 14 blast_furnace[facing=south]",
			"setblock -20 100 14 smoker[facing=south]",
			// 門（石磚牆框）
			"fill 12 100 14 16 103 14 stone_bricks",
			"setblock 14 100 14 oak_door[half=lower,facing=south]",
			"setblock 14 101 14 oak_door[half=upper,facing=south]",
			"setblock 11 100 18 oak_trapdoor[facing=south,half=top]",
		};

		for (String c : cmds) {
			world.getServer().runCommand(c);
		}

		for (int i = 0; i < C_BLOCKS.length; i++) {
			BlockPos b = cPos(i);
			world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d %s", b.getX(), b.getY(), b.getZ(), C_BLOCKS[i]));
		}

		for (int i = 0; i < B_BLOCKS.length; i++) {
			BlockPos b = bPos(i);
			world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d %s", b.getX(), b.getY(), b.getZ(), B_BLOCKS[i]));
		}
	}

	/** 用真實的伺服器端流程點方塊（方塊的 useItemOn → 物品的 useOn → 方塊的 useWithoutItem），點在指定的面中央 */
	private static void click(ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
		var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
		player.setItemInHand(hand, held);
		Vec3 at = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		BlockHitResult hit = new BlockHitResult(at, face, pos, false);
		var r = player.gameMode.useItemOn(player, player.level(), held, hand, hit);
		log("click {} {} with {} -> {} | now {}", pos, face, held.getItem(), r.getClass().getSimpleName(), player.level().getBlockState(pos));
	}

	private static void cTier(int i, ClientGameTestContext context, TestSingleplayerContext world) {
		String part = C_PARTS[i];
		BlockPos at = cPos(i);
		String key = "cc_c_" + part.toLowerCase(Locale.ROOT);
		tp(world, at.getX() + 0.5, Y, at.getZ() + 2.4, 180, 30);
		context.waitTicks(6);
		world.getClientWorld().waitForChunksRender();

		switch (part) {
			case "Chest", "Barrel", "Hopper", "Dispenser" -> {
				server(world, pl -> use(pl, at));
				context.waitTicks(4);
				close(world);
			}
			case "Bookshelf" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.BOOK)));
			case "Shelf" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.APPLE)));
			case "Pot" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.DIAMOND)));
			case "FlowerPot" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.POPPY)));
			case "Note" -> server(world, pl -> click(pl, at, Direction.SOUTH, ItemStack.EMPTY));
			case "Lever", "Button", "Repeater", "Comparator", "Daylight" -> server(world, pl -> click(pl, at, Direction.UP, ItemStack.EMPTY));
			case "Strip" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.DIAMOND_AXE)));
			case "Till" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.DIAMOND_HOE)));
			case "Path" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.DIAMOND_SHOVEL)));
			case "Bonemeal" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.BONE_MEAL)));
			case "Berry" -> server(world, pl -> click(pl, at, Direction.SOUTH, ItemStack.EMPTY));
			default -> log("cTier: unknown part {}", part);
		}

		shots(context, key, new int[]{2, 5, 9, 14}, false);
		context.waitTicks(6);
	}

	private static void bTier(int i, ClientGameTestContext context, TestSingleplayerContext world) {
		String part = B_PARTS[i];
		BlockPos at = bPos(i);
		String key = "cc_b_" + part.toLowerCase(Locale.ROOT);
		tp(world, at.getX() + 0.5, Y, at.getZ() + 3.3, 180, 22);
		context.waitTicks(8);

		switch (part) {
			case "Cauldron" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.WATER_BUCKET)));
			case "Lava" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.LAVA_BUCKET)));
			case "Wash" -> server(world, pl -> {
				ItemStack dyed = new ItemStack(Items.LEATHER_CHESTPLATE);
				dyed.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0xFF2020));
				click(pl, at, Direction.UP, dyed);
			});
			case "Wax" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.HONEYCOMB)));
			case "Scrape" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.DIAMOND_AXE)));
			case "Candle", "Campfire" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.FLINT_AND_STEEL)));
			case "Food" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.BEEF)));
			case "Douse" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.DIAMOND_SHOVEL)));
			case "Tnt" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.FLINT_AND_STEEL)));
			case "Jukebox" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.MUSIC_DISC_CAT)));
			case "Lectern" -> server(world, pl -> click(pl, at, Direction.UP, new ItemStack(Items.WRITABLE_BOOK)));
			case "Bell" -> server(world, pl -> {
				boolean r = ((net.minecraft.world.level.block.BellBlock)net.minecraft.world.level.block.Blocks.BELL).attemptToRing(pl, pl.level(), at, Direction.SOUTH);
				log("bell -> {}", r);
			});
			case "Compost" -> server(world, pl -> {
				for (int k = 0; k < 40 && pl.level().getBlockState(at).getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) < 7; k++) {
					click(pl, at, Direction.UP, new ItemStack(Items.WHEAT_SEEDS));
				}
			});
			case "Harvest", "Golem", "Cake" -> server(world, pl -> click(pl, at, Direction.UP, ItemStack.EMPTY));
			case "Honey" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.GLASS_BOTTLE)));
			case "Comb", "Pumpkin" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.SHEARS)));
			case "Spawner" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.ZOMBIE_SPAWN_EGG)));
			case "Ender", "Shulker" -> server(world, pl -> click(pl, at, Direction.SOUTH, ItemStack.EMPTY));
			case "Dye" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.RED_DYE)));
			case "Glow" -> server(world, pl -> click(pl, at, Direction.SOUTH, new ItemStack(Items.GLOW_INK_SAC)));
			case "Stonecutter" -> server(world, pl -> {
				use(pl, at);
				AbstractContainerMenu menu = pl.containerMenu;
				menu.getSlot(0).set(new ItemStack(Items.STONE, 16));
				menu.slotsChanged(menu.getSlot(0).container);
				menu.clickMenuButton(pl, 0);
				log("stonecutter result: {}", menu.getSlot(1).getItem());
				menu.clicked(1, 0, ClickType.QUICK_MOVE, pl);
			});
			case "Loom" -> server(world, pl -> {
				use(pl, at);
				AbstractContainerMenu menu = pl.containerMenu;
				menu.getSlot(0).set(new ItemStack(Items.WHITE_BANNER));
				menu.getSlot(1).set(new ItemStack(Items.RED_DYE));
				menu.slotsChanged(menu.getSlot(0).container);
				menu.clickMenuButton(pl, 1);
				log("loom result: {}", menu.getSlot(3).getItem());
				menu.clicked(3, 0, ClickType.QUICK_MOVE, pl);
			});
			case "Crafter" -> {
				world.getServer().runCommand(String.format(Locale.ROOT, "item replace block %d %d %d container.0 with oak_log", at.getX(), at.getY(), at.getZ()));
				world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d redstone_block", at.getX() + 1, at.getY(), at.getZ()));
			}
			default -> log("bTier: unknown part {}", part);
		}

		shots(context, key, part.equals("Crafter") ? new int[]{6, 9, 13, 18, 26, 36} : new int[]{3, 8, 14, 22, 32, 44}, false);

		if (part.equals("Tnt")) {
			world.getServer().runCommand("kill @e[type=tnt]");
		}

		close(world);
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ 各方塊

	private static void enchant(ClientGameTestContext context, TestSingleplayerContext world) {
		// 三個按鈕 = 三種花費 = 三階演出
		int[][] ticks = {
			{14, 34, 55, 70, 90, 115, 140, 165, 185},
			{14, 34, 55, 70, 90, 115, 140, 165, 185},
			{14, 34, 55, 62, 72, 90, 110, 135, 160, 185, 200}
		};

		for (int b = 0; b < 3; b++) {
			int button = b;
			tp(world, 0.5, Y, 3.2, 180, 25);
			context.waitTicks(10);
			server(world, player -> {
				use(player, ENCHANT);
				AbstractContainerMenu menu = player.containerMenu;
				player.experienceLevel = 60;
				menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
				menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));
				menu.slotsChanged(menu.getSlot(0).container);
			});
			context.waitTicks(10);
			server(world, player -> {
				int cost = ((net.minecraft.world.inventory.EnchantmentMenu)player.containerMenu).costs[button];
				boolean ok = player.containerMenu.clickMenuButton(player, button);
				log("enchant button {} cost {} click: {}", button, cost, ok);
			});
			shots(context, "cc_enchant_t" + (b + 1), ticks[b], true);
			context.waitTicks(30);
			context.takeScreenshot("cc_enchant_t" + (b + 1) + "_99_after");
			server(world, player -> logDrops(player, ENCHANT));
			close(world);
			context.waitTicks(10);
		}
	}

	private static void anvil(ClientGameTestContext context, TestSingleplayerContext world) {
		// 第一次：一般改名（輕鍛）；第二次：強度全開（重鍛：漫畫分格時停）
		for (int run = 0; run < 2; run++) {
			boolean heavy = run == 1;
			context.runOnClient(mc -> com.chuuni.client.fx.Settings.intensity = heavy ? 2 : 1);
			tp(world, 14.5, Y, 3.2, 180, 25);
			context.waitTicks(10);
			server(world, player -> {
				use(player, ANVIL);
				AnvilMenu menu = (AnvilMenu)player.containerMenu;
				menu.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
				menu.setItemName("天叢雲劍");
				menu.slotsChanged(menu.getSlot(0).container);
				log("anvil result: {}", menu.getSlot(2).getItem());
				menu.clicked(2, 0, ClickType.PICKUP, player);
			});
			int[] ticks = heavy ? new int[]{30, 58, 64, 75, 92, 110, 130, 160, 185, 205} : new int[]{12, 30, 42, 55, 62, 72, 90, 105, 125, 145, 165};
			shots(context, heavy ? "cc_anvil_heavy" : "cc_anvil", ticks, true);
			context.waitTicks(30);
			context.takeScreenshot(heavy ? "cc_anvil_heavy_99_after" : "cc_anvil_99_after");
			server(world, player -> player.containerMenu.setCarried(ItemStack.EMPTY));
			close(world);
			context.waitTicks(10);
		}

		context.runOnClient(mc -> com.chuuni.client.fx.Settings.intensity = 1);
	}

	private static void craft(ClientGameTestContext context, TestSingleplayerContext world) {
		tp(world, -13.5, Y, 3.2, 180, 30);
		context.waitTicks(10);
		server(world, player -> {
			use(player, CRAFT);
			AbstractContainerMenu menu = player.containerMenu;
			menu.getSlot(5).set(new ItemStack(Items.OAK_LOG, 6));
			menu.slotsChanged(menu.getSlot(5).container);
			log("craft result: {}", menu.getSlot(0).getItem());
			// shift 量產 → COMBO
			menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
		});
		shots(context, "cc_craft", new int[]{8, 22, 34, 41, 48, 60, 72}, true);
		context.waitTicks(20);
		close(world);

		// 背包 2×2：掌中構築
		server(world, player -> {
			player.inventoryMenu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, 1));
			player.inventoryMenu.slotsChanged(player.inventoryMenu.getSlot(1).container);
			player.inventoryMenu.clicked(0, 0, ClickType.PICKUP, player);
		});
		shots(context, "cc_craft_hand", new int[]{6, 12}, false);
		server(world, player -> player.containerMenu.setCarried(ItemStack.EMPTY));
		context.waitTicks(20);
	}

	private static void cartography(ClientGameTestContext context, TestSingleplayerContext world) {
		tp(world, 0.5, Y, 17.6, 180, 25);
		context.waitTicks(10);
		server(world, player -> {
			use(player, MAP);
			AbstractContainerMenu menu = player.containerMenu;
			ItemStack map = MapItem.create((ServerLevel)player.level(), 0, 14, (byte)0, true, false);
			menu.getSlot(0).set(map);
			menu.getSlot(1).set(new ItemStack(Items.MAP));
			menu.slotsChanged(menu.getSlot(0).container);
		});
		context.waitTicks(4);
		server(world, player -> {
			log("map result: {}", player.containerMenu.getSlot(2).getItem());
			player.containerMenu.clicked(2, 0, ClickType.PICKUP, player);
		});
		shots(context, "cc_map", new int[]{10, 30, 50, 57, 64, 80, 95, 110, 125, 140}, true);
		context.waitTicks(30);
		context.takeScreenshot("cc_map_99_after");
		context.waitTicks(20);
		server(world, player -> player.containerMenu.setCarried(ItemStack.EMPTY));
		close(world);
	}

	private static void furnace(ClientGameTestContext context, TestSingleplayerContext world) {
		// 取出成品
		tp(world, -13.5, Y, 17.6, 180, 20);
		context.waitTicks(10);
		server(world, player -> {
			AbstractFurnaceBlockEntity be = (AbstractFurnaceBlockEntity)player.level().getBlockEntity(FURNACE);
			be.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
			use(player, FURNACE);
			player.containerMenu.clicked(2, 0, ClickType.QUICK_MOVE, player);
		});
		shots(context, "cc_furnace", new int[]{8, 26, 45, 52, 60, 75, 92, 110, 125}, true);
		context.waitTicks(30);
		context.takeScreenshot("cc_furnace_99_after");
		server(world, player -> logDrops(player, FURNACE));
		context.waitTicks(20);
		close(world);

		// 高爐煉製中（每件冒出火之精靈）
		tp(world, -16.5, Y + 0.5, 18.2, 180, 15);
		server(world, player -> {
			AbstractFurnaceBlockEntity be = (AbstractFurnaceBlockEntity)player.level().getBlockEntity(BLAST);
			be.setItem(0, new ItemStack(Items.RAW_IRON, 4));
			be.setItem(1, new ItemStack(Items.COAL, 4));
		});
		boolean seen = false;

		for (int i = 0; i < 160 && !seen; i++) {
			context.waitTick();
			seen = context.computeOnClient(mc -> Effects.count() > 0);
		}

		log("blast smelt effect seen: {}", seen);
		context.waitTicks(4);
		context.takeScreenshot("cc_blast_smelt_a");
		context.waitTicks(8);
		context.takeScreenshot("cc_blast_smelt_b");

		// 取出高爐成品（藍焰版）
		context.waitTicks(10);
		server(world, player -> {
			AbstractFurnaceBlockEntity be = (AbstractFurnaceBlockEntity)player.level().getBlockEntity(BLAST);
			be.setItem(2, new ItemStack(Items.IRON_INGOT, 3));
			use(player, BLAST);
			player.containerMenu.clicked(2, 0, ClickType.QUICK_MOVE, player);
		});
		shots(context, "cc_blast", new int[]{8, 26, 45, 52, 60, 75, 92, 110}, true);
		context.waitTicks(40);
		server(world, player -> logDrops(player, BLAST));
		close(world);

		// 煙燻爐取出（煙雲祕法）
		tp(world, -19.5, Y, 17.6, 180, 20);
		context.waitTicks(10);
		server(world, player -> {
			AbstractFurnaceBlockEntity be = (AbstractFurnaceBlockEntity)player.level().getBlockEntity(SMOKER);
			be.setItem(2, new ItemStack(Items.COOKED_BEEF, 5));
			use(player, SMOKER);
			player.containerMenu.clicked(2, 0, ClickType.QUICK_MOVE, player);
		});
		shots(context, "cc_smoker", new int[]{8, 26, 45, 52, 60, 75, 92, 110}, true);
		context.waitTicks(40);
		context.takeScreenshot("cc_smoker_99_after");
		server(world, player -> logDrops(player, SMOKER));
		close(world);
	}

	private static void door(ClientGameTestContext context, TestSingleplayerContext world) {
		tp(world, 14.5, Y, 17.2, 180, 10);
		context.waitTicks(10);

		for (int i = 0; i < 10; i++) {
			server(world, player -> useLooking(player, DOOR));
			context.waitTicks(3);

			if (i == 0) {
				context.takeScreenshot("cc_door_open_a");
				context.waitTicks(3);
				context.takeScreenshot("cc_door_open_b");
			}
			else if (i == 1) {
				context.takeScreenshot("cc_door_close_a");
			}
			else if (i == 4) {
				context.waitTicks(4);
				context.takeScreenshot("cc_door_combo5");
			}
			else if (i == 9) {
				context.waitTicks(4);
				context.takeScreenshot("cc_door_combo10");
			}
			else {
				context.waitTicks(4);
			}
		}

		context.waitTicks(30);
		tp(world, 11.5, Y + 0.5, 20.5, 180, 30);
		context.waitTicks(5);
		server(world, player -> useLooking(player, new BlockPos(11, Y, 18)));
		context.waitTicks(4);
		context.takeScreenshot("cc_trapdoor");
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ A 級

	private static void aTier(String part, ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ticks = {10, 28, 44, 52, 60, 76, 96, 116, 136};
		String key = "cc_" + part.toLowerCase(Locale.ROOT);
		var main = net.minecraft.world.InteractionHand.MAIN_HAND;

		switch (part) {
			case "Smithing" -> {
				BlockPos at = new BlockPos(28, Y, 14);
				tp(world, 28.5, Y, 17.2, 180, 25);
				context.waitTicks(10);
				server(world, player -> {
					use(player, at);
					AbstractContainerMenu menu = player.containerMenu;
					menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
					menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
					menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
					menu.slotsChanged(menu.getSlot(0).container);
				});
				context.waitTicks(2);
				server(world, player -> {
					log("smithing result: {}", player.containerMenu.getSlot(3).getItem());
					player.containerMenu.clicked(3, 0, ClickType.QUICK_MOVE, player);
				});
				shots(context, key, ticks, true);
				context.waitTicks(40);
				context.takeScreenshot(key + "_99_after");
				server(world, player -> logDrops(player, at));
				close(world);
			}
			case "Grindstone" -> {
				BlockPos at = new BlockPos(42, Y, 14);
				tp(world, 42.5, Y, 17.2, 180, 25);
				context.waitTicks(10);
				server(world, player -> {
					use(player, at);
					ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
					var reg = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
					sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 5);
					sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 3);
					AbstractContainerMenu menu = player.containerMenu;
					menu.getSlot(0).set(sword);
					menu.slotsChanged(menu.getSlot(0).container);
				});
				context.waitTicks(2);
				server(world, player -> player.containerMenu.clicked(2, 0, ClickType.PICKUP, player));
				shots(context, key, ticks, true);
				context.waitTicks(30);
				server(world, player -> player.containerMenu.setCarried(ItemStack.EMPTY));
				close(world);
			}
			case "Brewing" -> {
				BlockPos at = new BlockPos(56, Y, 14);
				tp(world, 56.5, Y, 17.2, 180, 25);
				context.waitTicks(10);
				server(world, player -> {
					var be = (net.minecraft.world.level.block.entity.BrewingStandBlockEntity)player.level().getBlockEntity(at);

					for (int i = 0; i < 3; i++) {
						be.setItem(i, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
					}

					use(player, at);

					for (int i = 0; i < 3; i++) {
						player.containerMenu.clicked(i, 0, ClickType.QUICK_MOVE, player);
					}
				});
				shots(context, key, ticks, true);
				context.waitTicks(30);
				close(world);
			}
			case "Beacon" -> {
				BlockPos at = new BlockPos(40, Y, 0);
				tp(world, 40.5, Y, 4.5, 180, 15);
				context.waitTicks(100);
				server(world, player -> {
					use(player, at);
					var menu = (net.minecraft.world.inventory.BeaconMenu)player.containerMenu;
					menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
					log("beacon levels {}", menu.getLevels());
					menu.updateEffects(java.util.Optional.of(net.minecraft.world.effect.MobEffects.SPEED), java.util.Optional.empty());
				});
				shots(context, key, new int[]{10, 28, 46, 54, 62, 80, 100, 120, 140}, true);
				context.waitTicks(30);
				close(world);
			}
			case "Portal" -> {
				tp(world, 29, Y, 33, 180, 10);
				context.waitTicks(10);
				world.getServer().runCommand("setblock 28 101 28 fire");
				shots(context, key, new int[]{10, 28, 46, 54, 62, 80, 100, 120, 140}, true);
				context.waitTicks(30);
			}
			case "End" -> {
				tp(world, 44.5, Y, 33.5, 180, 30);
				context.waitTicks(10);
				server(world, player -> eye(player, new BlockPos(46, Y, 27)));
				context.waitTicks(6);
				context.takeScreenshot(key + "_00_eye11");
				context.waitTicks(20);
				server(world, player -> eye(player, new BlockPos(46, Y, 28)));
				shots(context, key, new int[]{10, 30, 42, 50, 58, 66, 84, 104, 124, 144}, true);
				context.waitTicks(60);
				context.takeScreenshot(key + "_99_after");
				tp(world, 44.5, Y + 6, 40.5, 180, 30);
				context.waitTicks(10);
				context.takeScreenshot(key + "_99_after_far");
			}
			case "Vault" -> {
				BlockPos at = new BlockPos(60, Y, 28);
				tp(world, 60.5, Y, 31.2, 180, 20);
				context.waitTicks(10);
				server(world, player -> {
					player.setItemInHand(main, new ItemStack(Items.TRIAL_KEY));
					BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at).add(0, 0, 0.5), Direction.SOUTH, at, false);
					var r = player.level().getBlockState(at).useItemOn(player.getMainHandItem(), player.level(), player, main, hit);
					log("vault -> {}", r.getClass().getSimpleName());
				});
				shots(context, key, ticks, true);
				context.waitTicks(30);
			}
			case "Bed" -> {
				BlockPos at = new BlockPos(28, Y, 41);
				tp(world, 29.5, Y, 41.5, 90, 20);
				context.waitTicks(10);
				server(world, player -> use(player, at));
				shots(context, key, new int[]{10, 30, 50, 64, 80, 100, 120}, true);
				context.waitTicks(20);
				server(world, player -> player.stopSleeping());
			}
			case "Egg" -> {
				tp(world, 44.5, Y, 46, 180, 15);
				context.waitTicks(10);
				server(world, player -> use(player, new BlockPos(44, Y, 42)));
				shots(context, key, new int[]{6, 16, 24, 32, 44, 60, 80}, true);
				context.waitTicks(20);
			}
			case "Brush" -> {
				BlockPos at = new BlockPos(60, Y, 42);
				tp(world, 60.5, Y, 45, 180, 30);
				context.waitTicks(10);
				server(world, player -> {
					long now = player.level().getGameTime();

					for (int i = 0; i < 20 && player.level().getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity be; i++) {
						be.brush(now + i * 11L, (ServerLevel)player.level(), player, Direction.UP, new ItemStack(Items.BRUSH));
					}

					log("brush done: block now {}", player.level().getBlockState(at));
				});
				shots(context, key, ticks, true);
				context.waitTicks(40);
				context.takeScreenshot(key + "_99_after");
				server(world, player -> logDrops(player, at));
			}
			case "Lodestone" -> {
				BlockPos at = new BlockPos(72, Y, 42);
				tp(world, 72.5, Y, 45.2, 180, 25);
				context.waitTicks(10);
				server(world, player -> {
					player.setItemInHand(main, new ItemStack(Items.COMPASS));
					BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false);
					var r = Items.COMPASS.useOn(new net.minecraft.world.item.context.UseOnContext(player, main, hit));
					log("lodestone -> {}", r.getClass().getSimpleName());
				});
				shots(context, key, ticks, true);
				context.waitTicks(30);
			}
			case "Anchor" -> {
				BlockPos at = new BlockPos(28, Y, 0);
				tp(world, 28.5, Y, 3.2, 180, 25);
				context.waitTicks(10);

				for (int i = 0; i < 4; i++) {
					server(world, player -> {
						player.setItemInHand(main, new ItemStack(Items.GLOWSTONE, 4));
						BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false);
						player.level().getBlockState(at).useItemOn(player.getMainHandItem(), player.level(), player, main, hit);
					});
					context.waitTicks(i == 3 ? 10 : 6);
				}

				context.takeScreenshot(key + "_00_charge4");
				context.waitTicks(20);

				// 地獄裡設定重生點（契約）
				world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0.5 100 3.2 180 25");
				context.waitTicks(40);
				world.getServer().runCommand("execute in minecraft:the_nether run fill -4 99 -4 4 99 6 polished_blackstone");
				world.getServer().runCommand("execute in minecraft:the_nether run fill -4 100 -4 4 104 6 air");
				world.getServer().runCommand("execute in minecraft:the_nether run setblock 0 100 0 respawn_anchor[charges=4]");
				world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0.5 100 3.2 180 25");
				context.waitTicks(40);
				world.getClientWorld().waitForChunksRender();
				server(world, player -> {
					player.setItemInHand(main, ItemStack.EMPTY);
					use(player, new BlockPos(0, Y, 0));
				});
				shots(context, key, ticks, true);
				context.waitTicks(30);
				world.getServer().runCommand("execute in minecraft:overworld run tp @a 28.5 100 3.2 180 25");
				context.waitTicks(40);
				world.getClientWorld().waitForChunksRender();

				// 主世界用 → 爆炸
				server(world, player -> {
					player.setItemInHand(main, ItemStack.EMPTY);
					use(player, at);
				});
				context.waitTicks(8);
				context.takeScreenshot(key + "_98_blast");
			}
			default -> {
			}
		}
	}

	private static void eye(ServerPlayer player, BlockPos frame) {
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_EYE));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(frame).add(0, 0.4, 0), Direction.UP, frame, false);
		var r = Items.ENDER_EYE.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		log("eye {} -> {}", frame, r.getClass().getSimpleName());
	}


	private static void extra(String part, ClientGameTestContext context, TestSingleplayerContext world) {
		String key = "cc_x_" + part.toLowerCase(Locale.ROOT);
		var srv = world.getServer();

		switch (part) {
			case "Bell12" -> {
				BlockPos at = new BlockPos(30, Y, 120);
				srv.runCommand("fill 24 99 114 36 99 126 polished_deepslate");
				srv.runCommand("fill 24 100 114 36 108 126 air");
				srv.runCommand("setblock 30 100 120 bell[attachment=floor,facing=north]");
				tp(world, 30.5, Y, 126.5, 180, 15);
				context.waitTicks(10);
				world.getClientWorld().waitForChunksRender();

				for (int k = 1; k <= 12; k++) {
					server(world, pl -> ((net.minecraft.world.level.block.BellBlock)net.minecraft.world.level.block.Blocks.BELL).attemptToRing(pl, pl.level(), at,
						Direction.SOUTH));

					if (k == 1 || k == 6 || k == 9 || k == 11) {
						context.waitTicks(5);
						context.takeScreenshot(String.format(Locale.ROOT, "%s_toll%02d", key, k));
						context.waitTicks(25);
					}
					else if (k == 12) {
						shots(context, key + "_toll12", new int[]{4, 14, 20, 30, 50}, false);
					}
					else {
						context.waitTicks(30);
					}
				}

				context.waitTicks(40);
			}
			case "Books" -> {
				BlockPos at = new BlockPos(26, Y, 116);
				srv.runCommand("fill 24 99 114 36 99 126 polished_deepslate");
				srv.runCommand("fill 24 100 114 28 104 120 air");
				srv.runCommand("setblock 26 100 116 chiseled_bookshelf[facing=south]");
				srv.runCommand("setblock 27 100 116 chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_2_occupied=true,slot_4_occupied=true]");
				tp(world, 26.9, Y, 118.6, 180, 20);
				context.waitTicks(10);
				world.getClientWorld().waitForChunksRender();
				double[][] hits = {{0.2, 0.75}, {0.5, 0.25}, {0.8, 0.75}};

				for (double[] h : hits) {
					server(world, pl -> {
						pl.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
						Vec3 hit = new Vec3(at.getX() + h[0], at.getY() + h[1], at.getZ() + 1.0);
						var r = pl.gameMode.useItemOn(pl, pl.level(), pl.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
							new BlockHitResult(hit, Direction.SOUTH, at, false));
						log("book -> {} | {}", r.getClass().getSimpleName(), pl.level().getBlockState(at));
					});
					context.waitTicks(4);
					context.takeScreenshot(String.format(Locale.ROOT, "%s_put_%.1f_%.2f", key, h[0], h[1]));
					context.waitTicks(10);
				}

				context.waitTicks(30);
				context.takeScreenshot(key + "_idle");
			}
			case "Pearl" -> {
				srv.runCommand("fill 24 99 97 60 99 140 polished_deepslate");
				tp(world, 40.5, Y, 135.5, 180, 0);
				context.waitTicks(10);
				server(world, pl -> {
					var pearl = new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(pl.level(), pl, new ItemStack(Items.ENDER_PEARL));
					pearl.shootFromRotation(pl, -20, 180, 0, 1.2f, 0);
					pl.level().addFreshEntity(pearl);
				});
				shots(context, key, new int[]{3, 8, 14, 22, 30, 40}, false);
			}
			case "Eye" -> {
				tp(world, 40.5, Y, 135.5, 150, -10);
				context.waitTicks(10);
				server(world, pl -> {
					var eye = new net.minecraft.world.entity.projectile.EyeOfEnder(pl.level(), pl.getX(), pl.getEyeY() - 0.4, pl.getZ());
					eye.setItem(new ItemStack(Items.ENDER_EYE));
					eye.signalTo(new Vec3(pl.getX() - 200, pl.getY(), pl.getZ() - 400));
					pl.level().addFreshEntity(eye);
				});
				shots(context, key, new int[]{5, 15, 30, 50, 70, 82, 90}, false);
			}
			case "Lock" -> {
				// 走真的路徑：寫伺服器設定檔 → 讀檔組封包 → 送給客戶端 → 套用 + 鎖住設定畫面
				try {
					java.nio.file.Path f = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("chuuni-server.properties");
					java.nio.file.Files.writeString(f, "forceClient=true\nsound=false\ntierC=false\nshake=0.5\nenabled=\n");
				}
				catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}

				server(world, pl -> {
					com.chuuni.ServerConfig.load();
					net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(pl, com.chuuni.ServerConfig.payload(true));
				});
				context.waitTicks(5);
				String state = context.computeOnClient(mc -> "locked=" + com.chuuni.client.fx.Settings.locked() + " sound=" + com.chuuni.client.fx.Settings.sound
					+ " tierC=" + com.chuuni.client.fx.Settings.tierC + " shake=" + com.chuuni.client.fx.Settings.shake + " enabledForced="
					+ com.chuuni.client.fx.Settings.forced("enabled"));
				log("lock: {}", state);
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.SettingsScreen(null)));
				context.waitTicks(10);
				context.takeScreenshot(key + "_screen");
				context.runOnClient(mc -> mc.setScreen(null));

				try {
					java.nio.file.Path f = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("chuuni-server.properties");
					java.nio.file.Files.writeString(f, "forceClient=false\n");
				}
				catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}

				server(world, pl -> {
					com.chuuni.ServerConfig.load();
					net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(pl, com.chuuni.ServerConfig.payload(true));
				});
				context.waitTicks(5);
				String after = context.computeOnClient(mc -> "locked=" + com.chuuni.client.fx.Settings.locked() + " sound=" + com.chuuni.client.fx.Settings.sound
					+ " tierC=" + com.chuuni.client.fx.Settings.tierC + " shake=" + com.chuuni.client.fx.Settings.shake);
				log("unlock: {}", after);
			}
			case "Explode" -> {
				srv.runCommand("fill 24 99 130 60 99 150 polished_deepslate");
				tp(world, 40.5, Y, 148.5, 180, 10);
				context.waitTicks(10);
				srv.runCommand("summon tnt 40.5 100 138.5 {fuse:2}");
				shots(context, key, new int[]{3, 5, 8, 12, 20, 32, 50}, false);
			}
			default -> log("extra: unknown part {}", part);
		}
	}

	/** 從玩家的視線方向丟出一個投擲物 */
	private static void shoot(ServerPlayer pl, net.minecraft.world.entity.projectile.Projectile e, float pitch, float speed) {
		e.setPos(pl.getX(), pl.getEyeY() - 0.1, pl.getZ());
		e.shootFromRotation(pl, pitch, pl.getYRot(), 0, speed, 0);
		pl.level().addFreshEntity(e);
	}

	private static void fresh(String part, ClientGameTestContext context, TestSingleplayerContext world) {
		String key = "cc_n_" + part.toLowerCase(Locale.ROOT);
		var srv = world.getServer();
		srv.runCommand("fill 24 99 97 60 99 150 polished_deepslate");
		srv.runCommand("fill 24 100 97 60 112 150 air");
		srv.runCommand("kill @e[type=!player]");
		tp(world, 40.5, Y, 140.5, 180, 0);
		context.waitTicks(8);

		switch (part) {
			case "Screens" -> {
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.SettingsScreen(null)));
				context.waitTicks(5);
				context.takeScreenshot(key + "_main");
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.EffectsScreen(null, false)));
				context.waitTicks(5);
				context.takeScreenshot(key + "_each");
				// 管理員：先讓伺服器送一次（單人世界主人 = 管理員）
				server(world, pl -> com.chuuni.ServerConfig.set(pl.level().getServer(), "fx.explosion", "false"));
				context.waitTicks(5);
				String st = context.computeOnClient(mc -> "admin=" + com.chuuni.client.fx.Settings.serverAdmin + " explosionAllowed="
					+ com.chuuni.client.fx.Settings.allowed(com.chuuni.net.FxPayload.Kind.EXPLOSION) + " forced=" + com.chuuni.client.fx.Settings.forced("fx.explosion"));
				log("server fx.explosion=false: {}", st);
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.SettingsScreen(null)));
				context.waitTicks(5);
				context.takeScreenshot(key + "_main_admin");
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.EffectsScreen(null, true)));
				context.waitTicks(5);
				context.takeScreenshot(key + "_server_general");
				context.runOnClient(mc -> mc.setScreen(new com.chuuni.client.EffectsScreen(null, false)));
				context.waitTicks(5);
				context.takeScreenshot(key + "_each_locked");
				server(world, pl -> com.chuuni.ServerConfig.set(pl.level().getServer(), "fx.explosion", ""));
				context.waitTicks(5);
				st = context.computeOnClient(mc -> "explosionAllowed=" + com.chuuni.client.fx.Settings.allowed(com.chuuni.net.FxPayload.Kind.EXPLOSION));
				log("server fx.explosion=default: {}", st);
				context.runOnClient(mc -> mc.setScreen(null));
			}
			case "Snowball" -> {
				server(world, pl -> shoot(pl, new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(pl.level(), pl, new ItemStack(Items.SNOWBALL)),
					-15, 1.3f));
				shots(context, key, new int[]{3, 8, 14, 20, 28}, false);
			}
			case "Potion" -> {
				tp(world, 40.5, Y, 140.5, 180, 25);
				context.waitTicks(4);
				server(world, pl -> shoot(pl, new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion(pl.level(), pl,
					net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION)), -10,
					0.6f));
				shots(context, key, new int[]{3, 8, 14, 20, 30, 45}, false);
			}
			case "Arrow" -> {
				server(world, pl -> {
					var a = new net.minecraft.world.entity.projectile.arrow.Arrow(pl.level(), pl, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
					a.setCritArrow(true);
					shoot(pl, a, -2, 2.5f);
				});
				shots(context, key, new int[]{2, 5, 9, 14, 22}, false);
			}
			case "Trident" -> {
				srv.runCommand("setblock 40 100 128 polished_deepslate");
				server(world, pl -> shoot(pl, new net.minecraft.world.entity.projectile.arrow.ThrownTrident(pl.level(), pl, new ItemStack(Items.TRIDENT)), 2, 2f));
				shots(context, key, new int[]{2, 5, 9, 14, 24}, false);
			}
			case "Wind" -> {
				server(world, pl -> {
					var w = new net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge(pl, pl.level(), pl.getX(), pl.getEyeY() - 0.1, pl.getZ());
					w.shootFromRotation(pl, 10, pl.getYRot(), 0, 1.5f, 0);
					pl.level().addFreshEntity(w);
				});
				shots(context, key, new int[]{2, 5, 9, 14, 22}, false);
			}
			case "Firework" -> {
				tp(world, 40.5, Y, 146.5, 180, -72);
				context.waitTicks(4);
				server(world, pl -> {
					ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
					fw.set(net.minecraft.core.component.DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(1, java.util.List.of(
						new net.minecraft.world.item.component.FireworkExplosion(net.minecraft.world.item.component.FireworkExplosion.Shape.LARGE_BALL,
							it.unimi.dsi.fastutil.ints.IntList.of(0xFF4060), it.unimi.dsi.fastutil.ints.IntList.of(0xFFE080), true, true))));
					var r = new net.minecraft.world.entity.projectile.FireworkRocketEntity(pl.level(), pl, 40.5, Y + 0.5, 143.5, fw);
					pl.level().addFreshEntity(r);
				});
				shots(context, key, new int[]{5, 15, 25, 32, 38, 46, 60}, false);
			}
			case "Crit", "Sweep" -> {
				srv.runCommand("summon zombie 40.5 100 137.5 {NoAI:1b,Rotation:[0f,0f]}");
				context.waitTicks(5);
				server(world, pl -> {
					var z = pl.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, pl.getBoundingBox().inflate(8)).getFirst();
					pl.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));

					if (part.equals("Crit")) {
						pl.crit(z);
					}
					else {
						com.chuuni.Triggers.sweep(pl, z);
					}
				});
				shots(context, key, new int[]{2, 4, 7, 12}, false);
			}
			case "Totem" -> {
				tp(world, 40.5, Y, 140.5, 180, 0);
				srv.runCommand("summon armor_stand 40.5 100 137.5 {Rotation:[0f,0f]}");
				context.waitTicks(5);
				server(world, pl -> {
					var a = pl.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, pl.getBoundingBox().inflate(8)).getFirst();
					pl.level().broadcastEntityEvent(a, (byte)35);
					pl.level().broadcastEntityEvent(pl, (byte)35);
				});
				shots(context, key, new int[]{3, 8, 15, 25, 40}, false);
			}
			case "Boss" -> {
				srv.runCommand("summon elder_guardian 40.5 101 130.5 {NoAI:1b}");
				context.waitTicks(10);
				srv.runCommand("kill @e[type=elder_guardian]");
				shots(context, key, new int[]{3, 10, 20, 35, 60, 90}, false);
			}
			case "LevelUp" -> {
				tp(world, 40.5, Y, 140.5, 180, 30);
				context.waitTicks(4);
				srv.runCommand("xp set @a 9 levels");
				context.waitTicks(10);
				srv.runCommand("xp add @a 1 levels");
				shots(context, key, new int[]{3, 10, 20, 35}, false);
				srv.runCommand("xp add @a 1 levels");
				shots(context, key + "_small", new int[]{3, 12}, false);
			}
			case "Advance", "Challenge" -> {
				tp(world, 40.5, Y, 136.5, 0, 0);
				srv.runCommand("summon armor_stand 40.5 100 139.5 {Invisible:1b}");
				context.waitTicks(4);
				server(world, pl -> com.chuuni.Triggers.advancement(pl, part.equals("Challenge") ? 1 : 0, new ItemStack(part.equals("Challenge") ? Items.ELYTRA
					: Items.STONE_PICKAXE)));
				// 從第三人稱看自己頭上
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
				shots(context, key, new int[]{3, 10, 20, 35}, false);
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
				srv.runCommand("advancement grant @a only minecraft:story/mine_stone");
				context.waitTicks(5);
				String st = context.computeOnClient(mc -> "effects=" + Effects.count());
				log("real advancement grant: {}", st);
				context.waitTicks(40);
			}
			case "Elytra" -> {
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
				srv.runCommand("item replace entity @a armor.chest with elytra");
				srv.runCommand("tp @a 40.5 108 140.5 180 10");
				context.waitTicks(3);
				server(world, ServerPlayer::startFallFlying);
				shots(context, key, new int[]{3, 6, 10, 16}, false);
				srv.runCommand("item replace entity @a armor.chest with air");
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
				context.waitTicks(40);
			}
			case "Riptide" -> {
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
				server(world, pl -> pl.startAutoSpinAttack(20, 8f, new ItemStack(Items.TRIDENT)));
				shots(context, key, new int[]{3, 7, 12, 20}, false);
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			}
			case "Sleep" -> {
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
				server(world, com.chuuni.Triggers::sleep);
				shots(context, key, new int[]{5, 15, 30, 50}, false);
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			}
			default -> log("fresh: unknown part {}", part);
		}

		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ 工具

	/** 在 ticks 時間點截圖；cinematic 則記錄鏡頭狀態 */
	private static void shots(ClientGameTestContext context, String prefix, int[] ticks, boolean cinematic) {
		int done = 0;

		for (int i = 0; i < ticks.length; i++) {
			context.waitTicks(ticks[i] - done);
			done = ticks[i];
			context.takeScreenshot(String.format(Locale.ROOT, "%s_%02d_t%02d", prefix, i + 1, ticks[i]));

			if (i == 0) {
				String state = context.computeOnClient(mc -> "effects=" + Effects.count() + " cinematic=" + Cinematic.active() + " particles=" + Particles.count());
				log("{} @{}: {}", prefix, ticks[i], state);
			}
		}
	}

	private static void use(ServerPlayer player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		var result = player.level().getBlockState(pos).useWithoutItem(player.level(), player, hit);
		log("use {} -> {} menu={}", pos, result, player.containerMenu.getClass().getSimpleName());
	}

	/** 用玩家視線真的射向方塊，得到實際點到的位置與面（門的封印要貼在點到的面上） */
	private static void useLooking(ServerPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(Vec3.atCenterOf(pos).subtract(eye).scale(1.6));
		// 只對這個方塊的外形做射線判定（門打開後不會穿過門縫打到地板）
		BlockHitResult hit = player.level().getBlockState(pos).getShape(player.level(), pos).clip(eye, far, pos);

		if (hit == null) {
			hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.getApproximateNearest(eye.subtract(Vec3.atCenterOf(pos))), pos, false);
		}

		BlockPos at = hit.getBlockPos();
		var result = player.level().getBlockState(at).useWithoutItem(player.level(), player, hit);
		log("use(look) {} face {} at {} -> {}", at, hit.getDirection(), hit.getLocation(), result.getClass().getSimpleName());
	}

	/** 專屬掉落物：附近的物品實體與歸屬 */
	private static void logDrops(ServerPlayer player, BlockPos pos) {
		var items = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));

		for (var e : items) {
			Object target = null;

			try {
				var f = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("target");
				f.setAccessible(true);
				target = f.get(e);
			}
			catch (ReflectiveOperationException ex) {
				target = "?";
			}

			log("drop near {}: {} x{} self={} | still in inventory: {}", pos, e.getItem().getItem(), e.getItem().getCount(), player.getUUID().equals(target),
				player.getInventory().countItem(e.getItem().getItem()));
		}

		if (items.isEmpty()) {
			log("drop near {}: NONE", pos);
		}
	}

	private static void close(TestSingleplayerContext world) {
		server(world, ServerPlayer::closeContainer);
	}

	private static void server(TestSingleplayerContext world, Consumer<ServerPlayer> action) {
		world.getServer().runOnServer(server -> action.accept(player(server)));
	}

	private static void tp(TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch) {
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
