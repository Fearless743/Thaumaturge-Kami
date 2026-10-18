package mod.emt.kami.items.tools;

import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Mallet of the Avalanche. Beyond the regular smash ({@code SHOCK}) it can
 * be switched with sneak + right-click to:
 * <ul>
 *   <li>{@code QUAKE}: a smash attack detonates a 3.5-block shockwave around the impact, dealing
 *       AoE damage and knockback to nearby enemies.</li>
 *   <li>{@code AVALANCHE}: a smash attack against a target at full health deals a percentage of the
 *       target's maximum health as extra damage.</li>
 * </ul>
 */
public class ItemAwakenedMallet extends ItemIchoriumMallet {

    public ItemAwakenedMallet(Item.Properties properties) {
        super(ItemIchoriumTrident.weaponProperties(
                properties.rarity(Rarity.EPIC).fireResistant(),
                ModMaterials.AWAKENED_ICHORIUM, 6.0F, ATTACK_SPEED_BASELINE, 1.5F));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        EnumQuakeMode mode = EnumQuakeMode.getMode(stack);
        if (mode == EnumQuakeMode.QUAKE && MaceItem.canSmashAttack(attacker)) {
            quakeShockwave(attacker, target);
        }
    }

    private static void quakeShockwave(LivingEntity attacker, LivingEntity target) {
        Level level = attacker.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.MACE_SMASH_GROUND_HEAVY, attacker.getSoundSource(), 1.5F, 0.8F);
        serverLevel.levelEvent(2013, target.getOnPos(), 750);
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                target.getX(), target.getY() + 0.5D, target.getZ(), 25, 1.5D, 0.5D, 1.5D, 0.05D);

        float baseDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        //Additive by design: each shockwave target takes a full normal attack's worth of damage
        //with the quake percentage added on top, instead of the percentage replacing the hit.
        float shockDamage = baseDamage + baseDamage * 0.5F;
        DamageSource shockSource = attacker instanceof Player player
                ? attacker.damageSources().playerAttack(player)
                : attacker.damageSources().mobAttack(attacker);
        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(3.5D),
                e -> e != attacker && e != target && !e.isSpectator()
                        && !attacker.isAlliedTo(e)
                        && !(e instanceof TamableAnimal animal && animal.isTame() && animal.isOwnedBy(attacker))
                        && !(e instanceof ArmorStand armorStand && armorStand.isMarker())
                        && !(e instanceof Player other && other.isCreative() && other.getAbilities().flying))) {
            Vec3 direction = nearby.position().subtract(target.position()).add(0.0D, 0.2D, 0.0D);
            if (direction.lengthSqr() > 1.0D) {
                direction = direction.normalize();
            }
            nearby.hurtServer(serverLevel, shockSource, shockDamage);
            nearby.push(direction.x, 0.5F, direction.z);
            if (nearby instanceof ServerPlayer otherPlayer) {
                otherPlayer.connection.send(new ClientboundSetEntityMotionPacket(otherPlayer));
            }
        }
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
                EnumQuakeMode mode = EnumQuakeMode.getMode(heldStack).nextMode();
                EnumQuakeMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.quake_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumQuakeMode mode = EnumQuakeMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.quake_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    public enum EnumQuakeMode {
        SHOCK(ChatFormatting.GRAY),
        QUAKE(ChatFormatting.GOLD),
        AVALANCHE(ChatFormatting.DARK_RED);

        private final ChatFormatting textColor;

        EnumQuakeMode(ChatFormatting textColor) {
            this.textColor = textColor;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public EnumQuakeMode nextMode() {
            EnumQuakeMode[] values = EnumQuakeMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumQuakeMode getMode(ItemStack stack) {
            EnumQuakeMode[] values = EnumQuakeMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.QUAKE_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumQuakeMode mode) {
            stack.set(ModDataComponentsKAMI.QUAKE_MODE.get(), mode.ordinal());
        }
    }

    /**
     * Applies the Avalanche's execute-style bonus against unharmed targets.
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
            if (heldStack.isEmpty() || !(heldStack.getItem() instanceof ItemAwakenedMallet)) {
                return;
            }
            if (EnumQuakeMode.getMode(heldStack) != EnumQuakeMode.AVALANCHE) {
                return;
            }
            if (!MaceItem.canSmashAttack(attacker)) {
                return;
            }
            LivingEntity target = event.getEntity();
            if (target.getHealth() < target.getMaxHealth() - 1.0E-4F) {
                return;
            }
            float avalancheDamage = target.getMaxHealth() * (float) ConfigHandlerKami.tools.avalancheDamage;
            //Additive by design ("extra damage"): the strike keeps its full smash/enchantment/potion
            //bonuses and the avalanche value is added on top. A Math.max here would discard all of
            //them whenever the percentage out-scales the hit.
            event.setAmount(event.getAmount() + avalancheDamage);
        }
    }
}
