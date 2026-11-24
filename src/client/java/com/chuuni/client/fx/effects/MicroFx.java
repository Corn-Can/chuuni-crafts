package com.chuuni.client.fx.effects;

import java.util.List;

import com.chuuni.client.fx.Particles;
import com.chuuni.client.fx.Particles.Kind;
import com.chuuni.client.fx.Sfx;
import com.chuuni.client.post.CinemaPost;
import com.chuuni.client.render.Draw;
import com.chuuni.client.render.Draw.Mode;
import com.chuuni.net.FxPayload;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * C 級微型演出的骨架：每天會按上百次，所以 1 秒內、不出招式名、不染畫面、不動鏡頭、沒有法陣——
 * 只在那個方塊上做一個「有手感」的小動作（光、碎屑、聲音）。
 */
public abstract class MicroFx extends MinorFx {
	protected MicroFx(FxPayload p) {
		super(p);
		end = 0.8;
	}

	protected void sfx(String name, float pitch) {
		Sfx.play(mine, name, c, 0.55f, pitch);
	}

	/** 方塊的水平朝向（FACING / HORIZONTAL_FACING），沒有時用點到的面 */
	protected Direction facing() {
		BlockState s = state();

		if (s != null && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return s.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}

		if (s != null && s.hasProperty(BlockStateProperties.FACING)) {
			return s.getValue(BlockStateProperties.FACING);
		}

		return dir;
	}

	/** 方塊實際外形的範圍（世界座標）；拉桿、按鈕、中繼器、陶罐等不是整格的方塊，特效要對準它真正的樣子 */
	protected net.minecraft.world.phys.AABB box() {
		var level = net.minecraft.client.Minecraft.getInstance().level;
		BlockState s = state();

		if (level != null && s != null) {
			var shape = s.getShape(level, pos);

			if (!shape.isEmpty()) {
				return shape.bounds().move(pos);
			}
		}

		return new net.minecraft.world.phys.AABB(pos);
	}

	protected Vec3 boxCenter() {
		return box().getCenter();
	}

	/** 外形頂面中央 */
	protected Vec3 boxTop() {
		var b = box();
		return new Vec3((b.minX + b.maxX) / 2, b.maxY, (b.minZ + b.maxZ) / 2);
	}

	/** 外形在 d 方向那一面的中央 */
	protected Vec3 boxFace(Direction d) {
		var b = box();
		Vec3 ctr = b.getCenter();
		double x = d.getStepX() > 0 ? b.maxX : d.getStepX() < 0 ? b.minX : ctr.x;
		double y = d.getStepY() > 0 ? b.maxY : d.getStepY() < 0 ? b.minY : ctr.y;
		double z = d.getStepZ() > 0 ? b.maxZ : d.getStepZ() < 0 ? b.minZ : ctr.z;
		return new Vec3(x, y, z).add(d.getStepX() * 0.01, d.getStepY() * 0.01, d.getStepZ() * 0.01);
	}

	protected static Vec3 vec(Direction d) {
		return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
	}

	/** 與 d 垂直的水平軸（d 為上下時用 X） */
	protected static Vec3 across(Direction d) {
		return d.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(-d.getStepZ(), 0, d.getStepX());
	}

	protected static void burst(Kind kind, Vec3 at, Vec3 dir, int n, float speed, float spread, int c0, int c1, float size, float gravity, double life) {
		for (int i = 0; i < n; i++) {
			Vec3 v = dir.scale(Particles.rand(speed * 0.5f, speed)).add(Particles.randomDir(spread));
			Particles.spawn(kind, at, v, life * Particles.rand(0.7f, 1.2f)).size(size, size * 0.4f).color(c0, c1).physics(gravity, 2f).spin(8f);
		}
	}
}
