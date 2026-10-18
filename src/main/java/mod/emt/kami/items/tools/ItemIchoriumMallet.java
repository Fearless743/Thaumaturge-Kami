package mod.emt.kami.items.tools;

import mod.emt.kami.items.ModMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;

/**
 * The Ichorium Mallet. A slow, knockback-heavy crushing weapon modelled after the vanilla mace
 * (including its smash attack), with the Ichorium material's durability and damage bonus.
 */
public class ItemIchoriumMallet extends MaceItem {
    public static final float ATTACK_DAMAGE_BASELINE = 4.5F;
    public static final float ATTACK_SPEED_BASELINE = -3.5F;
    public static final float KNOCKBACK_BASELINE = 1.0F;

    public ItemIchoriumMallet(Item.Properties properties) {
        super(ItemIchoriumTrident.weaponProperties(
                properties.rarity(Rarity.EPIC).fireResistant(),
                ModMaterials.ICHORIUM, ATTACK_DAMAGE_BASELINE, ATTACK_SPEED_BASELINE, KNOCKBACK_BASELINE));
    }
}
