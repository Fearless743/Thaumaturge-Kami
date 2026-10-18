package mod.emt.kami.items.tools;

import mod.emt.kami.Kami;
import mod.emt.kami.config.ConfigHandlerKami;
import mod.emt.kami.entities.EntityThrownTrident;
import mod.emt.kami.items.ModMaterials;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.PlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Trident of the Stormcaller. Beyond the regular throw-and-return
 * ({@code THROW}) it can be switched with sneak + right-click to:
 * <ul>
 *   <li>{@code THUNDER}: the impact strike summons a lightning bolt at the impact point.</li>
 *   <li>{@code TIDE}: the trident stays in hand — charging it with right-click and releasing
 *       dashes the wielder forward with a riptide surge (vanilla spin-attack dash), crashing
 *       through enemies. Works anywhere, no water, rain or Riptide enchantment required; an
 *       applied Riptide enchantment adds extra thrust on top.</li>
 * </ul>
 */
public class ItemAwakenedTrident extends ItemIchoriumTrident {

    /** Built-in dash strength of the TIDE surge: equivalent to a vanilla Riptide III. */
    public static final float TIDE_DASH_STRENGTH = 3.0F;

    /** Fraction of the throw's damage added on top of a THUNDER impact strike as lightning bonus. */
    public static final float THUNDER_DAMAGE_BONUS = 0.5F;

    public static BiConsumer<EntityThrownTrident, LivingEntity> DAMAGE_CONSUMER = (trident, hitEntity) -> {
        if (!trident.level().isClientSide() && hitEntity != null) {
            hitEntity.hurtServer((ServerLevel) trident.level(),
                    trident.damageSources().thrown(trident, trident.getOwner()), trident.getDamage());
        }
    };
    public static BiConsumer<EntityThrownTrident, LivingEntity> THUNDER_CONSUMER = (trident, hitEntity) -> {
        if (!(trident.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = hitEntity != null ? hitEntity.getX() : trident.getX();
        double y = hitEntity != null ? hitEntity.getY() : trident.getY();
        double z = hitEntity != null ? hitEntity.getZ() : trident.getZ();
        if (hitEntity != null) {
            //Additive by design: the strike lands as a full thrown hit (identical to DAMAGE mode)
            //and the thunder percentage is added on top instead of replacing the hit. The bonus is
            //folded into the same strike because a separate lower-damage hit in the same tick would
            //be swallowed by the invulnerability window; the summoned bolt carries the lightning.
            float baseDamage = trident.getDamage();
            hitEntity.hurtServer(serverLevel, trident.damageSources().thrown(trident, trident.getOwner()),
                    baseDamage + baseDamage * THUNDER_DAMAGE_BONUS);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(x, y, z);
            serverLevel.addFreshEntity(bolt);
        }
    };

    public ItemAwakenedTrident(Item.Properties properties) {
        super(weaponProperties(properties.rarity(Rarity.EPIC).fireResistant(),
                ModMaterials.AWAKENED_ICHORIUM, 5.5F, -2.9F, 0.0F));
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
                EnumStormMode mode = EnumStormMode.getMode(heldStack).nextMode();
                EnumStormMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.storm_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        // TIDE charges its riptide dash anywhere, so unlike the vanilla TridentItem#use there is
        // no isInWaterOrRain() gate here.
        if (heldStack.nextDamageWillBreak()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /**
     * In TIDE mode the trident is never thrown: releasing the charge launches the wielder with a
     * vanilla-style riptide dash (spin attack on everything passed through), regardless of water
     * or rain. A Riptide enchantment on the stack adds its strength on top of the built-in surge.
     * Every other mode falls back to the regular boomerang throw — Riptide is deliberately inert
     * there (its only effect on this weapon is boosting the TIDE surge).
     */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int timeHeld = this.getUseDuration(stack, entity) - remainingTime;
        if (timeHeld < TridentItem.THROW_THRESHOLD_TIME) {
            return false;
        }
        if (EnumStormMode.getMode(stack) != EnumStormMode.TIDE || player.isPassenger()) {
            return super.releaseUsing(stack, level, entity, remainingTime);
        }
        // Dash body copied from vanilla TridentItem#releaseUsing; strength is a built-in surge
        // (vanilla Riptide III) plus any actual Riptide enchantment on the stack.
        float strength = TIDE_DASH_STRENGTH + EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float xd = -Mth.sin(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        float yd = -Mth.sin(xRot * (float) (Math.PI / 180.0));
        float zd = Mth.cos(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
        player.push(xd * strength / dist, yd * strength / dist, zd * strength / dist);
        player.startAutoSpinAttack(20, 8.0F, stack);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.5D, player.getZ(),
                    30, 0.4D, 0.3D, 0.4D, 0.2D);
        }
        level.playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumStormMode mode = EnumStormMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.storm_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    public enum EnumStormMode {
        THROW(ChatFormatting.GRAY, DAMAGE_CONSUMER),
        THUNDER(ChatFormatting.YELLOW, THUNDER_CONSUMER),
        // TIDE never throws the trident (it dashes the wielder instead), so its impact consumer
        // only exists to satisfy the enum; the plain damage consumer is used as a safe fallback.
        TIDE(ChatFormatting.AQUA, DAMAGE_CONSUMER);

        private final ChatFormatting textColor;
        private final BiConsumer<EntityThrownTrident, LivingEntity> onImpactConsumer;

        EnumStormMode(ChatFormatting textColor, BiConsumer<EntityThrownTrident, LivingEntity> onImpactConsumer) {
            this.textColor = textColor;
            this.onImpactConsumer = onImpactConsumer;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public void onImpact(EntityThrownTrident trident, LivingEntity hitEntity) {
            this.onImpactConsumer.accept(trident, hitEntity);
        }

        public EnumStormMode nextMode() {
            EnumStormMode[] values = EnumStormMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumStormMode getMode(ItemStack stack) {
            EnumStormMode[] values = EnumStormMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.STORM_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumStormMode mode) {
            stack.set(ModDataComponentsKAMI.STORM_MODE.get(), mode.ordinal());
        }
    }
}
