package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ItemIchoriumHoe extends HoeItem implements IWarpingGear {
    public ItemIchoriumHoe(Item.Properties properties) {
        super(ModMaterials.ICHORIUM, -4.0F, 0.0F, properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
