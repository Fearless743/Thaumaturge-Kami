package mod.emt.kami.items.tools;

import com.google.common.collect.ImmutableList;
import mod.emt.kami.api.item.IAreaBreakTool;
import mod.emt.kami.entities.EntityThrownAxe;
import mod.emt.kami.registry.ModDataComponentsKAMI;
import mod.emt.kami.registry.ModSoundsKAMI;
import mod.emt.kami.utils.helpers.HarvestHelper;
import mod.emt.kami.utils.helpers.PlayerHelper;
import mod.emt.kami.utils.helpers.TreeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The Awakened Ichorium Axe. It can be charged and thrown like a boomerang ({@link EntityThrownAxe})
 * and, while sneaking is not held, fells trees and sweeps a small 3x3 area of logs.
 *
 * <p>The legacy {@code onBlockStartBreak} hook no longer exists; the area sweep is triggered from
 * {@link Item#mineBlock} instead, guarded by the raytraced block position so blocks broken by the
 * sweep itself cannot recursively trigger another sweep.</p>
 */
public class ItemAwakenedAxe extends ItemIchoriumAxe implements IAreaBreakTool {
    public static final int THROW_TICKS = 72000;

    public static BiConsumer<EntityThrownAxe, LivingEntity> DAMAGE_CONSUMER = (entityAxe, hitEntity) -> {
        if (!entityAxe.level().isClientSide() && hitEntity != null) {
            hitEntity.hurtServer((ServerLevel) entityAxe.level(),
                    entityAxe.damageSources().thrown(entityAxe, entityAxe.getOwner()), entityAxe.getDamage());
        }
    };
    public static BiConsumer<EntityThrownAxe, LivingEntity> LIGHTNING_CONSUMER = (entityAxe, hitEntity) -> {
        if (!(entityAxe.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = hitEntity != null ? hitEntity.getX() : entityAxe.getX();
        double y = hitEntity != null ? hitEntity.getY() : entityAxe.getY();
        double z = hitEntity != null ? hitEntity.getZ() : entityAxe.getZ();
        if (hitEntity != null) {
            hitEntity.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), entityAxe.getDamage() - 5.0F);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(x, y, z);
            serverLevel.addFreshEntity(bolt);
        }
    };
    public static BiConsumer<EntityThrownAxe, LivingEntity> DETONATION_CONSUMER = (entityAxe, hitEntity) -> {
        if (!entityAxe.level().isClientSide()) {
            entityAxe.level().explode(entityAxe.getOwner(), entityAxe.getX(), entityAxe.getY(), entityAxe.getZ(),
                    entityAxe.getSpeed() * 2.0F, false, Level.ExplosionInteraction.BLOCK);
        }
    };
    public static BiConsumer<EntityThrownAxe, LivingEntity> EXPLOSION_CONSUMER = (entityAxe, hitEntity) -> {
        if (!entityAxe.level().isClientSide()) {
            entityAxe.level().explode(entityAxe.getOwner(), entityAxe.getX(), entityAxe.getY(), entityAxe.getZ(),
                    entityAxe.getSpeed() * 5.0F, true, Level.ExplosionInteraction.TNT);
        }
    };

    public ItemAwakenedAxe(Item.Properties properties) {
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
                EnumImpactMode mode = EnumImpactMode.getMode(heldStack).nextMode();
                EnumImpactMode.setMode(heldStack, mode);
                player.sendOverlayMessage(
                        Component.translatable("tooltip.kami.tool.impact_mode." + mode)
                                .withStyle(style -> style.withColor(mode.getTextColor())));
            }
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            // Entering the use state must be explicit on 26.1: returning CONSUME alone never
            // starts it, so the release hook (the boomerang throw) would never run.
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return THROW_TICKS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player) || level.isClientSide()) {
            return false;
        }
        int duration = this.getUseDuration(stack, player) - remainingTime;
        float velocity = getThrowVelocity(duration);
        if (velocity < 0.1F) {
            return false;
        }

        EntityThrownAxe entityAxe = new EntityThrownAxe(level, player, stack.copy());
        entityAxe.setDamage((float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        entityAxe.thrownFromSlot = player.getInventory().getSelectedSlot();
        entityAxe.setSpeedModifier(velocity);
        level.addFreshEntity(entityAxe);
        level.playSound(null, player.blockPosition(), ModSoundsKAMI.ITEM_ICHOR_THROW.get(), SoundSource.PLAYERS,
                3.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        stack.setCount(0);
        return true;
    }

    /** Mirrors the old {@code ItemBow#getArrowVelocity} charge curve. */
    private static float getThrowVelocity(int duration) {
        float f = duration / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        EnumImpactMode mode = EnumImpactMode.getMode(stack);
        consumer.accept(Component.translatable("tooltip.kami.tool.impact_mode." + mode)
                .withStyle(style -> style.withColor(mode.getTextColor())));
        super.appendHoverText(stack, context, display, consumer, flag);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && owner instanceof Player player && !player.isShiftKeyDown()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
                if (TreeHelper.isTreeStructure(level, pos)) {
                    TreeHelper.fellTree(stack, pos, player);
                } else {
                    HarvestHelper.harvestExtraBlocks(player, stack,
                            this.getBreakAreaPositions(player, stack, pos, false));
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public ImmutableList<BlockPos> getBreakAreaPositions(Player player, ItemStack stack, BlockPos origin, boolean includeOrigin) {
        if (!player.isShiftKeyDown() && !player.level().getBlockState(origin).isAir()) {
            HitResult trace = PlayerHelper.rayTrace(player, 0);
            if (trace instanceof BlockHitResult blockHit
                    && !TreeHelper.isTreeStructure(player.level(), blockHit.getBlockPos())) {
                return HarvestHelper.getHarvestArea(player.level(), player, blockHit, 3, 1, includeOrigin, true);
            }
        }
        return ImmutableList.of();
    }

    @Override
    public boolean spawnDropsAtPlayer(Player player, ItemStack stack) {
        return true;
    }

    public enum EnumImpactMode {
        DAMAGE(ChatFormatting.GRAY, DAMAGE_CONSUMER),
        LIGHTNING(ChatFormatting.BLUE, LIGHTNING_CONSUMER),
        DETONATION(ChatFormatting.DARK_GREEN, DETONATION_CONSUMER),
        EXPLOSION(ChatFormatting.DARK_RED, EXPLOSION_CONSUMER);

        private final ChatFormatting textColor;
        private final BiConsumer<EntityThrownAxe, LivingEntity> onHitConsumer;

        EnumImpactMode(ChatFormatting textColor, BiConsumer<EntityThrownAxe, LivingEntity> onHitConsumer) {
            this.textColor = textColor;
            this.onHitConsumer = onHitConsumer;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getTextColor() {
            return this.textColor;
        }

        public void onImpact(EntityThrownAxe entityAxe, LivingEntity entityHit) {
            this.onHitConsumer.accept(entityAxe, entityHit);
        }

        public EnumImpactMode nextMode() {
            EnumImpactMode[] values = EnumImpactMode.values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static EnumImpactMode getMode(ItemStack stack) {
            EnumImpactMode[] values = EnumImpactMode.values();
            int ordinal = stack.getOrDefault(ModDataComponentsKAMI.IMPACT_MODE.get(), 0);
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }

        public static void setMode(ItemStack stack, EnumImpactMode mode) {
            stack.set(ModDataComponentsKAMI.IMPACT_MODE.get(), mode.ordinal());
        }
    }
}
