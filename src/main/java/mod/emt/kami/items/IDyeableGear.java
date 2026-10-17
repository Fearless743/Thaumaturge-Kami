package mod.emt.kami.items;

import net.minecraft.world.item.ItemStack;

public interface IDyeableGear {
    int getDyedColor(ItemStack stack);

    void setDyedColor(ItemStack stack, int color);
}
