package mod.emt.kami.handlers;

import mod.emt.kami.Kami;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.items.armor.ItemAwakenedArmor;
import mod.emt.kami.items.armor.ItemIchorweaveArmor;
import mod.emt.kami.items.tools.ItemIchoriumAxe;
import mod.emt.kami.items.tools.ItemIchoriumPickaxe;
import mod.emt.kami.items.tools.ItemIchoriumShovel;
import mod.emt.kami.items.tools.ItemIchoriumSword;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Kami.MOD_ID)
public class CommonEventHandler {
    public static final Set<UUID> FLYING_PLAYERS = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockHarvestPost(BlockDropsEvent event) {
        if (event.getBreaker() instanceof Player player && !player.level().isClientSide()) {
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.getItem() instanceof IAreaBreakTool tool && tool.spawnDropsAtPlayer(player, heldStack)) {
                //Repositioning drops below player.
                double posX = player.getX();
                double posY = player.getY() + 0.5D;
                double posZ = player.getZ();
                for (ItemEntity drop : event.getDrops()) {
                    drop.setPos(posX, posY, posZ);
                    drop.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helm.isEmpty() && helm.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_HOOD.get())) {
            float newSpeed = event.getOriginalSpeed();
            if (player.isUnderWater()) {
                //Underwater break speed is 20% of normal speed
                newSpeed = event.getOriginalSpeed() / 0.2F;
            }
            if (!player.onGround()) {
                //Airborne break speed is 20% of normal speed
                newSpeed = event.getOriginalSpeed() / 0.2F;
            }

            if (newSpeed > event.getNewSpeed()) {
                event.setNewSpeed(newSpeed);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        double distance = event.getDistance();
        LivingEntity entity = event.getEntity();
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (entity instanceof Player) {
            if (!legs.isEmpty() && legs.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get())) {
                // TODO(port): Thaumaturge exposed Thaumcraft's ItemCloudRing.jumpList; there is no
                // equivalent on 26.1, so the cloud-ring gating of this reduction was dropped.
                distance = (distance / 3.0D) - 2.0D;
            }
        }
        if (!boots.isEmpty() && boots.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_BOOTS.get())) {
            distance -= 2.0D;
        }
        event.setDistance(Math.max(0.0D, distance));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        //Disable flying for players without chest
        Player player = event.getEntity();
        UUID playerId = PlayerHelper.getUUIDFromPlayer(player);
        if (!player.isCreative() && player.getAbilities().mayfly && FLYING_PLAYERS.contains(playerId)) {
            ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chestStack.isEmpty()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                FLYING_PLAYERS.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        LivingEntity entity = event.getEntity();
        if (event.getSlot() == EquipmentSlot.HEAD) {
            if (from.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_HOOD.get()) && !to.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_HOOD.get())) {
                MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
                if (effect != null && effect.getDuration() <= ItemAwakenedArmor.POTION_DURATION_MAX) {
                    entity.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
        } else if (event.getSlot() == EquipmentSlot.LEGS) {
            if (from.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get()) && !to.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get())) {
                MobEffectInstance effect = entity.getEffect(MobEffects.FIRE_RESISTANCE);
                if (effect != null && effect.getDuration() <= ItemAwakenedArmor.POTION_DURATION_MAX) {
                    entity.removeEffect(MobEffects.FIRE_RESISTANCE);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack leggings = entity.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.isEmpty() && leggings.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get())) {
            if (event.getSource().is(DamageTypes.HOT_FLOOR)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPotionApplied(MobEffectEvent.Applicable event) {
        if (event.getResult() != MobEffectEvent.Applicable.Result.DO_NOT_APPLY) {
            LivingEntity entity = event.getEntity();
            ItemStack leggings = entity.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
            Holder<MobEffect> effect = event.getEffectInstance().getEffect();
            //Leggings remove Poison and Wither.
            if (!leggings.isEmpty() && leggings.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get())) {
                if (effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER)) {
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                }
            }
            //Boots remove Slowness
            if (!boots.isEmpty() && boots.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_BOOTS.get())) {
                if (effect.is(MobEffects.SLOWNESS)) {
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = PlayerHelper.getUUIDFromPlayer(event.getEntity());
        FLYING_PLAYERS.remove(playerId);
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        // Unbreaking still applies to items on anvils regardless of whether the items accept it in enchantment tables or not
        // This event should fix that
        if (event.getLeft().isEmpty() || event.getRight().isEmpty()) {
            return;
        }

        Item left = event.getLeft().getItem();
        if (left instanceof ItemIchoriumAxe || left instanceof ItemIchoriumPickaxe || left instanceof ItemIchoriumShovel
                || left instanceof ItemIchoriumSword || left instanceof ItemIchorweaveArmor) {
            boolean hasUnbreaking = EnchantmentHelper.getEnchantmentsForCrafting(event.getRight()).keySet().stream()
                    .anyMatch(holder -> holder.is(Enchantments.UNBREAKING));
            if (hasUnbreaking) {
                event.setCanceled(true);
            }
        }
    }
}
