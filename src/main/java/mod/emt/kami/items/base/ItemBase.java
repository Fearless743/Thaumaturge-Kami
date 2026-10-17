package mod.emt.kami.items.base;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ItemBase extends Item {
    public ItemBase(Item.Properties properties) {
        super(properties.rarity(Rarity.EPIC));
    }
}
