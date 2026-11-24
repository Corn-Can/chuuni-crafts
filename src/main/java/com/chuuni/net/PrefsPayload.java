package com.chuuni.net;

import com.chuuni.Chuuni;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S：玩家的設定。rewards = 要不要讓伺服器保管 A 級演出的成品（關掉動畫時維持原版：物品照常留在格子 / 直接掉出） */
public record PrefsPayload(boolean rewards) implements CustomPacketPayload {
	public static final Type<PrefsPayload> TYPE = new Type<>(Chuuni.id("prefs"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PrefsPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, PrefsPayload::rewards, PrefsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
