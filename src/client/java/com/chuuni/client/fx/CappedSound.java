package com.chuuni.client.fx;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** 只播長素材的開頭：maxTicks 內最後 FADE tick 淡出，然後停止（素材檔本身不剪） */
final class CappedSound extends AbstractTickableSoundInstance {
	private static final int FADE = 12;

	private final float base;
	private final int maxTicks;
	private int age;

	/** pos == null → UI（不衰減、跟著玩家） */
	CappedSound(SoundEvent ev, float volume, float pitch, Vec3 pos, int maxTicks) {
		super(ev, pos == null ? SoundSource.MASTER : SoundSource.BLOCKS, RandomSource.create());
		this.base = volume;
		this.volume = volume;
		this.pitch = pitch;
		this.maxTicks = Math.max(FADE + 1, maxTicks);

		if (pos == null) {
			relative = true;
			attenuation = SoundInstance.Attenuation.NONE;
		}
		else {
			x = pos.x;
			y = pos.y;
			z = pos.z;
		}
	}

	@Override
	public void tick() {
		age++;
		int left = maxTicks - age;

		if (left <= 0) {
			stop();
		}
		else if (left < FADE) {
			volume = base * left / FADE;
		}
	}
}
