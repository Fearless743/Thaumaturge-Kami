package mod.emt.kami.items.base;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

public class ItemBaseEnchanted extends ItemBase {
    public ItemBaseEnchanted(Item.Properties properties) {
        super(properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }
}
