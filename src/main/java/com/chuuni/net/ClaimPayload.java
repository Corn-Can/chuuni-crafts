package com.chuuni.net;

import com.chuuni.Chuuni;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S：演出結束（物品已在畫面中降下）→ 請伺服器把成品變成只有自己能撿的掉落物。內容由伺服器記錄的「待領取」決定，客戶端不能指定。 */
public record ClaimPayload() implements CustomPacketPayload {
	public static final Type<ClaimPayload> TYPE = new Type<>(Chuuni.id("claim"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClaimPayload> CODEC = StreamCodec.unit(new ClaimPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
