package mod.emt.kami.items.tools;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import mod.emt.kami.items.ModMaterials;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Ichorium Spear. A long-reach piercing weapon built on the vanilla data-driven spear
 * ({@code Item.Properties#spear}), giving it the full vanilla package: piercing left-click stabs
 * ({@code PIERCING_WEAPON}) and the long-press charge with first/third-person charging animations
 * ({@code KINETIC_WEAPON}).
 * <p>
 * The vanilla kinetic stab loop is disabled here (its condition thresholds are unreachable): it
 * deals damage only on fast mounted charges, and its damage is based on the attacker's
 * <em>base</em> attack attribute (1.0 for players), so the vanilla long-press charge is a no-op on
 * foot. Worse, the vanilla loop unconditionally marks every scanned entity as "recently stabbed",
 * which starves any supplementary loop that shares that memory. The charge-stab is therefore owned
 * entirely by {@link #inventoryTick}: while the charge is held past the delay, entities along the
 * stab line are hit through the vanilla {@code stabAttack} pipeline (enchantment damage, knockback,
 * rider dismount, durability) at most once per {@link #CONTACT_COOLDOWN_TICKS}, tracked in a
 * per-player cooldown map of our own.
 * <p>
 * The unreachable thresholds also keep the charge pose raised forever: the "tired lowering" phase
 * of {@code SpearAnimations} is derived from the condition windows, so finite windows would lower
 * the spear mid-charge.
 */
public class ItemIchoriumSpear extends Item implements IWarpingGear {
    public static final float ATTACK_DURATION = 0.85F;
    public static final float DAMAGE_MULTIPLIER = 1.0F;
    public static final float CHARGE_DELAY = 0.6F;
    /** Endless window for the kinetic stab conditions: keeps the charge pose from ever lowering. */
    public static final int CHARGE_WINDOW_TICKS = 72000;
    public static final float DISMOUNT_TIME = 2.5F;
    /** Per-entity cooldown (ticks) between stab hits on the same target while holding the charge. */
    public static final int CONTACT_COOLDOWN_TICKS = 10;
    public static final int CONTACT_COOLDOWN_PRUNE_TICKS = 100;

    /** Per-player stab cooldown: player UUID -> (target entity id -> last stab game time). */
    private static final Map<UUID, Long2LongOpenHashMap> LAST_STAB = new WeakHashMap<>();

    public ItemIchoriumSpear(Item.Properties properties) {
        super(spearProperties(properties, ModMaterials.ICHORIUM));
    }

    protected static Item.Properties spearProperties(Item.Properties properties, ToolMaterial material) {
        // Vanilla spear component set, but the kinetic stab conditions are made unreachable:
        // endless windows (so the SpearAnimations "tired lowering" phase, which is derived from
        // the window lengths, never triggers) combined with impossible speed thresholds (so the
        // vanilla loop never stabs — it would otherwise mark every scanned entity as recently
        // stabbed with 0 damage and starve the supplementary loop below).
        return properties
                .rarity(Rarity.EPIC)
                .fireResistant()
                .spear(material, ATTACK_DURATION, DAMAGE_MULTIPLIER, CHARGE_DELAY,
                        2.5F, 11.0F, 6.75F, 5.1F, 4.6F, 15.0F)
                .component(net.minecraft.core.component.DataComponents.KINETIC_WEAPON,
                        new net.minecraft.world.item.component.KineticWeapon(
                                10, (int) (CHARGE_DELAY * 20.0F),
                                net.minecraft.world.item.component.KineticWeapon.Condition.ofAttackerSpeed(
                                        CHARGE_WINDOW_TICKS, Float.MAX_VALUE),
                                net.minecraft.world.item.component.KineticWeapon.Condition.ofAttackerSpeed(
                                        CHARGE_WINDOW_TICKS, Float.MAX_VALUE),
                                net.minecraft.world.item.component.KineticWeapon.Condition.ofRelativeSpeed(
                                        CHARGE_WINDOW_TICKS, Float.MAX_VALUE),
                                0.38F, DAMAGE_MULTIPLIER,
                                Optional.of(SoundEvents.SPEAR_USE), Optional.of(SoundEvents.SPEAR_HIT)));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player player)
                || !player.isUsingItem()
                || player.getUseItem() != stack
                || player.getTicksUsingItem() < (int) (CHARGE_DELAY * 20.0F)
                || stack.nextDamageWillBreak()) {
            return;
        }
        long gameTime = level.getGameTime();
        Long2LongOpenHashMap cooldowns = LAST_STAB.computeIfAbsent(player.getUUID(), k -> new Long2LongOpenHashMap());
        cooldowns.values().removeIf(time -> gameTime - time > CONTACT_COOLDOWN_PRUNE_TICKS);

        InteractionHand hand = player.getUsedItemHand();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER;
        boolean anyHit = false;
        for (EntityHitResult hitResult : ProjectileUtil.getHitEntitiesAlong(player, player.getAttackRangeWith(stack),
                e -> PiercingWeapon.canHitEntity(player, e), ClipContext.Block.COLLIDER)
                .<Collection<EntityHitResult>>map(a -> List.of(), e -> e)) {
            Entity target = hitResult.getEntity();
            if (cooldowns.get(target.getId()) + CONTACT_COOLDOWN_TICKS > gameTime) {
                continue;
            }
            cooldowns.put(target.getId(), gameTime);
            // Full damage, knockback and rider dismount through the vanilla stab pipeline.
            if (player.stabAttack(hand.asEquipmentSlot(), target, damage, true, true, true)) {
                anyHit = true;
            }
        }
        if (anyHit) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));
        }
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 1;
    }
}
