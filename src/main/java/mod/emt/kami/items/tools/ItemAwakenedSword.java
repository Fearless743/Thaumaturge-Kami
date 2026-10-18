package mod.emt.kami.items.tools;

import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The Awakened Ichorium Sword. In {@link EnumSlayerMode#NORMAL} it behaves like a plain
 * high-damage sword; the other modes deal a percentage of the target's maximum health to
 * matching targets (an "execute" style strike) instead of flat damage.
 *
 * <p>Dynamic per-stack attribute modifiers no longer exist, so the flat NORMAL-mode damage and
 * the titan-slayer percentage strike are applied through a {@link LivingIncomingDamageEvent}
 * subscriber ({@link EventHandler}) instead of {@code getAttributeModifiers}/{@code hurtEnemy}.</p>
 */
public class ItemAwakenedSword extends ItemIchoriumSword {
    /**
     * The inherited sword attributes contribute {@code 3.0 + 5.0 = 8.0} damage; the original
     * NORMAL mode replaced that with a flat {@code 15.0}, i.e. an extra {@code +7.0}.
     */
    public static final float NORMAL_MODE_DAMAGE_BONUS = 7.0F;

    public static final Predicate<LivingEntity> UNDEAD_PREDICATE = LivingEntity::isInvertedHealAndHarm;
    public static final Predicate<LivingEntity> DRAGON_PREDICATE = entity ->
            entity instanceof EnderDragon || containsNeedle(entity, "dragon") || containsNeedle(entity, "draconic");
    public static final Predicate<LivingEntity> ABOMINATION_PREDICATE = entity ->
            containsNeedle(entity, "eldritch") || containsNeedle(entity, "taint") || containsNeedle(entity, "abomination");
    public static final Predicate<LivingEntity> LIVING_PREDICATE = entity ->
            !UNDEAD_PREDICATE.test(entity) && !DRAGON_PREDICATE.test(entity) && !ABOMINATION_PREDICATE.test(entity);

    private static boolean containsNeedle(Entity entity, String needle) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && key.getPath().contains(needle);
    }

    public ItemAwakenedSword(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        HitResult trace = PlayerHelper.rayTrace(player, 0);
        if (player.isShiftKeyDown() && (trace == null || trace.getType() == HitResult.Type.MISS)) {
            if (level.isClientSide()) {
                level.playSound(player, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_TOGGLE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.5F);
            } else {
                EnumSlayerMode mode = EnumSlayerMode.getMode(heldStack).nextMode();
                EnumSlayerMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.slayer_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumSlayerMode mode = EnumSlayerMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.slayer_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    public enum EnumSlayerMode {
        NORMAL(ChatFormatting.GRAY, entity -> false),
        LIVING(ChatFormatting.DARK_RED, LIVING_PREDICATE),
        UNDEAD(ChatFormatting.DARK_GREEN, UNDEAD_PREDICATE),
        DRAGON(ChatFormatting.BLUE, DRAGON_PREDICATE),
        ABOMINATION(ChatFormatting.DARK_PURPLE, ABOMINATION_PREDICATE);

        private final ChatFormatting textColor;
        private final Predicate<LivingEntity> entityPredicate;

        EnumSlayerMode(ChatFormatting textColor, Predicate<LivingEntity> entityPredicate) {
            this.textColor = textColor;
            this.entityPredicate = entityPredicate;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public boolean isEntityValid(LivingEntity entity) {
            return this.entityPredicate.test(entity);
        }

        public EnumSlayerMode nextMode() {
            EnumSlayerMode[] values = EnumSlayerMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumSlayerMode getMode(ItemStack stack) {
            EnumSlayerMode[] values = EnumSlayerMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.SLAYER_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumSlayerMode mode) {
            stack.set(ModDataComponentsKAMI.SLAYER_MODE.get(), mode.ordinal());
        }
    }

    /**
     * Applies the sword's mode-dependent damage. Declared as a nested subscriber so it does not
     * need to touch the mod-wide {@code CommonEventHandler}.
     */
    @EventBusSubscriber(modid = mod.emt.kami.Kami.MOD_ID)
    public static final class EventHandler {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            Entity attackerEntity = event.getSource().getEntity();
            if (!(attackerEntity instanceof LivingEntity attacker)) {
                return;
            }
            ItemStack heldStack = attacker.getMainHandItem();
            if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemAwakenedSword)) {
                return;
            }

            LivingEntity target = event.getEntity();
            EnumSlayerMode mode = EnumSlayerMode.getMode(heldStack);

            if (mode == EnumSlayerMode.NORMAL) {
                // Original NORMAL mode set the weapon's base attack damage to a flat 15.0.
                event.setAmount(event.getAmount() + NORMAL_MODE_DAMAGE_BONUS);
                return;
            }

            if (!mode.isEntityValid(target)) {
                return;
            }

            // Only a fully charged swing lands the titan-slayer strike.
            boolean recharged = !(attacker instanceof Player player) || player.getAttackStrengthScale(0.5F) >= 1.0F;
            if (!recharged) {
                return;
            }

            float percentDamage = target.getMaxHealth() * (float) ConfigHandlerKami.tools.titanSlayerDamage;
            //Additive by design: the strike keeps its full damage (attack attribute, enchantments,
            //effects) and the slayer percentage is added on top. A Math.max-style replacement here
            //would discard all of those bonuses whenever the percentage out-scales the hit.
            event.setAmount(event.getAmount() + percentDamage);
        }
    }
}
