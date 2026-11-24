package com.chuuni;

import net.minecraft.world.item.ItemStack;

/** ItemCombinerMenu（鍛造台 / 鐵砧）記下的最後成品：shift 取出時 onTake 拿到空物品就用它 */
public interface LastResult {
	ItemStack chuuni$result(ItemStack taken);
}
