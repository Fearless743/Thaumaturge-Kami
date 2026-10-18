package mod.emt.kami.items.tools;

import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Spear of the Spellbreaker. Beyond the regular thrust it can be switched
 * with sneak + right-click to:
 * <ul>
 *   <li>{@code SPELLBREAK}: every strike adds extra flat damage (not requiring a charged swing).</li>
 *   <li>{@code LUNGE}: right-click performs an instant forward dash that damages every enemy in the
 *       spear's path, followed by a short cooldown.</li>
 * </ul>
 */
public class ItemAwakenedSpear extends ItemIchoriumSpear {
    public static final int LUNGE_COOLDOWN_TICKS = 40;

    public ItemAwakenedSpear(Item.Properties properties) {
        super(spearProperties(properties, ModMaterials.AWAKENED_ICHORIUM));
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
                EnumLanceMode mode = EnumLanceMode.getMode(heldStack).nextMode();
                EnumLanceMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.lance_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        if (EnumLanceMode.getMode(heldStack) == EnumLanceMode.LUNGE
                && !player.getCooldowns().isOnCooldown(heldStack)) {
            // The dash is applied on BOTH sides: the controlling client is authoritative for its
            // own velocity, so a server-only setDeltaMovement is overwritten every tick and the
            // lunge would be invisible (this mirrors how the vanilla riptide dash runs on both
            // sides). Damage/sound/stats stay server-side.
            Vec3 look = player.getViewVector(1.0F);
            double distance = ConfigHandlerKami.tools.lungeDistance;
            player.setDeltaMovement(look.normalize().scale(Math.max(2.0D, distance / 4.0D)).add(0.0D, 0.2D, 0.0D));
            if (level instanceof ServerLevel serverLevel) {
                performLungeDamage(serverLevel, player, heldStack, look, distance);
            }
            player.getCooldowns().addCooldown(heldStack, LUNGE_COOLDOWN_TICKS);
            return InteractionResult.SUCCESS;
        }
        // Otherwise fall back to the vanilla kinetic spear charging behaviour.
        return super.use(level, player, hand);
    }

    private static void performLungeDamage(ServerLevel level, Player player, ItemStack stack, Vec3 look, double distance) {
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        //Additive by design: every target takes a full normal attack's worth of damage and the
        //configured lunge percentage is added on top, mirroring the Titanslayer/Avalanche bonus
        //model instead of the strike damage being replaced by the percentage.
        float damage = baseDamage + baseDamage * (float) ConfigHandlerKami.tools.lungeDamageMultiplier;
        AABB path = player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e != player && !e.isSpectator() && !e.isAlliedTo(player))) {
            target.hurtServer(level, player.damageSources().playerAttack(player), damage);
        }
        level.playSound(null, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_THROW.get(),
                SoundSource.PLAYERS, 2.0F, 1.4F);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumLanceMode mode = EnumLanceMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.lance_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    public enum EnumLanceMode {
        THRUST(ChatFormatting.GRAY),
        SPELLBREAK(ChatFormatting.DARK_PURPLE),
        LUNGE(ChatFormatting.AQUA);

        private final ChatFormatting textColor;

        EnumLanceMode(ChatFormatting textColor) {
            this.textColor = textColor;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public EnumLanceMode nextMode() {
            EnumLanceMode[] values = EnumLanceMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumLanceMode getMode(ItemStack stack) {
            EnumLanceMode[] values = EnumLanceMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.LANCE_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumLanceMode mode) {
            stack.set(ModDataComponentsKAMI.LANCE_MODE.get(), mode.ordinal());
        }
    }

    /**
     * Applies the Spellbreaker's extra flat damage. Declared as a nested subscriber so it does not
     * need to touch the mod-wide {@code CommonEventHandler}.
     */
    @EventBusSubscriber(modid = Kami.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            Entity attackerEntity = event.getSource().getEntity();
            if (!(attackerEntity instanceof LivingEntity attacker)) {
                return;
            }
            ItemStack heldStack = attacker.getMainHandItem();
            if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemAwakenedSpear)) {
                return;
            }
            if (EnumLanceMode.getMode(heldStack) == EnumLanceMode.SPELLBREAK) {
                event.setAmount(event.getAmount() + (float) ConfigHandlerKami.tools.spellbreakerDamage);
            }
        }
    }
}
