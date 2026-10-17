package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ItemIchoriumPickaxe extends Item implements IWarpingGear {
    public ItemIchoriumPickaxe(Item.Properties properties) {
        super(ModMaterials.ICHORIUM.applyToolProperties(properties.rarity(Rarity.EPIC).fireResistant(), BlockTags.MINEABLE_WITH_PICKAXE, 1.0F, -2.8F, 0.0F));
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
