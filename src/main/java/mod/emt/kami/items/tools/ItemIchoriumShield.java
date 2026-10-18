package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * The Ichorium Shield. Blocking behaviour is entirely data-driven in this Minecraft version, so
 * the vanilla {@code BLOCKS_ATTACKS} component setup (delayed registration, since it needs resolved
 * damage type tags) is replicated with the Ichorium durability and rarity.
 */
public class ItemIchoriumShield extends Item implements IWarpingGear {
    public ItemIchoriumShield(Item.Properties properties) {
        this(properties, 1.0F);
    }

    /**
     * @param disableCooldownScale multiplier for the axe-induced shield-disable cooldown
     *                             ({@code seconds * scale}); {@code 0} means the shield can never
     *                             be disabled.
     */
    protected ItemIchoriumShield(Item.Properties properties, float disableCooldownScale) {
        super(properties
                .rarity(Rarity.EPIC)
                .fireResistant()
                .durability(ModMaterials.ICHORIUM.durability())
                .repairable(ModMaterials.ICHORIUM.repairItems())
                .enchantable(ModMaterials.ICHORIUM.enchantmentValue())
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F,
                        disableCooldownScale,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK))));
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
