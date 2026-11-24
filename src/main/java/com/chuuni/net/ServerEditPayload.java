package com.chuuni.net;

import com.chuuni.Chuuni;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S：管理員在遊戲內的設定畫面改伺服器設定（key = "" 表示重新讀取設定檔）。伺服器會再檢查一次權限。 */
public record ServerEditPayload(String key, String value) implements CustomPacketPayload {
	public static final Type<ServerEditPayload> TYPE = new Type<>(Chuuni.id("server_edit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerEditPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ServerEditPayload::key,
		ByteBufCodecs.STRING_UTF8, ServerEditPayload::value,
		ServerEditPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
