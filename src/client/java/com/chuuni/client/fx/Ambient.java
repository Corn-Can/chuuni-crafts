package com.chuuni.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.client.render.FxRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * 世界中的常駐光（不是一次性的演出，只要方塊在就一直亮）：
 * <ul>
 *   <li>終界門框嵌著眼 → 每顆眼一道細的虛空光柱（B 級）</li>
 *   <li>終界門已開啟 → 十二道光柱合成中央一道大光柱 + 緩緩旋轉的十二法陣（B 級）</li>
 *   <li>雕紋書架 → 有書的格子微微發光、偶爾飄出符文（C 級）</li>
 * </ul>
 * 每 10 tick 掃描玩家附近的區塊（只看調色盤可能含這些方塊的區段），新出現的光會「長出來」。
 */
public final class Ambient {
	private static final Identifier BEAM = FxRender.tex("beam");
	private static final Identifier GLOW = FxRender.tex("glow");
	private static final Identifier SEAL = FxRender.tex("circle_seal");
	private static final Identifier RING = FxRender.tex("ring");
	private static final int VOIDC = 0x60FFD0;
	private static final int INK = 0xC8A0FF;
	private static final int CHUNKS = 3;
	private static final double RANGE = 52;

	private record Shelf(BlockPos pos, Direction facing, int mask) {
	}

	private record Hold(Vec3 at, double[] until) {
	}

	private static List<BlockPos> eyes = List.of();
	private static List<Vec3> portals = List.of();
	private static List<Shelf> shelves = List.of();
	/** 第一次看到的時間（新出現的光從 0 長出來） */
	private static final Map<Long, Double> BORN = new HashMap<>();
	private static final List<Hold> HOLDS = new ArrayList<>();
	private static int timer;
	private static boolean firstScan = true;

	private Ambient() {
	}

	private static double now() {
		return Util.getMillis() / 1000.0;
	}

	/** 方塊剛變（放書、嵌眼）：3 tick 後重掃（方塊更新封包比演出封包晚到） */
	public static void rescan() {
		timer = Math.max(timer, 7);
	}

	/** 大演出進行中：at 附近 5 格內的常駐光先藏起來，直到 {@link #release(Vec3)} 後淡入 */
	public static void hold(Vec3 at) {
		HOLDS.add(new Hold(at, new double[]{now() + 30}));
	}

	public static void release(Vec3 at) {
		for (Hold h : HOLDS) {
			if (h.at.distanceToSqr(at) < 1) {
				h.until[0] = Math.min(h.until[0], now());
			}
		}
	}

	private static float held(Vec3 p, double now) {
		float k = 1;

		for (Hold h : HOLDS) {
			if (h.at.distanceToSqr(p) < 25) {
				k = Math.min(k, (float)Mth.clamp((now - h.until[0]) / 1.2, 0, 1));
			}
		}

		return k;
	}

	public static boolean any() {
		return !eyes.isEmpty() || !portals.isEmpty() || !shelves.isEmpty();
	}

	// ------------------------------------------------------------------ 掃描

	static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			if (any() || !BORN.isEmpty()) {
				eyes = List.of();
				portals = List.of();
				shelves = List.of();
				BORN.clear();
				HOLDS.clear();
				firstScan = true;
			}

			return;
		}

		double now = now();
		HOLDS.removeIf(h -> now - h.until[0] > 2);

		if (++timer >= 10) {
			timer = 0;
			scan(mc.level, mc.player.blockPosition());
		}

		ambientParticles(now);
	}

	private static void scan(ClientLevel level, BlockPos at) {
		List<BlockPos> frames = new ArrayList<>();
		List<Shelf> books = new ArrayList<>();
		Set<Long> portalBlocks = new HashSet<>();
		int pcx = at.getX() >> 4, pcz = at.getZ() >> 4, psy = at.getY() >> 4;

		for (int cx = pcx - CHUNKS; cx <= pcx + CHUNKS; cx++) {
			for (int cz = pcz - CHUNKS; cz <= pcz + CHUNKS; cz++) {
				LevelChunk ch = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);

				if (ch == null) {
					continue;
				}

				for (int sy = psy - 3; sy <= psy + 3; sy++) {
					int idx = level.getSectionIndexFromSectionY(sy);

					if (idx < 0 || idx >= ch.getSections().length) {
						continue;
					}

					LevelChunkSection s = ch.getSection(idx);

					if (s.hasOnlyAir() || !s.maybeHas(Ambient::interesting)) {
						continue;
					}

					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								BlockState st = s.getBlockState(x, y, z);

								if (!interesting(st)) {
									continue;
								}

								BlockPos p = new BlockPos((cx << 4) + x, (sy << 4) + y, (cz << 4) + z);

								if (st.is(Blocks.END_PORTAL)) {
									portalBlocks.add(p.asLong());
								}
								else if (st.is(Blocks.END_PORTAL_FRAME)) {
									if (st.getValue(EndPortalFrameBlock.HAS_EYE)) {
										frames.add(p);
									}
								}
								else {
									int mask = 0;

									for (int i = 0; i < 6; i++) {
										if (st.getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i))) {
											mask |= 1 << i;
										}
									}

									if (mask != 0) {
										books.add(new Shelf(p, st.getValue(ChiseledBookShelfBlock.FACING), mask));
									}
								}
							}
						}
					}
				}
			}
		}

		// 已開啟的門：中心 = 八方都是門方塊的那一格；門框旁邊就是門方塊的眼併入中央大光柱
		List<Vec3> centers = new ArrayList<>();

		for (long l : portalBlocks) {
			BlockPos p = BlockPos.of(l);
			boolean mid = true;

			for (int dx = -1; dx <= 1 && mid; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (!portalBlocks.contains(p.offset(dx, 0, dz).asLong())) {
						mid = false;
						break;
					}
				}
			}

			if (mid) {
				centers.add(new Vec3(p.getX() + 0.5, p.getY() + 0.76, p.getZ() + 0.5));
			}
		}

		frames.removeIf(f -> {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				if (portalBlocks.contains(f.relative(d).asLong())) {
					return true;
				}
			}

			return false;
		});

		Vec3 eye = Vec3.atCenterOf(at);
		frames.sort((a, b) -> Double.compare(Vec3.atCenterOf(a).distanceToSqr(eye), Vec3.atCenterOf(b).distanceToSqr(eye)));
		books.sort((a, b) -> Double.compare(Vec3.atCenterOf(a.pos).distanceToSqr(eye), Vec3.atCenterOf(b.pos).distanceToSqr(eye)));

		if (frames.size() > 48) {
			frames = new ArrayList<>(frames.subList(0, 48));
		}

		if (books.size() > 160) {
			books = new ArrayList<>(books.subList(0, 160));
		}

		// 第一次掃描（剛進世界）時直接亮著；之後新出現的才從 0 長出來
		double now = firstScan ? -100 : now();
		Set<Long> alive = new HashSet<>();

		for (BlockPos f : frames) {
			alive.add(f.asLong());
			BORN.putIfAbsent(f.asLong(), now);
		}

		for (Vec3 c : centers) {
			long k = BlockPos.containing(c).asLong() ^ 0x5A5A5A5AL;
			alive.add(k);
			BORN.putIfAbsent(k, now);
		}

		for (Shelf s : books) {
			for (int i = 0; i < 6; i++) {
				if ((s.mask & 1 << i) != 0) {
					long k = s.pos.asLong() * 8 + i;
					alive.add(k);
					BORN.putIfAbsent(k, now);
				}
			}
		}

		BORN.keySet().retainAll(alive);
		eyes = frames;
		portals = centers;
		shelves = books;
		firstScan = false;
	}

	private static boolean interesting(BlockState s) {
		return s.is(Blocks.END_PORTAL_FRAME) || s.is(Blocks.CHISELED_BOOKSHELF) || s.is(Blocks.END_PORTAL);
	}

	// ------------------------------------------------------------------ 繪製

	private static float grow(long key, double now, double seconds) {
		Double b = BORN.get(key);
		return b == null ? 1 : (float)Mth.clamp((now - b) / seconds, 0, 1);
	}

	private static float ease(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	private static float distFade(Vec3 p, Vec3 cam) {
		return (float)Mth.clamp((RANGE - p.distanceTo(cam)) / 10, 0, 1);
	}

	/** 雕紋書架第 slot 格（0~2 上排、3~5 下排，由正面看左 → 右）的中心，貼在正面外側 */
	public static Vec3 slotCenter(BlockPos pos, Direction facing, int slot) {
		double rx = (slot % 3 + 0.5) / 3.0;
		double ry = slot < 3 ? 0.75 : 0.25;
		double x, z;

		switch (facing) {
			case NORTH -> {
				x = 1 - rx;
				z = -0.012;
			}
			case SOUTH -> {
				x = rx;
				z = 1.012;
			}
			case WEST -> {
				x = -0.012;
				z = rx;
			}
			default -> {
				x = 1.012;
				z = 1 - rx;
			}
		}

		return new Vec3(pos.getX() + x, pos.getY() + ry, pos.getZ() + z);
	}

	public static void render(Draw d) {
		if (!Settings.enabled || !any()) {
			return;
		}

		double now = now();
		Vec3 cam = d.cam;

		if (Settings.tierB) {
			for (BlockPos f : eyes) {
				Vec3 base = new Vec3(f.getX() + 0.5, f.getY() + 0.82, f.getZ() + 0.5);
				float a = distFade(base, cam) * held(base, now);

				if (a <= 0.01f) {
					continue;
				}

				float g = ease(grow(f.asLong(), now, 0.7));
				float pulse = 0.8f + 0.2f * Mth.sin((float)(now * 2.4 + (f.getX() * 7 + f.getZ() * 13) * 0.37));
				d.cylinder(Mode.GLOW, BEAM, base, 0.17f, 7.5f * g, 12, 1, 0, 2, (float)(-now * 0.7), 1.5f, r(VOIDC), g(VOIDC), b(VOIDC), 0.3f * a * pulse, 0);
				d.cylinder(Mode.GLOW, BEAM, base, 0.05f, 8f * g, 8, 1, 0.5f, 2, (float)(-now * 1.3), 1f, 1f, 1f, 1f, 0.45f * a, 0);
				d.billboard(Mode.GLOW, GLOW, base.x, base.y, base.z, 0.45f * (0.6f + 0.4f * g), 0, r(VOIDC), g(VOIDC), b(VOIDC), 0.55f * a * pulse);
				// 剛嵌入時頂端的光點衝上去
				if (g < 1) {
					d.billboard(Mode.GLOW, GLOW, base.x, base.y + 7.5 * g, base.z, 0.5f * (1 - g), 0, 1f, 1f, 1f, (1 - g) * a);
				}
			}

			for (Vec3 c : portals) {
				float a = distFade(c, cam) * held(c, now);

				if (a <= 0.01f) {
					continue;
				}

				float g = ease(grow(BlockPos.containing(c).asLong() ^ 0x5A5A5A5AL, now, 1.2));
				float pulse = 0.85f + 0.15f * Mth.sin((float)(now * 1.7));
				d.cylinder(Mode.GLOW, BEAM, c, 1.35f, 44f * g, 32, 1, 0, 4, (float)(-now * 0.5), 1.15f, r(VOIDC), g(VOIDC), b(VOIDC), 0.16f * a * pulse, 0);
				d.cylinder(Mode.GLOW, BEAM, c, 0.42f, 46f * g, 16, 1, 0.5f, 4, (float)(-now * 0.9), 1f, 1f, 1f, 1f, 0.28f * a, 0);
				// 門面上緩緩旋轉的十二法陣
				d.flat(Mode.GLOW, SEAL, c.add(0, 0.02, 0), 1.45f * g, (float)(now * 0.15), r(VOIDC), g(VOIDC), b(VOIDC), 0.32f * a);
				d.flat(Mode.GLOW, RING, c.add(0, 0.03, 0), 2.35f * g, 0, r(VOIDC), g(VOIDC), b(VOIDC), 0.28f * a * pulse);
				twelveStar(d, c.add(0, 0.04, 0), 2.2f * g, (float)(-now * 0.1), VOIDC, 0.22f * a);
			}
		}

		if (Settings.tierC) {
			for (Shelf s : shelves) {
				Vec3 mid = Vec3.atCenterOf(s.pos);
				float a = distFade(mid, cam) * Math.min(1, (float)(28 - mid.distanceTo(cam)) / 6);

				if (a <= 0.01f) {
					continue;
				}

				Vector3f across = new Vector3f(-s.facing.getStepZ(), 0, s.facing.getStepX()).mul(0.17f);
				Vector3f up = new Vector3f(0, 0.22f, 0);
				Vector3f across2 = new Vector3f(across).mul(0.55f);
				Vector3f up2 = new Vector3f(up).mul(0.6f);

				for (int i = 0; i < 6; i++) {
					if ((s.mask & 1 << i) == 0) {
						continue;
					}

					float g = grow(s.pos.asLong() * 8 + i, now, 0.5);
					// 剛放進去：先亮一下再降到常駐亮度
					float flare = g < 1 ? Mth.sin(g * Mth.PI) * 0.7f : 0;
					float breathe = 0.75f + 0.25f * Mth.sin((float)(now * 1.6 + s.pos.asLong() % 97 + i * 1.3));
					float k = a * (Math.min(1, g * 2) * 0.45f * breathe + flare);
					Vec3 at = slotCenter(s.pos, s.facing, i);
					d.disc(Mode.GLOW, GLOW, at, across, up, 1f, 0, r(INK), g(INK), b(INK), k);
					d.disc(Mode.GLOW, GLOW, at, across2, up2, 1f, 0, 1f, 0.95f, 1f, k * 0.6f);
				}
			}
		}
	}

	/** 十二芒星（{12/5} 星形多邊形）+ 十二個節點 */
	public static void twelveStar(Draw d, Vec3 c, float radius, float spin, int col, float a) {
		if (a <= 0.01f || radius <= 0.01f) {
			return;
		}

		for (int i = 0; i < 12; i++) {
			double a0 = spin + i * Math.PI / 6;
			double a1 = spin + (i + 5) * Math.PI / 6;
			d.streak(Mode.GLOW, FxRender.tex("spark"), c.x + Math.cos(a0) * radius, c.y, c.z + Math.sin(a0) * radius, c.x + Math.cos(a1) * radius, c.y,
				c.z + Math.sin(a1) * radius, 0.05f, r(col), g(col), b(col), a);
			d.billboard(Mode.GLOW, GLOW, c.x + Math.cos(a0) * radius, c.y + 0.02, c.z + Math.sin(a0) * radius, 0.16f, 0, 1f, 1f, 1f, a * 1.5f);
		}
	}

	public static void lights(List<CinemaPost.Light> lights) {
		if (!Settings.enabled || !Settings.tierB) {
			return;
		}

		double now = now();

		for (Vec3 c : portals) {
			lights.add(new CinemaPost.Light(c.add(0, 1, 0), 6f, r(VOIDC), g(VOIDC), b(VOIDC), 0.25f * held(c, now), true));
		}
	}

	private static double lastParticles;

	private static void ambientParticles(double now) {
		if (!Settings.enabled) {
			return;
		}

		double dt = Math.min(0.2, now - lastParticles);
		lastParticles = now;

		if (Settings.tierB) {
			for (Vec3 c : portals) {
				if (held(c, now) < 1) {
					continue;
				}

				for (int i = 0; i < count(6, dt); i++) {
					double ang = Particles.RNG.nextDouble() * Math.PI * 2;
					double rr = Particles.RNG.nextDouble() * 1.4;
					Particles.spawn(Kind.FLARE, c.add(Math.cos(ang) * rr, 0.1, Math.sin(ang) * rr), new Vec3(0, Particles.rand(1.5f, 3f), 0), 2.5)
						.size(0.07f, 0.01f).color(0xFFFFFF, VOIDC).physics(0, 0).fadeIn(0.3f).spin(2f);
				}
			}
		}

		if (Settings.tierC && !shelves.isEmpty()) {
			for (int i = 0; i < count(Math.min(4, shelves.size() * 0.15), dt); i++) {
				Shelf s = shelves.get(Particles.RNG.nextInt(Math.min(shelves.size(), 40)));
				int slot = Particles.RNG.nextInt(6);

				if ((s.mask & 1 << slot) == 0) {
					continue;
				}

				Vec3 out = new Vec3(s.facing.getStepX(), 0, s.facing.getStepZ());
				Particles.spawn(Kind.GLYPH, slotCenter(s.pos, s.facing, slot), out.scale(0.15).add(0, 0.12, 0), 1.6).size(0.05f, 0.02f).color(0xFFFFFF, INK)
					.physics(0, 0.5f).fadeIn(0.3f);
			}
		}
	}

	private static int count(double perSecond, double dt) {
		double n = perSecond * dt;
		int k = (int)n;
		return k + (Particles.RNG.nextDouble() < n - k ? 1 : 0);
	}

	private static float r(int c) {
		return (c >> 16 & 255) / 255f;
	}

	private static float g(int c) {
		return (c >> 8 & 255) / 255f;
	}

	private static float b(int c) {
		return (c & 255) / 255f;
	}
}
