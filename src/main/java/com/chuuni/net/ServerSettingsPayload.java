package com.chuuni.net;

import com.chuuni.Chuuni;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C：伺服器的設定（進服時、以及管理員改設定後送出）。
 *
 * @param force  伺服器是否開了 forceClient（只是讓客戶端顯示用；實際要套用哪些項目看 values）
 * @param values properties 格式「key=value」：客戶端必須套用、而且不能自己改的項目（沒列到的讓玩家自己決定）
 * @param admin  收到的玩家能不能改伺服器設定（OP / 單人世界的主人）
 * @param raw    管理員才有：伺服器設定檔的完整內容（給設定畫面顯示每一項目前的值）
 */
public record ServerSettingsPayload(boolean force, String values, boolean admin, String raw) implements CustomPacketPayload {
	public static final Type<ServerSettingsPayload> TYPE = new Type<>(Chuuni.id("server_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerSettingsPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, ServerSettingsPayload::force,
		ByteBufCodecs.STRING_UTF8, ServerSettingsPayload::values,
		ByteBufCodecs.BOOL, ServerSettingsPayload::admin,
		ByteBufCodecs.STRING_UTF8, ServerSettingsPayload::raw,
		ServerSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
