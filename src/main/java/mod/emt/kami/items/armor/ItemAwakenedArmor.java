package mod.emt.kami.items.armor;

import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.handlers.CommonEventHandler;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.registry.ModItemsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.function.Consumer;

/**
 * The Awakened Ichorweave robe set. Each piece grants a passive ability while worn (night vision
 * and water breathing from the hood, creative flight from the robe, fire resistance from the
 * leggings, and movement/jump bonuses from the boots), and wearing the full set further softens
 * incoming damage.
 *
 * <p>The 1.12 implementation hooked ThaumicAPI's movement-ability handler, Thaumcraft's cloud-ring
 * double jump and a custom network packet; none of those exist on 26.1, so the abilities are
 * applied through the modern per-item {@link #inventoryTick} hook and transient attribute
 * modifiers.</p>
 */
public class ItemAwakenedArmor extends ItemIchorweaveArmor {
    public static final int POTION_DURATION_MAX = 310;
    public static final int POTION_DURATION_MIN = 301;

    /**
     * Fraction of incoming damage ignored while the full set is worn. The 1.12 original reported an
     * absorption ratio of 0.2 per piece; Forge summed that across the four worn pieces, so the set
     * reduced damage by 80% in total (as the tooltip and research text state).
     */
    public static final float SET_DAMAGE_REDUCTION = 0.8F;

    private static final Identifier BOOTS_SPEED_MODIFIER =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "awakened_boots_speed");
    private static final Identifier BOOTS_JUMP_MODIFIER =
            Identifier.fromNamespaceAndPath(Kami.MOD_ID, "awakened_boots_jump");

    public ItemAwakenedArmor(EquipmentSlot slot, Item.Properties properties) {
        super(slot, ModMaterials.AWAKENED_ICHORWEAVE, properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return 10;
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return 3;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        //Aqua Affinity is disabled on the Awakened Hood, matching the original.
        if (stack.is(ModItemsKAMI.AWAKENED_ICHORWEAVE_HOOD.get()) && enchantment.is(Enchantments.AQUA_AFFINITY)) {
            return false;
        }
        return super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.translatable("tooltip.kami.awakened").withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, display, consumer, flag);
        //The "pieces equipped" line needs the viewing player, which is only available on the client.
        //It is appended by mod.emt.kami.client.ClientItemTooltips.
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || slot != getEquipmentSlot() || !player.isAlive()) {
            return;
        }
        switch (slot) {
            case HEAD -> tickHelmet(player);
            case CHEST -> tickChestplate(player);
            case LEGS -> tickLeggings(player);
            case FEET -> tickBoots(player);
            default -> {
            }
        }
    }

    protected void tickHelmet(Player player) {
        //Water breathing plus night vision in the dark.
        boolean activateNightVision = false;
        if (player.isUnderWater()) {
            player.setAirSupply(300);
            activateNightVision = true;
        } else {
            activateNightVision = shouldNightVisionActivate(player);
        }

        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (activateNightVision) {
            if (effect == null || effect.getDuration() <= POTION_DURATION_MIN) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 250, 0, false, false));
            }
        } else if (effect != null && effect.getDuration() <= POTION_DURATION_MAX) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    protected boolean shouldNightVisionActivate(LivingEntity entity) {
        Level level = entity.level();
        BlockPos eyePos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        if (level.getMaxLocalRawBrightness(eyePos) < 7) {
            return true;
        }

        HitResult trace = PlayerHelper.rayTrace(entity, 24.0D, 0.0F, true);
        if (trace instanceof BlockHitResult blockHit) {
            return level.getMaxLocalRawBrightness(blockHit.getBlockPos().relative(blockHit.getDirection())) < 7;
        } else if (trace != null && trace.getType() != HitResult.Type.MISS) {
            return level.getMaxLocalRawBrightness(BlockPos.containing(trace.getLocation())) < 7;
        }
        return false;
    }

    protected void tickChestplate(Player player) {
        //Creative flight and no fall damage.
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            CommonEventHandler.FLYING_PLAYERS.add(PlayerHelper.getUUIDFromPlayer(player));
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
    }

    protected void tickLeggings(Player player) {
        MobEffectInstance effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (effect == null || effect.getDuration() <= POTION_DURATION_MIN) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, POTION_DURATION_MAX, 0, false, false));
        }
    }

    protected void tickBoots(Player player) {
        //Jump height and movement speed, applied as transient attribute modifiers while the boots
        //are worn and not flying.
        boolean apply = !player.getAbilities().flying && !player.isFallFlying();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);

        if (apply) {
            if (speed != null && !speed.hasModifier(BOOTS_SPEED_MODIFIER)) {
                speed.addTransientModifier(new AttributeModifier(BOOTS_SPEED_MODIFIER,
                        ConfigHandlerKami.armor.bootsMovementBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (jump != null && !jump.hasModifier(BOOTS_JUMP_MODIFIER)) {
                jump.addTransientModifier(new AttributeModifier(BOOTS_JUMP_MODIFIER,
                        ConfigHandlerKami.armor.bootsJumpBoost, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            if (speed != null) {
                speed.removeModifier(BOOTS_SPEED_MODIFIER);
            }
            if (jump != null) {
                jump.removeModifier(BOOTS_JUMP_MODIFIER);
            }
        }
    }

    public boolean hasArmorSet(LivingEntity player) {
        return getEquippedPieces(player) == 4;
    }

    public static int getEquippedPieces(LivingEntity player) {
        int count = 0;
        if (hasHelm(player)) count++;
        if (hasChest(player)) count++;
        if (hasLeggings(player)) count++;
        if (hasBoots(player)) count++;
        return count;
    }

    public static boolean hasHelm(LivingEntity player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItemsKAMI.AWAKENED_ICHORWEAVE_HOOD.get());
    }

    public static boolean hasChest(LivingEntity player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItemsKAMI.AWAKENED_ICHORWEAVE_ROBE.get());
    }

    public static boolean hasLeggings(LivingEntity player) {
        return player.getItemBySlot(EquipmentSlot.LEGS).is(ModItemsKAMI.AWAKENED_ICHORWEAVE_LEGGINGS.get());
    }

    public static boolean hasBoots(LivingEntity player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItemsKAMI.AWAKENED_ICHORWEAVE_BOOTS.get());
    }

    /**
     * Replaces the legacy {@code ISpecialArmor} set bonus: while the full set is worn, a flat
     * percentage of incoming damage is absorbed.
     */
    @EventBusSubscriber(modid = Kami.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onIncomingDamage(LivingIncomingDamageEvent event) {
            LivingEntity entity = event.getEntity();
            if (hasHelm(entity) && hasChest(entity) && hasLeggings(entity) && hasBoots(entity)) {
                event.setAmount(event.getAmount() * (1.0F - SET_DAMAGE_REDUCTION));
            }
        }
    }
}
