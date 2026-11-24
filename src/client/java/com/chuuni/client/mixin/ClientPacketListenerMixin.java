package com.chuuni.client.mixin;

import com.chuuni.client.fx.ClientWatch;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 收到爆炸封包 → 爆炸演出（純客戶端，伺服器不用裝模組也有） */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleExplosion", at = @At("TAIL"))
	private void chuuni$explode(ClientboundExplodePacket packet, CallbackInfo ci) {
		ClientWatch.explosion(packet);
	}

	/** 實體事件（不死圖騰發動 = 35） */
	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void chuuni$entityEvent(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo ci) {
		ClientWatch.entityEvent(packet);
	}
}
