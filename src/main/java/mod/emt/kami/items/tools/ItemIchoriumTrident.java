package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.Kami;
import mod.emt.kami.entities.EntityThrownTrident;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.registry.ModSoundsKAMI;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;

/**
 * The Ichorium Trident. A heavy piercing weapon that can be charged and thrown; unlike the vanilla
 * trident, the thrown weapon always boomerangs back to its owner ({@link EntityThrownTrident}).
 */
public class ItemIchoriumTrident extends TridentItem implements IWarpingGear {
    public static final Identifier KNOCKBACK_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "weapon_knockback");
    public ItemIchoriumTrident(Item.Properties properties) {
        super(weaponProperties(properties.rarity(Rarity.EPIC).fireResistant(),
                ModMaterials.ICHORIUM, 3.5F, -2.9F, 0.0F));
    }

    /**
     * Shared property setup for weapons whose attributes are assembled manually (trident, mallet):
     * the baseline damage/speed/knockback values are added on top of the material's damage bonus.
     */
    public static Item.Properties weaponProperties(Item.Properties properties, ToolMaterial material,
                                                   float damageBaseline, float speedBaseline, float knockbackBaseline) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damageBaseline + material.attackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speedBaseline,
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (knockbackBaseline != 0.0F) {
            builder.add(Attributes.ATTACK_KNOCKBACK,
                    new AttributeModifier(KNOCKBACK_MODIFIER_ID, knockbackBaseline,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return properties
                .durability(material.durability())
                .repairable(material.repairItems())
                .enchantable(material.enchantmentValue())
                .attributes(builder.build())
                .component(DataComponents.TOOL, TridentItem.createToolProperties())
                .component(DataComponents.WEAPON, new Weapon(1));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int timeHeld = this.getUseDuration(stack, entity) - remainingTime;
        if (timeHeld < TridentItem.THROW_THRESHOLD_TIME || stack.nextDamageWillBreak()) {
            return false;
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (level instanceof ServerLevel serverLevel) {
            stack.hurtWithoutBreaking(1, player);
            EntityThrownTrident trident = new EntityThrownTrident(level, player, stack.copy());
            trident.setDamage((float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            trident.thrownFromSlot = player.getInventory().getSelectedSlot();
            level.addFreshEntity(trident);
            level.playSound(null, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_THROW.get(), SoundSource.PLAYERS,
                    3.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            stack.setCount(0);
        }
        return true;
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
