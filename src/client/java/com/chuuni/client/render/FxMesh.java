package com.chuuni.client.render;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.chuuni.Chuuni;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * 特效模型（tools/gen_meshes.py 產生的四邊形清單）。每個四邊形 32 個 float：4 × (x,y,z,u,v,nx,ny,nz)。
 * 第一次使用時從資源載入並快取。
 */
public record FxMesh(Identifier texture, float[] data) {
	private static final Map<String, FxMesh> CACHE = new HashMap<>();

	public static FxMesh get(String name) {
		return CACHE.computeIfAbsent(name, FxMesh::load);
	}

	private static FxMesh load(String name) {
		Identifier id = Chuuni.id("fxmodels/" + name + ".json");

		try {
			Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);

			if (res.isEmpty()) {
				Chuuni.LOGGER.error("missing fx model {}", id);
				return null;
			}

			try (Reader reader = res.get().openAsReader()) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
				JsonArray quads = obj.getAsJsonArray("quads");
				float[] data = new float[quads.size() * 32];
				int i = 0;

				for (var q : quads) {
					for (var f : q.getAsJsonArray()) {
						data[i++] = f.getAsFloat();
					}
				}

				return new FxMesh(Identifier.parse(obj.get("texture").getAsString()), data);
			}
		}
		catch (Exception e) {
			Chuuni.LOGGER.error("failed to load fx model {}", id, e);
			return null;
		}
	}
}
