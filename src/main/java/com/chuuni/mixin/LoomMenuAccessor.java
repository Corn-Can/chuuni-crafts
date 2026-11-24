package com.chuuni.mixin;

import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LoomMenu.class)
public interface LoomMenuAccessor {
	@Accessor("access")
	ContainerLevelAccess chuuni$access();
}
