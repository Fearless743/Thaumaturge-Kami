package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ItemIchoriumSword extends Item implements IWarpingGear {
    public ItemIchoriumSword(Item.Properties properties) {
        super(ModMaterials.ICHORIUM.applySwordProperties(properties.rarity(Rarity.EPIC).fireResistant(), 3.0F, -2.4F));
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
