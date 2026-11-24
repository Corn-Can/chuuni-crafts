package com.chuuni.mixin;

import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CartographyTableMenu.class)
public interface CartographyTableMenuAccessor {
	@Accessor("access")
	ContainerLevelAccess chuuni$access();
}
