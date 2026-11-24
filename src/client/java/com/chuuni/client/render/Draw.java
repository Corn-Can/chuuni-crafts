package com.chuuni.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 每幀的特效繪圖工具：座標一律用世界座標（內部已減去鏡頭位置）。
 * 顏色以 0~1 浮點傳入；加法混色時 alpha 等同亮度。
 */
public final class Draw {
	public enum Mode {
		GLOW, ALPHA, SOLID
	}

	public final MultiBufferSource buffers;
	public final SubmitNodeCollector queue;
	public final PoseStack pose;
	public final Vec3 cam;
	private final Vector3f left;
	private final Vector3f up;
	private final Vector3f forward;
	private static final ItemStackRenderState ITEM_STATE = new ItemStackRenderState();

	public Draw(MultiBufferSource buffers, SubmitNodeCollector queue, PoseStack pose, Camera camera) {
		this.buffers = buffers;
		this.queue = queue;
		this.pose = pose;
		this.cam = camera.position();
		this.left = new Vector3f(camera.leftVector());
		this.up = new Vector3f(camera.upVector());
		this.forward = new Vector3f(camera.forwardVector());
	}

	public VertexConsumer buffer(Mode mode, Identifier tex) {
		return buffers.getBuffer(switch (mode) {
			case GLOW -> FxRender.glow(tex);
			case ALPHA -> FxRender.alpha(tex);
			case SOLID -> FxRender.solid(tex);
		});
	}

	private Matrix4f m() {
		return pose.last().pose();
	}

	private void v(VertexConsumer c, double x, double y, double z, float u, float vv, float r, float g, float b, float a) {
		c.addVertex(m(), (float)(x - cam.x), (float)(y - cam.y), (float)(z - cam.z)).setUv(u, vv).setColor(r, g, b, a);
	}

	// ------------------------------------------------------------------ 平面

	/**
	 * 平面圓盤（以 ax、bx 兩軸張開的正方形貼圖），rot 為面內旋轉（弧度）。
	 * 水平圓盤：ax = (1,0,0)、bx = (0,0,1)。
	 */
	public void disc(Mode mode, Identifier tex, Vec3 c, Vector3fc ax, Vector3fc bx, float radius, float rot, float r, float g, float b, float a) {
		if (a <= 0.002f || radius <= 0.001f) {
			return;
		}

		VertexConsumer vc = buffer(mode, tex);
		float cs = Mth.cos(rot) * radius;
		float sn = Mth.sin(rot) * radius;
		// 旋轉後的兩軸
		float ux = ax.x() * cs + bx.x() * sn, uy = ax.y() * cs + bx.y() * sn, uz = ax.z() * cs + bx.z() * sn;
		float wx = -ax.x() * sn + bx.x() * cs, wy = -ax.y() * sn + bx.y() * cs, wz = -ax.z() * sn + bx.z() * cs;
		v(vc, c.x - ux - wx, c.y - uy - wy, c.z - uz - wz, 0, 0, r, g, b, a);
		v(vc, c.x + ux - wx, c.y + uy - wy, c.z + uz - wz, 1, 0, r, g, b, a);
		v(vc, c.x + ux + wx, c.y + uy + wy, c.z + uz + wz, 1, 1, r, g, b, a);
		v(vc, c.x - ux + wx, c.y - uy + wy, c.z - uz + wz, 0, 1, r, g, b, a);
	}

	public static final Vector3f X = new Vector3f(1, 0, 0);
	public static final Vector3f Y = new Vector3f(0, 1, 0);
	public static final Vector3f Z = new Vector3f(0, 0, 1);

	/** 水平圓盤 */
	public void flat(Mode mode, Identifier tex, Vec3 c, float radius, float rot, float r, float g, float b, float a) {
		disc(mode, tex, c, X, Z, radius, rot, r, g, b, a);
	}

	/** 面向鏡頭的公告板（uv 子區域） */
	public void billboard(Mode mode, Identifier tex, double x, double y, double z, float size, float roll, float r, float g, float b, float a,
		float u0, float v0, float u1, float v1) {
		if (a <= 0.002f || size <= 0.0005f) {
			return;
		}

		VertexConsumer vc = buffer(mode, tex);
		float cs = Mth.cos(roll) * size;
		float sn = Mth.sin(roll) * size;
		float rx = left.x * cs + up.x * sn, ry = left.y * cs + up.y * sn, rz = left.z * cs + up.z * sn;
		float ux = -left.x * sn + up.x * cs, uy = -left.y * sn + up.y * cs, uz = -left.z * sn + up.z * cs;
		v(vc, x + rx - ux, y + ry - uy, z + rz - uz, u0, v1, r, g, b, a);
		v(vc, x - rx - ux, y - ry - uy, z - rz - uz, u1, v1, r, g, b, a);
		v(vc, x - rx + ux, y - ry + uy, z - rz + uz, u1, v0, r, g, b, a);
		v(vc, x + rx + ux, y + ry + uy, z + rz + uz, u0, v0, r, g, b, a);
	}

	public void billboard(Mode mode, Identifier tex, double x, double y, double z, float size, float roll, float r, float g, float b, float a) {
		billboard(mode, tex, x, y, z, size, roll, r, g, b, a, 0, 0, 1, 1);
	}

	/** 兩點之間面向鏡頭的帶狀條（火花拖尾、光痕）。貼圖 u：from(0) → to(1) */
	public void streak(Mode mode, Identifier tex, double x0, double y0, double z0, double x1, double y1, double z1, float width,
		float r, float g, float b, float a) {
		if (a <= 0.002f) {
			return;
		}

		float dx = (float)(x1 - x0), dy = (float)(y1 - y0), dz = (float)(z1 - z0);
		// 帶寬方向 = 線段方向 × 視線方向
		float vx = (float)((x0 + x1) / 2 - cam.x), vy = (float)((y0 + y1) / 2 - cam.y), vz = (float)((z0 + z1) / 2 - cam.z);
		float sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
		float len = Mth.sqrt(sx * sx + sy * sy + sz * sz);

		if (len < 1e-6f) {
			return;
		}

		float k = width / 2 / len;
		sx *= k;
		sy *= k;
		sz *= k;
		VertexConsumer vc = buffer(mode, tex);
		v(vc, x0 - sx, y0 - sy, z0 - sz, 0, 0, r, g, b, a);
		v(vc, x1 - sx, y1 - sy, z1 - sz, 1, 0, r, g, b, a);
		v(vc, x1 + sx, y1 + sy, z1 + sz, 1, 1, r, g, b, a);
		v(vc, x0 + sx, y0 + sy, z0 + sz, 0, 1, r, g, b, a);
	}

	/**
	 * 垂直開口圓柱（光柱、立體符文環）。u 沿圓周（重複 uRepeat 次，加 uOff 可旋轉流動），v 由下而上（重複 vRepeat 次、vOff 流動）。
	 * topScale：頂端半徑倍率（做成錐形的光柱）。
	 */
	public void cylinder(Mode mode, Identifier tex, Vec3 base, float radius, float height, int segments, float uRepeat, float uOff,
		float vRepeat, float vOff, float topScale, float r, float g, float b, float a, float topAlpha) {
		if (a <= 0.002f && topAlpha <= 0.002f) {
			return;
		}

		VertexConsumer vc = buffer(mode, tex);
		float rt = radius * topScale;

		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments;
			float a1 = Mth.TWO_PI * (i + 1) / segments;
			float u0 = uOff + uRepeat * i / segments;
			float u1 = uOff + uRepeat * (i + 1) / segments;
			double x0 = Mth.cos(a0), z0 = Mth.sin(a0), x1 = Mth.cos(a1), z1 = Mth.sin(a1);
			v(vc, base.x + x0 * radius, base.y, base.z + z0 * radius, u0, vOff + vRepeat, r, g, b, a);
			v(vc, base.x + x1 * radius, base.y, base.z + z1 * radius, u1, vOff + vRepeat, r, g, b, a);
			v(vc, base.x + x1 * rt, base.y + height, base.z + z1 * rt, u1, vOff, r, g, b, topAlpha);
			v(vc, base.x + x0 * rt, base.y + height, base.z + z0 * rt, u0, vOff, r, g, b, topAlpha);
		}
	}

	/** 水平的平面環帶（內徑 → 外徑），u 沿圓周、v 由內到外 */
	public void annulus(Mode mode, Identifier tex, Vec3 c, float inner, float outer, int segments, float uRepeat, float uOff,
		float r, float g, float b, float aInner, float aOuter) {
		VertexConsumer vc = buffer(mode, tex);

		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments;
			float a1 = Mth.TWO_PI * (i + 1) / segments;
			float u0 = uOff + uRepeat * i / segments;
			float u1 = uOff + uRepeat * (i + 1) / segments;
			double x0 = Mth.cos(a0), z0 = Mth.sin(a0), x1 = Mth.cos(a1), z1 = Mth.sin(a1);
			v(vc, c.x + x0 * inner, c.y, c.z + z0 * inner, u0, 1, r, g, b, aInner);
			v(vc, c.x + x1 * inner, c.y, c.z + z1 * inner, u1, 1, r, g, b, aInner);
			v(vc, c.x + x1 * outer, c.y, c.z + z1 * outer, u1, 0, r, g, b, aOuter);
			v(vc, c.x + x0 * outer, c.y, c.z + z0 * outer, u0, 0, r, g, b, aOuter);
		}
	}

	// ------------------------------------------------------------------ 模型與物品

	/**
	 * 畫 3D 模型。transform 以模型座標 → 世界座標（相對於 at）。shade：依法線做簡單的頂光明暗。
	 */
	public void mesh(Mode mode, FxMesh mesh, Vec3 at, Matrix4f transform, float r, float g, float b, float a, boolean shade) {
		if (mesh == null || a <= 0.002f) {
			return;
		}

		VertexConsumer vc = buffer(mode, mesh.texture());
		Matrix4f world = new Matrix4f(m()).translate((float)(at.x - cam.x), (float)(at.y - cam.y), (float)(at.z - cam.z)).mul(transform);
		Vector3f p = new Vector3f();
		Vector3f n = new Vector3f();
		float[] q = mesh.data();

		for (int i = 0; i < q.length; i += 32) {
			float s = 1f;

			if (shade) {
				transform.transformDirection(n.set(q[i + 5], q[i + 6], q[i + 7])).normalize();
				s = 0.55f + 0.35f * Math.max(0f, n.y) + 0.15f * Math.max(0f, n.x * 0.6f + n.z * 0.8f);
			}

			for (int k = 0; k < 4; k++) {
				int o = i + k * 8;
				world.transformPosition(p.set(q[o], q[o + 1], q[o + 2]));
				vc.addVertex(p.x, p.y, p.z).setUv(q[o + 3], q[o + 4]).setColor(Math.min(1f, r * s), Math.min(1f, g * s), Math.min(1f, b * s), a);
			}
		}
	}

	/** 浮空展示物品（以地面掉落物的顯示方式，全亮） */
	public void item(ItemStack stack, Vec3 at, float scale, float yawRad, float pitchRad) {
		if (stack.isEmpty() || queue == null) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		mc.getItemModelResolver().updateForTopItem(ITEM_STATE, stack, ItemDisplayContext.GROUND, mc.level, null, 0);
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
		pose.mulPose(new org.joml.Quaternionf().rotateY(yawRad).rotateX(pitchRad));
		pose.scale(scale, scale, scale);
		ITEM_STATE.submit(pose, queue, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}

	public Vector3f cameraForward() {
		return forward;
	}
}
