package mod.emt.kami.utils.helpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

public class ItemHelper {
    public static ItemStack setUnbreakable(ItemStack stack) {
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return stack;
    }
}
