package mod.emt.kami.items.tools;

import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import mod.emt.kami.utils.helpers.PlayerHelper;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Shield of the Warden. In addition to regular blocking ({@code GUARD}) it
 * can be switched with sneak + right-click to:
 * <ul>
 *   <li>{@code REFLECT}: after a successful block, a fraction of the blocked damage is bounced back
 *       at the attacker as thorns damage.</li>
 *   <li>{@code BARRIER}: blocked damage is amplified (blocks more than vanilla) and a successful
 *       block grants the blocker short-lived resistance.</li>
 * </ul>
 */
public class ItemAwakenedShield extends ItemIchoriumShield {

    public ItemAwakenedShield(Item.Properties properties) {
        //disableCooldownScale = 0: the Warden can never be disabled — an axe hit on a blocking
        //Warden neither starts the disable cooldown nor plays the shield-break cue.
        super(properties, 0.0F);
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
                EnumWardMode mode = EnumWardMode.getMode(heldStack).nextMode();
                EnumWardMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.ward_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        // Vanilla Item.use starts the blocking state for items carrying BLOCKS_ATTACKS.
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumWardMode mode = EnumWardMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.ward_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    public enum EnumWardMode {
        GUARD(ChatFormatting.GRAY),
        REFLECT(ChatFormatting.BLUE),
        BARRIER(ChatFormatting.GREEN);

        private final ChatFormatting textColor;

        EnumWardMode(ChatFormatting textColor) {
            this.textColor = textColor;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public EnumWardMode nextMode() {
            EnumWardMode[] values = EnumWardMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumWardMode getMode(ItemStack stack) {
            EnumWardMode[] values = EnumWardMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.WARD_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumWardMode mode) {
            stack.set(ModDataComponentsKAMI.WARD_MODE.get(), mode.ordinal());
        }
    }

    /**
     * Applies the Warden's mode-dependent blocking effects. Declared as a nested subscriber so it
     * does not need to touch the mod-wide {@code CommonEventHandler}.
     */
    @EventBusSubscriber(modid = Kami.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onShieldBlock(LivingShieldBlockEvent event) {
            LivingEntity blocker = event.getEntity();
            ItemStack shieldStack = blocker.getUseItem();
            if (shieldStack.isEmpty() || !(shieldStack.getItem() instanceof ItemAwakenedShield)) {
                return;
            }

            EnumWardMode mode = EnumWardMode.getMode(shieldStack);
            if (!event.getBlocked() || event.getBlockedDamage() <= 0.0F) {
                return;
            }

            switch (mode) {
                case REFLECT -> {
                    Entity attacker = event.getDamageSource().getEntity();
                    if (attacker instanceof LivingEntity livingAttacker && blocker != livingAttacker) {
                        float reflected = event.getBlockedDamage() * (float) ConfigHandlerKami.tools.wardReflectance;
                        if (reflected > 0.0F) {
                            livingAttacker.hurtOrSimulate(blocker.damageSources().thorns(blocker), reflected);
                        }
                    }
                }
                case BARRIER -> {
                    event.setBlockedDamage(event.getBlockedDamage() * (float) ConfigHandlerKami.tools.wardBarrierBoost);
                    blocker.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0), blocker);
                }
                default -> {
                }
            }
        }
    }
}
