package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;

public class ItemIchoriumShovel extends ShovelItem implements IWarpingGear {
    public ItemIchoriumShovel(Item.Properties properties) {
        super(ModMaterials.ICHORIUM, 1.5F, -3.0F, properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
