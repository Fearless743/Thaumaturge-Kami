package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

import java.util.List;

/**
 * The Ichorium Shield. Like the other Ichorium gear it is netherite-grade,
 * fire-resistant and grants a measure of warp to its bearer.
 */
public class ItemIchoriumShield extends ShieldItem implements IWarpingGear {
    public ItemIchoriumShield(Item.Properties properties) {
        super(properties.durability(3000).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
